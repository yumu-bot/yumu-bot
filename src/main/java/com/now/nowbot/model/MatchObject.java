package com.now.nowbot.model;

import java.time.LocalDateTime;
import java.util.List;

public class MatchObject {
    int match_id;
    LocalDateTime strat_time;
    LocalDateTime end_time;
    String name;

    List<Event> events;
    List usets;

    class Event{
        Long id;
        detail type;
        String detail_text;
        Integer user_id;
    }
    enum detail{
        player_left,
        player_joined,
        host_changed,
        other,
        match_created,
        match_disbanded,
    }
}
