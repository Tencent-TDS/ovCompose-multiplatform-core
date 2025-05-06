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

#import "TMMTextKitRender.h"
#import "TMMComposeMemoryCache.h"
#import "TMMComposeTextAttributes.h"
#import "TMMDrawUtils.h"
#import "TMMTextKitContext.h"
#import "TMMTextKitShadower.h"
#import "TMMTextKitTailTruncater.h"

static NSCharacterSet *_defaultAvoidTruncationCharacterSet(void) {
    static NSCharacterSet *truncationCharacterSet;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        NSMutableCharacterSet *mutableCharacterSet = [[NSMutableCharacterSet alloc] init];
        [mutableCharacterSet formUnionWithCharacterSet:[NSCharacterSet whitespaceAndNewlineCharacterSet]];
        [mutableCharacterSet addCharactersInString:@".,!?:;"];
        truncationCharacterSet = mutableCharacterSet;
    });
    return truncationCharacterSet;
}

@implementation TMMTextKitRender {
    CGSize _calculatedSize;
}

#pragma mark - Initialization

- (instancetype)initWithTextKitAttributes:(TMMComposeTextAttributes *)textAttributes
                          constrainedSize:(const CGSize)constrainedSize
                             textHashCode:(NSNumber *)textHashCode {
    if (self = [super init]) {
        _constrainedSize = constrainedSize;
        _textHashCode = textHashCode;

        _shadower = [[TMMTextKitShadower alloc] initWithShadowOffset:textAttributes.shadowOffset
                                                         shadowColor:textAttributes.finalShadowColor
                                                       shadowOpacity:textAttributes.shadowOpacity
                                                        shadowRadius:textAttributes.shadowRadius];

        // We must inset the constrained size by the size of the shadower.
        CGSize shadowConstrainedSize = [_shadower insetSizeWithConstrainedSize:_constrainedSize];

        _context = [[TMMTextKitContext alloc] initWithAttributedString:textAttributes.attributeString constrainedSize:constrainedSize];

        _truncater = [[TMMTextKitTailTruncater alloc] initWithContext:_context
                                           truncationAttributedString:textAttributes.truncationAttributedString
                                               avoidTailTruncationSet:_defaultAvoidTruncationCharacterSet()
                                                      constrainedSize:shadowConstrainedSize];

        _textAttributes = textAttributes;
        [self _calculateSize];
    }
    return self;
}

#pragma mark - Sizing

- (void)_calculateSize {
    // Force glyph generation and layout, which may not have happened yet (and isn't triggered by
    // -usedRectForTextContainer:).
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        [layoutManager ensureLayoutForTextContainer:textContainer];
    }];

    CGRect constrainedRect = { CGPointZero, _constrainedSize };
    __block CGRect boundingRect;
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        NSRange range = [layoutManager glyphRangeForTextContainer:textContainer];
        boundingRect = [layoutManager boundingRectForGlyphRange:range inTextContainer:textContainer];
    }];
    boundingRect = CGRectIntersection(boundingRect, constrainedRect);
    _calculatedSize = boundingRect.size;
}

- (CGSize)size {
    return _calculatedSize;
}

- (void)relayoutWithMaxWidth:(float)maxWidth maxHeight:(float)maxHeight maxLine:(int)maxLines lineBreakMode:(NSLineBreakMode)lineBreakMode {
    __block CGRect boundingRect;
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        CGSize constraints = CGSizeMake(maxWidth, maxHeight);
        // 更新文本约束以及LineBreakMode、MaxLines
        textContainer.size = constraints;
        textContainer.lineBreakMode = lineBreakMode;
        textContainer.maximumNumberOfLines = maxLines;
        [layoutManager ensureLayoutForTextContainer:textContainer];
        // 获取文本宽高
        NSRange range = [layoutManager glyphRangeForTextContainer:textContainer];
        boundingRect = [layoutManager boundingRectForGlyphRange:range inTextContainer:textContainer];
        CGRect constrainedRect = { CGPointZero, constraints };
        boundingRect = CGRectIntersection(boundingRect, constrainedRect);
    }];
    _calculatedSize = boundingRect.size;
}

