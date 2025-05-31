/*
 * Tencent is pleased to support the open source community by making ovCompose available.
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

#import "OVComposePreloadFuncs.h"

extern void *org_jetbrains_skia_ColorFilter__1nGetLuma(void);

/*
 耗时 3ms
 sk_sp<SkColorFilter> SkLumaColorFilter::Make() {
     static const SkRuntimeEffect* effect = SkMakeCachedRuntimeEffect(
         SkRuntimeEffect::MakeForColorFilter,
         "half4 main(half4 inColor) {"
             "return saturate(dot(half3(0.2126, 0.7152, 0.0722), inColor.rgb)).000r;"
         "}"
     ).release();
     SkASSERT(effect);

     return effect->makeColorFilter(SkData::MakeEmpty());
 }
 */

FOUNDATION_EXTERN void OVComposePreloadSkiaObjects(void) {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        dispatch_async(dispatch_get_global_queue(0, 0), ^{
            // 预热 SkLumaColorFilter
            org_jetbrains_skia_ColorFilter__1nGetLuma();
        });
    });
}
