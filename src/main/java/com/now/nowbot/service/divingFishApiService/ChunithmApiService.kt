package com.now.nowbot.service.divingFishApiService;

import com.now.nowbot.model.json.ChuBestScore;

public interface ChunithmApiService {

    ChuBestScore getChunithmBest30Recent10(Long qq);

    ChuBestScore getChunithmBest30Recent10(String probername);
}
