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

#pragma mark - Path & Geometry Attributes

/// 路径填充规则 (Winding Rule)
/// 决定了在绘制路径时，哪些区域被认为是“内部”需要填充的。
typedef NS_ENUM(NSInteger, TMMNativeDrawPathFillType) {
    /// 非零环绕规则 (Non-Zero Winding Rule)
    /// 从点向外发射射线，计算与路径相交的“环绕数”。如果非0，则认为在内部。
    /// 這是大多数矢量图形的默认行为。
    TMMNativeDrawPathFillTypeNonZero,
    /// 奇偶规则 (Even-Odd Rule)
    /// 从点向外发射射线，计算与路径相交的次数。如果是奇数次，则认为在内部。
    TMMNativeDrawPathFillTypeEvenOdd,
    // 对应 Skia/Android 的 Path.FillType
};

/// 路径布尔运算操作
/// 用于组合两个路径生成新的路径。
typedef NS_ENUM(NSInteger, TMMNativeDrawPathOperation) {
    /// 差集 (A - B)：保留第一个路径中不与第二个路径重叠的部分。
    TMMNativeDrawPathOperationDifference,
    /// 交集 (A ∩ B)：保留两个路径重叠的部分。
    TMMNativeDrawPathOperationIntersect,
    /// 并集 (A ∪ B)：保留两个路径的所有区域。
    TMMNativeDrawPathOperationUnion,
    /// 异或 (A XOR B)：保留两个路径不重叠的区域（并集减去交集）。
    TMMNativeDrawPathOperationXor,
    /// 反向差集 (B - A)：保留第二个路径中不与第一个路径重叠的部分。
    TMMNativeDrawPathOperationReverseDifference,
    // 对应 SkPathOp
};

/// 裁剪操作 (Clip Operation)
/// 定义当前的裁剪区域如何与新的几何图形进行组合。
typedef NS_ENUM(NSInteger, TMMNativeDrawClipOp) {
    /// 差集：从当前裁剪区域中减去指定的区域。
    TMMNativeDrawClipOpDifference,
    /// 交集：保留当前裁剪区域与指定区域的重叠部分（默认行为）。
    TMMNativeDrawClipOpIntersect,
};

/// 点的绘制模式
/// 决定了如何解释和绘制一系列坐标点。
typedef NS_ENUM(NSInteger, TMMNativeDrawPointMode) {
    /// 画点：每个坐标绘制为一个独立的点。
    TMMNativeDrawPointModePoints,
    /// 画线段：每两个点作为一对，绘制独立的线段 (0-1, 2-3)。
    TMMNativeDrawPointModeLines,
    /// 画多边形/折线：所有点依次连接 (0-1-2-3...)。
    TMMNativeDrawPointModePolygon,
};

#pragma mark - Blend Modes