#pragma mark - Drawing

- (void)drawInContext:(CGContextRef)context bounds:(CGRect)bounds {
    // We add an assertion so we can track the rare conditions where a graphics context is not present
    NSAssert(context, @"This is no good without a context.");

    CGRect shadowInsetBounds = [_shadower insetRectWithConstrainedRect:bounds];

    CGContextSaveGState(context);
    //  [_shadower setShadowInContext:context];
    UIGraphicsPushContext(context);

    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        NSRange glyphRange = [layoutManager glyphRangeForBoundingRect:bounds inTextContainer:textContainer];
        [layoutManager drawBackgroundForGlyphRange:glyphRange atPoint:shadowInsetBounds.origin];
        [layoutManager drawGlyphsForGlyphRange:glyphRange atPoint:shadowInsetBounds.origin];
    }];

    UIGraphicsPopContext();
    CGContextRestoreGState(context);
}

- (void)updateTextAttribute:(NSString *)key value:(id)value {
    NSArray<TMMComposeTextSpanAttributes *> *spanStyles = self->_textAttributes.spanStyles;
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        [textStorage addAttribute:key value:value range:NSMakeRange(0, textStorage.length)];
        // 更新富文本颜色，必须要在整体颜色之后覆盖，否则不生效
        for (TMMComposeTextSpanAttributes *spanStyle in spanStyles) {
            [spanStyle processSpanTextColor:textStorage];
        }
    }];
}

- (NSUInteger)numberOfGlyphs {
    __block NSUInteger numberOfGlyphs = 0;
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        numberOfGlyphs = [layoutManager numberOfGlyphs];
    }];

    return numberOfGlyphs;
}

- (CGFloat)getBaselineByIndex:(NSInteger)index {
    __block CGFloat baseLine = 0.0f;
    if (index < 0) {
        return baseLine;
    }
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        NSUInteger numberOfGlyphs = [layoutManager numberOfGlyphs];
        NSRange lineGlyphRange;
        // 获取当前字形的行片段
        CGRect lineFragmentRect = [layoutManager lineFragmentRectForGlyphAtIndex:index effectiveRange:&lineGlyphRange];
        // 获取当前字形的基线偏移
        CGFloat baselineOffset = [layoutManager locationForGlyphAtIndex:index].y;
        baseLine = lineFragmentRect.origin.y + baselineOffset;
    }];
    return baseLine;
}

- (NSUInteger)lineCount {
    __block NSUInteger lineCount = 0;
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        NSUInteger numberOfGlyphs = [layoutManager numberOfGlyphs];
        for (NSRange lineRange = { 0, 0 }; NSMaxRange(lineRange) < numberOfGlyphs;) {
            CGRect lineRect = [layoutManager lineFragmentRectForGlyphAtIndex:NSMaxRange(lineRange) effectiveRange:&lineRange];
            if (CGRectIsEmpty(lineRect)) {
                break;
            }
            lineCount++;
        }
    }];
    return lineCount > 0 ? lineCount : 1;
}

/// 获取坐标对应的字符所在索引位置
- (NSInteger)getOffsetForPositionX:(float)x y:(float)y {
    __block NSInteger index = 0;
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        CGPoint location = CGPointMake(x, y);
        // 获取字符索引
        NSUInteger characterIndex = [layoutManager characterIndexForPoint:location
                                                          inTextContainer:textContainer
                                 fractionOfDistanceBetweenInsertionPoints:nil];
        // 检查字符索引是否有效, 无效则用默认值
        if (characterIndex < textStorage.length) {
            index = characterIndex;
        }
    }];
    return index;
}

