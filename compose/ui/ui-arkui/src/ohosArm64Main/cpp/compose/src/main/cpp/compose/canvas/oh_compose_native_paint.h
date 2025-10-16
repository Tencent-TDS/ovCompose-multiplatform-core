#ifndef ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHCOMPOSENATIVEPATIN_H
#define ANDROIDX_COMPOSE_UI_ARKUI_UTILS_OHCOMPOSENATIVEPATIN_H

#include "cstdint"
#include "../constants/oh_native_enums.h"
#include "native_drawing/drawing_types.h"
#include "../shader/oh_native_basic_shader.h"


namespace androidx::compose::ui::arkui::utils {
class OHComposeNativePaint {
public:
    OHComposeNativePaint();
    ~OHComposeNativePaint();
    // kt侧设置的 alpha
    float alpha;

    // kt侧设置的 color
    uint32_t color;

    // kt侧设置的 blendMode
    OH_Drawing_BlendMode blendMode;

    // kt侧设置的 paintingStyle
    OH_Native_Draw_PaintingStyle style;

    // kt侧设置的 strokeWidth
    float strokeWidth;

    // kt侧设置的 strokeCap
    OH_Native_Draw_StrokeCap strokeCap;

    // kt侧设置的 strokeJoin
    OH_Native_Draw_StrokeJoin strokeJoin;

    // kt侧设置的 strokeMiterLimit
    float strokeMiterLimit;

    // kt侧设置的 filterQuality
    OH_Native_Draw_FilterQuality filterQuality;

    // kt侧设置的，是否开启抗锯齿
    bool isAntiAlias;

    // kt侧设置的 shader
    OH::NativeBasicShader* shader;

    // kt侧设置的 pathEffect
    OH_Drawing_PathEffect* pathEffect;

    // kt侧设置的 颜色 filter，通常是 OHGaussianBlurFilter
    OH_Drawing_ColorFilter* colorFilter;
};
} // namespace androidx::compose::ui::arkui::utils

#endif