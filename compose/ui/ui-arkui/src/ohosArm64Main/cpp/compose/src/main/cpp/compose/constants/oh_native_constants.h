#ifndef OH_NATIVE_CONSTANTS_H
#define OH_NATIVE_CONSTANTS_H

#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
#ifdef NDEBUG
#define OH_ALWAYS_INLINE inline
#else
#define OH_ALWAYS_INLINE inline
#endif

typedef struct OHNativeCanvasProxy *OHNativeCanvasProxy_Handle;
typedef struct OHComposeNativePaint *OHComposeNativePaint_Handle;
typedef struct OH_Drawing_ShaderEffect *OH_Drawing_ShaderEffect_Handle;
typedef struct OH_Drawing_ColorFilter *OH_Drawing_ColorFilter_Handle;
typedef struct OH_Drawing_PathEffect *OH_Drawing_PathEffect_Handle;
typedef struct BaseRenderNode *BaseRenderNode_Handle;
typedef struct ParagraphHandle *ParagraphHandle_Handle;
typedef struct NativeBasicShader *NativeBasicShader_Handle;
typedef struct OHComposeNativeColorFilter *OHComposeNativeColorFilter_Handle;
typedef struct SpanStyleRange *SpanStyleRange_Handle;
typedef struct OH_Drawing_Path *OH_Drawing_Path_Handle;

const uint32_t CLEAR_COLOR = 0x00000000;

// The count for drawing types
const int OH_Native_Drawing_Type_Count = 30;

#ifdef __cplusplus
}
#endif
/** @} */
#endif