/// 判断第lineIndex行是否被截断
- (BOOL)isLineEllipsized:(int)lineIndex {
    // 和Skia一样返回false
    return NO;
}

/// 根据字符所在的offset偏移，返回字符所在的矩形
- (CGRect)getCursorRect:(int)offset {
    __block CGRect rect = CGRectZero;
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        // 获取该字符的 glyph 范围
        NSRange glyphRange;
        NSRange actualCharacterRange;
        glyphRange = [layoutManager glyphRangeForCharacterRange:NSMakeRange(offset, 1) actualCharacterRange:&actualCharacterRange];
        // 获取该字符的矩形区域
        rect = [layoutManager boundingRectForGlyphRange:glyphRange inTextContainer:textContainer];
    }];
    return rect;
}

/// 根据字符所在的offset偏移（即第offset个字符），返回字符所在的行
- (int)getLineForOffset:(int)offset {
    __block int line = -1;
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        // 获取该字符的 glyph 范围
        NSRange glyphRange;
        [layoutManager glyphRangeForCharacterRange:NSMakeRange(offset, 1) actualCharacterRange:&glyphRange];
        // 计算行号
        NSUInteger index = 0;
        while (index <= glyphRange.location) {
            // 获取每一行的 range
            NSRange range;
            [layoutManager lineFragmentRectForGlyphAtIndex:index effectiveRange:&range];
            index = NSMaxRange(range);
            // 如果当前的Range超过Long最大值，代表已经到最后一个符号，行号不用再加
            if (index < LONG_MAX) {
                line++;
            }
        }
    }];
    return line >= 0 ? line : 0;
}

/// 返回lineIndex所在行的高度
- (float)getLineHeight:(int)lineIndex {
    return CGRectGetHeight([self getRectByLineIndex:lineIndex]);
}

/// 返回lineIndex所在行的宽度
- (float)getLineWidth:(int)lineIndex {
    return CGRectGetWidth([self getRectByLineIndex:lineIndex]);
}

/// 返回lineIndex所在行的Bottom坐标
- (float)getLineBottom:(int)lineIndex {
    return CGRectGetMaxY([self getRectByLineIndex:lineIndex]);
}

/// 返回lineIndex所在行的Left坐标
- (float)getLineLeft:(int)lineIndex {
    return CGRectGetMinX([self getRectByLineIndex:lineIndex]);
}

/// 返回lineIndex所在行的Right坐标
- (float)getLineRight:(int)lineIndex {
    return CGRectGetMaxX([self getRectByLineIndex:lineIndex]);
}

/// 返回lineIndex所在行的Top坐标
- (float)getLineTop:(int)lineIndex {
    return CGRectGetMinY([self getRectByLineIndex:lineIndex]);
}

/// 返回lineIndex所在行的第一个光标所在的Index
- (NSUInteger)getLineStart:(int)lineIndex {
    return [self characterRangeForLine:lineIndex].location;
}

/// 返回lineIndex所在行的最后一个光标所在的Index
- (NSUInteger)getLineEnd:(int)lineIndex visibleEnd:(BOOL)visibleEnd {
    return NSMaxRange([self characterRangeForLine:lineIndex]) - 1;
}

/// 根据字符所在的offset偏移找到该字符单词的范围
- (NSRange)getWordBoundary:(int)offset {
    __block NSRange range = NSMakeRange(NSNotFound, 0);
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        NSString *text = [textStorage string];
        NSUInteger length = text.length;
        if (offset < text.length) {
            // 定义包含空格和换行符的字符集
            NSCharacterSet *whitespaceAndNewlineSet = [NSCharacterSet whitespaceAndNewlineCharacterSet];
            // 从字符索引向前查找空格和换行符的起始位置
            NSUInteger startIndex = offset;
            while (startIndex > 0 && ![whitespaceAndNewlineSet characterIsMember:[text characterAtIndex:startIndex - 1]]) {
                startIndex--;
            }
            // 从字符索引向后查找空格和换行符的结束位置
            NSUInteger endIndex = offset;
            while (endIndex < length && ![whitespaceAndNewlineSet characterIsMember:[text characterAtIndex:endIndex]]) {
                endIndex++;
            }
            // 返回空格和换行符的范围
            range = NSMakeRange(startIndex, endIndex - startIndex);
        }
    }];
    return range;
}

