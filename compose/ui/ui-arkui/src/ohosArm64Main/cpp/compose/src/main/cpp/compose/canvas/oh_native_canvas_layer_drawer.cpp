#include "oh_native_canvas_layer_drawer.h"

#include <arkui/native_render.h>
#include <arkui/native_type.h>
#include <native_drawing/drawing_path.h>
#include <native_drawing/drawing_rect.h>
#include <multimedia/image_framework/image/pixelmap_native.h>

#include "../cache/oh_native_text_image_cache.h"
#include "../constants/oh_native_constants.h"
#include "../constants/oh_native_enums.h"
#include "../render_node/oh_arc_render_node.h"
#include "../render_node/oh_async_task_render_node.h"
#include "../render_node/oh_circle_gradient_render_node.h"
#include "../render_node/oh_clip_render_node.h"
#include "../render_node/oh_image_display_render_node.h"
#include "../render_node/oh_line_gradient_render_node.h"
#include "../render_node/oh_line_render_node.h"
#include "../render_node/oh_oval_render_node.h"
#include "../render_node/oh_path_render_node.h"
#include "../render_node/oh_points_render_node.h"
#include "../render_node/oh_rect_gradient_render_node.h"
#include "../render_node/oh_roundrect_gradient_render_node.h"
#include "../render_node/oh_path_gradient_render_node.h"
#include "../shader/oh_native_image_shader.h"
#include "../shader/oh_native_linear_gradient_shader.h"
#include "../xcomponent_log.h"
#include "../trace/oh_systrace_section.h"

#include <cfloat>

namespace OH {
void OHRenderNodeDrawRect(const float left, const float top, const float right, const float bottom,
                          NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          const OH::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawRect");
    const float strokeWidth = paint->strokeWidth;
    const int32_t x = saveState->translateX + left - strokeWidth / 2;
    const int32_t y = saveState->translateY + top - strokeWidth / 2;

    const int32_t width = right - left + strokeWidth;
    const int32_t height = bottom - top + strokeWidth;

    renderNodeForDrawing->setTransform(saveState->transform.data())
        ->setPosition(x, y)
        ->setSize(width, height)
        ->setBorderCornerRadius(0);
    if (!shader) {
        if (paint->style == OH_Native_Draw_PaintingStyle::Stroke) {
            renderNodeForDrawing->setBorderWidth(strokeWidth)
                ->setBackgroundColor(CLEAR_COLOR)
                ->setBorderColor(paint->color);
        } else {
            renderNodeForDrawing->setBorderWidth(0)
                ->setBackgroundColor(paint->color);
        }
    } else {
        // apply shader
        LOGI("OHRenderNodeDrawRect: apply shader start: %{public}p", shader);
        static_cast<RectGradientRenderNode *>(renderNodeForDrawing)
            ->drawRect(left, top, right, bottom, strokeWidth, shader, paint->style, paint->colorFilter);
    }
}

void OHRenderNodeDrawClipRect(const float left, const float top, const float right, const float bottom,
                              const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing, const OH_Native_Draw_ClipOp clipOp) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawClipRect");
    ArkUI_RectShapeOption *shape = OH_ArkUI_RenderNodeUtils_CreateRectShapeOption();
    // 计算 bounds：clip 是相对于 RenderNode 的 bounds 的
    const int32_t width = static_cast<int32_t>(right - left);
    const int32_t height = static_cast<int32_t>(bottom - top);
    const int32_t x = static_cast<int32_t>(left + saveState->translateX);
    const int32_t y = static_cast<int32_t>(top + saveState->translateY);
    if (shape) {
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, 0, ARKUI_EDGE_DIRECTION_LEFT);
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, 0, ARKUI_EDGE_DIRECTION_TOP);
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, width, ARKUI_EDGE_DIRECTION_RIGHT);
        OH_ArkUI_RenderNodeUtils_SetRectShapeOptionEdgeValue(shape, height, ARKUI_EDGE_DIRECTION_BOTTOM);
    }
    ArkUI_RenderNodeClipOption *clipOption = OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromRectShape(shape);
    OH_ArkUI_RenderNodeUtils_DisposeRectShapeOption(shape);
    if (clipOption) {
        // Clip 场景（有子节点）：使用 setPosition + setSize + setTranslate 组合
        // 1. setPosition(x, y): 设置 RenderNode 在父坐标系中的绝对位置
        // 2. setSize(width, height): 设置 RenderNode 的尺寸
        //    - 对于 ClipRenderNode（使用 ContentModifier）：决定 Canvas 绘制区域
        //    - 对于子节点：提供布局尺寸参考
        // 3. setTranslate(-x, -y): 负向平移让子节点可以使用原始坐标系 (0,0)
        //    这样子节点仍然可以使用绘制时的原始坐标，不需要调整
        renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
            ->setPosition(x, y)
            ->setSize(width, height)
            ->setTranslate(-static_cast<float>(x), -static_cast<float>(y))
            ->setClip(clipOption);
    }
}

