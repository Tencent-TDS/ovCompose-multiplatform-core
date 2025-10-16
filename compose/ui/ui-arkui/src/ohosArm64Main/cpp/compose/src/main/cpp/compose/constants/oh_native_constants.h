#ifndef OH_NATIVE_CONSTANTS_H
#define OH_NATIVE_CONSTANTS_H

#include <cmath>

namespace OH {

#ifdef NDEBUG
#define OH_ALWAYS_INLINE inline
#else
#define OH_ALWAYS_INLINE inline
#endif
constexpr uint32_t CLEAR_COLOR = 0x00000000;

// The count for drawing types
constexpr int OH_Native_Drawing_Type_Count = 30;

} // namespace OH

#endif
