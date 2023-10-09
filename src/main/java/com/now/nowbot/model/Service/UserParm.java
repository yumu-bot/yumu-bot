package com.now.nowbot.model.Service;

import com.now.nowbot.model.enums.OsuMode;

public record UserParm(Long qq, String name, OsuMode mode, boolean at) {
}