void OHRenderNodeDrawClipPath(OH_Drawing_Path_Handle path, OH_Native_Draw_ClipOp clipOp,
                              const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawClipPath");

    if (!path) {
        LOGE("OHRenderNodeDrawClipPath: path is null");
        return;
    }

    // 计算路径的 bounds 并设置 ClipRenderNode 的 position 和 size
    int32_t pathWidth = 0;  // Default fallback
    int32_t pathHeight = 0; // Default fallback

    float pathLeft = 0.0f, pathTop = 0.0f, pathRight = 0.0f, pathBottom = 0.0f;
    OH_Drawing_Rect *boundsRect = OH_Drawing_RectCreate(0.0f, 0.0f, 0.0f, 0.0f);
    if (boundsRect != nullptr) {
        OH_Drawing_PathGetBounds(reinterpret_cast<OH_Drawing_Path *>(path), boundsRect);
        pathLeft = OH_Drawing_RectGetLeft(boundsRect);
        pathTop = OH_Drawing_RectGetTop(boundsRect);
        pathRight = OH_Drawing_RectGetRight(boundsRect);
        pathBottom = OH_Drawing_RectGetBottom(boundsRect);

        // 计算 position 和 size
        pathWidth = static_cast<int32_t>(pathRight - pathLeft);
        pathHeight = static_cast<int32_t>(pathBottom - pathTop);

        // Ensure minimum size
        if (pathWidth <= 0) pathWidth = 1;
        if (pathHeight <= 0) pathHeight = 1;

        OH_Drawing_RectDestroy(boundsRect);
    }

    // 将 BaseRenderNode 转换为 ClipRenderNode
    auto *clipNode = static_cast<ClipRenderNode *>(renderNodeForDrawing);
    if (!clipNode) {
        LOGE("OHRenderNodeDrawClipPath: renderNodeForDrawing is not ClipRenderNode");
        return;
    }

    // 应用变换状态并设置 bounds
    const int32_t x = saveState->translateX + pathLeft;
    const int32_t y = saveState->translateY + pathTop;
    clipNode->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setPosition(x, y)
        ->setSize(pathWidth, pathHeight)
        ->setTranslate(-static_cast<float>(x), -static_cast<float>(y));

    // 设置裁剪路径
    clipNode->setClipPath(reinterpret_cast<OH_Drawing_Path *>(path), clipOp);

    LOGI("OHRenderNodeDrawClipPath: path=%{public}p, clipOp=%{public}d, bounds=(%f,%f,%f,%f), position=(%d,%d), size=(%d,%d), translate=(%f,%f)",
         path, clipOp, pathLeft, pathTop, pathRight, pathBottom, x, y, pathWidth, pathHeight, saveState->translateX, saveState->translateY);
}

