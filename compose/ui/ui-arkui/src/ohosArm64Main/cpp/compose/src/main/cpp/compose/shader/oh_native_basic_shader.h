#ifndef OH_NATIVE_BASIC_SHADER_H
#define OH_NATIVE_BASIC_SHADER_H
#define NATIVE_EXPORT __attribute__((visibility("default")))

#include <cstdint>

namespace OH {
    // shader 基类，由子类实现，并在kt侧完成初始化
    class NATIVE_EXPORT NativeBasicShader {
    public:
        virtual ~NativeBasicShader() = default;
        virtual uint64_t propertyHash() = 0;
    };
}
#endif