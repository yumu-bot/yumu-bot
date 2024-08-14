package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.FriendException;
import com.now.nowbot.util.CmdRange;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.now.nowbot.util.command.CommandPatternStaticKt.FLAG_RANGE;

@Service("FRIEND")
public class FriendService implements MessageService<FriendService.FriendParam> {
    private static final Logger log = LoggerFactory.getLogger(FriendService.class);
    @Resource
    BindDao           bindDao;
    @Resource
    OsuUserApiService userApiService;
    @Resource
    ImageService      imageService;

    public record FriendParam(Integer offset, Integer limit) {
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<FriendParam> data) {
        var m = Instruction.FRIEND.matcher(messageText);
        if (!m.find()) {
            return false;
        }
        var range = m.group(FLAG_RANGE);
        if (Objects.isNull(range)) {
            data.setValue(new FriendParam(0, 12));
            return true;
        }
        var s = range.split("[\\-－—]");
        if (s.length >= 2 && StringUtils.hasText(s[0]) && StringUtils.hasText(s[1])) {
            data.setValue(
                    new FriendParam(
                            DataUtil.parseRange2Offset(Integer.parseInt(s[0]), Integer.parseInt(s[1])),

                            DataUtil.parseRange2Limit(Integer.parseInt(s[0]), Integer.parseInt(s[1]))
                    )
            );
        } else if (s.length == 1 && StringUtils.hasText(s[0])) {
            data.setValue(
                    new FriendParam(0, Integer.parseInt(s[0]))
            );
        }

        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, FriendParam param) throws Throwable {
        var from = event.getSubject();
        BinUser binUser;
        OsuUser osuUser;

        List<MicroUser> friends = new ArrayList<>();

        //拿到参数,默认1-24个
        int offset = param.offset(), limit = param.limit();
        boolean doRandom = (offset == 0 && limit == 12);

        if (limit == 0 || 100 < limit - offset) {
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
            osuUser = userApiService.getPlayerInfo(binUser);
        } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized e) {
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

        for (int i = offset; i < limit && i < friendList.size(); i++) {
            if (doRandom) {
                friends.add(friendList.get(index[i]));
            } else {
                try {
                    friends.add(friendList.get(offset + i));
                } catch (IndexOutOfBoundsException e) {
                    log.error("Friend: 莫名其妙的数组越界", e);
                    throw new FriendException(FriendException.Type.FRIEND_Send_Error);
                }
            }
        }

        if (CollectionUtils.isEmpty(friends)) throw new FriendException(FriendException.Type.FRIEND_Client_NoFriend);

        try {
            var image = imageService.getPanelA1(osuUser, friends);
            from.sendImage(image);
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