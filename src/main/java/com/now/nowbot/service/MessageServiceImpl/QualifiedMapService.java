package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.NowbotApplication;
import com.now.nowbot.model.JsonData.Search;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.QualifiedMapException;
import com.now.nowbot.util.Pattern4ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.regex.Matcher;

@Service("QUALIFIEDMAP")
public class QualifiedMapService implements MessageService<Matcher> {
    @Resource
    OsuBeatmapApiService beatmapApiService;
    @Resource
    ImageService         imageService;

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Pattern4ServiceImpl.QUALIFIEDMAP.matcher(event.getRawMessage().trim());
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
            Search data = null;
            int resultCount = 0;
            do {
                if (data == null) {
                    data = beatmapApiService.searchBeatmap(query);
                    resultCount += data.getBeatmapsets().size();
                    continue;
                }
                page ++;
                query.put("page", page);
                var result = beatmapApiService.searchBeatmap(query);
                resultCount += result.getBeatmapsets().size();
                data.getBeatmapsets().addAll(result.getBeatmapsets());
            } while (resultCount < data.getTotal() && page < page_aim);

            if (resultCount == 0) throw new QualifiedMapException(QualifiedMapException.Type.Q_Result_NotFound);

            data.setResultCount(Math.min(data.getTotal(), range));
            data.setRule(status);
            data.sortBeatmapDiff();
            var img = imageService.getPanelA2(data);
            event.getSubject().sendImage(img);
        } catch (Exception e) {
            NowbotApplication.log.error("QuaMap: ", e);
            throw new QualifiedMapException(QualifiedMapException.Type.Q_Send_Error);
        }
    }

    private static String getStatus(String status) {
        return switch (status.toLowerCase()) {
            case "0", "p" -> "pending";
            case "1", "r" -> "ranked";
            case "2", "a" -> "approved";
            case "4", "l" -> "loved";
            case "-1", "5", "w" -> "wip";
            case "-2", "6", "g" -> "graveyard";
            default -> "qualified";
        };
    }

    private static String getSort(String sort) {
        return switch (sort.toLowerCase()) {
            case "t", "t+", "ta", "title", "title asc", "title_asc" -> "title_asc";
            case "t-", "td", "title desc", "title_desc" -> "title_desc";
            case "a", "a+", "aa", "artist", "artist asc", "artist_asc" -> "artist_asc";
            case "a-", "ad", "artist desc", "artist_desc" -> "artist_desc";
            case "d", "d+", "da", "difficulty", "difficulty asc", "difficulty_asc", "s", "s+", "sa", "star", "star asc", "star_asc" ->
                    "difficulty_asc";
            case "d-", "dd", "difficulty desc", "difficulty_desc", "s-", "sd", "star desc", "star_desc" ->
                    "difficulty_desc";
            case "m", "m+", "ma", "map", "rating", "rating asc", "rating_asc" -> "rating_asc";
            case "m-", "md", "map desc", "rating desc", "rating_desc" -> "rating_desc";
            case "p", "p+", "pa", "plays", "pc asc", "plays asc", "plays_asc" -> "plays_asc";
            case "p-", "pd", "pc desc", "plays desc", "plays_desc" -> "plays_desc";
            case "r", "r+", "ra", "ranked", "time asc", "ranked asc", "ranked_asc" -> "ranked_asc";
            case "r-", "rd", "time desc", "ranked desc", "ranked_desc" -> "ranked_desc";
            default -> "relevance_desc";
        };
    }
}