void OHRenderNodeDrawClipRoundRect(const float left, const float top, const float right, const float bottom,
                                   const float topLeftRadiusX, const float topLeftRadiusY,
                                   const float topRightRadiusX, const float topRightRadiusY,
                                   const float bottomRightRadiusX, const float bottomRightRadiusY,
                                   const float bottomLeftRadiusX, const float bottomLeftRadiusY,
                                   const RenderNodeSaveState *saveState,
                                   BaseRenderNode *renderNodeForDrawing, const OH_Native_Draw_ClipOp clipOp) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawClipRoundRect");
    LOGI("OHRenderNodeDrawClipRoundRect: rect=(%{public}f,%{public}f,%{public}f,%{public}f), "
         "radii TL(%{public}f,%{public}f), TR(%{public}f,%{public}f), BR(%{public}f,%{public}f), BL(%{public}f,%{public}f), RenderNode: %{public}p",
         left, top, right, bottom,
         topLeftRadiusX, topLeftRadiusY,
         topRightRadiusX, topRightRadiusY,
         bottomRightRadiusX, bottomRightRadiusY,
         bottomLeftRadiusX, bottomLeftRadiusY, renderNodeForDrawing);

    // 使用 BaseRenderNode + SetClip API，类似 OHRenderNodeDrawClipRect 的实现
    ArkUI_RoundRectShapeOption *shapeOption = OH_ArkUI_RenderNodeUtils_CreateRoundRectShapeOption();
    if (shapeOption) {
        const float width = right - left;
        const float height = bottom - top;

        // 计算 bounds：clip 是相对于 RenderNode 的 bounds 的
        const int32_t boundsWidth = static_cast<int32_t>(width);
        const int32_t boundsHeight = static_cast<int32_t>(height);
        const int32_t boundsX = static_cast<int32_t>(left + saveState->translateX);
        const int32_t boundsY = static_cast<int32_t>(top + saveState->translateY);

        // 设置边缘值（相对于 RenderNode 的 bounds）
        OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionEdgeValue(shapeOption, 0, ARKUI_EDGE_DIRECTION_LEFT);
        OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionEdgeValue(shapeOption, 0, ARKUI_EDGE_DIRECTION_TOP);
        OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionEdgeValue(shapeOption, width, ARKUI_EDGE_DIRECTION_RIGHT);
        OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionEdgeValue(shapeOption, height, ARKUI_EDGE_DIRECTION_BOTTOM);

        // 设置4个角的圆角坐标
        OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionCornerXY(shapeOption, topLeftRadiusX, topLeftRadiusY, ARKUI_CORNER_DIRECTION_TOP_LEFT);
        OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionCornerXY(shapeOption, topRightRadiusX, topRightRadiusY, ARKUI_CORNER_DIRECTION_TOP_RIGHT);
        OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionCornerXY(shapeOption, bottomRightRadiusX, bottomRightRadiusY, ARKUI_CORNER_DIRECTION_BOTTOM_RIGHT);
        OH_ArkUI_RenderNodeUtils_SetRoundRectShapeOptionCornerXY(shapeOption, bottomLeftRadiusX, bottomLeftRadiusY, ARKUI_CORNER_DIRECTION_BOTTOM_LEFT);

        // 从 RoundRectShape 创建裁剪选项
        ArkUI_RenderNodeClipOption *clipOption = OH_ArkUI_RenderNodeUtils_CreateRenderNodeClipOptionFromRoundRectShape(shapeOption);
        OH_ArkUI_RenderNodeUtils_DisposeRoundRectShapeOption(shapeOption);

        if (clipOption) {
            // 在设置 clip 之前，先设置 bounds，确保 clip 基于正确的 bounds
            renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
                ->setPosition(boundsX, boundsY)
                ->setSize(width, height)
                // 负向 translate 让 Clip RenderNode 的局部坐标重置为 (0,0)
                ->setTranslate(-static_cast<float>(boundsX), -static_cast<float>(boundsY))
                ->setClip(clipOption);

            LOGI("OHRenderNodeDrawClipRoundRect: Successfully applied clip using SetClip API, bounds=(%d,%d,%d,%d)", boundsX, boundsY, boundsWidth, boundsHeight);
        } else {
            LOGE("OHRenderNodeDrawClipRoundRect: Failed to create clip option from RoundRectShape");
        }
    } else {
        LOGE("OHRenderNodeDrawClipRoundRect: Failed to create RoundRectShapeOption");
    }
}

