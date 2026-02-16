package com.now.nowbot.model.skill

import com.now.nowbot.model.beatmapParse.HitObject
import com.now.nowbot.model.beatmapParse.hitObject.HitObjectType.*
import com.now.nowbot.model.beatmapParse.parse.ManiaBeatmapAttributes
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

class SkillMania6(val file: ManiaBeatmapAttributes) : Skill() {

    // --- 1. 核心数据结构 ---

    data class ManiaAction(
        val startTime: Int,
        val objects: List<HitObject>
    ) {
        // val columnMask: Int = objects.fold(0) { mask, obj -> mask or (1 shl obj.column) }
        val count: Int = objects.size
    }

    data class Finger(
        val hand: Int,    // 0: 左手, 1: 右手
        val type: Int,    // 0-4 分别代表小指到大拇指
        val isThumb: Boolean = type == 4
    )

    object ManiaLayout {
        fun getFingerMapping(keyCount: Int): List<Finger> {
            return when (keyCount) {
                1 -> listOf(Finger(1, 4))
                2 -> listOf(Finger(0, 3), Finger(1, 3))
                3 -> listOf(Finger(0, 3), Finger(1, 4), Finger(1, 3))
                4 -> listOf(Finger(0, 2), Finger(0, 3), Finger(1, 3), Finger(1, 2))
                5 -> listOf(Finger(0, 2), Finger(0, 3), Finger(1, 4), Finger(1, 3), Finger(1, 2))
                6 -> listOf(Finger(0, 1), Finger(0, 2), Finger(0, 3), Finger(1, 3), Finger(1, 2), Finger(1, 1))
                7 -> listOf(Finger(0, 1), Finger(0, 2), Finger(0, 3), Finger(1, 4), Finger(1, 3), Finger(1, 2), Finger(1, 1))
                8 -> listOf(Finger(0, 0)) + getFingerMapping(7)
                9 -> getFingerMapping(4) + listOf(Finger(1, 4)) + getFingerMapping(4)
                10 -> listOf(Finger(0, 0), Finger(0, 1), Finger(0, 2), Finger(0, 3), Finger(0, 4),
                    Finger(1, 4), Finger(1, 3), Finger(1, 2), Finger(1, 1), Finger(1, 0))
                else -> List(keyCount) { Finger(it % 2, it / 2) }
            }
        }
    }

    /**
     * 手部状态追踪器：记录每个轨道的实时物理状态
     */
    class HandStateTracker(keyCount: Int) {
        // 记录每个轨道被占据到什么时候（用于处理 LN）
        val columnBusyUntil = IntArray(keyCount) { -1 }
        // 记录每个轨道上一次点击的时间（用于计算 Jack）
        val lastTapTime = IntArray(keyCount) { -1 }

        fun update(action: ManiaAction) {
            for (obj in action.objects) {
                lastTapTime[obj.column] = obj.startTime
                if (obj.type == LONGNOTE) {
                    columnBusyUntil[obj.column] = obj.endTime
                }
            }
        }

        // 获取当前正在按住（Holding）的轨道数量
        fun getHoldingCount(currentTime: Int): Int {
            return columnBusyUntil.count { it > currentTime }
        }

        // 检查特定轨道是否正在被占用
        fun isColumnOccupied(column: Int, currentTime: Int): Boolean {
            return columnBusyUntil[column] > currentTime
        }
    }

    object HandWeightConfig {
        /**
         * 同手手指干涉权重表
         * 索引说明：0:小, 1:无, 2:中, 3:食, 4:拇
         */
        private val weightTable = arrayOf(
            //          小(0)  无(1)  中(2)  食(3)  拇(4)
            floatArrayOf(0.0f, 1.8f, 1.6f, 1.2f, 1.4f), // 小指 (0)
            floatArrayOf(1.8f, 0.0f, 1.4f, 1.0f, 1.2f), // 无指 (1)
            floatArrayOf(1.6f, 1.4f, 0.0f, 1.0f, 1.2f), // 中指 (2)
            floatArrayOf(1.2f, 1.0f, 1.0f, 0.0f, 1.2f), // 食指 (3)
            floatArrayOf(1.4f, 1.2f, 1.2f, 1.2f, 0.0f)  // 拇指 (4)
        )

