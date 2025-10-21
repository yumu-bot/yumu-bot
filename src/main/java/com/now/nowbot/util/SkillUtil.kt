package com.now.nowbot.util

import kotlin.math.sqrt

/**
 *
 */
object SkillUtil {
    fun calculateEuclideanDistance(a: DoubleArray, b: DoubleArray): Double {
        require(a.size == 6 && b.size == 6) {
            "两条数据必须要六个维度：${a.size}，${b.size}"
        }

        val sum = (0..5).toList().sumOf { i ->
            val diff = a[i] - b[i]
            diff * diff
        }

        return sqrt(sum)
    }


    // 实现一个简化的K-Means聚类算法
    fun simpleKMeans(data: List<Pair<Long, DoubleArray>>, k: Int = 1, maxIterations: Int = 100): List<List<Pair<Long, DoubleArray>>> {

        // 随机初始化K个质心
        var centroids = data.shuffled().take(k).toMutableList()
        // 初始化K个空的分组
        val clusters = List(k) { mutableListOf<Pair<Long, DoubleArray>>() }

        repeat(maxIterations) {
            // 清空上一次迭代的分组
            clusters.forEach { it.clear() }

            // 将每个数据点分配到最近的质心所在的组
            for (point in data) {
                var minDistance = Double.POSITIVE_INFINITY
                var assignedClusterIndex = 0

                for ((index, centroid) in centroids.withIndex()) {
                    val distance = calculateEuclideanDistance(point.second, centroid.second)
                    if (distance < minDistance) {
                        minDistance = distance
                        assignedClusterIndex = index
                    }
                }
                clusters[assignedClusterIndex].add(point)
            }

            // 重新计算每个组的质心（取组内所有点在每个维度上的平均值）
            val newCentroids = mutableListOf<Pair<Long, DoubleArray>>()
            for (cluster in clusters) {
                if (cluster.isEmpty()) {
                    // 如果某个组没有数据点，保留旧质心或随机初始化
                    newCentroids.add(centroids[clusters.indexOf(cluster)])
                    continue
                }

                val newCentroid = DoubleArray(6) { dimIndex ->
                    cluster.map { it.second[dimIndex] }.average()
                }

                val mostClose = cluster.map { it.first to calculateEuclideanDistance(it.second, newCentroid) }
                    .minByOrNull { it.second }?.first ?: -1

                newCentroids.add(mostClose to newCentroid)
            }

            // 检查质心是否基本不再变化（收敛）
            var converged = true
            for (i in newCentroids.indices) {
                if (calculateEuclideanDistance(centroids[i].second, newCentroids[i].second) > 0.001) {
                    converged = false
                    break
                }
            }
            if (converged) {
                return@repeat
            }
            centroids = newCentroids
        }
        return clusters
    }



}