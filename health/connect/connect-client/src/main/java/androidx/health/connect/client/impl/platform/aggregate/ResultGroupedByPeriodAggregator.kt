/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.health.connect.client.impl.platform.aggregate

import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.impl.platform.toLocalTimeWithDefaultZoneFallback
import androidx.health.connect.client.records.InstantaneousRecord
import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.records.Record
import java.time.LocalDateTime
import java.time.Period
import kotlin.math.max
import kotlin.math.min

/**
 * Implementation of [ResultGroupedByPeriodAggregator] that aggregates into
 * [AggregationResultGroupedByPeriod] buckets.
 *
 * @param initProcessor initialization function for an [AggregationProcessor] that does the actual
 *   computation per bucket.
 */
internal class ResultGroupedByPeriodAggregator<T : Record>(
    private val timeRange: LocalTimeRange,
    private val bucketPeriod: Period,
    private val initProcessor: (LocalTimeRange) -> AggregationProcessor<T>
) : Aggregator<T, List<AggregationResultGroupedByPeriod>> {

    private val totalBuckets: Int

    private val bucketProcessors = mutableMapOf<Int, AggregationProcessor<T>>()

    init {
        val bucketIndexOfEndTime = getBucketIndex(timeRange.endTime)
        val isEndTimeAlsoBucketEnd =
            (timeRange.startTime + (bucketPeriod.multipliedBy(bucketIndexOfEndTime))) ==
                timeRange.endTime

        // Bucked index is zero-based and end time is exclusive. When the end time matches a bucket
        // end its index will correspond to a start of a bucket outside of the query range, which
        // matches the number of buckets.
        totalBuckets =
            if (isEndTimeAlsoBucketEnd) bucketIndexOfEndTime else bucketIndexOfEndTime + 1
    }

    override fun filterAndAggregate(record: T) {
        if (!AggregatorUtils.contributesToAggregation(record, timeRange)) {
            return
        }

        val startBucketIndex: Int
        val endBucketIndex: Int

        when (record) {
            is InstantaneousRecord -> {
                startBucketIndex =
                    getBucketIndex(
                        record.time.toLocalTimeWithDefaultZoneFallback(record.zoneOffset)
                    )
                endBucketIndex = startBucketIndex
            }
            is IntervalRecord -> {
                startBucketIndex =
                    getBucketIndex(
                        record.startTime.toLocalTimeWithDefaultZoneFallback(record.startZoneOffset)
                    )
                endBucketIndex =
                    getBucketIndex(
                        record.endTime.toLocalTimeWithDefaultZoneFallback(record.endZoneOffset)
                    )
            }
            else -> error("Unsupported value for aggregation: $record")
        }

        val bucketRange = max(startBucketIndex, 0)..min(endBucketIndex, totalBuckets - 1)

        for (index in bucketRange) {
            val timeRange = getBucketTimeRange(index)
            if (AggregatorUtils.contributesToAggregation(record, timeRange)) {
                bucketProcessors.getOrPut(index) { initProcessor(timeRange) }.processRecord(record)
            }
        }
    }

    override fun getResult(): List<AggregationResultGroupedByPeriod> {
        return bucketProcessors.map {
            AggregationResultGroupedByPeriod(
                result = it.value.getProcessedAggregationResult(),
                startTime = getBucketTimeRange(it.key).startTime,
                endTime = getBucketTimeRange(it.key).endTime
            )
        }
    }

    private fun getBucketIndex(time: LocalDateTime): Int {
        var bucketEndTime = timeRange.startTime
        var index = -1
        while (time >= bucketEndTime) {
            bucketEndTime += bucketPeriod
            index++
        }
        return index
    }

    private fun getBucketTimeRange(index: Int): LocalTimeRange {
        val bucketStartTime = timeRange.startTime + bucketPeriod.multipliedBy(index)
        val bucketEndTime = minOf(bucketStartTime + bucketPeriod, timeRange.endTime)
        return LocalTimeRange(bucketStartTime, bucketEndTime)
    }
}
