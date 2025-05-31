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

#import "TMMNativeClipLayer.h"

@implementation TMMNativeClipView

+ (Class)layerClass {
    return [TMMNativeClipLayer class];
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
        @"title" : @"ClipView",
        @"properties" : @[
            @{
                @"title" : @"View 信息",
                @"valueType" : @"string",
                @"section" : @"ClipView 详细信息",
                @"value" : [NSString stringWithFormat:@"%@", self]
            },
            @{
                @"title" : @"window 信息",
                @"valueType" : @"string",
                @"section" : @"ClipView window信息",
                @"value" : [NSString stringWithFormat:@"%@", self.window]
            }
        ]
    };
}

@end

@implementation TMMNativeClipLayer

- (instancetype)init {
    if (self = [super init]) {
        [super setMasksToBounds:NO];
    }
    return self;
}

#pragma mark - super override
- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)setMasksToBounds:(BOOL)masksToBounds {
}

- (void)setCornerRadius:(CGFloat)cornerRadius {
}

- (void)setBounds:(CGRect)bounds {
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    CGPoint origin = frame.origin;
    [super setBounds:CGRectMake(origin.x, origin.y, frame.size.width, frame.size.height)];
}

#pragma mark - debug
- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"clip",
        @"properties" : @[ @{
            @"title" : @"Layer 信息",
            @"valueType" : @"string",
            @"section" : @"Layer 详细信息",
            @"value" : [NSString stringWithFormat:@"%@", self]
        } ]
    };
}

@end