void OHRenderNodeDrawSaveLayer(const float left, const float top, const float right, const float bottom,
                               const OHComposeNativePaint *paint, const RenderNodeSaveState *saveState,
                               BaseRenderNode *renderNodeForDrawing) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawSaveLayer");

    // 计算图层 bounds
    const int32_t x = static_cast<int32_t>(left) + saveState->translateX;
    const int32_t y = static_cast<int32_t>(top) + saveState->translateY;
    const int32_t width = static_cast<int32_t>(right - left);
    const int32_t height = static_cast<int32_t>(bottom - top);

    // 应用变换状态并设置 bounds
    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setBounds(x, y, width, height)
        ->setBackgroundColor(CLEAR_COLOR); // 设置背景色为透明

    // 应用 paint 的 opacity（如果存在）
    if (paint) {
        renderNodeForDrawing->setOpacity(paint->alpha);
    }

    // TODO: 应用 paint 的 colorFilter 和 blendMode（如果 ArkUI 支持）
    // 目前 ArkUI RenderNode 可能不支持这些属性，需要通过 ContentModifier 实现

    LOGI("OHRenderNodeDrawSaveLayer: bounds=(%f,%f,%f,%f), position=(%d,%d), size=(%d,%d), opacity=%f",
         left, top, right, bottom, x, y, width, height, paint ? paint->alpha : 1.0f);
}

void OHRenderNodeDrawRoundRect(const float left, const float top, const float right, const float bottom,
                               const float radiusX, const float radiusY, const NativeBasicShader *shader,
                               const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                               const OH::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawRoundRect");
    const float strokeWidth = paint->strokeWidth;
    const int32_t x = saveState->translateX + left - strokeWidth / 2;
    const int32_t y = saveState->translateY + top - strokeWidth / 2;

    const int32_t width = right - left + strokeWidth;
    const int32_t height = bottom - top + strokeWidth;

    const float maxRadius = std::min(width / 2, height / 2);
    const float radius = std::min(radiusX + strokeWidth / 2, maxRadius);

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setPosition(x, y)
        ->setSize(width, height);

    if (!shader) {
        renderNodeForDrawing->setBorderCornerRadius(radius);
        if (paint->style == OH_Native_Draw_PaintingStyle::Stroke) {
            renderNodeForDrawing->setBorderWidth(strokeWidth)
                ->setBackgroundColor(CLEAR_COLOR)
                ->setBorderColor(paint->color);
        } else {
            renderNodeForDrawing->setBorderWidth(0)
                ->setBackgroundColor(paint->color);
        }
    } else {
        // 使用 RoundRectGradientRenderNode 绘制带渐变的圆角矩形
        static_cast<RoundRectGradientRenderNode *>(renderNodeForDrawing)
            ->drawRoundRect(left, top, right, bottom, radius, radius, strokeWidth,
                            const_cast<NativeBasicShader *>(shader), paint->style, paint->colorFilter);
    }
}

