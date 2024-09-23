package com.now.nowbot.model.service;

import com.now.nowbot.model.enums.OsuMode;

public record UserParam(Long qq, String name, OsuMode mode, boolean at) {
}
