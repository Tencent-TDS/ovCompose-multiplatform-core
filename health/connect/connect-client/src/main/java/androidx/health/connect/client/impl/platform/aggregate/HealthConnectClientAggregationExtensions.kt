/*
 * Copyright 2024 The Android Open Source Project
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

@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.aggregate

import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.impl.converters.datatype.RECORDS_CLASS_NAME_MAP
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Max buffer to account for overlapping records that have startTime < timeRangeFilter.startTime
val RECORD_START_TIME_BUFFER: Duration = Duration.ofDays(1)

private val AGGREGATION_FALLBACK_RECORD_TYPES =
    setOf(
        BloodPressureRecord::class,
        CyclingPedalingCadenceRecord::class,
        NutritionRecord::class,
        SpeedRecord::class,
        StepsCadenceRecord::class
    )

internal suspend fun HealthConnectClient.aggregateFallback(
    request: AggregateRequest
): AggregationResult {
    val aggregationResult =
        AGGREGATION_FALLBACK_RECORD_TYPES.associateWith { recordType ->
                request.withFilteredMetrics {
                    it.dataTypeName == RECORDS_CLASS_NAME_MAP[recordType]!!
                }
            }
            .filterValues { it.metrics.isNotEmpty() }
            .map {
                val recordType = it.key
                val recordTypeRequest = it.value

                // Calculate the aggregation result for a single record type
                when (recordType) {
                    BloodPressureRecord::class -> aggregateBloodPressure(recordTypeRequest)
                    CyclingPedalingCadenceRecord::class ->
                        aggregateSeriesRecord(
                            CyclingPedalingCadenceRecord::class,
                            recordTypeRequest
                        )
                    NutritionRecord::class -> aggregateNutritionTransFatTotal(recordTypeRequest)
                    SpeedRecord::class ->
                        aggregateSeriesRecord(SpeedRecord::class, recordTypeRequest)
                    StepsCadenceRecord::class ->
                        aggregateSeriesRecord(StepsCadenceRecord::class, recordTypeRequest)
                    else -> error("Invalid record type for aggregation fallback: $recordType")
                }
            }
            .reduceOrNull {
                // Reduce into a single AggregationResult containing metrics across all the record
                // types above
                accumulator,
                element ->
                accumulator + element
            }
    return aggregationResult ?: AggregationResult(emptyMap(), emptyMap(), emptySet())
}

internal suspend fun HealthConnectClient.aggregateFallback(
    request: AggregateGroupByPeriodRequest
): List<AggregationResultGroupedByPeriod> {
    return AGGREGATION_FALLBACK_RECORD_TYPES.associateWith { recordType ->
            request.withFilteredMetrics { it.dataTypeName == RECORDS_CLASS_NAME_MAP[recordType]!! }
        }
        .filterValues { it.metrics.isNotEmpty() }
        .flatMap {
            val recordType = it.key
            val recordTypeRequest = it.value

            val buckets: List<AggregationResultGroupedByPeriod> =
                when (recordType) {
                    BloodPressureRecord::class -> aggregateBloodPressure(recordTypeRequest)
                    CyclingPedalingCadenceRecord::class ->
                        aggregateSeriesRecord(
                            CyclingPedalingCadenceRecord::class,
                            recordTypeRequest
                        )
                    NutritionRecord::class -> aggregateNutritionTransFatTotal(recordTypeRequest)
                    SpeedRecord::class ->
                        aggregateSeriesRecord(SpeedRecord::class, recordTypeRequest)
                    StepsCadenceRecord::class ->
                        aggregateSeriesRecord(StepsCadenceRecord::class, recordTypeRequest)
                    else -> error("Invalid record type for aggregation fallback: $recordType")
                }

            buckets
        }
        .groupingBy { it.startTime }
        .reduce { _, accumulator, element ->
            AggregationResultGroupedByPeriod(
                startTime = accumulator.startTime,
                endTime = accumulator.endTime,
                result = accumulator.result + element.result
            )
        }
        .values
        .sortedBy { it.startTime }
}

internal suspend fun HealthConnectClient.aggregateFallback(
    request: AggregateGroupByDurationRequest
): List<AggregationResultGroupedByDuration> {
    return AGGREGATION_FALLBACK_RECORD_TYPES.associateWith { recordType ->
            request.withFilteredMetrics { it.dataTypeName == RECORDS_CLASS_NAME_MAP[recordType]!! }
        }
        .filterValues { it.metrics.isNotEmpty() }
        .flatMap {
            val recordType = it.key
            val recordTypeRequest = it.value

            val buckets: List<AggregationResultGroupedByDurationWithMinTime> =
                when (recordType) {
                    BloodPressureRecord::class -> aggregateBloodPressure(recordTypeRequest)
                    CyclingPedalingCadenceRecord::class ->
                        aggregateSeriesRecord(
                            CyclingPedalingCadenceRecord::class,
                            recordTypeRequest
                        )
                    NutritionRecord::class -> aggregateNutritionTransFatTotal(recordTypeRequest)
                    SpeedRecord::class ->
                        aggregateSeriesRecord(SpeedRecord::class, recordTypeRequest)
                    StepsCadenceRecord::class ->
                        aggregateSeriesRecord(StepsCadenceRecord::class, recordTypeRequest)
                    else -> error("Invalid record type for aggregation fallback: $recordType")
                }

            buckets
        }
        .groupingBy { it.aggregationResultGroupedByDuration.startTime }
        .reduce { startTime, accumulator, element ->
            AggregationResultGroupedByDurationWithMinTime(
                aggregationResultGroupedByDuration =
                    AggregationResultGroupedByDuration(
                        startTime = startTime,
                        endTime = accumulator.aggregationResultGroupedByDuration.endTime,
                        result =
                            accumulator.aggregationResultGroupedByDuration.result +
                                element.aggregationResultGroupedByDuration.result,
                        zoneOffset =
                            minOf(accumulator, element, compareBy { it.minTime })
                                .aggregationResultGroupedByDuration
                                .zoneOffset
                    ),
                minTime = minOf(accumulator.minTime, element.minTime)
            )
        }
        .map { it.value.aggregationResultGroupedByDuration }
        .sortedBy { it.startTime }
}

internal suspend fun <T : Record, R> HealthConnectClient.aggregate(
    readRecordsRequest: ReadRecordsRequest<T>,
    aggregator: Aggregator<T, R>
): R {
    readRecordsFlow(readRecordsRequest).collect { records ->
        records.forEach { aggregator.filterAndAggregate(it) }
    }
    return aggregator.getResult()
}

/** Reads all existing records that satisfy [request]. */
internal fun <T : Record> HealthConnectClient.readRecordsFlow(
    request: ReadRecordsRequest<T>
): Flow<List<T>> {
    return flow {
        var currentRequest = request
        do {
            val response = readRecords(currentRequest)
            emit(response.records)
            currentRequest = currentRequest.withPageToken(response.pageToken)
        } while (currentRequest.pageToken != null)
    }
}

internal fun TimeRangeFilter.withBufferedStart(): TimeRangeFilter {
    return TimeRangeFilter(
        startTime = startTime?.minus(RECORD_START_TIME_BUFFER),
        endTime = endTime,
        localStartTime = localStartTime?.minus(RECORD_START_TIME_BUFFER),
        localEndTime = localEndTime
    )
}

internal data class AvgData(var count: Int = 0, var total: Double = 0.0) {
    operator fun plusAssign(value: Double) {
        count++
        total += value
    }

    fun average() = total / count
}