/// 图像混合模式 (Porter-Duff & Compositing)
/// 决定了源像素 (Source, 即正在绘制的内容) 如何与目标像素 (Destination, 即画布上已有的内容) 合成。
/// 对应 CoreGraphics 的 CGBlendMode 或 Skia 的 SkBlendMode。
typedef NS_ENUM(NSInteger, TMMNativeDrawBlendMode) {
    /// 清除：清除目标区域 (结果为透明)。 [0, 0]
    TMMNativeDrawBlendModeClear,
    /// 源：只保留源像素，丢弃目标像素。 [Sa, Sc]
    TMMNativeDrawBlendModeSrc,
    /// 目标：保留目标像素，丢弃源像素 (无操作)。 [Da, Dc]
    TMMNativeDrawBlendModeDst,
    /// 源覆盖 (默认)：源像素覆盖在目标像素之上。 [Sa + (1 - Sa)*Da, Rc = Sc + (1 - Sa)*Dc]
    TMMNativeDrawBlendModeSrcOver,
    /// 目标覆盖：目标像素覆盖在源像素之上（即把新内容画在旧内容下面）。
    TMMNativeDrawBlendModeDstOver,
    /// 源内嵌：在两者重叠区域显示源像素。
    TMMNativeDrawBlendModeSrcIn,
    /// 目标内嵌：在两者重叠区域显示目标像素。
    TMMNativeDrawBlendModeDstIn,
    /// 源外侧：在不重叠区域显示源像素。
    TMMNativeDrawBlendModeSrcOut,
    /// 目标外侧：在不重叠区域显示目标像素。
    TMMNativeDrawBlendModeDstOut,
    /// 目标置顶：在重叠区域显示目标像素，在不重叠区域显示源像素。
    TMMNativeDrawBlendModeDstAtop,
    /// 源置顶：在重叠区域显示源像素，在不重叠区域显示目标像素。
    TMMNativeDrawBlendModeSrcAtop,
    /// 异或：只显示不重叠的区域。
    TMMNativeDrawBlendModeXor,
    /// 加法：亮度相加。
    TMMNativeDrawBlendModePlus,
    /// 调制 (Modulate)：颜色值相乘 (r=Sr*Dr...)。
    TMMNativeDrawBlendModeModulate,
    /// 滤色 (Screen)：类似两张幻灯片投影，颜色变亮。
    TMMNativeDrawBlendModeScreen,
    /// 叠加 (Overlay)：结合 Multiply 和 Screen，取决于目标颜色。
    TMMNativeDrawBlendModeOverlay,
    /// 变暗 (Darken)：取两者中较暗的颜色值。
    TMMNativeDrawBlendModeDarken,
    /// 变亮 (Lighten)：取两者中较亮的颜色值。
    TMMNativeDrawBlendModeLighten,
    /// 颜色减淡 (ColorDodge)
    TMMNativeDrawBlendModeColorDodge,
    /// 颜色加深 (ColorBurn)
    TMMNativeDrawBlendModeColorBurn,
    /// 强光 (Hardlight)
    TMMNativeDrawBlendModeHardlight,
    /// 柔光 (Softlight)
    TMMNativeDrawBlendModeSoftlight,
    /// 差值 (Difference)：计算两者颜色的绝对差异。
    TMMNativeDrawBlendModeDifference,
    /// 排除 (Exclusion)：类似差值但对比度更低。
    TMMNativeDrawBlendModeExclusion,
    /// 正片叠底 (Multiply)：结果色总是较暗。
    TMMNativeDrawBlendModeMultiply,
    /// 色相 (Hue)：使用源的色相，目标的饱和度和亮度。
    TMMNativeDrawBlendModeHue,
    /// 饱和度 (Saturation)：使用源的饱和度，目标的色相和亮度。
    TMMNativeDrawBlendModeSaturation,
    /// 颜色 (Color)：使用源的色相和饱和度，目标的亮度。
    TMMNativeDrawBlendModeColor,
    /// 亮度 (Luminosity)：使用源的亮度，目标的色相和饱和度。
    TMMNativeDrawBlendModeLuminosity,
    /// 未知混合模式
    TMMNativeDrawBlendModeUnknown,
};

#pragma mark - Paint Attributes

/// 绘制风格
/// 决定是填充形状内部还是只绘制轮廓。
typedef NS_ENUM(NSInteger, TMMNativeDrawPaintingStyle) {
    /// 填充模式 (Fill)
    TMMNativeDrawPaintingStyleFill,
    /// 描边模式 (Stroke)
    TMMNativeDrawPaintingStyleStroke,
};

/// 线帽样式 (Stroke Cap)
/// 决定开放路径两端端点的形状。
typedef NS_ENUM(NSInteger, TMMNativeDrawStrokeCap) {
    /// 平头 (Butt)：线条在端点处直接截断，没有额外形状。
    TMMNativeDrawStrokeCapButt,
    /// 圆头 (Round)：端点处增加一个半圆。
    TMMNativeDrawStrokeCapRound,
    /// 方头 (Square)：端点处增加一个矩形（高度为线宽，长度为线宽的一半）。
    TMMNativeDrawStrokeCapSquare,
};

/// 线段连接样式 (Stroke Join)
/// 决定路径转角处的连接形状。
typedef NS_ENUM(NSInteger, TMMNativeDrawStrokeJoin) {
    /// 斜接 (Miter)
    /// 外边缘延伸直到相交，形成尖角（受 MiterLimit 限制）。
    TMMNativeDrawStrokeJoinMiter,

    /// 圆角 (Round)
    /// 转角处绘制圆弧连接。
    TMMNativeDrawStrokeJoinRound,

    /// 斜角 (Bevel)
    /// 转角处直接切平，形成斜面。
    TMMNativeDrawStrokeJoinBevel,
};

/// 图像过滤质量 (用于缩放)
/// 决定在缩放图片时使用的采样算法质量。
typedef NS_ENUM(NSInteger, TMMNativeDrawFilterQuality) {
    /// 无过滤 (最近邻插值)，速度最快，像素化明显。
    TMMNativeDrawFilterQualityNone,
    /// 低质量 (双线性插值)，平衡速度和质量。
    TMMNativeDrawFilterQualityLow,
    /// 中等质量 (可能包含 Mipmap 处理)。
    TMMNativeDrawFilterQualityMedium,
    /// 高质量 (双三次插值或其他高质量算法)，速度最慢，效果最好。
    TMMNativeDrawFilterQualityHigh,
};