void OHRenderNodeDrawLine(const float x1, const float y1, const float x2, const float y2, NativeBasicShader *shader,
                          const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                          const OH::OHComposeNativePaint *paint) {
    const float strokeWidth = paint->strokeWidth;
    const float halfStroke = strokeWidth / 2.0f;
    // 考虑strokeWidth，调整position和size
    const int32_t x = static_cast<int32_t>(std::min(x1, x2) - halfStroke);
    const int32_t y = static_cast<int32_t>(std::min(y1, y2) - halfStroke);
    const int32_t width = static_cast<int32_t>(abs(x2 - x1) + strokeWidth);
    const int32_t height = static_cast<int32_t>(abs(y2 - y1) + strokeWidth);

    // LineRenderNode 使用 ContentModifier，必须设置 position 和 size
    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setPosition(saveState->translateX + x, saveState->translateY + y)
        ->setSize(width, height);

    if (!shader) {
        static_cast<LineRenderNode *>(renderNodeForDrawing)
            ->drawLine(x1, y1, x2, y2, strokeWidth, paint->color, paint->strokeCap);
    } else {
        static_cast<LineGradientRenderNode *>(renderNodeForDrawing)
            ->drawLine(x1, y1, x2, y2, strokeWidth, dynamic_cast<NativeLinearGradientShader *>(shader),
                       paint->strokeCap);
    }
}

void OHRenderNodeDrawCircle(const float centerX, const float centerY, const float radius,
                            const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                            BaseRenderNode *renderNodeForDrawing,
                            const OH::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawCircle");

    const float strokeWidth = paint->strokeWidth;
    const int32_t x = saveState->translateX + centerX - radius - strokeWidth / 2;
    const int32_t y = saveState->translateY + centerY - radius - strokeWidth / 2;

    const int32_t width = 2 * radius + strokeWidth;
    const int32_t height = 2 * radius + strokeWidth;

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setBounds(x, y, width, height);

    if (!shader) {
        // 无 shader：使用 BaseRenderNode 的边框属性绘制圆形
        renderNodeForDrawing->setBorderCornerRadius(static_cast<uint32_t>(radius));
        if (paint->style == OH_Native_Draw_PaintingStyle::Stroke) {
            renderNodeForDrawing->setBorderWidth(strokeWidth)
                ->setBorderColor(paint->color)
                ->setBackgroundColor(CLEAR_COLOR);
        } else {
            renderNodeForDrawing->setBorderWidth(0)->setBackgroundColor(paint->color);
        }
    } else {
        // 有 shader：使用 CircleGradientRenderNode 绘制带渐变的圆形
        // 圆心坐标需要转换为相对于 RenderNode 左上角的坐标
        const float relCenterX = radius + strokeWidth / 2;
        const float relCenterY = radius + strokeWidth / 2;

        LOGI("OHRenderNodeDrawCircle: draw circle with shader, center=(%{public}f,%{public}f), radius=%{public}f",
             relCenterX, relCenterY, radius);

        static_cast<CircleGradientRenderNode *>(renderNodeForDrawing)
            ->drawCircle(relCenterX, relCenterY, radius, strokeWidth,
                         const_cast<NativeBasicShader *>(shader),
                         paint->style, paint->colorFilter);
    }
}

void OHRenderNodeDrawOval(const float left, const float top, const float right, const float bottom,
                          const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          const OH::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawOval");
    const float strokeWidth = paint->strokeWidth;
    const int32_t x = saveState->translateX + left - strokeWidth / 2;
    const int32_t y = saveState->translateY + top - strokeWidth / 2;
    const int32_t width = right - left + strokeWidth;
    const int32_t height = bottom - top + strokeWidth;

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setBounds(x, y, width, height);

    if (!shader) {
        static_cast<OvalRenderNode *>(renderNodeForDrawing)
            ->drawOval(left, top, right, bottom, strokeWidth, paint->color, paint->style);
    } else {
        // TODO: 实现带shader的椭圆绘制（需要创建OvalGradientRenderNode）
    }
}

