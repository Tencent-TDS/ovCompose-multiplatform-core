#ifndef OH_NATIVE_BASIC_SHADER_H
#define OH_NATIVE_BASIC_SHADER_H

#include "../constants/oh_native_enums.h"

#include <cstdint>

namespace OH {
/**
 * @brief Abstract base class representing a native basic shader.
 *
 * This class defines the interface for native shader implementations,
 * requiring derived classes to provide a method for computing a property hash.
 */
class NativeBasicShader {
public:
    virtual ~NativeBasicShader();
    virtual uint64_t propertyHash() = 0;
    virtual OH_Native_Shader_Type getType();
};
} // namespace OH
#endif