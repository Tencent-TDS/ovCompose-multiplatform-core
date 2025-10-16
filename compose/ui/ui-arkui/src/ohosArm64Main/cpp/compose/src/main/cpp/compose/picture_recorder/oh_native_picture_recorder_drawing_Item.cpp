#include "oh_native_picture_recorder_drawing_Item.h"

namespace OH{
    DrawingItem DrawingItem::DrawingPopItem = {
            .itemHash = static_cast<uint64_t>(OH_Native_Drawing_Type::DrawingTypePop),
            .contentsHash = static_cast<uint64_t>(OH_Native_Drawing_Type::DrawingTypePop),
            .clipIndex = 0,
            .drawingType = OH_Native_Drawing_Type::DrawingTypePop
    };
}// namespace OH