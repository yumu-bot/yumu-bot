package com.now.nowbot.service.MessageService;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.ServiceException.QualifiedMapException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.regex.Matcher;

@Service("Q")
public class QualifiedMapService implements MessageService {
    @Resource
    OsuGetService osuGetService;
    @Resource
    ImageService imageService;
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        // 获取参数
        var status = matcher.group("status");
        var sort = matcher.group("sort");
        var range_str = matcher.group("range");
        int range;

        if (status == null) status = "q";
        if (sort == null) sort = "ranked_asc";

        if (range_str == null) {
            range = 12 - 1; //从0开始
        } else {
            try {
                range = Integer.parseInt(range_str) - 1;
            } catch (Exception e) {
                throw new QualifiedMapException(QualifiedMapException.Type.Q_Parameter_Error);
            }
        }

        if (range <= 0 || range > 200) throw new QualifiedMapException(QualifiedMapException.Type.Q_Parameter_OutOfRange);



        int page = (int) (Math.floor(range / 50f) + 1);// 这里需要重复获取，page最多取4页（200个），总之我不知道怎么实现

        var query = new HashMap<String, Object>();
        status = getStatus(status);
        query.put("s", status);
        query.put("sort", getSort(sort));
        query.put("page", page);

        try {
            var d = osuGetService.searchBeatmap(query);

            d.setRule(status); // rule是 status
            d.setResultCount(range + 1);
            d.sortBeatmapDiff();
            var img = imageService.getPanelA2(d);
            event.getSubject().sendImage(img);
        } catch (Exception e) {
            //throw new LogException("Q: ", e);
            throw new QualifiedMapException(QualifiedMapException.Type.Q_Send_Error);
        }
    }

    private static String getStatus(String status) {
        switch (status.toLowerCase()) {
            case "0":
            case "p":
                return  "pending";
            case "1":
            case "r":
                return "ranked";
            case "2":
            case "a":
                return "approved";
            case "3":
            case "q":
                return "qualified";
            case "4":
            case "l":
                return "loved";
            case "-1":
            case "5":
            case "w":
                return "wip";
            case "-2":
            case "6":
            case "g":
            default:
                return "graveyard";
        }
    }

    private static String getSort(String sort) {
        switch (sort.toLowerCase()) {
            case "t":
            case "t1":
            case "ta":
            case "title":
            case "title asc":
            case "title_asc":
                return "title_asc";
            case "t0":
            case "td":
            case "title desc":
            case "title_desc":
                return "title_desc";

            case "a":
            case "a1":
            case "aa":
            case "artist":
            case "artist asc":
            case "artist_asc":
                return "artist_asc";
            case "a0":
            case "ad":
            case "artist desc":
            case "artist_desc":
                return "artist_desc";

            case "d":
            case "d1":
            case "da":
            case "difficulty":
            case "difficulty asc":
            case "difficulty_asc":
            case "s":
            case "s1":
            case "sa":
            case "star":
            case "star asc":
            case "star_asc":
                return "difficulty_asc";
            case "d0":
            case "dd":
            case "difficulty desc":
            case "difficulty_desc":
            case "s0":
            case "sd":
            case "star desc":
            case "star_desc":
                return "difficulty_desc";

            case "m":
            case "m1":
            case "ma":
            case "map":
            case "rating":
            case "rating asc":
            case "rating_asc":
                return "rating_asc";
            case "m0":
            case "md":
            case "map desc":
            case "rating desc":
            case "rating_desc":
                return "rating_desc";

            case "p":
            case "p1":
            case "pa":
            case "plays":
            case "pc asc":
            case "plays asc":
            case "plays_asc":
                return "plays_asc";
            case "p0":
            case "pd":
            case "pc desc":
            case "plays desc":
            case "plays_desc":
                return "plays_desc";

            case "r":
            case "r1":
            case "ra":
            case "ranked":
            case "time asc":
            case "ranked asc":
            case "ranked_asc":
                return "ranked_asc";
            case "r0":
            case "rd":
            case "time desc":
            case "ranked desc":
            case "ranked_desc":
                return "ranked_desc";

            default: return "relevance_desc";
        }
    }
}
