package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.FriendException;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("Friend")
public class FriendService implements MessageService<Matcher> {
    BindDao bindDao;
    OsuGetService osuGetService;

    ImageService imageService;

    @Autowired
    public FriendService(OsuGetService osuGetService, BindDao bindDao, ImageService imageService) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.imageService = imageService;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(friend(s)?|f(?![a-zA-Z_]))+\\s*(?<n>\\d+)?(\\s*[:-]\\s*(?<m>\\d+))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        BinUser binMe;
        OsuUser osuMe;

        List<MicroUser> friends = new ArrayList<>();

        //拿到参数,默认1-24个
        int n1 = 0, n2;
        boolean doRandom = true;
        if (matcher.group("m") == null) {
            n2 = matcher.group("n") == null ? 12 : Integer.parseInt(matcher.group("n"));
        } else {
            doRandom = false;
            n1 = Integer.parseInt(matcher.group("n"));
            n2 = Integer.parseInt(matcher.group("m"));
            if (n1 > n2) {
                n1 ^= n2;
                n2 ^= n1;
                n1 ^= n2;
            }
            n1--;
        }
        n2--;
        if (n2 == 0 || 100 < n2 - n1) {
            throw new FriendException(FriendException.Type.FRIEND_Client_ParameterOutOfBounds);
            //throw new TipsException("参数范围错误!");
        }

        try {
            binMe = bindDao.getUser(event.getSender().getId());
        } catch (Exception e) {
            throw new FriendException(FriendException.Type.FRIEND_Me_NoPermission);
        }

        if (!binMe.isAuthorized()) {
            throw new FriendException(FriendException.Type.FRIEND_Me_NoPermission);
            //无权限
        }

        try {
            osuMe = osuGetService.getPlayerInfo(binMe);
        } catch (Exception e) {
            throw new FriendException(FriendException.Type.FRIEND_Me_NotFound);
        }

        var friendList = osuGetService.getFriendList(binMe);

        int[] index = null;
        if (doRandom) {
            //构造随机数组
            index = new int[friendList.size()];
            for (int i = 0; i < index.length; i++) {
                index[i] = i;
            }
            for (int i = 0; i < index.length; i++) {
                int rand = rand(i, index.length);
                if (rand != 1) {
                    int temp = index[rand];
                    index[rand] = index[i];
                    index[i] = temp;
                }
            }
        }

        for (int i = n1; i <= n2 && i < friendList.size(); i++) {
            if (doRandom) {
                friends.add(friendList.get(index[i]));
            } else {
                try {
                    friends.add(friendList.get(i));
                } catch (IndexOutOfBoundsException e) {
                    NowbotApplication.log.error("Friend: 莫名其妙的数组越界", e);
                }
            }
        }

        if (CollectionUtils.isEmpty(friends)) throw new FriendException(FriendException.Type.FRIEND_Client_NoFriend);

        try {
            var data = imageService.getPanelA1(osuMe, friends);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            NowbotApplication.log.error("Friend: ", e);
            throw new FriendException(FriendException.Type.FRIEND_Send_Error);
        }
    }


    static final Random random = new Random();

    static int rand(int min, int max) {
        return min + random.nextInt(max - min);
    }

}