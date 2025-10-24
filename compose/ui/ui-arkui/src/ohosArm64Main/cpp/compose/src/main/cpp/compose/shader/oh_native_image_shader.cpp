
#include <cstdint>
#include <vector>
#include "native_drawing/drawing_shader_effect.h"
#include "oh_native_image_shader.h"

namespace OH {
NativeImageShader::NativeImageShader(){};
NativeImageShader::~NativeImageShader(){};

uint64_t NativeImageShader::propertyHash() { return reinterpret_cast<uint64_t>(this); }

NativeImageShader *NativeImageShader::setTileMode(OH_Drawing_TileMode tileModeX, OH_Drawing_TileMode tileModeY) {
    this->tileModeX = tileModeX;
    this->tileModeY = tileModeY;
    return this;
}

OH_Native_Shader_Type NativeImageShader::getType() { return OH_Native_Shader_Type::ImageShader; };
} // namespace OH