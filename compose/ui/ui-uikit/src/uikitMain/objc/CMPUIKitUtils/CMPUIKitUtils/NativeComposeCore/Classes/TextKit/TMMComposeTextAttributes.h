/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights
 * reserved.
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
#import "TMMNativeEnums.h"
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/// 保存 Compose 的 TextStyle 以及其他属性
@interface TMMComposeTextAttributes : NSObject

/// 字体大小
@property (nonatomic, assign) int fontSize;

/// 对齐方式
@property (nonatomic, assign) NSTextAlignment align;

/// 字重
@property (nonatomic, assign) UIFontWeight fontWeight;

/// 行高
@property (nonatomic, assign) float lineHeight;

/// 字间距
@property (nonatomic, assign) float letterSpace;

/// 字体颜色
@property (nonatomic, assign) uint64_t foregroundColor;

/// 背景颜色
@property (nonatomic, assign) uint64_t backgroundColor;

/// 阴影偏移
@property (nonatomic, assign) CGSize shadowOffset;

/// 阴影颜色
@property (nonatomic, assign) uint64_t shadowColor;

/// 阴影透明度
@property (nonatomic, assign) CGFloat shadowOpacity;

/// 阴影圆角
@property (nonatomic, assign) CGFloat shadowRadius;

/// 文本
@property (nonatomic, copy) NSString *content;

/// 字体倾斜类型
@property (nonatomic, assign) TMMNativeItalicType italicType;

/// 文本装饰
@property (nonatomic, assign) TMMNativeTextDecorator textDecorator;

/// 字体名称
@property (nonatomic, nullable, copy) NSString *fontFamily;

/// kt 侧的 span 属性
@property (nonatomic, nullable, strong) NSArray<TMMComposeTextSpanAttributes *> *spanStyles;

/// 根据TMMComposeTextAttributes的属性创建UIFont对象
- (UIFont *)createUIFont;

/// 根据TMMComposeTextAttributes的属性创建ParagraphStyle对象
- (NSMutableParagraphStyle *)createParagraphStyle;

/// 获取生成的 attributeString
- (NSAttributedString *)attributeString;

/// 获取最终的阴影颜色
- (UIColor *)finalShadowColor;

/// 获取截断的attributeString
- (NSAttributedString *)truncationAttributedString;
@end

NS_ASSUME_NONNULL_END