void OHRenderNodeDrawArc(const float left, const float top, const float right, const float bottom,
                         const float startAngle, const float sweepAngle, const bool useCenter,
                         const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                         BaseRenderNode *renderNodeForDrawing,
                         const OH::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawArc");
    const float strokeWidth = paint->strokeWidth;
    const int32_t x = saveState->translateX + left - strokeWidth / 2;
    const int32_t y = saveState->translateY + top - strokeWidth / 2;
    const int32_t width = right - left + strokeWidth;
    const int32_t height = bottom - top + strokeWidth;

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setBounds(x, y, width, height);

    if (!shader) {
        static_cast<ArcRenderNode *>(renderNodeForDrawing)
            ->drawArc(left, top, right, bottom, startAngle, sweepAngle, useCenter, strokeWidth, paint->color,
                      paint->style);
    } else {
        // TODO: 实现带shader的圆弧绘制（需要创建ArcGradientRenderNode）
    }
}

void OHRenderNodeDrawPath(OH_Drawing_Path_Handle path, const NativeBasicShader *shader, const RenderNodeSaveState *saveState,
                          BaseRenderNode *renderNodeForDrawing,
                          const OH::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawPath");
    const float strokeWidth = paint->strokeWidth;

    // Calculate bounds from path using OH_Drawing_PathGetBounds
    int32_t x = 0;
    int32_t y = 0;
    int32_t width = 100;  // Default fallback
    int32_t height = 100; // Default fallback

    if (path != nullptr) {
        OH_Drawing_Rect *boundsRect = OH_Drawing_RectCreate(0.0f, 0.0f, 0.0f, 0.0f);
        if (boundsRect != nullptr) {
            OH_Drawing_PathGetBounds(path, boundsRect);

            // Get rect values
            float left = OH_Drawing_RectGetLeft(boundsRect);
            float top = OH_Drawing_RectGetTop(boundsRect);
            float right = OH_Drawing_RectGetRight(boundsRect);
            float bottom = OH_Drawing_RectGetBottom(boundsRect);

            // Account for stroke width
            const float halfStroke = strokeWidth / 2.0f;
            x = saveState->translateX + static_cast<int32_t>(left - halfStroke);
            y = saveState->translateY + static_cast<int32_t>(top - halfStroke);
            width = static_cast<int32_t>((right - left) + strokeWidth);
            height = static_cast<int32_t>((bottom - top) + strokeWidth);

            // Ensure minimum size
            if (width <= 0) width = 1;
            if (height <= 0) height = 1;

            OH_Drawing_RectDestroy(boundsRect);
        }
    }

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setPosition(x, y)
        ->setSize(width, height);

    if (!shader) {
        static_cast<PathRenderNode *>(renderNodeForDrawing)
            ->drawPath(path, strokeWidth, paint->color, paint->style, paint->colorFilter);
    } else {
        // 使用 PathGradientRenderNode 绘制带渐变的路径
        static_cast<PathGradientRenderNode *>(renderNodeForDrawing)
            ->drawPath(path, strokeWidth, const_cast<NativeBasicShader *>(shader), paint->style, paint->colorFilter);
    }
}

void OHRenderNodeDrawImageRect(OH_PixelmapNative *pixelMap, int32_t srcX, int32_t srcY, int32_t srcWidth,
                               int32_t srcHeight, int32_t dstX, int32_t dstY, int32_t dstWidth, int32_t dstHeight,
                               const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                               const OH::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawImageRect");
    LOGI("OHRenderNodeDrawImageRect: src=(%{public}d, %{public}d, %{public}d, %{public}d), "
         "dst=(%{public}d, %{public}d, %{public}d, %{public}d)",
         srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight);

    // ImageDisplayRenderNode 使用 ContentModifier，必须设置 position 和 size
    // ContentModifier 的 Canvas 绘制区域由 size 决定
    const int32_t posX = saveState->translateX + dstX;
    const int32_t posY = saveState->translateY + dstY;

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setPosition(posX, posY)
        ->setSize(dstWidth, dstHeight);
    auto *imageNode = static_cast<ImageDisplayRenderNode *>(renderNodeForDrawing);

    // 调用ImageDisplayRenderNode的drawImageRect方法
    imageNode->drawImageRect(pixelMap, srcX, srcY, srcWidth, srcHeight, dstX, dstY, dstWidth, dstHeight, paint->colorFilter,
                             paint->filterQuality);
}

