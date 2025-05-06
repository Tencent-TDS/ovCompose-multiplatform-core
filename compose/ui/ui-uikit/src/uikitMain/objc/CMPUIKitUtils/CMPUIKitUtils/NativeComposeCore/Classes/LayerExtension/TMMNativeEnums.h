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

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

#ifdef DEBUG
#define TMM_ALWAYS_INLINE
#else
#define TMM_ALWAYS_INLINE OS_ALWAYS_INLINE
#endif

// 文本斜体和正常体的宏定义
#define TEXT_MATRIX_NORMAL CGAffineTransformMake(1, 0, 0, 1, 0, 0)
#define TEXT_MATRIX_ITALIC CGAffineTransformMake(1, 0, tanf(15 * (CGFloat)M_PI / 180), 1, 0, 0)

typedef NS_ENUM(NSInteger, TMMNativeDrawPathFillType) {
    TMMNativeDrawPathFillTypeNonZero,
    TMMNativeDrawPathFillTypeEvenOdd,
};

typedef NS_ENUM(NSInteger, TMMNativeDrawPathOperation) {
    TMMNativeDrawPathOperationDifference,
    TMMNativeDrawPathOperationIntersect,
    TMMNativeDrawPathOperationUnion,
    TMMNativeDrawPathOperationXor,
    TMMNativeDrawPathOperationReverseDifference,
};

typedef NS_ENUM(NSInteger, TMMNativeDrawClipOp) {
    TMMNativeDrawClipOpDifference,
    TMMNativeDrawClipOpIntersect,
};

typedef NS_ENUM(NSInteger, TMMNativeDrawPointMode) {
    TMMNativeDrawPointModePoints,
    TMMNativeDrawPointModeLines,
    TMMNativeDrawPointModePolygon,
};

typedef NS_ENUM(NSInteger, TMMNativeDrawBlendMode) {
    TMMNativeDrawBlendModeClear,
    TMMNativeDrawBlendModeSrc,
    TMMNativeDrawBlendModeDst,
    TMMNativeDrawBlendModeSrcOver,
    TMMNativeDrawBlendModeDstOver,
    TMMNativeDrawBlendModeSrcIn,
    TMMNativeDrawBlendModeDstIn,
    TMMNativeDrawBlendModeSrcOut,
    TMMNativeDrawBlendModeDstOut,
    TMMNativeDrawBlendModeDstAtop,
    TMMNativeDrawBlendModeSrcAtop,
    TMMNativeDrawBlendModeXor,
    TMMNativeDrawBlendModePlus,
    TMMNativeDrawBlendModeModulate,
    TMMNativeDrawBlendModeScreen,
    TMMNativeDrawBlendModeOverlay,
    TMMNativeDrawBlendModeDarken,
    TMMNativeDrawBlendModeLighten,
    TMMNativeDrawBlendModeColorDodge,
    TMMNativeDrawBlendModeColorBurn,
    TMMNativeDrawBlendModeHardlight,
    TMMNativeDrawBlendModeSoftlight,
    TMMNativeDrawBlendModeDifference,
    TMMNativeDrawBlendModeExclusion,
    TMMNativeDrawBlendModeMultiply,
    TMMNativeDrawBlendModeHue,
    TMMNativeDrawBlendModeSaturation,
    TMMNativeDrawBlendModeColor,
    TMMNativeDrawBlendModeLuminosity,
    TMMNativeDrawBlendModeUnknown,
};

typedef NS_ENUM(NSInteger, TMMNativeDrawPaintingStyle) {
    TMMNativeDrawPaintingStyleFill,
    TMMNativeDrawPaintingStyleStroke,
};

typedef NS_ENUM(NSInteger, TMMNativeDrawStrokeCap) {
    TMMNativeDrawStrokeCapButt,
    TMMNativeDrawStrokeCapRound,
    TMMNativeDrawStrokeCapSquare,
};

typedef NS_ENUM(NSInteger, TMMNativeDrawStrokeJoin) {
    // Joins between line segments form sharp corners.
    TMMNativeDrawStrokeJoinMiter,

    // Joins between line segments are semi-circular.
    TMMNativeDrawStrokeJoinRound,

    // Joins between line segments connect the corners of the butt ends of the
    // line segments to give a beveled appearance.
    TMMNativeDrawStrokeJoinBevel,
};

typedef NS_ENUM(NSInteger, TMMNativeDrawFilterQuality) {
    TMMNativeDrawFilterQualityNone,
    TMMNativeDrawFilterQualityLow,
    TMMNativeDrawFilterQualityMedium,
    TMMNativeDrawFilterQualityHigh,
};

// 参见 https://developer.android.com/develop/ui/compose/graphics/draw/brush?hl=zh-cn
typedef NS_ENUM(NSInteger, TMMNativeTileMode) {
    TMMNativeTileModeClamp,
    TMMNativeTileModeRepeated,
    TMMNativeTileModeMirror,
    TMMNativeTileModeDecal,
};

/// 注意：新增 TMMNativeDrawingType 需要增加 TMMNativeDrawingTypeCount
static const NSInteger TMMNativeDrawingTypeCount = 30;

typedef NS_ENUM(NSInteger, TMMNativeDrawingType) {
    TMMNativeDrawingTypeNone,
    TMMNativeDrawingTypeRect,
    TMMNativeDrawingTypeShaderRect,
    TMMNativeDrawingTypeLine,
    TMMNativeDrawingTypeShaderLine,
    TMMNativeDrawingTypeOval,
    TMMNativeDrawingTypeShaderOval,
    TMMNativeDrawingTypeCircle,
    TMMNativeDrawingTypeShaderCircle,
    TMMNativeDrawingTypeArc,
    TMMNativeDrawingTypeShaderArc,
    TMMNativeDrawingTypePath,
    TMMNativeDrawingTypeShaderPath,
    TMMNativeDrawingTypeImage,
    TMMNativeDrawingTypeShaderImage,
    TMMNativeDrawingTypeImageRect,
    TMMNativeDrawingTypeImageData,
    TMMNativeDrawingTypePoints,
    TMMNativeDrawingTypeShaderPoints,
    TMMNativeDrawingTypeRowPoints,
    TMMNativeDrawingTypeShaderRowPoints,
    TMMNativeDrawingTypeRowVertices,
    TMMNativeDrawingTypeShaderRowVertices,
    TMMNativeDrawingTypeDrawLayer,
    TMMNativeDrawingTypeSave,
    TMMNativeDrawingTypeRestore,
    TMMNativeDrawingTypeClip,
    TMMNativeDrawingTypePop,
};

typedef NS_ENUM(NSInteger, TMMNativeColorFilterType) {
    TMMNativeColorFilterTypeBlend,
    TMMNativeColorFilterTypeMatrix,
    TMMNativeColorFilterTypeLighting,
    TMMNativeColorFilterTypeUnknown
};

typedef NS_ENUM(NSInteger, TMMNativeItalicType) { TMMNativeItalicNone, TMMNativeItalicNormal, TMMNativeItalicSpecific };

typedef NS_ENUM(NSInteger, TMMNativeTextDecorator) { TMMNativeTextDecoratorNone, TMMNativeTextDecoratorUnderLine, TMMNativeTextDecoratorLineThrough };

NS_ASSUME_NONNULL_END
