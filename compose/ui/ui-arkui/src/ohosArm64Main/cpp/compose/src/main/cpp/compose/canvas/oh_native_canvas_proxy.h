#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXY_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHNATIVECANVASPROXY_H

#include <native_drawing/drawing_canvas.h>
#include "../paragraph/oh_native_paragraph_builder.h"
#include "../picture_recorder/oh_native_picture_recorder.h"
#include "../render_node/oh_base_render_node.h"
#include "../paint/oh_compose_native_paint.h"

namespace androidx::compose::ui::arkui::utils {
class OHNativeCanvasProxy {
public:
    explicit OHNativeCanvasProxy(OH::BaseRenderNode *rootNode);
    ~OHNativeCanvasProxy();
    static OH::OHComposeNativePaint *Paint();
    OH::BaseRenderNode *getRenderNode() const;
    void beginDraw();
    void attachToRootView();
    void setParent(const OHNativeCanvasProxy *canvasParentProxy);
    void finishDraw();
    void save();
    void restore();
    void translate(float dx, float dy);
    void scale(float sx, float sy);
    void rotate(float degrees);
    void skew(float sx, float sy);
    void concat(const float *matrix16);
    void setClipToBounds(bool clipToBounds);
    void setPosition(int32_t x, int32_t y);
    void setSize(int32_t width, int32_t height);
    void setBounds(int32_t originX, int32_t originY, int32_t boundsWidth, int32_t boundsHeight);
    void setPivot(float px, float py);
    void setOpacity(float opacity);
    void applyTransformMatrix(float rotationX, float rotationY, float rotationZ, float scaleX, float scaleY,
                              float translateX, float translateY, double transformM34);

    void drawRect(float left, float top, float right, float bottom, OH::OHComposeNativePaint *paint);
    void drawRoundRect(float left, float top, float right, float bottom, float radiusX, float radiusY,
                       OH::OHComposeNativePaint *paint);
    void drawLine(float x1, float y1, float x2, float y2, OH::OHComposeNativePaint *paint);
    void drawCircle(float centerX, float centerY, float radius, OH::OHComposeNativePaint *paint);
    void drawOval(float left, float top, float right, float bottom, OH::OHComposeNativePaint *paint);
    void drawArc(float left, float top, float right, float bottom, float startAngle, float sweepAngle,
                 bool useCenter, OH::OHComposeNativePaint *paint);
    void drawPath(OH_Drawing_Path *path, OH::OHComposeNativePaint *paint);
    void drawImageRect(void *pixelMap, int32_t srcX, int32_t srcY, int32_t srcWidth, int32_t srcHeight,
                       int32_t dstX, int32_t dstY, int32_t dstWidth, int32_t dstHeight, OH::OHComposeNativePaint *paint);
    void drawPoints(OH_Drawing_PointMode pointMode, const float *points, size_t pointCount, OH::OHComposeNativePaint *paint);
    void drawLayer(OH::BaseRenderNode *renderNode);
    void drawParagraph(OH::Paragraph *paragraph);
    void clipRect(float left, float top, float right, float bottom, OH_Native_Draw_ClipOp clipOp);
    void clipPath(OH_Drawing_Path *path, OH_Native_Draw_ClipOp clipOp);
    void clipRoundRect(float left, float top, float right, float bottom,
                       float topLeftRadiusX, float topLeftRadiusY,
                       float topRightRadiusX, float topRightRadiusY,
                       float bottomRightRadiusX, float bottomRightRadiusY,
                       float bottomLeftRadiusX, float bottomLeftRadiusY,
                       OH_Native_Draw_ClipOp clipOp);
    void clearClip();
    void saveLayer(float left, float top, float right, float bottom, OH::OHComposeNativePaint *paint);
    void enableZ();
    void disableZ();
    void drawLayerWithSubproxy(const OHNativeCanvasProxy *subProxy);
    void markSelfAsNodeGroup() const;
    void removeCanvasNodeFromParent() const;

    void setShadowWithElevation(float shadowElevation, float shadowRadius, float shadowColorRed,
                                float shadowColorGreen, float shadowColorBlue, float shadowColorAlpha);
    void clearShadow();

    void drawTextPixelMap(void *pixelMapNative, int32_t cacheKey, int32_t width, int32_t height);
    void drawTextPixelMapWithPtr(void *pixelMapPtr, int32_t width, int32_t height);
    bool needRedrawImageWithHashCode(int32_t hashCode, int32_t width, int32_t height);
    void asyncDrawIntoCanvas(std::function<int64_t()> globalTask, int32_t paragraphHashCode, int32_t width, int32_t height,
                             std::function<void(void *, int64_t)> onMainThreadUpdate);
    int64_t imageFromImageBitmap(void *pixelMapNative, int32_t paragraphHashCode);

private:
    OH::BaseRenderNode *rootNode_;
    std::unique_ptr<OH::BaseRenderNode> canvasNode_;
    OH::PictureRecorder _pictureRecorder;
};
} // namespace androidx::compose::ui::arkui::utils

#endif