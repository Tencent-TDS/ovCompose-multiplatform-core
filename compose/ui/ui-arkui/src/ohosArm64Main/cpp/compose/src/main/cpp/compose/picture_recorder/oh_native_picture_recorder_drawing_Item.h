#ifndef OH_NATIVE_PICTURE_RECORDER_DRAWING_ITEM_H
#define OH_NATIVE_PICTURE_RECORDER_DRAWING_ITEM_H

#include "../constants/oh_native_enums.h"
#include <stdint.h>

namespace OH {

// 描述一个绘制指令
struct DrawingItem {
    uint64_t itemHash = 0;
    uint64_t contentsHash = 0;
    int clipIndex = 0;
    OH_Native_Drawing_Type drawingType = OH_Native_Drawing_Type::DrawingTypeNone;

    static DrawingItem DrawingPopItem;
};

} // namespace OH

#endif // OH_NATIVE_PICTURE_RECORDER_DRAWING_ITEM_H
