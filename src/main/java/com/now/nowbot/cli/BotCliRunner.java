package com.now.nowbot.cli;

import com.now.nowbot.service.MessageService.MessageService;
import com.now.nowbot.util.Instruction;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.regex.Matcher;

import static org.mockito.Mockito.*;

@Component
public class BotCliRunner implements CommandLineRunner {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(String... args) throws Exception {
        // 开启一个新线程运行命令行读取，避免阻塞 Spring Boot 主线程
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("=================================================");
            System.out.println("  Nowbot 运行时内嵌 CLI 模式已启动。");
            System.out.println("  您可以直接输入原生的 Bot 指令进行本地测试。");
            System.out.println("  示例: !ymbpht:osu Muziyami");
            System.out.println("  输入 'exit' 退出 CLI 线程。");
            System.out.println("=================================================");

            while (true) {
                System.out.print("Nowbot-CLI> ");
                String input = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(input)) {
                    System.out.println("退出 CLI 线程。");
                    break;
                }

                if (input.isEmpty()) {
                    continue;
                }

                boolean matched = false;

                // 核心逻辑：直接复用你的 Instruction 枚举和正则网关
                for (var ins : Instruction.values()) {
                    Matcher matcher = ins.getRegex().matcher(input);

                    if (matcher.find() && applicationContext != null) {
                        matched = true;
                        System.out.println("⚡ 匹配到指令 [" + ins.name() + "] -> 正在调用服务: " + ins.getName());

                        try {
                            // 1. 从 Spring 容器动态获取对应的 Service
                            var service = (MessageService) applicationContext.getBean(ins.getName());

                            // 2. Mock 模拟一个标准的 MessageEvent
                            MessageEvent mockEvent = mockMessageEvent(input);

                            // 3. 直接分发执行
                            service.HandleMessage(mockEvent, matcher);

                            System.out.println("✅ 指令执行完毕。\n");
                        } catch (Throwable e) {
                            System.err.println("❌ 指令执行期间发生异常: " + e.getMessage());
                            e.printStackTrace();
                        }
                        break; // 匹配成功一条指令后，跳出当前循环，等待下一次输入
                    }
                }

                if (!matched) {
                    System.out.println("❌ 未匹配到任何预设的 Bot 指令，请检查输入或正则表达式。");
                }
            }
        }, "CLI-Thread").start();
    }


    /**
     * 为 CLI 模式快速 Mock 一个 MessageEvent
     * 避免业务代码或 AOP 切面在调用 event.getSender() 或 event.getSubject() 时报空指针
     */
    private MessageEvent mockMessageEvent(String rawText) {
        MessageEvent mockEvent = mock(MessageEvent.class);

        // 1. 模拟基础消息
        var messageChain = new MessageChainBuilder().append(new PlainText(rawText)).build();
        when(mockEvent.getMessage()).thenReturn(messageChain);

        // ==================== 🛠️ 新增：模拟发送人 (Sender) ====================
        // 从而完美解决 CheckAspect.checkPermission 中的 event.getSender().getId() 报错
        net.mamoe.mirai.contact.User mockSender = mock(net.mamoe.mirai.contact.User.class);
        // 这里你可以固定一个测试用的 QQ 号，比如 12345678L
        when(mockSender.getId()).thenReturn(12345678L);
        when(mockSender.getNick()).thenReturn("CLI_Admin");
        // 如果你的权限切面还要检查群名片或群权限，可以在这里继续补 when(mockSender.getPermission())...

        when(mockEvent.getSender()).thenReturn(mockSender);
        // ===================================================================

        // 2. 深度定制一个 Contact（就是业务代码里的 from）
        Contact mockSubject = mock(Contact.class);

        // 核心拦截 1：拦截 from.sendMessage(message)
        when(mockSubject.sendMessage(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);

            // 判断是否为消息链 (MessageChain)
            if (msg instanceof MessageChain) {
                MessageChain chain = (MessageChain) msg;

                // 在 Java 中，MessageChain 可以直接当作 Collection/Iterable 来遍历
                for (SingleMessage singleMsg : chain) {
                    if (singleMsg instanceof Image) {
                        Image image = (Image) singleMsg;
                        System.out.println("🖼️ [CLI 拦截] 监测到 Bot 发送了图片，准备保存...");
                        System.out.println(image.contentToString());
                    }
                }
            } else if (msg instanceof Image) {
                // 兜底：万一发送的不是链，而是一个孤零零的单体 Image 对象
                System.out.println("🖼️ [CLI 拦截] 监测到 Bot 发送了单张图片，准备保存...");
                System.out.println(msg.contentToString());
            }

            // 如果是普通的纯文本回复，直接打印在命令行
            if (!(msg instanceof MessageChain) || !msg.contentToString().contains("[图片]")) {
                System.out.println("\n💬 [Bot 命令行回复] -> " + msg.contentToString());
            }
            return null;
        });

        // 核心拦截 2：有些代码会先调用 from.uploadImage(ExternalResource)
        when(mockSubject.uploadImage(any())).thenAnswer(invocation -> {
            System.out.println("📤 [CLI 拦截] 监测到 Bot 正在调用 uploadImage 上传资源...");

            // 构造一个假图片返回，保证业务不崩溃
            Image mockImage = mock(Image.class);
            when(mockImage.getImageId()).thenReturn("{01E9451B-70ED-EAE3-B37C-101F1EEBF5B5}.png");

            try {
                var resource = (net.mamoe.mirai.utils.ExternalResource) invocation.getArgument(0);
                File outputFile = new File("D:/cli_output_upload.png");
                // 通过 Resource 的输入流直接保存
                try (InputStream in = resource.inputStream()) {
                    Files.copy(in, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("💾 [本地缓存] 成功将上传的资源直接保存至: " + outputFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("❌ 拦截上传流失败，转为常规模式: " + e.getMessage());
            }

            return mockImage;
        });

        when(mockEvent.getSubject()).thenReturn(mockSubject);
        return mockEvent;
    }
}