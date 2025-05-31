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

#import "TMMComposeTextLayer.h"
#import "TMMComposeMemoryCache.h"
#import "TMMComposeTextAttributes.h"
#import "TMMComposeTextSpanAttributes.h"
#import "TMMDrawUtils.h"
#import "TMMNativeComposeAdaptivedCanvas.h"
#import "TMMTextKitRender.h"
#import <UIKit/UIKit.h>

TMMComposeMemoryCache *sharedTMMTextKitRendererCache(void) {
    static TMMComposeMemoryCache *memeoryCache;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        memeoryCache = [[TMMComposeMemoryCache alloc] init];
        memeoryCache.name = @"__TMMTextKitRendererCache";
        memeoryCache.countLimit = 500;
        memeoryCache.costLimit = 6 * 1024 * 1025;
    });

    return memeoryCache;
}

static NSNumber *tmmCreateSpanCacheKey(NSArray<TMMComposeTextSpanAttributes *> *attributes) {
    if (attributes == nil || attributes.count <= 0) {
        return @(0);
    }
    // 根据 attributes 的大小创建一个相同大小的 NSNumber 数组
    NSMutableArray<NSNumber *> *floatArray = [NSMutableArray arrayWithCapacity:attributes.count];
    // 遍历 attributes 数组，为 floatArray 赋值
    for (TMMComposeTextSpanAttributes *spanAttribute in attributes) {
        NSUInteger hash = (spanAttribute != nil) ? spanAttribute.hash : 0;
        [floatArray addObject:@(hash)];
    }
    return @(TMMFNVHashFloatArray([floatArray copy])); // 返回不可变的 NSArray
}

static NSNumber *tmmCreateTextCacheKey(TMMComposeTextAttributes *attributes, CGSize constraintSize) {
    NSArray *hashArray = @[
        @(attributes.fontSize), @(attributes.align), @(attributes.fontWeight), @(attributes.lineHeight), @(attributes.letterSpace),
        @(attributes.backgroundColor), @(attributes.fontFamily.hash), @(attributes.content.hash), @(constraintSize.width), @(constraintSize.height),
        tmmCreateSpanCacheKey(attributes.spanStyles)
    ];
    return @(TMMFNVHashFloatArray(hashArray));
}

TMMTextKitRender *rendererForAttributes(TMMComposeTextAttributes *attributes, const CGSize constrainedSize) {
    CGSize cacheSize = CGSizeMake(roundf(constrainedSize.width), roundf(constrainedSize.height));

    TMMComposeMemoryCache *cache = sharedTMMTextKitRendererCache();

    NSNumber *hashCode = tmmCreateTextCacheKey(attributes, cacheSize);

    TMMTextKitRender *renderer = [cache objectForKey:hashCode];

    if (!renderer) {
        renderer = [[TMMTextKitRender alloc] initWithTextKitAttributes:attributes constrainedSize:constrainedSize textHashCode:hashCode];
        [cache setObject:renderer forKey:hashCode];
    }
    return renderer;
}

static void renderCacheWithKey(TMMTextKitRender *render, TMMComposeTextAttributes *attributes, const CGSize constrainedSize) {
    CGSize cacheSize = CGSizeMake(roundf(constrainedSize.width), roundf(constrainedSize.height));
    TMMComposeMemoryCache *cache = sharedTMMTextKitRendererCache();
    NSNumber *hashCode = tmmCreateTextCacheKey(attributes, cacheSize);
    [cache setObject:render forKey:hashCode];
}

@interface TMMComposeTextLayer ()

/// 背景色
@property (nonatomic, strong) UIColor *bgColor;

/// 当前的 TMMTextKitRender
@property (nonatomic, strong) TMMTextKitRender *textRender;

@end

@implementation TMMComposeTextLayer : CALayer

- (CGPoint)measureAndLayout:(TMMComposeTextAttributes *)attributes maxWidth:(float)maxWidth maxHeight:(float)maxHeight {
    CGFloat density = TMMComposeCoreDeviceDensity();
    self.contentsScale = density;
    if (attributes.backgroundColor != 0) {
        _bgColor = TMMComposeCoreMakeUIColorFromULong(attributes.backgroundColor);
    }
    _textRender = rendererForAttributes(attributes, CGSizeMake(maxWidth / density, maxHeight / density));
    CGSize size = [_textRender size];
    // 设置Layer的宽高
    self.frame = CGRectMake(0, 0, size.width, size.height);
    return CGPointMake(size.width, size.height);
}

