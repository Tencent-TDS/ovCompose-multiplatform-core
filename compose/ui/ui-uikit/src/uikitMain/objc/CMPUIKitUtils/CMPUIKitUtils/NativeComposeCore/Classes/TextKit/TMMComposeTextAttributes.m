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
#import "TMMDrawUtils.h"
#import "TMMNativeEnums.h"
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/// 保存Compose的TextStyle以及其他属性
@implementation TMMComposeTextAttributes : NSObject
/// 根据TMMComposeTextStyle的属性创建UIFont对象
- (UIFont *_Nonnull)createUIFont {
    UIFont *result = [self createUIFontWithFamily];
    // 处理斜体
    if (_italicType == TMMNativeItalicSpecific) {
        CGAffineTransform matrix = TEXT_MATRIX_ITALIC;
        UIFontDescriptor *desc = [result.fontDescriptor fontDescriptorWithMatrix:matrix];
        return [UIFont fontWithDescriptor:desc size:_fontSize];
    }
    return result;
}

/// 根据FontFamily创建UIFont对象
- (UIFont *_Nonnull)createUIFontWithFamily {
    if (_fontFamily == nil || _fontFamily.length == 0) {
        return [UIFont systemFontOfSize:(_fontSize) weight:_fontWeight];
    }
    UIFontDescriptor *fontDesc = [UIFontDescriptor fontDescriptorWithFontAttributes:@{
        UIFontDescriptorFamilyAttribute : _fontFamily,
        UIFontDescriptorTraitsAttribute : @ { UIFontWeightTrait : @(_fontWeight) }
    }];
    UIFont *font = [UIFont fontWithDescriptor:fontDesc size:_fontSize];
    if (font) {
        return font;
    }
    NSString *fontName = [UIFont fontNamesForFamilyName:_fontFamily].firstObject;
    if (fontName) {
        UIFontDescriptor *fontDesc = [UIFontDescriptor fontDescriptorWithFontAttributes:@{
            UIFontDescriptorFamilyAttribute : fontName,
            UIFontDescriptorTraitsAttribute : @ { UIFontWeightTrait : @(_fontWeight) }
        }];
        font = [UIFont fontWithDescriptor:fontDesc size:_fontSize];
    } else {
        if (@available(iOS 13.0, *)) {
            if ([_fontFamily.lowercaseString containsString:@"monospace"]) {
                font = [UIFont monospacedSystemFontOfSize:_fontSize weight:_fontWeight];
            } else {
                font = [UIFont systemFontOfSize:(_fontSize) weight:_fontWeight];
            }
        } else {
            font = [UIFont systemFontOfSize:(_fontSize) weight:_fontWeight];
        }
    }
    return font;
}

/// 根据TMMComposeTextAttributes的属性创建ParagraphStyle对象
- (NSMutableParagraphStyle *)createParagraphStyle {
    NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
    style.alignment = _align;
    if (_lineHeight != NAN) {
        //        style.maximumLineHeight = _lineHeight;
        style.minimumLineHeight = _lineHeight;
    }
    return style;
}

- (NSAttributedString *)attributeString {
    UIFont *uiFont = [self createUIFont];
    NSMutableDictionary *attributes = @{
        NSFontAttributeName : uiFont,
        NSParagraphStyleAttributeName : [self createParagraphStyle],
        NSKernAttributeName : @([self letterSpace]),
        NSUnderlineStyleAttributeName : (_textDecorator == TMMNativeTextDecoratorUnderLine) ? @(NSUnderlineStyleSingle) : @(NSUnderlineStyleNone),
        NSStrikethroughStyleAttributeName : (_textDecorator == TMMNativeTextDecoratorLineThrough) ? @(NSUnderlineStyleSingle)
                                                                                                  : @(NSUnderlineStyleNone),
    }
                                          .mutableCopy;

    NSMutableAttributedString *attributedString = [[NSMutableAttributedString alloc] initWithString:_content attributes:attributes];
    // 使用spanStyles中的属性覆盖掉部分属性，实现富文本
    for (TMMComposeTextSpanAttributes *spanStyle in _spanStyles) {
        [spanStyle processSpanTextAttributes:attributedString uiFont:uiFont italic:_italicType];
    }
    return attributedString;
}

- (UIColor *)finalShadowColor {
    return TMMComposeCoreMakeUIColorFromULong(_shadowColor);
}

- (NSAttributedString *)truncationAttributedString {
    return [[NSAttributedString alloc] initWithString:@"..."];
}

@end

NS_ASSUME_NONNULL_END