/// 平铺模式 (Tile Mode)
/// 决定 Shader (如渐变或纹理) 在超出原始边界时如何填充区域。
/// 参见 https://developer.android.com/develop/ui/compose/graphics/draw/brush?hl=zh-cn
typedef NS_ENUM(NSInteger, TMMNativeTileMode) {
    /// 夹紧 (Clamp)：使用边缘的颜色填充剩余区域。
    TMMNativeTileModeClamp,
    /// 重复 (Repeat)：在水平和垂直方向重复图像/渐变。
    TMMNativeTileModeRepeated,
    /// 镜像 (Mirror)：在水平和垂直方向重复，但每次重复时翻转图像。
    TMMNativeTileModeMirror,
    /// 贴花 (Decal)：超出边界的部分渲染为透明黑 (仅渲染原始边界内的内容)。
    TMMNativeTileModeDecal,
};

#pragma mark - Drawing Commands

/// 注意：新增 TMMNativeDrawingType 需要增加 TMMNativeDrawingTypeCount
static const NSInteger TMMNativeDrawingTypeCount = 30;

/// 绘制指令类型
/// 用于标识具体的绘制操作或图层操作。
/// `Shader` 前缀通常表示该形状带有渐变或纹理填充。
typedef NS_ENUM(NSInteger, TMMNativeDrawingType) {
    /// 无操作
    TMMNativeDrawingTypeNone,
    /// 绘制矩形
    TMMNativeDrawingTypeRect,
    /// 绘制带 Shader 的矩形
    TMMNativeDrawingTypeShaderRect,
    /// 绘制线段
    TMMNativeDrawingTypeLine,
    /// 绘制带 Shader 的线段
    TMMNativeDrawingTypeShaderLine,
    /// 绘制椭圆
    TMMNativeDrawingTypeOval,
    /// 绘制带 Shader 的椭圆
    TMMNativeDrawingTypeShaderOval,
    /// 绘制圆形
    TMMNativeDrawingTypeCircle,
    /// 绘制带 Shader 的圆形
    TMMNativeDrawingTypeShaderCircle,
    /// 绘制圆弧
    TMMNativeDrawingTypeArc,
    /// 绘制带 Shader 的圆弧
    TMMNativeDrawingTypeShaderArc,
    /// 绘制自定义路径
    TMMNativeDrawingTypePath,
    /// 绘制带 Shader 的自定义路径
    TMMNativeDrawingTypeShaderPath,
    /// 绘制图像对象 (UIImage/CGImage)
    TMMNativeDrawingTypeImage,
    /// 绘制带 Shader 的图像
    TMMNativeDrawingTypeShaderImage,
    /// 绘制图像的一部分到指定区域 (SrcRect -> DstRect)
    TMMNativeDrawingTypeImageRect,
    /// 绘制原始图像数据 (Raw Data)
    TMMNativeDrawingTypeImageData,
    /// 绘制点集
    TMMNativeDrawingTypePoints,
    /// 绘制带 Shader 的点集
    TMMNativeDrawingTypeShaderPoints,
    /// 绘制点阵行 (可能用于特定优化)
    TMMNativeDrawingTypeRowPoints,
    TMMNativeDrawingTypeShaderRowPoints,
    /// 绘制顶点 (通常用于 Mesh/Triangles 绘制)
    TMMNativeDrawingTypeRowVertices,
    TMMNativeDrawingTypeShaderRowVertices,
    /// 绘制图层 (对应 saveLayer)
    TMMNativeDrawingTypeDrawLayer,
    /// 保存当前画布状态 (Matrix, Clip 等)
    TMMNativeDrawingTypeSave,
    /// 恢复上次保存的画布状态
    TMMNativeDrawingTypeRestore,
    /// 执行裁剪操作
    TMMNativeDrawingTypeClip,
    /// 弹出/结束操作
    TMMNativeDrawingTypePop,
};

#pragma mark - Color & Text Attributes

/// 颜色过滤器类型
/// 用于修改绘制对象的颜色。
typedef NS_ENUM(NSInteger, TMMNativeColorFilterType) {
    /// 混合模式过滤器 (例如：将颜色与源像素进行 Mode 混合)
    TMMNativeColorFilterTypeBlend,
    /// 颜色矩阵过滤器 (4x5 矩阵变换，常用于滤镜效果)
    TMMNativeColorFilterTypeMatrix,
    /// 光照过滤器 (模拟漫反射或镜面光照)
    TMMNativeColorFilterTypeLighting,
    /// 未知类型
    TMMNativeColorFilterTypeUnknown
};

/// 字体斜体类型
typedef NS_ENUM(NSInteger, TMMNativeItalicType) {
    /// 无斜体
    TMMNativeItalicNone,
    /// 标准斜体 (Italic / Oblique)
    TMMNativeItalicNormal,
    /// 特定斜体风格
    TMMNativeItalicSpecific
};

/// 文本装饰线类型
typedef NS_ENUM(NSInteger, TMMNativeTextDecorator) {
    /// 无装饰
    TMMNativeTextDecoratorNone,
    /// 下划线
    TMMNativeTextDecoratorUnderLine,
    /// 删除线 (中划线)
    TMMNativeTextDecoratorLineThrough
};

NS_ASSUME_NONNULL_END