void OHRenderNodeDrawPoints(OH_Drawing_PointMode pointMode, const float *points, size_t pointCount,
                            const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                            OH::OHComposeNativePaint *paint) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawPoints");
    LOGI("OHRenderNodeDrawPoints: pointMode=%{public}d, pointCount=%{public}zu", pointMode, pointCount);

    // 计算边界框（PointsRenderNode 内部也会计算，但我们需要先应用 translate）
    if (pointCount == 0) {
        return;
    }

    float minX = FLT_MAX, minY = FLT_MAX;
    float maxX = -FLT_MAX, maxY = -FLT_MAX;
    for (size_t i = 0; i < pointCount; ++i) {
        const float x = points[i * 2];
        const float y = points[i * 2 + 1];
        minX = std::min(minX, x);
        maxX = std::max(maxX, x);
        minY = std::min(minY, y);
        maxY = std::max(maxY, y);
    }

    const float strokeWidth = paint->strokeWidth;
    const float halfStroke = strokeWidth / 2.0f;
    const int32_t boundsX = static_cast<int32_t>(minX - halfStroke);
    const int32_t boundsY = static_cast<int32_t>(minY - halfStroke);
    const int32_t boundsWidth = static_cast<int32_t>(maxX - minX + strokeWidth);
    const int32_t boundsHeight = static_cast<int32_t>(maxY - minY + strokeWidth);

    // PointsRenderNode 使用 ContentModifier，必须设置 position 和 size
    const int32_t posX = saveState->translateX + boundsX;
    const int32_t posY = saveState->translateY + boundsY;

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setPosition(posX, posY)
        ->setSize(boundsWidth, boundsHeight);

    // 调用PointsRenderNode的drawPoints方法
    static_cast<PointsRenderNode *>(renderNodeForDrawing)
        ->drawPoints(pointMode, points, pointCount, paint);
}

void OHRenderNodeDrawText(const RenderNodeSaveState *saveState, Paragraph *paragraphNode) {
    static int frameCount = 0;
    frameCount++;

    const float originX = saveState->translateX;
    const float originY = saveState->translateY;
    const int32_t width = paragraphNode->getWidth();
    const int32_t height = paragraphNode->getHeight();

    // 保存绘制位置到 Paragraph 对象
    paragraphNode->setTransform(saveState->transform.data())
        ->setTranslate(originX, originY)
        ->setSize(width, height);
    paragraphNode->paint();
}

void OHRenderNodeDrawTextPixelMap(OH_PixelmapNative *pixelMap, int32_t cacheKey, int32_t width, int32_t height,
                                  const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawTextPixelMap");
    LOGI("OHRenderNodeDrawTextPixelMap: cacheKey=%{public}d, width=%{public}d, height=%{public}d", cacheKey, width, height);

    // 注意：不再在C++侧缓存PixelMap，因为现在由Kotlin侧的PixelMapCacheManager统一管理
    // OHNativeTextImageCache::sharedInstance().setPixelMap(cacheKey, pixelMap);

    // 设置RenderNode的transform和translate
    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setSize(width, height);

    auto *imageNode = static_cast<ImageDisplayRenderNode *>(renderNodeForDrawing);

    // 绘制完整图像（文本图像通常不需要裁剪）
    // 使用 (0, 0, width, height) 作为源矩形，目标矩形也是 (0, 0, width, height)
    imageNode->drawImageRect(pixelMap, 0, 0, width, height, 0, 0, width, height, nullptr,
                             OH_Native_Draw_FilterQuality::None);
}

