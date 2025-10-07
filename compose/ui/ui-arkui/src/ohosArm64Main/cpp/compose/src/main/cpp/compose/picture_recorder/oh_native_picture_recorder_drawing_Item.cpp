#include "oh_native_picture_recorder_drawing_Item.h"

namespace OH{
    DrawingItem DrawingItem::DrawingPopItem = {
            .itemHash = static_cast<uint64_t>(OHNativeDrawingType::Pop),
            .contentsHash = static_cast<uint64_t>(OHNativeDrawingType::Pop),
            .clipIndex = 0,
            .drawingType = OHNativeDrawingType::Pop
    };
}// namespace OH