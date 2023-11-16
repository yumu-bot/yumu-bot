package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.FriendException;
import com.now.nowbot.util.QQMsgUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("FRIEND")
public class FriendService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(FriendService.class);
    BindDao bindDao;
    OsuGetService osuGetService;
    OsuUserApiService userApiService;

    ImageService imageService;

    @Autowired
    public FriendService(OsuGetService osuGetService, OsuUserApiService osuUserApiService, BindDao bindDao, ImageService imageService) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.imageService = imageService;
        userApiService = osuUserApiService;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(friend(s)?|f(?!\\S))\\s*(?<n>\\d+)?(\\s*[:-]\\s*(?<m>\\d+))?");

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
        BinUser binUser;
        OsuUser osuUser;

        List<MicroUser> friends = new ArrayList<>();

        //拿到参数,默认1-24个
        int n1 = 0, n2;
        boolean doRandom = true;
        String nStr = matcher.group("n");
        String mStr = matcher.group("m");
        if (Objects.nonNull(mStr) && Objects.nonNull(nStr)) {
            doRandom = false;
            n1 = Integer.parseInt(matcher.group("n"));
            n2 = Integer.parseInt(matcher.group("m"));
            if (n1 > n2) {
                n1 ^= n2;
                n2 ^= n1;
                n1 ^= n2;
            }
            n1--;
        } else {
            if (Objects.isNull(mStr)) {
                n2 = Objects.isNull(nStr) ? 12 : Integer.parseInt(nStr);
            } else {
                n2 = Integer.parseInt(mStr);
            }
        }
        n2--;
        if (n2 == 0 || 100 < n2 - n1) {
            throw new FriendException(FriendException.Type.FRIEND_Client_ParameterOutOfBounds);
        }

        try {
            binUser = bindDao.getUserFromQQ(event.getSender().getId());
        } catch (Exception e) {
            throw new FriendException(FriendException.Type.FRIEND_Me_NoPermission);
        }

        if (binUser == null) {
            throw new FriendException(FriendException.Type.FRIEND_Me_NoBind);
        } else if (!binUser.isAuthorized()) {
            throw new FriendException(FriendException.Type.FRIEND_Me_NoPermission);
            //无权限
        }

        try {
            osuUser = osuGetService.getPlayerInfo(binUser);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new FriendException(FriendException.Type.FRIEND_Me_TokenExpired);
        } catch (Exception e) {
            throw new FriendException(FriendException.Type.FRIEND_Me_NotFound);
        }

        List<MicroUser> friendList;
        try {
            friendList = userApiService.getFriendList(binUser);
        } catch (Exception e) {
            throw new FriendException(FriendException.Type.FRIEND_Me_FetchFailed);
        }

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
                    log.error("Friend: 莫名其妙的数组越界", e);
                    throw new FriendException(FriendException.Type.FRIEND_Send_Error);
                }
            }
        }

        if (CollectionUtils.isEmpty(friends)) throw new FriendException(FriendException.Type.FRIEND_Client_NoFriend);

        try {
            var data = imageService.getPanelA1(osuUser, friends);
            QQMsgUtil.sendImage(from, data);
        } catch (Exception e) {
            log.error("Friend: ", e);
            throw new FriendException(FriendException.Type.FRIEND_Send_Error);
        }
    }


    static final Random random = new Random();

    static int rand(int min, int max) {
        return min + random.nextInt(max - min);
    }

}