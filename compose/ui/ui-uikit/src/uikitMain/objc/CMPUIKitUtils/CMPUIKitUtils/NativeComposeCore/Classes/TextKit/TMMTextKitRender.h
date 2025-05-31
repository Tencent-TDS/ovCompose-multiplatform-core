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

#import <UIKit/UIKit.h>

@class TMMComposeTextAttributes;
@class TMMTextKitContext;
@class TMMTextKitShadower;
@class TMMComposeMemoryCache;
@protocol TMMTextKitTruncating;

NS_ASSUME_NONNULL_BEGIN

/// TextKit 对接的渲染器
@interface TMMTextKitRender : NSObject
/**
 Designated Initializer
 @discussion Sizing will occur as a result of initialization, so be careful when/where you use this.
 */
- (instancetype)initWithTextKitAttributes:(const TMMComposeTextAttributes *)textStyle
                          constrainedSize:(const CGSize)constrainedSize
                             textHashCode:(NSNumber *)textHashCode;

/// 当前的 context
@property (nonatomic, strong, readonly) TMMTextKitContext *context;

/// TextKit 的 truncater
@property (nonatomic, strong, readonly) id<TMMTextKitTruncating> truncater;

/// 阴影处理器
@property (nonatomic, strong, readonly) TMMTextKitShadower *shadower;

/// 约束 size
@property (nonatomic, assign, readonly) CGSize constrainedSize;

/// Text 的 hashCode
@property (nonatomic, strong, readonly) NSNumber *textHashCode;

/// 文本的属性
@property (nonatomic, nonnull, readonly) TMMComposeTextAttributes *textAttributes;

#pragma mark - Drawing
/*
 Draw the renderer's text content into the bounds provided.

 @param bounds The rect in which to draw the contents of the renderer.
 */
- (void)drawInContext:(CGContextRef)context bounds:(CGRect)bounds;

/// update the TextAttribute for value
/// - Parameters:
///   - key: NSString
///   - value: id
- (void)updateTextAttribute:(NSString *)key value:(id)value;

#pragma mark - Layout

/*
 Returns the computed size of the renderer given the constrained size and other parameters in the initializer.
 */
- (CGSize)size;

/*
 relayout textcontainer with new constraints and maxline、linebreakMode
 */
- (void)relayoutWithMaxWidth:(float)maxWidth maxHeight:(float)maxHeight maxLine:(int)maxLines lineBreakMode:(NSLineBreakMode)lineBreakMode;

#pragma mark - Text Ranges

/*
 The character range from the original attributedString that is displayed by the renderer given the parameters in the
 initializer.
 */
//- (std::vector<NSRange>)visibleRanges;

/*
 The number of lines shown in the string.
 */
- (NSUInteger)lineCount;

/// 返回 Glyph 数量
- (NSUInteger)numberOfGlyphs;

/// 获取第index个字符的宽度
- (CGFloat)getBaselineByIndex:(NSInteger)index;

/// 获取坐标对应的字符所在索引位置
- (NSInteger)getOffsetForPositionX:(float)x y:(float)y;

/// 判断第lineIndex行是否被截断
- (BOOL)isLineEllipsized:(int)lineIndex;

/// 根据字符所在的offset偏移，返回字符所在的矩形
- (CGRect)getCursorRect:(int)offset;

/// 根据字符所在的offset偏移（即第offset个字符），返回字符所在的行
- (int)getLineForOffset:(int)offset;

/// 返回lineIndex所在行的高度
- (float)getLineHeight:(int)lineIndex;

/// 返回lineIndex所在行的宽度
- (float)getLineWidth:(int)lineIndex;

/// 返回lineIndex所在行的Bottom坐标
- (float)getLineBottom:(int)lineIndex;

/// 返回lineIndex所在行的Left坐标
- (float)getLineLeft:(int)lineIndex;

/// 返回lineIndex所在行的Right坐标
- (float)getLineRight:(int)lineIndex;

/// 返回lineIndex所在行的Top坐标
- (float)getLineTop:(int)lineIndex;

/// 返回lineIndex所在行的第一个光标所在的Index
- (NSUInteger)getLineStart:(int)lineIndex;

/// 返回lineIndex所在行的最后一个光标所在的Index
- (NSUInteger)getLineEnd:(int)lineIndex visibleEnd:(BOOL)visibleEnd;

/// 根据字符所在的offset偏移找到该字符单词的范围
- (NSRange)getWordBoundary:(int)offset;

///  根据字符所在的start和end，返回所占行的Rect数组
- (NSArray<NSValue *> *)getRectsForRange:(int)start end:(int)end;

@end

NS_ASSUME_NONNULL_END
