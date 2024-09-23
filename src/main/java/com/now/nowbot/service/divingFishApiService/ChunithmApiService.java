package com.now.nowbot.service.divingFishApiService;

import com.now.nowbot.model.json.ChuBestPerformance;

public interface ChunithmApiService {

    ChuBestPerformance getChunithmBest30Recent10(Long qq);

    ChuBestPerformance getChunithmBest30Recent10(String probername);
}
