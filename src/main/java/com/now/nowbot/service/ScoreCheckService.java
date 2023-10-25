package com.now.nowbot.service;

import com.now.nowbot.model.JsonData.OsuUser;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.service.score.ScoreChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScoreCheckService {
    private final List<ScoreChecker> scoreCheckers;
    @Autowired
    ScoreCheckService(ApplicationContext applicationContext) {
        var res = applicationContext.getBeansOfType(ScoreChecker.class).values();
        scoreCheckers = new ArrayList<>(res);
    }

    ScoreChecker getCalculator(OsuUser user, Score score) {
        var dlist = scoreCheckers.stream()
                .filter(s -> s.canHandle(user, score)).toList();


        if (dlist.size() == 0) {
            return null;
        } else if (dlist.size() > 1) {
            dlist = dlist.stream().sorted((r1, r2) -> {
                if (score.getBeatMap() == null) return 0;
                var bid = score.getBeatMap().getBID();
                return r1.getWeight(bid) - r2.getWeight(bid);
            }).toList();
        }
        return dlist.get(0);
    }
    ScoreChecker getCalculator(String name) {
        var res = scoreCheckers.stream().filter(s -> s.getName().equals(name)).findFirst();
        return res.orElse(null);
    }
}