        fun getWeight(f1: Int, f2: Int): Float {
            return weightTable[f1][f2]
        }
    }

    /**
     * 难度矩阵：定义轨道间的物理联系
     */
    private fun generateMatrix(keyCount: Int): Array<FloatArray> {
        val fingers = ManiaLayout.getFingerMapping(keyCount)
        val matrix = Array(keyCount) { FloatArray(keyCount) }

        for (i in 0 until keyCount) {
            for (j in 0 until keyCount) {
                val f1 = fingers[i]
                val f2 = fingers[j]

                matrix[i][j] = if (f1.hand != f2.hand) {
                    0.8f // 跨手固定权重
                } else {
                    // 同手查表
                    HandWeightConfig.getWeight(f1.type, f2.type)
                }
            }
        }
        return matrix
    }

    // --- 2. 状态变量与初始化 ---

    private val actions: List<ManiaAction> = groupObjectsToActions(file.hitObjects)
    private val keyCount = file.cs.toInt()
    private val tracker = HandStateTracker(keyCount)
    private val interferenceMatrix = generateMatrix(keyCount)

    private val skillValues = FloatArray(7) { 0f }

    init {
        if (actions.isNotEmpty()) calculate()
    }

    // --- 3. 计算引擎 ---

    private fun calculate() {
        // 1. 初始化各维度的瞬时应力列表
        val rcStrains = mutableListOf<Float>()
        val lnStrains = mutableListOf<Float>()
        val coStrains = mutableListOf<Float>()
        val stStrains = mutableListOf<Float>()
        val spStrains = mutableListOf<Float>()
        val prStrains = mutableListOf<Float>()

        // 2. 遍历 Action 流计算瞬时应力
        for (i in actions.indices) {
            val curr = actions[i]
            val prev = actions.getOrNull(i - 1)
            val delta = if (prev != null) (curr.startTime - prev.startTime).coerceAtLeast(1) else 1000
            val speedFactor = 1000f / delta

            var currentRC = 0f
            var currentLN = 0f
            var currentSP = 0f
            var currentPR = 0f

            // --- A. Coordination (协调性) ---
            val holdingCount = tracker.getHoldingCount(curr.startTime)
            if (holdingCount > 0 && curr.count > 0) {
                coStrains.add((holdingCount * curr.count) * 1.5f)
            }

            // --- B. 模式与速度判定 (Rice, Speed, Shield, ReverseShield) ---
            if (prev != null) {
                for (currObj in curr.objects) {
                    for (prevObj in prev.objects) {
                        val isSameColumn = currObj.column == prevObj.column
                        val weight = interferenceMatrix[currObj.column][prevObj.column]

                        if (isSameColumn) {
                            val interval = (currObj.startTime - prevObj.startTime).coerceAtLeast(1)
                            val jackSpeed = 1000f / interval
                            when {
                                // Shield
                                prevObj.type == LONGNOTE -> {
                                    val releaseToPress = (currObj.startTime - prevObj.endTime).coerceAtLeast(1)
                                    currentLN += (1000f / releaseToPress) * 1.5f
                                }
                                // Reverse Shield
                                prevObj.type == CIRCLE && currObj.type == LONGNOTE -> {
                                    currentLN += jackSpeed * 1.3f
                                }
                                // Normal Jack
                                else -> currentRC += jackSpeed * 1.2f
                            }
                        } else {
                            // Stream / Trill
                            currentSP += speedFactor * weight
                            // Precision (Grace)
                            if (delta < 30) {
                                currentPR += speedFactor * weight * affectedByOD(file.od.toFloat())
                            }
                        }
                    }
                }
            }

            // --- C. Release & Tail (Release, Delayed Tail) ---
            for (col in 0 until keyCount) {
                if (tracker.columnBusyUntil[col] == curr.startTime) {
                    curr.objects.forEach { obj ->
                        val weight = interferenceMatrix[col][obj.column]
                        val releaseVal = speedFactor * weight * 0.8f
                        currentLN += releaseVal
                        currentPR += releaseVal * 0.5f * affectedByOD(file.od.toFloat())
                    }
                }
            }

            // --- D. Tail Precision ---
            curr.objects.filter { it.type == LONGNOTE }.forEach { ln ->
                val nextAction = actions.getOrNull(i + 1)
                if (nextAction != null) {
                    val tailDelta = abs(nextAction.startTime - ln.endTime).coerceAtLeast(1)
                    if (tailDelta < 80) {
                        currentPR += (1000f / tailDelta) * 0.5f
                    }
                }
            }

            // 存入瞬时值
            rcStrains.add(currentRC)
            lnStrains.add(currentLN)
            spStrains.add(currentSP)
            prStrains.add(currentPR)
            stStrains.add(curr.count.toFloat()) // Stamina/Density 基础值

            tracker.update(curr)
        }

        // 3. 聚合计算：使用降序衰减模型
        // 这种方式让 10% 的爆发段贡献 50% 以上的分数，同时长图会有更多的有效项累加
        skillValues[0] = aggregateStrains(rcStrains) * 1f   // RC
        skillValues[1] = aggregateStrains(lnStrains) * 1f   // LN
        skillValues[2] = aggregateStrains(coStrains) * 1f   // CO
        skillValues[3] = aggregateStrains(stStrains) * 1f   // ST
        skillValues[4] = aggregateStrains(spStrains) * 1f   // SP
        skillValues[5] = aggregateStrains(prStrains) * 1f   // PR
        skillValues[6] = 0f // SV 留空

        // 4. 应用长度补偿因子
        val lengthFactor = getLengthFactor(file.length)
        for (i in skillValues.indices) {
            skillValues[i] *= lengthFactor
        }
    }