/// 根据重新计算后的约束更新Text的布局
- (void)relayoutWithMaxWidth:(float)maxWidth maxHeight:(float)maxHeight maxLine:(int)maxLines lineBreakMode:(NSLineBreakMode)lineBreakMode {
    CGFloat density = TMMComposeCoreDeviceDensity();
    [_textRender relayoutWithMaxWidth:(maxWidth / density) maxHeight:(maxHeight / density) maxLine:maxLines lineBreakMode:lineBreakMode];
    // 设置Layer的宽高
    CGSize size = [_textRender size];
    self.frame = CGRectMake(0, 0, size.width, size.height);
}

- (void)paint:(TMMComposeAdaptivedCanvasView *)view color:(uint64_t)color {
    UIColor *foregroundColor = TMMComposeCoreMakeUIColorFromULong(color);
    [_textRender updateTextAttribute:NSForegroundColorAttributeName value:foregroundColor];

    [self setNeedsDisplay];
}

- (void)paintWithColor:(uint64_t)color {
    UIColor *foregroundColor = TMMComposeCoreMakeUIColorFromULong(color);
    [_textRender updateTextAttribute:NSForegroundColorAttributeName value:foregroundColor];

    [self setNeedsDisplay];
}

/// 获取第一行文本的baseline
- (CGFloat)getFirstBaseline {
    return [self getBaselineByIndex:0] * TMMComposeCoreDeviceDensity();
}

/// 获取最后一行文本的baseline
- (CGFloat)getLastBaseline {
    NSUInteger numberOfGlyphs = [_textRender numberOfGlyphs];
    return [self getBaselineByIndex:numberOfGlyphs - 1] * TMMComposeCoreDeviceDensity();
}

/// 获取index对应行的baseline
- (CGFloat)getBaselineByIndex:(NSInteger)index {
    return [_textRender getBaselineByIndex:index];
}

- (NSInteger)lineCount {
    // 获取文本的总行数
    return [_textRender lineCount];
}

- (void)drawInContext:(CGContextRef)ctx {
    CGRect boundsRect = CGContextGetClipBoundingBox(ctx);
    if (_bgColor) {
        CGContextSetFillColorWithColor(ctx, _bgColor.CGColor);
        CGContextFillRect(ctx, boundsRect);
    }
    [_textRender drawInContext:ctx bounds:boundsRect];
}

/// 获取坐标对应的字符所在索引位置
- (NSInteger)getOffsetForPositionX:(float)x y:(float)y {
    return [_textRender getOffsetForPositionX:x y:y];
}

/// 判断第lineIndex行是否被截断
- (BOOL)isLineEllipsized:(int)lineIndex {
    return [_textRender isLineEllipsized:lineIndex];
}

/// 根据字符所在的offset偏移，返回字符所在的矩形
- (CGRect)getCursorRect:(int)offset {
    return [_textRender getCursorRect:offset];
}

/// 根据字符所在的offset偏移（即第offset个字符），返回字符所在的行
- (int)getLineForOffset:(int)offset {
    return [_textRender getLineForOffset:offset];
}

/// 返回lineIndex所在行的高度
- (float)getLineHeight:(int)lineIndex {
    return [_textRender getLineHeight:lineIndex];
}

/// 返回lineIndex所在行的宽度
- (float)getLineWidth:(int)lineIndex {
    return [_textRender getLineWidth:lineIndex];
}

/// 返回lineIndex所在行的Bottom坐标
- (float)getLineBottom:(int)lineIndex {
    return [_textRender getLineBottom:lineIndex];
}

/// 返回lineIndex所在行的Left坐标
- (float)getLineLeft:(int)lineIndex {
    return [_textRender getLineLeft:lineIndex];
}

/// 返回lineIndex所在行的Right坐标
- (float)getLineRight:(int)lineIndex {
    return [_textRender getLineRight:lineIndex];
}

/// 返回lineIndex所在行的Top坐标
- (float)getLineTop:(int)lineIndex {
    return [_textRender getLineTop:lineIndex];
}

/// 返回lineIndex所在行的第一个光标所在的Index
- (NSUInteger)getLineStart:(int)lineIndex {
    return [_textRender getLineStart:lineIndex];
}

/// 返回lineIndex所在行的最后一个光标所在的Index
- (NSUInteger)getLineEnd:(int)lineIndex visibleEnd:(BOOL)visibleEnd {
    return [_textRender getLineEnd:lineIndex visibleEnd:visibleEnd];
}

/// 根据字符所在的offset偏移找到该字符单词的范围
- (NSRange)getWordBoundary:(int)offset {
    return [_textRender getWordBoundary:offset];
}

///  根据字符所在的start和end，返回所占行的Rect数组
- (NSArray<NSValue *> *)getRectsForRange:(int)start end:(int)end {
    return [_textRender getRectsForRange:start end:end];
}

@end
