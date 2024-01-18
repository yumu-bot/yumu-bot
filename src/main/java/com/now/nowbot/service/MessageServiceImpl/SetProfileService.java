package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.FileConfig;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.mapper.UserProfileMapper;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.ImageMessage;
import com.now.nowbot.qq.message.ReplayMessage;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Service("111")
public class SetProfileService implements MessageService<SetProfileService.Param> {
    private static Path FILE_DIV_PATH;
    @Resource
    OsuUserApiService userApiService;
    @Resource
    RestTemplate      restTemplate;
    @Resource
    BindDao           bindDao;
    @Resource
    UserProfileMapper userProfileMapper;

    @Autowired
    public SetProfileService(FileConfig fileConfig) {
        FILE_DIV_PATH = Path.of(fileConfig.getBgdir(), "user-profile");
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Param> data) throws Throwable {
        if (! messageText.contains("!ymset")) {
            return false;
        }

        BinUser u;
        try {
            u = bindDao.getUserFromQQ(event.getSender().getId());
        } catch (BindException e) {
            throw new TipsException("必须绑定才可以使用");
        }

        var repObj = event.getMessage().getMessageList().getFirst();
        if (! (repObj instanceof ReplayMessage replay)) {
            throw new TipsException("""
                    指令 `!ymset <type>` 回复图片消息以设置
                    下面是可用的 type 以及对应的推荐尺寸
                    banner      (1920*320)
                    card        (430*210)
                    """);
        }

        var msg = event.getBot().getMessage(replay.getId());
        ImageMessage img;
        if (Objects.isNull(msg) || Objects.isNull(img = QQMsgUtil.getType(msg, ImageMessage.class))) {
            throw new TipsException("没有图片或读取失败, 如果一直失败请重新发图片再尝试");
        }

        if (messageText.contains("banner")) {
            data.setValue(new Param(u.getOsuID(), Type.banner, img.getPath()));
        } else if (messageText.contains("card")) {
            data.setValue(new Param(u.getOsuID(), Type.card, img.getPath()));
        }
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, Param param) throws Throwable {
        var fileName = STR."\{param.uid}-\{param.type}.png";
        Path f = FILE_DIV_PATH.resolve(fileName);

        var data = restTemplate.getForObject(param.url, byte[].class);
        if (Objects.isNull(data)) throw new TipsException("下载图片失败, 请稍后尝试.");
        Files.write(f, data);
        var profile = userProfileMapper.getProfileById(param.uid());
        switch (param.type) {
            case card -> profile.setCard(f.toAbsolutePath().toString());
            case banner -> profile.setBanner(f.toAbsolutePath().toString());
        }
        userProfileMapper.saveAndFlush(profile);
        event.getSubject().sendMessage("设置成功!");
    }

    enum Type {
        banner,
        card,
    }

    record Param(long uid, Type type, String url) {
    }
}
