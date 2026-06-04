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

#import "TMMNativeFastClipView.h"
#import "TMMNativeBaseLayer.h"

@implementation TMMNativeFastClipView

+ (Class)layerClass {
    return [TMMNativeBaseLayer class];
}

- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    UIView *view = [super hitTest:point withEvent:event];
    if (view == self) {
        view = nil;
    }
    return view;
}

- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"FastClipView",
        @"properties" : @[
            @{
                @"title" : @"View 信息",
                @"valueType" : @"string",
                @"section" : @"FastClipView 详细信息",
                @"value" : [NSString stringWithFormat:@"%@", self]
            },
            @{
                @"title" : @"window 信息",
                @"valueType" : @"string",
                @"section" : @"FastClipView window信息",
                @"value" : [NSString stringWithFormat:@"%@", self.window]
            }
        ]
    };
}


@end
