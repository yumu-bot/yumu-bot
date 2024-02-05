package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.FileConfig;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.mapper.UserProfileMapper;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.ImageMessage;
import com.now.nowbot.qq.message.ReplyMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.ServiceException.CustomException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Service("CUSTOM")
public class CustomService implements MessageService<CustomService.CustomParam> {
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

    public record CustomParam(long uid, Type type, String url) {
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<CustomParam> data) throws Throwable {
        var matcher2 = Instructions.DEPRECATED_SET.matcher(messageText);
        if (matcher2.find() && Strings.isNotBlank(matcher2.group("set"))) {
            throw new CustomException(CustomException.Type.CUSTOM_Instruction_Deprecated);
        }

        var matcher = Instructions.CUSTOM.matcher(messageText);
        if (! matcher.find()) {
            return false;
        }

        BinUser u;
        try {
            u = bindDao.getUserFromQQ(event.getSender().getId());
        } catch (BindException e) {
            throw new CustomException(CustomException.Type.CUSTOM_Me_Nobind);
        }

        var firstMessage = event.getMessage().getMessageList().getFirst();

        if (! (firstMessage instanceof ReplyMessage reply)) {
            try {
                var md = DataUtil.getMarkdownFile("Help/custom.md");
                var image = imageService.getPanelA6(md, "help");
                var from = event.getSubject();
                from.sendImage(image);
                return false;
            } catch (Exception e) {
                throw new CustomException(CustomException.Type.CUSTOM_Instructions);
            }
        } else {

            var msg = event.getBot().getMessage(reply.getId());

            ImageMessage img;
            if (Objects.isNull(msg) || Objects.isNull(img = QQMsgUtil.getType(msg, ImageMessage.class))) {
                throw new CustomException(CustomException.Type.CUSTOM_Receive_NoPicture);
            }

            switch (matcher.group("type")) {
                case "c", "card" -> data.setValue(new CustomParam(u.getOsuID(), Type.card, img.getPath()));
                case null, default -> data.setValue(new CustomParam(u.getOsuID(), Type.banner, img.getPath()));
            }

            return true;
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, CustomParam param) throws Throwable {
        var fileName = STR."\{param.uid}-\{param.type}.png";
        Path f = FILE_DIV_PATH.resolve(fileName);

        var data = restTemplate.getForObject(param.url, byte[].class);
        if (Objects.isNull(data)) {
            throw new CustomException(CustomException.Type.CUSTOM_Receive_PictureFetchFailed);
        }
        Files.write(f, data);
        var profile = userProfileMapper.getProfileById(param.uid());
        switch (param.type) {
            case card -> profile.setCard(f.toAbsolutePath().toString());
            case banner -> profile.setBanner(f.toAbsolutePath().toString());
        }
        userProfileMapper.saveAndFlush(profile);
        event.getSubject().sendMessage(CustomException.Type.CUSTOM_Receive_PictureFetchFailed.message);
    }

    enum Type {
        banner,
        card,
        header,
        info,
        score,
        ppm,
    }
}
