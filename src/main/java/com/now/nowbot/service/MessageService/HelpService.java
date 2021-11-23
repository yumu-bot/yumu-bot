package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.NowbotConfig;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.utils.ExternalResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

@Service("help")
public class HelpService implements MessageService {
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
//        StringBuilder sb = new StringBuilder();
//        for(var ins: Instruction.values()) {
//            if(ins.getDesc()!=null)
//                sb.append(ins.getDesc()).append("\n");
//        }
//        from.sendMessage(sb.toString());
        from.uploadImage(ExternalResource.create(Files.readAllBytes(Path.of(NowbotConfig.BG_PATH).resolve("ExportFileV3/panel-help.png"))));
    }
}
