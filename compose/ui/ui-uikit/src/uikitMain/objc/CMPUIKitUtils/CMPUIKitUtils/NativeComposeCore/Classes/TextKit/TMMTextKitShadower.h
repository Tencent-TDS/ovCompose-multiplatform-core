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

NS_ASSUME_NONNULL_BEGIN

/// TextKit 阴影 处理器
@interface TMMTextKitShadower : NSObject

/// 初始化一个 TMMTextKitShadower
- (instancetype)initWithShadowOffset:(CGSize)shadowOffset
                         shadowColor:(UIColor *)shadowColor
                       shadowOpacity:(CGFloat)shadowOpacity
                        shadowRadius:(CGFloat)shadowRadius;

/**
 * @abstract The offset from the top-left corner at which the shadow starts.
 * @discussion A positive width will move the shadow to the right.
 *             A positive height will move the shadow downwards.
 */
@property (nonatomic, readonly, assign) CGSize shadowOffset;

//! CGColor in which the shadow is drawn
@property (nonatomic, readonly, strong) UIColor *shadowColor;

//! Alpha of the shadow
@property (nonatomic, readonly, assign) CGFloat shadowOpacity;

//! Radius, in pixels
@property (nonatomic, readonly, assign) CGFloat shadowRadius;

/**
 * @abstract The edge insets which represent shadow padding
 * @discussion Each edge inset is less than or equal to zero.
 *
 * Example:
 *  CGRect boundsWithoutShadowPadding; // Large enough to fit text, not large enough to fit the shadow as well
 *  UIEdgeInsets shadowPadding = [shadower shadowPadding];
 *  CGRect boundsWithShadowPadding = UIEdgeInsetsRect(boundsWithoutShadowPadding, shadowPadding);
 */
- (UIEdgeInsets)shadowPadding;

/// Calculates the size after applying shadow insets
/// @param constrainedSize The original size to be inset
/// @return The size after applying shadow padding
- (CGSize)insetSizeWithConstrainedSize:(CGSize)constrainedSize;

/// Calculates the rect after applying shadow insets
/// @param constrainedRect The original rect to be inset
/// @return The rect after applying shadow padding
- (CGRect)insetRectWithConstrainedRect:(CGRect)constrainedRect;

/// Reverses the inset operation to get the original size before shadow padding
/// @param insetSize The inset size to be outset
/// @return The original size before shadow padding was applied
- (CGSize)outsetSizeWithInsetSize:(CGSize)insetSize;

/// Reverses the inset operation to get the original rect before shadow padding
/// @param insetRect The inset rect to be outset
/// @return The original rect before shadow padding was applied
- (CGRect)outsetRectWithInsetRect:(CGRect)insetRect;

/// Adjusts a rect from internal coordinates (without shadow) to external coordinates (with shadow)
/// @param internalRect The rect in internal coordinates
/// @return The rect adjusted for shadow offset
- (CGRect)offsetRectWithInternalRect:(CGRect)internalRect;

/// Adjusts a point from internal coordinates (without shadow) to external coordinates (with shadow)
/// @param internalPoint The point in internal coordinates
/// @return The point adjusted for shadow offset
- (CGPoint)offsetPointWithInternalPoint:(CGPoint)internalPoint;

/// Adjusts a point from external coordinates (with shadow) to internal coordinates (without shadow)
/// @param externalPoint The point in external coordinates
/// @return The point reversed from shadow offset
- (CGPoint)offsetPointWithExternalPoint:(CGPoint)externalPoint;

/**
 * @abstract draws the shadow for text in the provided CGContext
 * @discussion Call within the text node's +drawRect method
 */
- (void)setShadowInContext:(CGContextRef)context;
@end

NS_ASSUME_NONNULL_END
