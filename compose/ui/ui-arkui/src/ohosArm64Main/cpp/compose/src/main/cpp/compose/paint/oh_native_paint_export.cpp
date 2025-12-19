/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 Tencent. All rights reserved.
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

#include "oh_compose_native_paint.h"
#include "../constants/oh_native_constants.h"
#include "../shader/oh_native_basic_shader.h"
#include "../xcomponent_log.h"
#include "compose/filter/oh_compose_native_color_filter.h"

#include <multimedia/image_framework/image/pixelmap_native.h>

EXTERN_C_START
void androidx_compose_ui_arkui_utils_DisposeOHComposeNativePaint(OHComposeNativePaint_Handle paintHandle) {
    auto paint = reinterpret_cast<OH::OHComposeNativePaint *>(paintHandle);
    LOGI("androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_"
         "DisposeOHComposeNativePaint %{public}p",
         paint);
    // delete paint here to avoid memory leak
    delete paint;
};

/// OHComposeNativePaint related methods
// Batch sync all paint properties in a single FFI call to optimize performance
void androidx_compose_ui_arkui_utils_OHComposeNativePaint_syncAll(
    OHComposeNativePaint_Handle paintPtr,
    const float alpha,
    const bool isAntiAlias,
    const uint64_t color,
    const float strokeWidth,
    uint32_t blendMode,
    uint32_t style,
    uint32_t strokeCap,
    uint32_t strokeJoin,
    uint32_t filterQuality,
    const float strokeMiterLimit,
    NativeBasicShader_Handle shaderPtr,
    OHComposeNativeColorFilter_Handle colorFilterPtr) {
    auto nativePaint = reinterpret_cast<OH::OHComposeNativePaint *>(paintPtr);

    // Set all properties in one batch
    nativePaint->alpha = alpha;
    nativePaint->isAntiAlias = isAntiAlias;
    nativePaint->color = static_cast<uint32_t>(color >> 32);
    nativePaint->strokeWidth = strokeWidth;
    nativePaint->blendMode = static_cast<OH_Drawing_BlendMode>(blendMode);
    nativePaint->style = static_cast<OH_Native_Draw_PaintingStyle>(style);
    nativePaint->strokeCap = static_cast<OH_Native_Draw_StrokeCap>(strokeCap);
    nativePaint->strokeJoin = static_cast<OH_Native_Draw_StrokeJoin>(strokeJoin);
    nativePaint->filterQuality = static_cast<OH_Native_Draw_FilterQuality>(filterQuality);
    nativePaint->strokeMiterLimit = strokeMiterLimit;
    nativePaint->shader = reinterpret_cast<OH::NativeBasicShader *>(shaderPtr);
    nativePaint->colorFilter = reinterpret_cast<OH::OHComposeNativeColorFilter *>(colorFilterPtr);

    LOGI("androidx_compose_ui_arkui_utils_OHComposeNativePaint_syncAll: "
         "alpha=%{public}f, color=0x%{public}X, strokeWidth=%{public}f",
         alpha, nativePaint->color, strokeWidth);
}
EXTERN_C_END