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

#import "TMMComposeTextAttributes.h"
#import "TMMComposeTextSpanAttributes.h"
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class TMMComposeAdaptivedCanvasView;
@class TMMCanvasViewProxy;

@protocol ITMMComposeText <NSObject>

- (CGPoint)measureAndLayout:(TMMComposeTextAttributes *)attributes maxWidth:(float)maxWidth maxHeight:(float)maxHeight;

/// 根据重新计算后的约束更新Text的布局
- (void)relayoutWithMaxWidth:(float)maxWidth maxHeight:(float)maxHeight maxLine:(int)maxLines lineBreakMode:(NSLineBreakMode)lineBreakMode;

/// 将文本绘制到TMMComposeText上
- (void)paint:(TMMComposeAdaptivedCanvasView *)view color:(uint64_t)color;

- (void)paintWithColor:(uint64_t)color;

/// 获取第一行文本的baseline
- (CGFloat)getFirstBaseline;

/// 获取最后一行文本的baseline
- (CGFloat)getLastBaseline;

/// 获取文本的总行数
- (NSInteger)lineCount;

/// 获取坐标对应的字符所在索引位置
- (NSInteger)getOffsetForPositionX:(float)x y:(float)y;

/// 判断第lineIndex行是否被截断
- (BOOL)isLineEllipsized:(int)lineIndex;

/// 根据字符所在的offset偏移（即第offset个字符），返回字符所在的矩形
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

/// TextKit 对接的文本Layer
@interface TMMComposeTextLayer : CALayer <ITMMComposeText>

@end

NS_ASSUME_NONNULL_END
