#ifndef OH_NATIVE_IMAGE_SHADER_H
#define OH_NATIVE_IMAGE_SHADER_H

#include "native_drawing/drawing_shader_effect.h"
#include "oh_native_basic_shader.h"

namespace OH {
class NativeImageShader final : public NativeBasicShader {
public:
    NativeImageShader();
    ~NativeImageShader() override;

    uint64_t propertyHash() override;
    OH_Native_Shader_Type getType() override;
    NativeImageShader *setTileMode(OH_Drawing_TileMode tileModeX, OH_Drawing_TileMode tileModeY);

    OH_Drawing_TileMode tileModeX;
    OH_Drawing_TileMode tileModeY;
    OH_Drawing_Image *image;
};
} // namespace OH
#endif