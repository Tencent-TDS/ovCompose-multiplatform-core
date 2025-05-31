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

#import "TMMNativeEnums.h"
#import <UIKit/UIKit.h>

#ifndef TMMComposeTextSpanAttributes_h
#define TMMComposeTextSpanAttributes_h

/// 对应的 Kotlin 侧 TextSpanAttributes
@interface TMMComposeTextSpanAttributes : NSObject

/// 起始点
@property (nonatomic, assign) int start;

/// 结束点
@property (nonatomic, assign) int end;

/// 字体大小
@property (nonatomic, assign) int fontSize;

/// 字体粗细
@property (nonatomic, assign) UIFontWeight fontWeight;

/// 字间距
@property (nonatomic, assign) float letterSpace;

/// 字体名称
@property (nonatomic, nullable, copy) NSString *fontFamily;

/// 字体颜色
@property (nonatomic, assign) uint64_t foregroundColor;

/// 背景颜色
@property (nonatomic, assign) uint64_t backgroundColor;

/// 斜体类型
@property (nonatomic, assign) TMMNativeItalicType italicType;

/// 下划线类型
@property (nonatomic, assign) TMMNativeTextDecorator textDecorator;

/// 创建一个 TMMComposeTextSpanAttributes
- (instancetype _Nonnull)initWithStart:(int)start
                                   end:(int)end
                              fontSize:(int)fontSize
                            fontWeight:(UIFontWeight)fontWeight
                           letterSpace:(float)letterSpace
                            fontFamily:(NSString *_Nullable)fontFamily
                       foregroundColor:(uint64_t)foregroundColor
                       backgroundColor:(uint64_t)backgroundColor
                            italicType:(TMMNativeItalicType)italicType
                         textDecorator:(TMMNativeTextDecorator)textDecorator;


/// 处理 SpanTextAttributes
- (void)processSpanTextAttributes:(NSMutableAttributedString *_Nonnull)string
                           uiFont:(UIFont *_Nonnull)uiFont
                           italic:(TMMNativeItalicType)parentItalic;
/// 根据 storage 处理 spanTextColor
/// - Parameter storage: NSTextStorage
- (void)processSpanTextColor:(NSTextStorage *_Nonnull)storage;

@end

#endif /* TMMComposeTextSpanAttributes_h */
