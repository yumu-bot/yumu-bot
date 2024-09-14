package com.now.nowbot.service.DivingFishApiService;

import com.now.nowbot.model.JsonData.ChuBestPerformance;

public interface ChuApiService {

    ChuBestPerformance getChunithmBest30Recent10(Long qq);

    ChuBestPerformance getChunithmBest30Recent10(String probername);
}