///  根据字符所在的start和end，返回所占行的Rect数组
- (NSArray<NSValue *> *)getRectsForRange:(int)start end:(int)end {
    __block NSMutableArray<NSValue *> *rectsArray = [NSMutableArray array];
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        // 首先找到start和end对应字符所在的行
        int startLine = [self getLineForOffset:start];
        int endLine = [self getLineForOffset:end];
        // 然后根据所在行和字符偏移量找到字符所在矩形的坐标
        while (startLine <= endLine) {
            // 获取startLine行的字符索引范围
            NSRange range = [self characterRangeForLine:startLine];
            // 找到该行字符起始的位置：使用较大的索引
            NSUInteger characterStart = start > range.location ? start : range.location;
            // 找到该行字符终止的位置：使用较小的索引
            NSInteger rangeEnd = NSMaxRange(range);
            NSUInteger characterEnd = end > rangeEnd ? rangeEnd : end;
            // 找到字符对应的矩形
            NSRange characterRange = NSMakeRange(characterStart, characterEnd - characterStart);
            NSRange glyphRange = [layoutManager glyphRangeForCharacterRange:characterRange actualCharacterRange:nil];
            // 获取该 glyph 范围的矩形
            CGRect rect = [layoutManager boundingRectForGlyphRange:glyphRange inTextContainer:textContainer];
            // 将矩形添加到数组中
            [rectsArray addObject:[NSValue valueWithCGRect:rect]];
            startLine++;
        }
    }];
    return [rectsArray copy];
}

#pragma mark - 封装函数

/// 根据lineIndex找到对应行所在的矩形
- (CGRect)getRectByLineIndex:(int)lineIndex {
    __block CGRect rect;
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        NSUInteger numberOfLines = 0;
        NSUInteger glyphIndex = 0;
        NSUInteger numberOfGlyphs = layoutManager.numberOfGlyphs;
        while (glyphIndex < numberOfGlyphs) {
            // 获取每一行的 range
            NSRange lineRange;
            CGRect lineRect = [layoutManager lineFragmentRectForGlyphAtIndex:glyphIndex effectiveRange:&lineRange];
            if (numberOfLines == lineIndex) {
                rect = lineRect;
                break;
            }
            glyphIndex = NSMaxRange(lineRange);
            numberOfLines++;
        }
    }];
    return rect;
}

/// 根据lineIndex找到该行字符索引的区间
- (NSRange)characterRangeForLine:(int)lineIndex {
    __block NSRange range = NSMakeRange(NSNotFound, 0);
    [_context performBlockWithLockedTextKitComponents:^(NSLayoutManager *layoutManager, NSTextStorage *textStorage, NSTextContainer *textContainer) {
        NSUInteger numberOfLines = 0;
        NSUInteger glyphIndex = 0;
        NSUInteger numberOfGlyphs = layoutManager.numberOfGlyphs;
        while (glyphIndex < numberOfGlyphs) {
            // 获取每一行的 range
            NSRange lineRange;
            [layoutManager lineFragmentRectForGlyphAtIndex:glyphIndex effectiveRange:&lineRange];
            if (numberOfLines == lineIndex) {
                // 将 glyph range 转换为字符 range
                range = [layoutManager characterRangeForGlyphRange:lineRange actualGlyphRange:nil];
            }
            glyphIndex = NSMaxRange(lineRange);
            numberOfLines++;
        }
    }];
    return range;
}

//- (std::vector<NSRange>)visibleRanges
//{
//  return _truncater.visibleRanges;
//}

@end
