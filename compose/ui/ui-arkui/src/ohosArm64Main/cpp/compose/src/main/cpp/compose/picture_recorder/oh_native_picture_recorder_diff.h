#ifndef OH_PICTURE_RECORDER_DIFF_H
#define OH_PICTURE_RECORDER_DIFF_H

#include <vector>
#include "oh_native_picture_recorder_drawing_Item.h"

namespace OH {

struct RangeIndexs {
    size_t from;
    size_t to;

    RangeIndexs(size_t f, size_t t) : from(f), to(t) {}
};

struct DiffResult {
    std::vector<size_t> deletsItems;
    std::vector<size_t> updatedItems;
    std::vector<size_t> insertItems;
    std::vector<RangeIndexs> movedItems;
};

DiffResult diffDrawCommands(const std::vector<DrawingItem>& oldArray,
                            const std::vector<DrawingItem>& newArray);

} // namespace OH

#endif // OH_PICTURE_RECORDER_DIFF_H