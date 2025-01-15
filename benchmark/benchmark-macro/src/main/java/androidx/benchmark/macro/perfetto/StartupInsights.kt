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

package androidx.benchmark.macro.perfetto

import androidx.benchmark.Insight
import androidx.benchmark.TraceDeepLink
import androidx.benchmark.TraceDeepLink.StartupSelectionParams
import androidx.benchmark.inMemoryTrace
import androidx.benchmark.traceprocessor.TraceProcessor
import perfetto.protos.AndroidStartupMetric.SlowStartReason
import perfetto.protos.AndroidStartupMetric.ThresholdValue.ThresholdUnit
import perfetto.protos.TraceMetrics

private fun SlowStartReason.getStudioSelectionParams(): TraceDeepLink.StudioSelectionParams? {
    trace_slice_sections?.apply {
        val start = start_timestamp
        val end = end_timestamp
        return TraceDeepLink.StudioSelectionParams(
            ts = if (start != null && end != null) start else null,
            dur = if (start != null && end != null) end - start else null,
            tid = slice_section.firstOrNull()?.thread_tid
        )
    }
    trace_thread_sections?.apply {
        val start = start_timestamp
        val end = end_timestamp
        return TraceDeepLink.StudioSelectionParams(
            ts = if (start != null && end != null) start else null,
            dur = if (start != null && end != null) end - start else null,
            tid = thread_section.firstOrNull()?.thread_tid
        )
    }
    // no thread sections or slices
    return null
}

private fun getDeepLink(
    traceOutputRelativePath: String,
    packageName: String,
    slowStartReason: SlowStartReason,
): TraceDeepLink {
    return TraceDeepLink(
        outputRelativePath = traceOutputRelativePath,
        perfettoUiParams =
            StartupSelectionParams(
                packageName = packageName,
                reasonId = slowStartReason.reason_id?.name
            ),
        studioParams = slowStartReason.getStudioSelectionParams()
    )
}

/**
 * Convert the SlowStartReason concept from Perfetto's android_startup metric to the Macrobenchmark
 * generic Insight format
 */
private fun SlowStartReason.toInsight(
    packageName: String,
    helpUrlBase: String?,
    traceOutputRelativePath: String,
    iterationIndex: Int,
): Insight {
    val thresholdUnit = expected_value!!.unit!!
    val unitSuffix =
        when (thresholdUnit) {
            ThresholdUnit.NS -> "ns"
            ThresholdUnit.PERCENTAGE -> "%"
            ThresholdUnit.COUNT -> " count"
            ThresholdUnit.TRUE_OR_FALSE -> ""
            else -> " ${thresholdUnit.toString().lowercase()}"
        }

    val thresholdValue = expected_value!!.value_!!

    val thresholdString =
        StringBuilder()
            .apply {
                append(" (expected: ")
                if (thresholdUnit == ThresholdUnit.TRUE_OR_FALSE) {
                    when (thresholdValue) {
                        0L -> append("false")
                        1L -> append("true")
                        else ->
                            throw IllegalArgumentException(
                                "Unexpected boolean value $thresholdValue"
                            )
                    }
                } else {
                    if (expected_value!!.higher_expected == true) append("> ")
                    if (expected_value!!.higher_expected == false) append("< ")
                    append(thresholdValue)
                    append(unitSuffix)
                }
                append(")")
            }
            .toString()

    val category =
        Insight.Category(
            titleUrl = helpUrlBase?.plus(reason_id!!.name),
            title = reason!!,
            postTitleLabel = thresholdString
        )

    val observedValue = requireNotNull(actual_value?.value_)
    return Insight(
        observedLabel =
            if (thresholdUnit == ThresholdUnit.TRUE_OR_FALSE) {
                require(observedValue in 0L..1L)
                if (observedValue == 0L) "false" else "true"
            } else {
                "$observedValue$unitSuffix"
            },
        deepLink =
            getDeepLink(
                traceOutputRelativePath = traceOutputRelativePath,
                packageName = packageName,
                slowStartReason = this
            ),
        iterationIndex = iterationIndex,
        category = category,
    )
}

internal fun TraceProcessor.Session.queryStartupInsights(
    helpUrlBase: String?,
    traceOutputRelativePath: String,
    iteration: Int,
    packageName: String
): List<Insight> =
    inMemoryTrace("extract insights") {
        TraceMetrics.ADAPTER.decode(queryMetricsProtoBinary(listOf("android_startup")))
            .android_startup
            ?.startup
            ?.filter { it.package_name == packageName } // TODO: fuzzy match?
            ?.flatMap { it.slow_start_reason_with_details }
            ?.map {
                it.toInsight(
                    packageName = packageName,
                    helpUrlBase = helpUrlBase,
                    traceOutputRelativePath = traceOutputRelativePath,
                    iterationIndex = iteration
                )
            } ?: emptyList()
    }
