package com.now.nowbot.service.MessageService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.entity.DrawLogLite;
import com.now.nowbot.mapper.DrawLogLiteRepository;
import com.now.nowbot.model.DrawConfig;
import com.now.nowbot.model.enums.DrawKind;
import com.now.nowbot.util.JacksonUtil;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.regex.Matcher;

@Service("draw")
public class DrawService implements MessageService {
    @Resource
    private BindDao bindDao;
    @Resource
    private DrawLogLiteRepository drawLogLiteRepository;

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var osuUser = bindDao.getUser(event.getSender().getId());

        var kind = drfaultConfig.getRandomKind(osuUser.getOsuID(), drawLogLiteRepository);
        var card = drfaultConfig.getRandomCard(kind);
        //保存抽卡记录
        drawLogLiteRepository.save(new DrawLogLite(card, kind));
    }

    private static DrawConfig getConfig(String config) {
        var jsonData = JacksonUtil.parseObject(config, JsonNode.class);
        if (jsonData == null) return null;
        var drawConfig = new DrawConfig();
        for (var kind : DrawKind.values()) {
            if (jsonData.has(kind.name())) {
                var kindData = jsonData.get(kind.name());
                var conf = new DrawConfig.Config(
                        kindData.get("name").asText("no name"),
                        kindData.get("width").asInt(100));
                drawConfig.kindConfig.put(kind, conf);
                if (kindData.get("cards").isArray() && kindData.get("cards").size() > 0) {
                    var cards = new ArrayList<DrawConfig.Card>(kindData.get("cards").size());
                    for (var cardData : kindData.get("cards")) {
                        var card = new DrawConfig.Card(
                                cardData.get("name").asText("no name"),
                                cardData.get("width").asInt(100),
                                cardData.get("info").asText("default")
                        );
                        cards.add(card);
                    }
                    drawConfig.cardList.put(kind, cards);
                }
            }
        }
        return drawConfig;
    }

    static String config = """
            {
              "N": {
                "name": "普通",
                "width": 15,
                "cards": [
                  {
                    "name": "n1",
                    "width": 100,
                    "info": "n"
                  },
                  {
                    "name": "n2",
                    "width": 100,
                    "info": "n"
                  },
                  {
                    "name": "n3",
                    "width": 100,
                    "info": "n"
                  },
                  {
                    "name": "n4",
                    "width": 100,
                    "info": "n"
                  },
                  {
                    "name": "n5",
                    "width": 100,
                    "info": "n"
                  },
                  {
                    "name": "n6",
                    "width": 100,
                    "info": "n"
                  },
                  {
                    "name": "n7",
                    "width": 100,
                    "info": "n"
                  },
                  {
                    "name": "n8",
                    "width": 100,
                    "info": "n"
                  }
                ]
              },
              "R": {
                "name": "稀有",
                "width": 4,
                "cards":[
                  {
                    "name": "r1",
                    "width": 100,
                    "info": "r"
                  },
                  {
                    "name": "r2",
                    "width": 100,
                    "info": "r"
                  },
                  {
                    "name": "r3",
                    "width": 100,
                    "info": "r"
                  }
                ]
              },
              "SR": {
                "name": "传说",
                "width": 1,
                "cards":[
                  {
                    "name": "sr1",
                    "width": 100,
                    "info": "n"
                  }
                ]
              }
            }
            """;
    private static final DrawConfig drfaultConfig = getConfig(config);
}
