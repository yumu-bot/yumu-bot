package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.Covers
import com.now.nowbot.model.osu.MicroUser

data class RoomRound(
    @field:JsonProperty("list_id")
    val playlistID: Long,
    @field:JsonProperty("beatmap_id")
    val beatmapID: Long,
    @field:JsonProperty("beatmapset_id")
    val beatmapsetID: Long,
    @field:JsonProperty("difficulty_rating")
    val starRating: Double?,
    @field:JsonProperty("covers")
    val covers: Covers?,
    @field:JsonProperty("scores")
    val scores: List<RoomRecordScore>,
    @field:JsonProperty("winner")
    val winnerID: Long,
)

data class RoomRecordScore(
    @field:JsonProperty("user")
    val user: MicroUser,

    @field:JsonProperty("score")
    val score: Long,

    @field:JsonProperty("combo")
    val combo: Int,

    @field:JsonProperty("accuracy")
    val accuracy: Double,

    @field:JsonProperty("health")
    val health: Long,

    @field:JsonProperty("win")
    val isWin: Boolean
)

data class RoomStatistics(
    private val room: Room
) {
    val roomID: Long = room.roomInfo.roomID

    // 用于对外暴露的最终计算结果列表
    val rounds: List<RoomRound>

    val duration = room.beatmaps.sumOf { it.totalLength }

    val players: List<Long> = room.users.map { it.userID }.sorted()

    val names: List<String> = players.map { i -> room.users.firstOrNull { u -> i == u.userID }?.username ?: "ID: $i" }

    val wins: List<Int>

    init {
        val calculatedRounds = mutableListOf<RoomRound>()

        var globalFactor = 0.5
        val calculateWinCounts = mutableMapOf<Long, Int>()
        val calculateHealthMap = mutableMapOf<Long, Long>()

        val initialHealthPool = 1_000_000L

        val sortedItems = room.items
            .filter { it.playedTime != null && !it.scores.isNullOrEmpty() }
            .sortedBy { it.playedTime!!.toInstant() }

        // 3. 开始逐局迭代计算
        for (item in sortedItems) {
            val currentScores = item.scores.orEmpty()
            if (currentScores.size < 2) continue

            // 找出本局最高分作为胜者
            val sortedScores = currentScores.sortedByDescending { it.score }
            val winnerScore = sortedScores.first()
            val winnerID = winnerScore.userID

            val previousWins = calculateWinCounts.getOrDefault(winnerID, 0)
            val winnerPersonalFactor = 0.5 + (previousWins * 0.5)

            val totalMultiplier = globalFactor + winnerPersonalFactor

            currentScores.forEach { score ->
                calculateHealthMap.putIfAbsent(score.userID, initialHealthPool)
            }

            // 计算伤害并扣除所有败者的血量
            val losers = sortedScores.drop(1)
            for (loserScore in losers) {
                val loserID = loserScore.userID

                val scoreDiff = winnerScore.score - loserScore.score

                if (scoreDiff == 0L) continue

                val calculatedDamage = (scoreDiff * totalMultiplier).toLong() + 50000L
                val currentHealth = calculateHealthMap.getOrDefault(loserID, initialHealthPool)

                val actualDamage = if (currentHealth - calculatedDamage <= 0 && currentHealth >= 1_000_000L) {
                    calculatedDamage.coerceAtMost(999_999L)
                } else {
                    calculatedDamage
                }

                calculateHealthMap[loserID] = (currentHealth - actualDamage).coerceAtLeast(0L)
            }

            val roundUsers = currentScores
                .sortedBy { score -> score.userID }
                .map { score ->
                RoomRecordScore(
                    user = room.users.firstOrNull { it.userID == score.userID } ?: MicroUser().apply {
                        this.userID = score.userID
                    },
                    score = score.score,
                    health = calculateHealthMap.getOrDefault(score.userID, initialHealthPool),
                    isWin = (score.userID == winnerID),
                    combo = score.maxCombo,
                    accuracy = score.accuracy,
                )
            }

            val b = room.beatmaps.firstOrNull { it.beatmapID == item.beatmapID }

            val s = room.beatmapsets.firstOrNull { it.beatmapsetID == b?.beatmapsetID }

            calculatedRounds.add(
                RoomRound(
                    playlistID = item.listID,
                    beatmapID = item.beatmapID,
                    beatmapsetID = s?.beatmapsetID ?: b?.beatmapsetID ?: 0L,
                    starRating = b?.starRating,
                    covers = s?.covers,
                    scores = roundUsers,
                    winnerID = winnerID
                )
            )

            globalFactor += 0.5
            calculateWinCounts[winnerID] = previousWins + 1
        }

        // 赋值给类的成员变量
        this.rounds = calculatedRounds
        this.wins = this.players.map { id -> calculatedRounds.count { it.winnerID == id } }
    }
}