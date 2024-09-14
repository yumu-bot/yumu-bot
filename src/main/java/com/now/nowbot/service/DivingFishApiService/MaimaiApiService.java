package com.now.nowbot.service.DivingFishApiService;

import com.now.nowbot.model.JsonData.MaiBestPerformance;
import com.now.nowbot.model.JsonData.MaiSong;
import com.now.nowbot.model.enums.MaiVersion;

import java.util.List;

public interface MaimaiApiService {
    MaiBestPerformance getMaimaiBest50(Long qq);

    MaiBestPerformance getMaimaiBest50(String probername);

    MaiBestPerformance getMaimaiBest50(Long qq, MaiVersion version);

    MaiBestPerformance getMaimaiBest50(String probername, MaiVersion version);


    List<MaiSong> getMaiMaiSongLibrary(); // 开销大，这个方法应该是每周用来存数据库的

    MaiSong getMaiMaiSong(Integer songID); // 查数据库

    MaiSong getMaiMaiSong(String title); // 查数据库

}
