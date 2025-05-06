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

#import "TMMComposeAdaptivedCanvasViewV2.h"
#import "TMMUIKitCanvasLayer.h"
#import "TMMUIKitPictureRecorder.h"
#import "UIViewUtils.h"

@implementation TMMComposeAdaptivedCanvasViewV2

+ (Class)layerClass {
    return [TMMUIKitCanvasLayer class];
}

- (void)prepareForReuse {
    if (self.hidden) {
        self.hidden = NO;
    }
    if (self.alpha < 1.0) {
        self.alpha = 1.0;
    }
    TMMCMPUIViewFastRemoveFromSuperview(self);
    NSArray<UIView *> *subviews = self.subviews;
    for (NSInteger i = subviews.count - 1; i >= 0; i--) {
        TMMCMPUIViewFastRemoveFromSuperview(subviews[i]);
    }
    [(TMMUIKitCanvasLayer *)self.layer prepareForReuse];
}

#pragma mark - overwrite
- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event {
    UIView *view = [super hitTest:point withEvent:event];
    return view == self ? nil : view;
}

- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"ViewV2",
        @"properties" : @[
            @{
                @"title" : @"ViewV2 信息",
                @"valueType" : @"string",
                @"section" : @"ViewV2 详细信息",
                @"value" : [NSString stringWithFormat:@"%@", self]
            },
            @{
                @"title" : @"window 信息",
                @"valueType" : @"string",
                @"section" : @"window信息",
                @"value" : [NSString stringWithFormat:@"%@", self.window]
            }
        ]
    };
}

@end