    /**
     * 降序衰减聚合函数
     * Σ (Strain\[i] * 0.95^i)
     */
    private fun aggregateStrains(strains: MutableList<Float>): Float {
        if (strains.isEmpty()) return 0f
        val sorted = strains.filter { it > 0 }.sortedDescending()
        var total = 0f
        var weight = 1.0f
        val decay = 0.95f // 越接近 1.0，长图加成越高；越小，则越看重爆发

        for (s in sorted) {
            total += s * weight
            weight *= decay
            if (weight < 0.001f) break
        }
        return total
    }

    /**
     * 长度修正因子：log 曲线，奖励耐力，惩罚超短图
     */
    private fun getLengthFactor(millis: Int): Float {
        val seconds = millis / 1000f
        return if (seconds < 30) {
            0.6f + 0.4f * (seconds / 30f)
        } else {
            // 在 30s 时为 1.0，在 300s(5min) 时约为 1.15
            0.85f + 0.15f * log10(seconds / 3f)
        }.coerceIn(0.1f, 1.5f)
    }

    // --- 4. 辅助函数 ---

    private fun groupObjectsToActions(hitObjects: List<HitObject>): List<ManiaAction> {
        if (hitObjects.isEmpty()) return emptyList()
        val actions = mutableListOf<ManiaAction>()
        var currentGroup = mutableListOf<HitObject>()
        for (obj in hitObjects) {
            if (currentGroup.isEmpty() || obj.startTime - currentGroup[0].startTime <= 3) {
                currentGroup.add(obj)
            } else {
                actions.add(ManiaAction(currentGroup[0].startTime, currentGroup))
                currentGroup = mutableListOf(obj)
            }
        }
        actions.add(ManiaAction(currentGroup[0].startTime, currentGroup))
        return actions
    }

    private fun affectedByOD(od: Float): Float {
        return exp(max(od - 7f, 0f) / 2f)
    }

    override val names = listOf("Rice", "LN", "Coordination", "Stamina", "Speed", "Precision", "SV")
    override val abbreviates = listOf("RC", "LN", "CO", "ST", "SP", "PR", "SV")
    override val values
        get() = skillValues.toList()
    override val bases
        get() = skillValues.toList()
    override val star: Float
        get() {
            // 使用 p=3 的闵可夫斯基和，能让突出的单项技能更显著地拉高星级
            val p = 3.0
            val sum = values.take(6).sumOf { it.toDouble().pow(p) }
            val rawStar = sum.pow(1.0 / p).toFloat()

            // 最终修正：SR = 基础星级 * (1 + 0.1 * SV难度)
            return rawStar * (1f + skillValues[6] * 0.1f)
        }
}