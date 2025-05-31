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

#import "TMMComposeTextSpanAttributes.h"
#import "TMMDrawUtils.h"
#import "TMMNativeEnums.h"
#import <Foundation/Foundation.h>

/// Kotlin侧Color.Unspecified对应的值
static const NSUInteger gUnspecifiedColor = 1 << 4;

@implementation TMMComposeTextSpanAttributes

- (instancetype _Nonnull)initWithStart:(int)start
                                   end:(int)end
                              fontSize:(int)fontSize
                            fontWeight:(UIFontWeight)fontWeight
                           letterSpace:(float)letterSpace
                            fontFamily:(NSString *_Nullable)fontFamily
                       foregroundColor:(uint64_t)foregroundColor
                       backgroundColor:(uint64_t)backgroundColor
                            italicType:(TMMNativeItalicType)italicType
                         textDecorator:(TMMNativeTextDecorator)textDecorator {
    self = [super init];
    if (self) {
        self.start = start;
        self.end = end;
        self.fontSize = fontSize;
        self.fontWeight = fontWeight;
        self.letterSpace = letterSpace;
        self.fontFamily = fontFamily;
        self.foregroundColor = foregroundColor;
        self.backgroundColor = backgroundColor;
        self.italicType = italicType;
        self.textDecorator = textDecorator;
    }
    return self;
}

- (void)processSpanTextAttributes:(NSMutableAttributedString *_Nonnull)attributeString
                           uiFont:(UIFont *_Nonnull)uiFont
                           italic:(TMMNativeItalicType)parentItalic {
    if (_end <= _start) {
        return;
    }
    // 创建富文本属性, 优先应用TMMComposeTextStyle的属性
    NSRange range = NSMakeRange(_start, (_end - _start));
    if (_fontSize > 0 || _fontWeight > 0 || _italicType != TMMNativeItalicNone) {
        UIFontDescriptor *descriptor = [uiFont fontDescriptor];
        // 更新富文本的Weight
        if (_fontWeight > 0) {
            NSDictionary<UIFontDescriptorAttributeName, id> *additional =
                @{ UIFontDescriptorTraitsAttribute : @ { UIFontWeightTrait : @(_fontWeight) } };
            descriptor = [descriptor fontDescriptorByAddingAttributes:additional];
        }
        // 更新富文本的UIFont
        UIFont *otherFont = [UIFont fontWithDescriptor:descriptor size:_fontSize];
        if (_italicType != parentItalic) {
            // 如果这段文本的斜体与整体的文本斜体不同，则转换矩阵
            if (_italicType == TMMNativeItalicNormal) {
                CGAffineTransform matrix = TEXT_MATRIX_NORMAL;
                UIFontDescriptor *desc = [otherFont.fontDescriptor fontDescriptorWithMatrix:matrix];
                otherFont = [UIFont fontWithDescriptor:desc size:_fontSize];
            } else if (_italicType == TMMNativeItalicSpecific) {
                CGAffineTransform matrix = TEXT_MATRIX_ITALIC;
                UIFontDescriptor *desc = [otherFont.fontDescriptor fontDescriptorWithMatrix:matrix];
                otherFont = [UIFont fontWithDescriptor:desc size:_fontSize];
            }
        }
        [attributeString addAttribute:NSFontAttributeName value:otherFont range:range];
    }
    if (_letterSpace > 0) {
        // 更新letterSpace
        [attributeString addAttribute:NSKernAttributeName value:@(_letterSpace) range:range];
    }
    if (_textDecorator != TMMNativeTextDecoratorNone) {
        // 更新下划线与删除线
        bool isUnderLine = (_textDecorator == TMMNativeTextDecoratorUnderLine);
        [attributeString addAttribute:NSUnderlineStyleAttributeName
                                value:isUnderLine ? @(NSUnderlineStyleSingle) : @(NSUnderlineStyleNone)
                                range:range];
        bool isLineThrougn = (_textDecorator == TMMNativeTextDecoratorLineThrough);
        [attributeString addAttribute:NSStrikethroughStyleAttributeName
                                value:isLineThrougn ? @(NSUnderlineStyleSingle) : @(NSUnderlineStyleNone)
                                range:range];
    }
}

- (void)processSpanTextColor:(NSTextStorage *_Nonnull)storage {
    if (_foregroundColor != gUnspecifiedColor) {
        // 更新文本颜色
        UIColor *uiColor = TMMComposeCoreMakeUIColorFromULong(_foregroundColor);
        [storage addAttribute:NSForegroundColorAttributeName value:uiColor range:NSMakeRange(_start, (_end - _start))];
    }
    if (_backgroundColor != gUnspecifiedColor) {
        // 更新背景颜色
        UIColor *uiColor = TMMComposeCoreMakeUIColorFromULong(_backgroundColor);
        [storage addAttribute:NSBackgroundColorAttributeName value:uiColor range:NSMakeRange(_start, (_end - _start))];
    }
}

- (NSUInteger)hash {
    const float floats[12] = {
        _start, _end, _fontSize, _fontWeight, _letterSpace, _fontFamily.hash, _foregroundColor, _backgroundColor, _italicType, _textDecorator,
    };
    return TMMFNVHash(floats, sizeof(floats));
}

@end
