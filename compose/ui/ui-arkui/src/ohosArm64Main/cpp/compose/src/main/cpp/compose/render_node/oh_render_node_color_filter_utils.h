#ifndef OH_RENDER_NODE_COLOR_FILTER_UTILS_H
#define OH_RENDER_NODE_COLOR_FILTER_UTILS_H

#include <native_drawing/drawing_brush.h>
#include <native_drawing/drawing_pen.h>
#include <native_drawing/drawing_filter.h>
#include "compose/filter/oh_compose_native_color_filter.h"

namespace OH {

/**
 * @brief 应用ColorFilter到Brush
 * 
 * 参考文档：绘制复杂效果.md - 颜色滤波器效果
 * 
 * @param brush OH_Drawing_Brush指针
 * @param colorFilter OHComposeNativeColorFilter指针，可以为null
 * @return 创建的OH_Drawing_Filter指针，调用者需要负责销毁；如果colorFilter为null，返回nullptr
 */
inline OH_Drawing_Filter* ApplyColorFilterToBrush(OH_Drawing_Brush* brush, OHComposeNativeColorFilter* colorFilter) {
    if (colorFilter == nullptr || brush == nullptr) {
        return nullptr;
    }
    
    // 创建Filter对象
    OH_Drawing_Filter* filter = OH_Drawing_FilterCreate();
    if (filter == nullptr) {
        return nullptr;
    }
    
    // 为Filter对象设置ColorFilter
    OH_Drawing_FilterSetColorFilter(filter, colorFilter->getNativeFilter());
    
    // 设置Brush的滤波器效果
    OH_Drawing_BrushSetFilter(brush, filter);
    
    return filter;
}

/**
 * @brief 应用ColorFilter到Pen
 * 
 * 参考文档：绘制复杂效果.md - 颜色滤波器效果
 * 
 * @param pen OH_Drawing_Pen指针
 * @param colorFilter OHComposeNativeColorFilter指针，可以为null
 * @return 创建的OH_Drawing_Filter指针，调用者需要负责销毁；如果colorFilter为null，返回nullptr
 */
inline OH_Drawing_Filter* ApplyColorFilterToPen(OH_Drawing_Pen* pen, OHComposeNativeColorFilter* colorFilter) {
    if (colorFilter == nullptr || pen == nullptr) {
        return nullptr;
    }
    
    // 创建Filter对象
    OH_Drawing_Filter* filter = OH_Drawing_FilterCreate();
    if (filter == nullptr) {
        return nullptr;
    }
    
    // 为Filter对象设置ColorFilter
    OH_Drawing_FilterSetColorFilter(filter, colorFilter->getNativeFilter());
    
    // 设置Pen的滤波器效果
    OH_Drawing_PenSetFilter(pen, filter);
    
    return filter;
}

} // namespace OH

#endif // OH_RENDER_NODE_COLOR_FILTER_UTILS_H

