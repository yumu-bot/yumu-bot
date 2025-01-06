package com.now.nowbot.dao

import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.ScoreStatisticLite
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ScoreDao(
    val scoreRepository: LazerScoreRepository,
    val scoreStatisticRepository: LazerScoreStatisticRepository,
    val osuScoreApiService: OsuScoreApiService,
) {
    @Transactional
    fun saveScore(uid:Long, modes:List<OsuMode>) {
        for (m in modes) {
            val scoreList = osuScoreApiService.getRecentScore(uid, m, 0, 999)
            val (scoreIdList, beatmapIdList) = scoreList.map { it.scoreID to it.beatMapID }.unzip()
            val alreadySaveScoreId = scoreRepository.getRecordId(scoreIdList)
            val alreadySaveBeatmapId = scoreStatisticRepository.getRecordBeatmapId(beatmapIdList, m.modeValue.toInt())

            val mapStatisticList = scoreList
                .filterNot { alreadySaveBeatmapId.contains(it.beatMapID) }
                .map { ScoreStatisticLite.createByBeatmap(it) }

            val (scoreLiteList, scoreStatisticList) = scoreList
                .filterNot { alreadySaveScoreId.contains(it.scoreID) }
                .map { LazerScoreLite(it) to ScoreStatisticLite.createByScore(it) }
                .unzip()

            scoreStatisticRepository.saveAll(mapStatisticList)
            scoreStatisticRepository.saveAll(scoreStatisticList)
            scoreRepository.saveAll(scoreLiteList)
        }
    }
}