package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.JsonData.Search;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.QualifiedMapException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("Q")
public class QualifiedMapService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(QualifiedMapService.class);
    @Resource
    OsuGetService osuGetService;
    @Resource
    ImageService imageService;

    Pattern pattern = Pattern.compile("[!！]\\s*(?i)(ym)?(qualified|qua(?![a-zA-Z_])|q(?![a-zA-Z_]))+\\s*([:：](?<mode>\\w+))?\\s*(#+(?<status>[-\\w]+))?\\s*(\\*?(?<sort>[-_+a-zA-Z]+))?\\s*(?<range>\\d+)?");

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
        // 获取参数
        var mode_str = matcher.group("mode");
        var status = matcher.group("status");
        var sort = matcher.group("sort");
        var range_str = matcher.group("range");
        short range;
        short mode = OsuMode.DEFAULT.getModeValue();

        if (mode_str != null) mode = OsuMode.getMode(mode_str).getModeValue();
        if (status == null) status = "q";
        if (sort == null) sort = "ranked_asc";

        if (range_str == null) {
            range = 12; //从0开始
        } else {
            try {
                range = Short.parseShort(range_str);
            } catch (Exception e) {
                throw new QualifiedMapException(QualifiedMapException.Type.Q_Parameter_Error);
            }
        }

        if (range < 1 || range > 999) throw new QualifiedMapException(QualifiedMapException.Type.Q_Parameter_OutOfRange);

        int page = 1;
        int page_aim = (int) Math.max(Math.floor(range / 50f) + 1, 10);// 这里需要重复获取，page最多取10页（500个），总之我不知道怎么实现

        var query = new HashMap<String, Object>();
        status = getStatus(status);
        query.put("m", mode);
        query.put("s", status);
        query.put("sort", getSort(sort));
        query.put("page", page);

        try {
            Search d = null;
            int resultCount = 0;
            do {
                if (d == null) {
                    d = osuGetService.searchBeatmap(query);
                    resultCount += d.getBeatmapsets().size();
                    continue;
                }
                page ++;
                query.put("page", page);
                var result = osuGetService.searchBeatmap(query);
                resultCount += result.getBeatmapsets().size();
                d.getBeatmapsets().addAll(result.getBeatmapsets());
            } while (resultCount < d.getTotal() && page < page_aim);

            //var d = osuGetService.searchBeatmap(query);

            d.setResultCount(Math.min(d.getTotal(), range));
            d.setRule(status);
            d.sortBeatmapDiff();
            var img = imageService.getPanelA2(d);
            event.getSubject().sendImage(img);
        } catch (Exception e) {
            // 这里要打印日志,自己创建log
            log.error("QuaMap: ", e);
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
                return "graveyard";
            case "3":
            case "q":
            default:
                return "qualified";
        }
    }

    private static String getSort(String sort) {
        switch (sort.toLowerCase()) {
            case "t":
            case "t+":
            case "ta":
            case "title":
            case "title asc":
            case "title_asc":
                return "title_asc";
            case "t-":
            case "td":
            case "title desc":
            case "title_desc":
                return "title_desc";

            case "a":
            case "a+":
            case "aa":
            case "artist":
            case "artist asc":
            case "artist_asc":
                return "artist_asc";
            case "a-":
            case "ad":
            case "artist desc":
            case "artist_desc":
                return "artist_desc";

            case "d":
            case "d+":
            case "da":
            case "difficulty":
            case "difficulty asc":
            case "difficulty_asc":
            case "s":
            case "s+":
            case "sa":
            case "star":
            case "star asc":
            case "star_asc":
                return "difficulty_asc";
            case "d-":
            case "dd":
            case "difficulty desc":
            case "difficulty_desc":
            case "s-":
            case "sd":
            case "star desc":
            case "star_desc":
                return "difficulty_desc";

            case "m":
            case "m+":
            case "ma":
            case "map":
            case "rating":
            case "rating asc":
            case "rating_asc":
                return "rating_asc";
            case "m-":
            case "md":
            case "map desc":
            case "rating desc":
            case "rating_desc":
                return "rating_desc";

            case "p":
            case "p+":
            case "pa":
            case "plays":
            case "pc asc":
            case "plays asc":
            case "plays_asc":
                return "plays_asc";
            case "p-":
            case "pd":
            case "pc desc":
            case "plays desc":
            case "plays_desc":
                return "plays_desc";

            case "r":
            case "r+":
            case "ra":
            case "ranked":
            case "time asc":
            case "ranked asc":
            case "ranked_asc":
                return "ranked_asc";
            case "r-":
            case "rd":
            case "time desc":
            case "ranked desc":
            case "ranked_desc":
                return "ranked_desc";

            default: return "relevance_desc";
        }
    }
}
