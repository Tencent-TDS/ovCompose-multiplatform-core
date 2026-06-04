/*
 * Tencent is pleased to support the open source community by making tvCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.ui.platform.v2.nativefoundation

import androidx.compose.runtime.ComposeTabService
import androidx.compose.ui.uikit.ExperimentalConfig
import androidx.compose.ui.uikit.utils.OVComposeExperimentalConfigCreate
import androidx.compose.ui.uikit.utils.OVComposeExperimentalConfigRelease
import androidx.compose.ui.uikit.utils.OVComposeExperimentalConfigSetEnableCALayerClipOpt
import androidx.compose.ui.uikit.utils.OVComposeExperimentalConfigSetEnableImageLog
import androidx.compose.ui.uikit.utils.OVComposeExperimentalConfigSetEnablePerspectiveTransformFix
import androidx.compose.ui.uikit.utils.OVComposeExperimentalConfigSetEnableTextAsyncPaint
import androidx.compose.ui.uikit.utils.OVComposeExperimentalConfigSetFixTextLeakType
import androidx.compose.ui.uikit.utils.TMMTextFixLeakType

private fun tmmTextFixLeakTypeFromInt(value: Int): TMMTextFixLeakType {
    return when (value) {
        0 -> TMMTextFixLeakType.TMMTextFixLeakTypeNone
        1 -> TMMTextFixLeakType.TMMTextFixLeakTypeByteArray
        2 -> TMMTextFixLeakType.TMMTextFixLeakTypeEncodeImage
        3 -> TMMTextFixLeakType.TMMTextFixLeakTypeDecodeImage
        else -> TMMTextFixLeakType.TMMTextFixLeakTypeNone // 默认返回 None
    }
}

internal fun createExperimentalConfig(outerConfig: ExperimentalConfig?): ExperimentalConfigImpl? {
    // 如果外面没有传，并且开启了实验
    if (outerConfig == null) {
        return ExperimentalConfigImpl(object : ExperimentalConfig {
            override val enableExperimentalRotate: Boolean = true
            override val textFixLeakType: TMMTextFixLeakType = tmmTextFixLeakTypeFromInt(ComposeTabService.iosTextLeakFixType)
        })
    }

    return ExperimentalConfigImpl(object : ExperimentalConfig {
        override val enableExperimentalRotate: Boolean = true

        override val enableCALayerClipOpt: Boolean = outerConfig.enableCALayerClipOpt

        override val enablePerspectiveTransformFix: Boolean =
            outerConfig.enablePerspectiveTransformFix

        override val enableTextAsyncPaint: Boolean = outerConfig.enableTextAsyncPaint

        override val enableImageLog: Boolean = outerConfig.enableImageLog

        override val textFixLeakType: TMMTextFixLeakType =
            if (outerConfig.textFixLeakType != TMMTextFixLeakType.TMMTextFixLeakTypeNone)
                outerConfig.textFixLeakType
            else
                tmmTextFixLeakTypeFromInt(ComposeTabService.iosTextLeakFixType)

    })
}

/* 实验性的配置 */
internal class ExperimentalConfigImpl (
    private val outerConfig: ExperimentalConfig
) : ExperimentalConfig by outerConfig {

    private fun createNativeExperimentalConfigIfNeeded(outerConfig: ExperimentalConfig): Long {
        return if (outerConfig.enableExperimentalRotate || outerConfig.enablePerspectiveTransformFix || outerConfig.enableTextAsyncPaint || outerConfig.textFixLeakType != TMMTextFixLeakType.TMMTextFixLeakTypeNone
        ) {
            val ptr = OVComposeExperimentalConfigCreate()
            OVComposeExperimentalConfigSetEnablePerspectiveTransformFix(
                ptr,
                outerConfig.enablePerspectiveTransformFix
            )
            OVComposeExperimentalConfigSetEnableTextAsyncPaint(
                ptr,
                outerConfig.enableTextAsyncPaint
            )
            OVComposeExperimentalConfigSetEnableCALayerClipOpt(
                ptr,
                outerConfig.enableCALayerClipOpt
            )
            OVComposeExperimentalConfigSetEnableImageLog(
                ptr,
                outerConfig.enableImageLog
            )
            OVComposeExperimentalConfigSetFixTextLeakType(
                ptr,
                outerConfig.textFixLeakType
            )
            ptr
        } else {
            0L
        }
    }

    var nativeExperimentalConfigPtr: Long = createNativeExperimentalConfigIfNeeded(outerConfig)
        private set

    fun dispose() {
        if (nativeExperimentalConfigPtr != 0L) {
            OVComposeExperimentalConfigRelease(nativeExperimentalConfigPtr)
            nativeExperimentalConfigPtr = 0L
        }
    }
}