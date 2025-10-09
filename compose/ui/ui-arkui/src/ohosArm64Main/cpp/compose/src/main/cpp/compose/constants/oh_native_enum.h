#ifndef OH_NATIVE_ENUM_H
#define OH_NATIVE_ENUM_H

#include <cmath>

namespace OH {

#ifdef NDEBUG
#define OH_ALWAYS_INLINE inline
#else
#define OH_ALWAYS_INLINE inline
#endif

// Enum to specify the type of RenderNode save state
    enum class RenderNodeSaveStateMakeType : int {
        SafeGuard,  // Safe guard
        Save,       // Pure save operation
        Clip        // Clip operation
    };

    enum class OHNativeDrawPathFillType : int {
        NonZero,
        EvenOdd
    };

    enum class OHNativeDrawPathOperation : int {
        Difference,
        Intersect,
        Union,
        Xor,
        ReverseDifference
    };

    enum class OHNativeDrawClipOp : int {
        Difference,
        Intersect
    };

    enum class OHNativeDrawPointMode : int {
        Points,
        Lines,
        Polygon
    };

    enum class OHNativeDrawBlendMode : int {
        Clear,
        Src,
        Dst,
        SrcOver,
        DstOver,
        SrcIn,
        DstIn,
        SrcOut,
        DstOut,
        DstAtop,
        SrcAtop,
        Xor,
        Plus,
        Modulate,
        Screen,
        Overlay,
        Darken,
        Lighten,
        ColorDodge,
        ColorBurn,
        Hardlight,
        Softlight,
        Difference,
        Exclusion,
        Multiply,
        Hue,
        Saturation,
        Color,
        Luminosity,
        Unknown
    };

    enum class OHNativeDrawPaintingStyle : int {
        Fill,
        Stroke
    };

    enum class OHNativeDrawStrokeCap : int {
        Butt,
        Round,
        Square
    };

    enum class OHNativeDrawStrokeJoin : int {
        Miter,
        Round,
        Bevel
    };

    enum class OHNativeDrawFilterQuality : int {
        None,
        Low,
        Medium,
        High
    };

    enum class OHNativeTileMode : int {
        Clamp,
        Repeated,
        Mirror,
        Decal
    };

// The count for drawing types
    constexpr int OHNativeDrawingTypeCount = 30;

    enum class OHNativeDrawingType :uint64_t {
        None,
        Rect,
        ShaderRect,
        Line,
        ShaderLine,
        Oval,
        ShaderOval,
        Circle,
        ShaderCircle,
        Arc,
        ShaderArc,
        Path,
        ShaderPath,
        Image,
        ShaderImage,
        ImageRect,
        ImageData,
        Points,
        ShaderPoints,
        RowPoints,
        ShaderRowPoints,
        RowVertices,
        ShaderRowVertices,
        DrawLayer,
        Save,
        Restore,
        Clip,
        Pop
    };

    enum class OHNativeColorFilterType : int {
        Blend,
        Matrix,
        Lighting,
        Unknown
    };

    enum class OHNativeItalicType : int {
        None,
        Normal,
        Specific
    };

    enum class OHNativeTextDecorator : int {
        None,
        UnderLine,
        LineThrough
    };

} // namespace OH

#endif
