package com.now.nowbot.service.MessageServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.entity.DrawLogLite;
import com.now.nowbot.mapper.DrawLogLiteRepository;
import com.now.nowbot.model.DrawConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.JacksonUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

@Service("DRAW")
public class DrawService implements MessageService<Matcher> {
    @Resource
    private BindDao bindDao;
    @Resource
    private DrawLogLiteRepository drawLogLiteRepository;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instruction.DRAW.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    @CheckPermission(test = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var binUser = bindDao.getUserFromQQ(event.getSender().getId());

        int times = 1;
        if (matcher.group("d") != null) {
            times = Integer.parseInt(matcher.group("d"));
        }

        int tenTimes = times / 10;
        times = times % 10;
        List<DrawConfig.Card> clist = new LinkedList<>();
        // 10 连
        for (int i = 0; i < tenTimes; i++) {
            var gradeList = defaultConfig.getGrade10(binUser.getOsuID(), drawLogLiteRepository);
            var cards = gradeList.stream().map(defaultConfig::getCard).toList();
            var cardLites = new ArrayList<DrawLogLite>(gradeList.size());
            for (int j = 0; j < gradeList.size(); j++) {
                cardLites.add(new DrawLogLite(cards.get(i), gradeList.get(i), binUser.getOsuID()));
            }
            drawLogLiteRepository.saveAll(cardLites);
            clist.addAll(cards);
        }
        // 单抽
        {
            for (int i = 0; i < times; i++) {
                var grade = defaultConfig.getGrade(binUser.getOsuID(), drawLogLiteRepository);
                var card = defaultConfig.getCard(grade);
                drawLogLiteRepository.save(new DrawLogLite(card, grade, binUser.getOsuID()));
                clist.add(card);
            }
        }

        StringBuilder sb = new StringBuilder();
        clist.forEach(c -> sb.append(c.name()).append(", "));
        event.getSubject().sendMessage(sb.toString());
    }

    private static DrawConfig getConfig(String config) {
        var jsonData = JacksonUtil.parseObject(config, JsonNode.class);
        if (jsonData == null) return null;

        return new DrawConfig(jsonData);
    }

    public static DrawConfig getDefaultConfig() {
        return defaultConfig;
    }

    static String config = """
            {
              "N": {
                "name": "普通",
                "weight": 15,
                "cards": [
                  {
                    "name": "n1",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n2",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n3",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n4",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n5",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n6",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n7",
                    "weight": 100,
                    "info": "n"
                  },
                  {
                    "name": "n8",
                    "weight": 100,
                    "info": "n"
                  }
                ]
              },
              "R": {
                "name": "稀有",
                "weight": 4,
                "cards":[
                  {
                    "name": "r1",
                    "weight": 100,
                    "info": "r"
                  },
                  {
                    "name": "r2",
                    "weight": 100,
                    "info": "r"
                  },
                  {
                    "name": "r3",
                    "weight": 100,
                    "info": "r"
                  }
                ]
              },
              "SR": {
                "name": "传说",
                "weight": 1,
                "cards":[
                  {
                    "name": "sr1",
                    "weight": 100,
                    "info": "n"
                  }
                ]
              },
              "SSR": {
                "name": "传说",
                "weight": 1,
                "cards":[
                  {
                    "name": "ssr1",
                    "weight": 100,
                    "info": "n"
                  }
                ]
              }
            }
            """;
    private static final DrawConfig defaultConfig = getConfig(config);
}