void OHRenderNodeDrawTextPixelMapWithPtr(OH_PixelmapNative *pixelMap, int32_t width, int32_t height,
                                         const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawTextPixelMapWithPtr");
    LOGI("OHRenderNodeDrawTextPixelMapWithPtr: width=%{public}d, height=%{public}d", width, height);

    if (pixelMap == nullptr) {
        LOGE("OHRenderNodeDrawTextPixelMapWithPtr: pixelMap is null");
        return;
    }

    // 设置RenderNode的transform和translate
    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setSize(width, height);

    auto *imageNode = static_cast<ImageDisplayRenderNode *>(renderNodeForDrawing);

    // 绘制完整图像（使用缓存的 PixelMap）
    imageNode->drawImageRect(pixelMap, 0, 0, width, height, 0, 0, width, height, nullptr,
                             OH_Native_Draw_FilterQuality::None);
}

void OHRenderNodeDrawTextAsyncTask(std::function<int64_t()> globalTask, int32_t width, int32_t height,
                                   const RenderNodeSaveState *saveState, BaseRenderNode *renderNodeForDrawing,
                                   std::function<void(void *, int64_t)> onMainThreadUpdate) {
    OH::SystraceSection trace("LayerDrawer:OHRenderNodeDrawTextAsyncTask");
    LOGI("OHRenderNodeDrawTextAsyncTask: width=%{public}d, height=%{public}d", width, height);

    renderNodeForDrawing->setTransform(const_cast<float *>(saveState->transform.data()))
        ->setTranslate(saveState->translateX, saveState->translateY)
        ->setSize(width, height);

    // 转换为 AsyncTaskRenderNode 并提交异步任务
    auto *asyncTaskNode = static_cast<AsyncTaskRenderNode *>(renderNodeForDrawing);

    // 包装回调，传递renderNode指针和pixelMapPtr
    auto wrappedCallback = [onMainThreadUpdate, asyncTaskNode](int64_t pixelMapPtr) {
        onMainThreadUpdate(asyncTaskNode, pixelMapPtr);
    };

    asyncTaskNode->commitAsyncTask(globalTask, width, height, wrappedCallback);
}

OH_PixelmapNative *OHNativeComposeHasTextImageCache(int32_t cacheKey) {
    OH::SystraceSection trace("LayerDrawer:OHNativeComposeHasTextImageCache");
    LOGI("OHNativeComposeHasTextImageCache: cacheKey=%{public}d", cacheKey);

    OH_PixelmapNative *pixelMap = OHNativeTextImageCache::sharedInstance().getPixelMap(cacheKey);
    if (pixelMap != nullptr) {
        LOGI("OHNativeComposeHasTextImageCache: found cache for key=%{public}d", cacheKey);
        // 注意：返回的指针需要由调用者负责管理生命周期（类似 iOS 的 __bridge_retained）
        return pixelMap;
    }

    LOGI("OHNativeComposeHasTextImageCache: cache miss for key=%{public}d", cacheKey);
    return nullptr;
}

OH_PixelmapNative *OHNativeComposePixelMapFromImageBitmap(OH_PixelmapNative *pixelMapNative, int32_t cacheKey) {
    OH::SystraceSection trace("LayerDrawer:OHNativeComposePixelMapFromImageBitmap");
    LOGI("OHNativeComposePixelMapFromImageBitmap: cacheKey=%{public}d", cacheKey);

    if (pixelMapNative == nullptr) {
        LOGE("OHNativeComposePixelMapFromImageBitmap: pixelMapNative is null");
        return nullptr;
    }

    // 注意：不再在C++侧缓存PixelMap，因为现在由Kotlin侧的PixelMapCacheManager统一管理
    // OHNativeTextImageCache::sharedInstance().setPixelMap(cacheKey, pixelMapNative);

    return pixelMapNative;
}

void OHRenderNodeDrawThrow(const int32_t status) {
    if (status != ARKUI_ERROR_CODE_NO_ERROR) {
        LOGE("OHRenderNodeDraw operation failed with status: %{public}d", status);
        throw std::runtime_error("OHRenderNode operation failed");
    }
}
} // namespace OH