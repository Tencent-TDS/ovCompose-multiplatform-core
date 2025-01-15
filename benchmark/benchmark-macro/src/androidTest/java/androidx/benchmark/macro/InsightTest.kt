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

package androidx.benchmark.macro

import androidx.benchmark.Insight
import androidx.benchmark.InsightSummary
import androidx.benchmark.TraceDeepLink
import androidx.benchmark.TraceDeepLink.StudioSelectionParams
import androidx.benchmark.createInsightSummaries
import androidx.benchmark.macro.perfetto.queryStartupInsights
import androidx.benchmark.traceprocessor.TraceProcessor
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InsightTest {
    private val api35ColdStart =
        createTempFileFromAsset(prefix = "api35_startup_cold_classinit", suffix = ".perfetto-trace")
            .absolutePath

    private val targetPackage = "androidx.compose.integration.hero.macrobenchmark.target"

    // TODO (b/377581661) take time ranges from SlowStartReason
    private fun startupDeepLinkOf(reasonId: String, studioSelectionParams: StudioSelectionParams) =
        TraceDeepLink(
            outputRelativePath = "/fake/output/relative/path.perfetto-trace",
            perfettoUiParams =
                TraceDeepLink.StartupSelectionParams(
                    packageName = targetPackage,
                    reasonId = reasonId,
                ),
            studioParams = studioSelectionParams
        )

    private val canonicalTraceInsights =
        listOf(
            Insight(
                observedLabel = "123305107ns",
                deepLink =
                    startupDeepLinkOf(
                        "POTENTIAL_CPU_CONTENTION_WITH_ANOTHER_PROCESS",
                        StudioSelectionParams(ts = 351868274168786, dur = 108779088, tid = 27246)
                    ),
                iterationIndex = 6,
                category =
                    Insight.Category(
                        titleUrl =
                            "https://d.android.com/test#POTENTIAL_CPU_CONTENTION_WITH_ANOTHER_PROCESS",
                        title = "Potential CPU contention with another process",
                        postTitleLabel = " (expected: < 100000000ns)"
                    )
            ),
            Insight(
                observedLabel = "328462261ns",
                deepLink =
                    startupDeepLinkOf(
                        "JIT_ACTIVITY",
                        StudioSelectionParams(ts = 351868810086686, dur = 641537910, tid = 27251)
                    ),
                iterationIndex = 6,
                category =
                    Insight.Category(
                        titleUrl = "https://d.android.com/test#JIT_ACTIVITY",
                        title = "JIT Activity",
                        postTitleLabel = " (expected: < 100000000ns)"
                    )
            ),
            Insight(
                observedLabel = "150 count",
                deepLink =
                    startupDeepLinkOf(
                        "JIT_COMPILED_METHODS",
                        StudioSelectionParams(ts = 351868803870454, dur = 619263677, tid = 27251)
                    ),
                iterationIndex = 6,
                category =
                    Insight.Category(
                        titleUrl = "https://d.android.com/test#JIT_COMPILED_METHODS",
                        title = "JIT compiled methods",
                        postTitleLabel = " (expected: < 65 count)"
                    )
            )
        )

    private val canonicalTraceInsightSummary =
        listOf(
            InsightSummary(
                category =
                    "[Potential CPU contention with another process](https://d.android.com/test#POTENTIAL_CPU_CONTENTION_WITH_ANOTHER_PROCESS) (expected: < 100000000ns)",
                observedV2 =
                    "seen in iterations: [6](file:///fake/output/relative/path.perfetto-trace)(123305107ns)",
                observedV3 =
                    "seen in iterations: [6](uri:///fake/output/relative/path.perfetto-trace?enablePlugins=android_startup&android_startup:selectionParams=eNodx8EKwyAMANC_2dE_6KGI0MIw0jp2DJkGK0VTUg_7_MHe7V2UTirsqfFEPavU_DVJ2iU3m9oHF6VRpZuDVUyjpPLhno5GeppBWng8lOmWjjVPAaLzcZ2faMMLLfh_weN7jQvOHuLiNgwbWLfvP03rLE0=&selectionParams=eNoFwbEBACAIA7BvnAGV1oFvWFwV_zepG30qnYahTtBbvhMqBJaQrXaGwYZ_46oKfA==)(123305107ns)"
            ),
            InsightSummary(
                category =
                    "[JIT Activity](https://d.android.com/test#JIT_ACTIVITY) (expected: < 100000000ns)",
                observedV2 =
                    "seen in iterations: [6](file:///fake/output/relative/path.perfetto-trace)(328462261ns)",
                observedV3 =
                    "seen in iterations: [6](uri:///fake/output/relative/path.perfetto-trace?enablePlugins=android_startup&android_startup:selectionParams=eNoFwTEKgDAMBdDbOOYGDuJUB6ciOMm3DW0pTSRm8Pi-9yB1FN4xeIZk05Y_SjoefZmaOBeDNxWqbEoDyfRmSXXAOjmssE_GeFWuluctxGtZYzhCPH86oCIg&selectionParams=eNoFwTECABAMA8DfmBs0YuhvLFbq_-7yRnOIEsxEimW9E-zwNias5F5RR3V84nsKZg==)(328462261ns)"
            ),
            InsightSummary(
                category =
                    "[JIT compiled methods](https://d.android.com/test#JIT_COMPILED_METHODS) (expected: < 65 count)",
                observedV2 =
                    "seen in iterations: [6](file:///fake/output/relative/path.perfetto-trace)(150 count)",
                observedV3 =
                    "seen in iterations: [6](uri:///fake/output/relative/path.perfetto-trace?enablePlugins=android_startup&android_startup:selectionParams=eNoFwUsKgDAMBcDbuMwNXKmg4g90X55tqEWaSOzC4zvzwN-IvCBzDQmmKXzkNT_6MiUpHA0lqdDFppThTU8Wf2XYTQUWuVTGeFVcCvU4HK5Z522YutbN3dGv7f4DVeskcw==&selectionParams=eNoFwakBACAIAMBtyPxgYBuKVXF_7-aWGKVnomSgmkK_U06LXTwCZndxsNEH4rIKbQ==)(150 count)"
            ),
        )

    @MediumTest
    @Test
    fun queryStartupInsights() {
        TraceProcessor.runSingleSessionServer(api35ColdStart) {
            assertThat(
                    queryStartupInsights(
                        helpUrlBase = "https://d.android.com/test#",
                        traceOutputRelativePath = "/fake/output/relative/path.perfetto-trace",
                        iteration = 6,
                        packageName = Packages.MISSING
                    )
                )
                .isEmpty()

            assertThat(
                    queryStartupInsights(
                        helpUrlBase = "https://d.android.com/test#",
                        traceOutputRelativePath = "/fake/output/relative/path.perfetto-trace",
                        iteration = 6,
                        packageName = targetPackage
                    )
                )
                .isEqualTo(canonicalTraceInsights)
        }
    }

    @MediumTest
    @Test
    fun createInsightSummaries() {
        assertThat(canonicalTraceInsights.createInsightSummaries().map { it.observedV3 })
            .isEqualTo(canonicalTraceInsightSummary.map { it.observedV3 })
    }
}
