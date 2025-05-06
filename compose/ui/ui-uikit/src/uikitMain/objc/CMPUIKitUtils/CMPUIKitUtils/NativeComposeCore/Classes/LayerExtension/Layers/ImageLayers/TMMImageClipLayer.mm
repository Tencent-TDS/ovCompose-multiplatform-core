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

#import "TMMImageClipLayer.h"
#import "TMMFastTintColorImageContentLayer.h"
#import "TMMNativeBaseLayer.h"

#pragma mark - TMMImageClipLayer
///   图片裁剪 Layer
@interface TMMImageClipLayer ()

/// 染色的 layer
@property (nonatomic, strong) TMMFastTintColorImageContentLayer *tintContentLayer;

/// 用于裁剪的 mask
@property (nonatomic, strong) TMMCANoAnimationShapeLayer *maskLayer;

@end

@implementation TMMImageClipLayer

- (instancetype)init {
    self = [super init];
    if (self) {
        self.position = CGPointZero;
        self.anchorPoint = CGPointZero;
    }
    return self;
}

- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)clearContents {
    [super clearContents];
    [self removeTintContentLayerIfNeeded];
}

- (void)removeTintContentLayerIfNeeded {
    if (_tintContentLayer.superlayer == self) {
        [_tintContentLayer removeFromSuperlayer];
    }
}

#pragma mark - public
- (void)setImage:(CGImageRef)imagePointer
       srcOffset:(const CGPoint)srcOffset
         srcSize:(const CGSize)srcSize
       dstOffset:(const CGPoint)dstOffset
         dstSize:(const CGSize)dstSize
         density:(CGFloat)density
       tintColor:(nonnull UIColor *)tintColor {
    if (srcSize.width <= 0 || srcSize.height <= 0 || imagePointer == NULL) {
        [self clearContents];
        return;
    }

    if (!tintColor) {
        [self removeTintContentLayerIfNeeded];
        [self setImage:imagePointer srcOffset:srcOffset srcSize:srcSize dstOffset:dstOffset dstSize:dstSize density:density];
    } else {
        [self setImageWillBeClipWithDensity:density
                                    dstSize:dstSize
                               imagePointer:imagePointer
                                  srcOffset:srcOffset
                                    srcSize:srcSize
                                  tintColor:tintColor];
    }
}

#pragma mark - private
- (void)setImage:(CGImageRef)imagePointer
       srcOffset:(const CGPoint &)srcOffset
         srcSize:(const CGSize &)srcSize
       dstOffset:(const CGPoint &)dstOffset
         dstSize:(const CGSize &)dstSize
         density:(CGFloat)density {
    [self targetLayerSetImage:self density:density dstSize:dstSize imagePointer:imagePointer srcOffset:srcOffset srcSize:srcSize tintColor:nil];
}

- (void)setImageWillBeClipWithDensity:(CGFloat)density
                              dstSize:(const CGSize &)dstSize
                         imagePointer:(CGImageRef)imagePointer
                            srcOffset:(const CGPoint &)srcOffset
                              srcSize:(const CGSize &)srcSize
                            tintColor:(UIColor *_Nonnull)tintColor {
    TMMFastTintColorImageContentLayer *tintContentLayer = self.tintContentLayer;
    if (tintContentLayer.superlayer != self) {
        [self addSublayer:tintContentLayer];
    }

    [self targetLayerSetImage:tintContentLayer
                      density:density
                      dstSize:dstSize
                 imagePointer:imagePointer
                    srcOffset:srcOffset
                      srcSize:srcSize
                    tintColor:tintColor];
}

/// 对 targetLayer 进行图片设置，会根据传入的参数进行缩放
- (void)targetLayerSetImage:(TMMFastTintColorImageContentLayer *)targetLayer
                    density:(CGFloat)density
                    dstSize:(const CGSize &)dstSize
               imagePointer:(CGImageRef)imagePointer
                  srcOffset:(const CGPoint &)srcOffset
                    srcSize:(const CGSize &)srcSize
                  tintColor:(UIColor *_Nullable)tintColor {
    // 1. 设置图片
    const size_t imageWidth = CGImageGetWidth(imagePointer);
    const size_t imageHeight = CGImageGetHeight(imagePointer);
    [targetLayer setImage:imagePointer tintColor:tintColor];

    // 2. 根据 srcOffset 和 srcSize 从原图上进行 clip，也就是将自己根据 maskLayer 进行裁剪
    UIBezierPath *path = [UIBezierPath bezierPathWithRect:CGRectMake(srcOffset.x, srcOffset.y, srcSize.width, srcSize.height)];
    self.maskLayer.path = [path CGPath];
    if (!self.mask) {
        self.mask = self.maskLayer;
    }

    // 3. 先设置自身大小为图片大小
    const CGRect frame = CGRectMake(0, 0, imageWidth, imageHeight);
    if (!CGRectEqualToRect(self.frame, frame)) {
        self.frame = frame;
        targetLayer.frame = frame;
        self.maskLayer.frame = frame;
    }

    // 4. 再缩放到 dst 的大小
    const float sx = (dstSize.width / srcSize.width) / density;
    const float sy = (dstSize.height / srcSize.height) / density;
    self.transform = CATransform3DScale(CATransform3DIdentity, sx, sy, 1);
}

#pragma mark - getter
- (TMMCANoAnimationShapeLayer *)maskLayer {
    if (_maskLayer) {
        return _maskLayer;
    }
    _maskLayer = [TMMCANoAnimationShapeLayer layer];
    return _maskLayer;
}

- (TMMFastTintColorImageContentLayer *)tintContentLayer {
    if (_tintContentLayer) {
        return _tintContentLayer;
    }
    _tintContentLayer = [TMMFastTintColorImageContentLayer layer];
    return _tintContentLayer;
}

- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : @"ImageClip",
    };
}

@end
