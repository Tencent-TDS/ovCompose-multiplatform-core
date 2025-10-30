#ifndef OH_NATIVE_RENDER_NODE_SAVE_STATE_H
#define OH_NATIVE_RENDER_NODE_SAVE_STATE_H

#include <string>
#include "../constants/oh_native_enums.h"
#include "../graphics/oh_native_transform.h"

namespace OH {

// Struct to hold the state of a RenderNode
struct RenderNodeSaveState {
    Transform3D transform;                     // Transformation matrix
    float translateX;                          // Translation along X axis
    float translateY;                          // Translation along Y axis
    int clipCount;                             // Number of clips
    OH_RenderNode_SaveState_MakeType makeType; // Type of the save (SafeGuard, Save, Clip)
};

// Inline function to create a safeguard RenderNodeSaveState
inline RenderNodeSaveState RenderNodeSaveStateCreateSafeGuard() {
    return RenderNodeSaveState{
        Transform3DIdentity,                        // Default identity transform
        0.0f,                                       // No translation
        0.0f,                                       // No translation
        0,                                          // No clip
        OH_RenderNode_SaveState_MakeType::SafeGuard // SafeGuard type
    };
}

inline std::string OHNSStringFromCATransform3D(const Transform3D &transform) {
    return "CATransform3D(" + std::to_string(transform.m11) + "," + std::to_string(transform.m12) + "," +
           std::to_string(transform.m13) + "," + std::to_string(transform.m14) + "," + std::to_string(transform.m21) +
           "," + std::to_string(transform.m22) + "," + std::to_string(transform.m23) + "," +
           std::to_string(transform.m24) + "," + std::to_string(transform.m31) + "," + std::to_string(transform.m32) +
           "," + std::to_string(transform.m33) + "," + std::to_string(transform.m34) + "," +
           std::to_string(transform.m41) + "," + std::to_string(transform.m42) + "," + std::to_string(transform.m43) +
           "," + std::to_string(transform.m44) + ")";
}

} // namespace OH

#endif // OH_NATIVE_RENDER_NODE_SAVE_STATE_H
