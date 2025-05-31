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

#import "TMMFastTintColorImageContentLayer.h"
#import "TMMNativeBaseLayer.h"

@interface TMMFastTintColorImageContentLayer ()

/// 内部承载 Image 的 layer
@property (nonatomic, strong) TMMCANoAnimationLayer *contentsLayer;

@end

@implementation TMMFastTintColorImageContentLayer

/// 重写去除 CALayer 的隐式动画 性能最好
- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)layoutSublayers {
    [super layoutSublayers];
    if (_contentsLayer) {
        if (!CGRectEqualToRect(_contentsLayer.frame, self.bounds)) {
            _contentsLayer.frame = self.bounds;
        }
    }
}

#pragma mark - public
- (void)clearContents {
    if (self.contents) {
        self.contents = nil;
    }
    if (self.mask) {
        self.mask = nil;
    }
    if (_contentsLayer) {
        [_contentsLayer removeFromSuperlayer];
    }
    self.backgroundColor = [UIColor clearColor].CGColor;
}

- (void)setImage:(CGImageRef)image tintColor:(nullable UIColor *)tintColor {
    if (!image) {
        [self clearContents];
        return;
    }

    if (!tintColor) {
        self.contents = (__bridge id _Nullable)(image);
        _contentsLayer.contents = nil;
        if (_contentsLayer.sublayers) {
            [_contentsLayer removeFromSuperlayer];
        }
        self.backgroundColor = [UIColor clearColor].CGColor;
    } else {
        CALayer *contentsLayer = self.contentsLayer;
        contentsLayer.contents = (__bridge id _Nullable)(image);
        contentsLayer.frame = self.bounds;
        if (self.mask != contentsLayer) {
            self.mask = contentsLayer;
        }
        if (self.backgroundColor != [tintColor CGColor]) {
            self.backgroundColor = [tintColor CGColor];
        }
    }
}

#pragma mark - getter
- (TMMCANoAnimationLayer *)contentsLayer {
    if (_contentsLayer) {
        return _contentsLayer;
    }
    _contentsLayer = [TMMCANoAnimationLayer layer];
    return _contentsLayer;
}

@end
