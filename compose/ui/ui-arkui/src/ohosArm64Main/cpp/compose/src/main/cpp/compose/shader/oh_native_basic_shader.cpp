
#include "oh_native_basic_shader.h"

namespace OH {
NativeBasicShader::~NativeBasicShader() {};

uint64_t NativeBasicShader::propertyHash() {
    return reinterpret_cast<uint64_t>(this);
}

OH_Native_Shader_Type NativeBasicShader::getType() { return OH_Native_Shader_Type::NoneTypeShader; };

} // namespace OH