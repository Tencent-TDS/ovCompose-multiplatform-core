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

#import "TMMTextKitShadower.h"

static inline CGSize _insetSize(CGSize size, UIEdgeInsets insets) {
    return UIEdgeInsetsInsetRect(CGRectMake(0, 0, size.width, size.height), insets).size;
}

static inline UIEdgeInsets _invertInsets(UIEdgeInsets insets) {
    return UIEdgeInsetsMake(-insets.top, -insets.left, -insets.bottom, -insets.right);
}

@implementation TMMTextKitShadower {
    UIEdgeInsets _calculatedShadowPadding;
}

- (instancetype)initWithShadowOffset:(CGSize)shadowOffset
                         shadowColor:(UIColor *)shadowColor
                       shadowOpacity:(CGFloat)shadowOpacity
                        shadowRadius:(CGFloat)shadowRadius {
    if (self = [super init]) {
        _shadowOffset = shadowOffset;
        _shadowColor = shadowColor;
        _shadowOpacity = shadowOpacity;
        _shadowRadius = shadowRadius;
        _calculatedShadowPadding = UIEdgeInsetsMake(-INFINITY, -INFINITY, INFINITY, INFINITY);
    }
    return self;
}

/*
 * This method is duplicated here because it gets called frequently, and we were
 * wasting valuable time constructing a state object to ask it.
 */
- (BOOL)_shouldDrawShadow {
    return _shadowOpacity != 0.0 && _shadowColor != nil && (_shadowRadius != 0 || !CGSizeEqualToSize(_shadowOffset, CGSizeZero));
}

- (void)setShadowInContext:(CGContextRef)context {
    if ([self _shouldDrawShadow]) {
        CGColorRef textShadowColor = CGColorRetain(_shadowColor.CGColor);
        CGSize textShadowOffset = _shadowOffset;
        CGFloat textShadowOpacity = _shadowOpacity;
        CGFloat textShadowRadius = _shadowRadius;

        if (textShadowOpacity != 1.0) {
            CGFloat inherentAlpha = CGColorGetAlpha(textShadowColor);

            CGColorRef oldTextShadowColor = textShadowColor;
            textShadowColor = CGColorCreateCopyWithAlpha(textShadowColor, inherentAlpha * textShadowOpacity);
            CGColorRelease(oldTextShadowColor);
        }

        CGContextSetShadowWithColor(context, textShadowOffset, textShadowRadius, textShadowColor);

        CGColorRelease(textShadowColor);
    }
}

- (UIEdgeInsets)shadowPadding {
    if (_calculatedShadowPadding.top == -INFINITY) {
        if (![self _shouldDrawShadow]) {
            return UIEdgeInsetsZero;
        }

        UIEdgeInsets shadowPadding = UIEdgeInsetsZero;

        // min values are expected to be negative for most typical shadowOffset and
        // blurRadius settings:
        shadowPadding.top = fminf(0.0f, _shadowOffset.height - _shadowRadius);
        shadowPadding.left = fminf(0.0f, _shadowOffset.width - _shadowRadius);

        shadowPadding.bottom = fminf(0.0f, -_shadowOffset.height - _shadowRadius);
        shadowPadding.right = fminf(0.0f, -_shadowOffset.width - _shadowRadius);

        _calculatedShadowPadding = shadowPadding;
    }

    return _calculatedShadowPadding;
}

- (CGSize)insetSizeWithConstrainedSize:(CGSize)constrainedSize {
    return _insetSize(constrainedSize, _invertInsets([self shadowPadding]));
}

- (CGRect)insetRectWithConstrainedRect:(CGRect)constrainedRect {
    return UIEdgeInsetsInsetRect(constrainedRect, _invertInsets([self shadowPadding]));
}

- (CGSize)outsetSizeWithInsetSize:(CGSize)insetSize {
    return _insetSize(insetSize, [self shadowPadding]);
}

- (CGRect)outsetRectWithInsetRect:(CGRect)insetRect {
    return UIEdgeInsetsInsetRect(insetRect, [self shadowPadding]);
}

- (CGRect)offsetRectWithInternalRect:(CGRect)internalRect {
    return (CGRect) { .origin = [self offsetPointWithInternalPoint:internalRect.origin], .size = internalRect.size };
}

- (CGPoint)offsetPointWithInternalPoint:(CGPoint)internalPoint {
    UIEdgeInsets shadowPadding = [self shadowPadding];
    return (CGPoint) { internalPoint.x + shadowPadding.left, internalPoint.y + shadowPadding.top };
}

- (CGPoint)offsetPointWithExternalPoint:(CGPoint)externalPoint {
    UIEdgeInsets shadowPadding = [self shadowPadding];
    return (CGPoint) { externalPoint.x - shadowPadding.left, externalPoint.y - shadowPadding.top };
}

@end
