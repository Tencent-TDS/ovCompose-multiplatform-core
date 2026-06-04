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

#import "TMMLookInDebugUtils.h"

static NSInteger gTMMViewProxyDebugId = 1;

BOOL TMMLookInDebugEnabel(void) {
    static BOOL enable = NO;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        enable = [[NSUserDefaults standardUserDefaults] boolForKey:@"kQLDebugConfig_enableComposeDebugUI"];
    });
    return enable;
}

NSMutableDictionary <NSNumber *, id> * TMMLookInDebugViewProxyMap(void) {
    static NSMutableDictionary *dic = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        dic = @{}.mutableCopy;
    });
    return dic;
}

NSInteger TMMAllocViewProxyDebugId(void) {
    return gTMMViewProxyDebugId++;
}
