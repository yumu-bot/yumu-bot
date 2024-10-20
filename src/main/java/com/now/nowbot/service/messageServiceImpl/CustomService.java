package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.config.FileConfig;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.mapper.UserProfileMapper;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.ImageMessage;
import com.now.nowbot.qq.message.ReplyMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.serviceException.BindException;
import com.now.nowbot.throwable.serviceException.CustomException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

@Service("CUSTOM")
public class CustomService implements MessageService<CustomService.CustomParam> {
    private static final Logger log = LoggerFactory.getLogger(CustomService.class);
    private static Path FILE_DIV_PATH;
    @Resource
    RestTemplate restTemplate;
    @Resource
    BindDao bindDao;
    @Resource
    ImageService imageService;
    @Resource
    UserProfileMapper userProfileMapper;

    @Autowired
    public CustomService(FileConfig fileConfig) {
        FILE_DIV_PATH = Path.of(fileConfig.getBgdir(), "user-profile");
    }

    public record CustomParam(@NonNull long uid, @NonNull Type type, @Nullable String url) {
        //url == null 是删除图片
    }

    @Override
    @SuppressWarnings("all")
    public boolean isHandle(MessageEvent event, String messageText, DataValue<CustomParam> data) throws Throwable {
        var from = event.getSubject();

        var matcher = Instruction.CUSTOM.matcher(messageText);
        if (! matcher.find()) {
            return false;
        }

        BinUser u;
        try {
            u = bindDao.getUserFromQQ(event.getSender().getId(), true);
        } catch (BindException e) {
            throw new CustomException(CustomException.Type.CUSTOM_Me_TokenExpired);
        }

        var firstMessage = event.getMessage().getMessageList().getFirst();

        String imgPath = null;

        var operateStr = matcher.group("operate");
        var typeStr = matcher.group("type");

        Operate operate;
        Type type;

        if (StringUtils.hasText(typeStr)) {
            operate = switch (operateStr) {
                case "s", "save", "a", "add" -> Operate.ADD;
                case "c", "clear", "d", "delete", "r", "remove" -> Operate.DELETE;
                case null, default -> Operate.UNKNOWN;
            };

            type = switch (typeStr) {
                case "c", "card" -> Type.CARD;
                default -> Type.BANNER;
            };

        } else {
            // 只有一个字段，默认添加，直接跳出
            operate = Operate.ADD;

            type = switch (operateStr) {
                case "c", "card" -> Type.CARD;
                case null, default -> Type.BANNER;
            };
        }

        ReplyMessage reply = null;

        if (operate == Operate.ADD) {
            if (firstMessage instanceof ReplyMessage r) {
                reply = r;
            } else {
                operate = Operate.UNKNOWN;
            }
        }

        switch (operate) {
            case ADD -> {
                // 正常
                if (Objects.isNull(event.getBot())) {
                    throw new CustomException(CustomException.Type.CUSTOM_Receive_NoBot);
                }

                var msg = event.getBot().getMessage(reply.getId());
                ImageMessage img;

                if (Objects.isNull(msg) || Objects.isNull(img = QQMsgUtil.getType(msg, ImageMessage.class))) {
                    //消息为空，并且不是回复的图片。询问是否删除
                    var receipt = from.sendMessage(CustomException.Type.CUSTOM_Question_Clear.message);

                    var lock = ASyncMessageUtil.getLock(event, 30 * 1000);
                    event = lock.get();

                    if (Objects.isNull(event) || ! event.getRawMessage().toUpperCase().contains("OK")) {
                        //不删除。失败撤回
                        from.recall(receipt);
                        return false;
                    } else {
                        //确定删除
                        from.recall(receipt);
                    }
                } else {
                    //成功
                    imgPath = img.getPath();
                }
            }
            case UNKNOWN -> {
                // 不是回复。发送引导
                try {
                    var md = DataUtil.getMarkdownFile("Help/custom.md");
                    var image = imageService.getPanelA6(md, "help");
                    from.sendImage(image);
                    return false;
                } catch (Exception e) {
                    throw new CustomException(CustomException.Type.CUSTOM_Instructions);
                }
            }
        }

        data.setValue(new CustomParam(u.getOsuID(), type, imgPath));

        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, CustomParam param) throws Throwable {
        var fileName = param.uid + "-" + param.type + ".png";
        Path path = FILE_DIV_PATH.resolve(fileName);

        byte[] data = new byte[0];

        if (Objects.nonNull(param.url)) {
            data = restTemplate.getForObject(param.url, byte[].class);
            if (Objects.isNull(data)) {
                throw new CustomException(CustomException.Type.CUSTOM_Receive_PictureFetchFailed);
            }
        }

        var profile = userProfileMapper.getProfileById(param.uid());

        if (Objects.nonNull(param.url)) {
            // 保存
            try {
                Files.write(path, data);
            } catch (Exception e) {
                log.error("自定义：文件添加失败", e);
                throw new CustomException(CustomException.Type.CUSTOM_Set_Failed, param.type);
            }
            switch (param.type) {
                case CARD -> profile.setCard(path.toAbsolutePath().toString());
                case BANNER -> profile.setBanner(path.toAbsolutePath().toString());
            }
            userProfileMapper.saveAndFlush(profile);
            throw new CustomException(CustomException.Type.CUSTOM_Set_Success, param.type);
        } else {
            // 删除
            try {
                Files.delete(path);
            } catch (NoSuchFileException e) {
                throw new CustomException(CustomException.Type.CUSTOM_Clear_NoSuchFile, param.type);
            } catch (Exception e) {
                log.error("自定义：文件删除失败", e);
                throw new CustomException(CustomException.Type.CUSTOM_Clear_Failed, param.type);
            }
            switch (param.type) {
                case CARD -> profile.setCard(null);
                case BANNER -> profile.setBanner(null);
            }
            userProfileMapper.saveAndFlush(profile);
            throw new CustomException(CustomException.Type.CUSTOM_Clear_Success, param.type);
        }
    }

    enum Operate {
        ADD,
        DELETE,
        UNKNOWN
    }

    enum Type {
        BANNER,
        CARD,
        AVATAR_FRAME,
        PANEL_INFO,
        PANEL_SCORE,
        PANEL_PPM,
    }
}
