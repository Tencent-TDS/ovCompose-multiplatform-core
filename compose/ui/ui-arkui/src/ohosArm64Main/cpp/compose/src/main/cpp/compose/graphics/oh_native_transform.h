#ifndef OH_NATIVE_TRANSFORM_H
#define OH_NATIVE_TRANSFORM_H

#include <array>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <string>

#include "../constants/oh_native_constants.h"

namespace OH {

class Transform3D {
public:
    union {
        struct {
            float m11, m12, m13, m14;
            float m21, m22, m23, m24;
            float m31, m32, m33, m34;
            float m41, m42, m43, m44;
        };
        float matrix[16]; // 连续数组，便于直接传递
    };

    constexpr Transform3D() noexcept
        : m11(1.0f), m12(0.0f), m13(0.0f), m14(0.0f), m21(0.0f), m22(1.0f), m23(0.0f), m24(0.0f), m31(0.0f), m32(0.0f),
          m33(1.0f), m34(0.0f), m41(0.0f), m42(0.0f), m43(0.0f), m44(1.0f) {}

    explicit Transform3D(const float *data) noexcept { std::memcpy(matrix, data, sizeof(matrix)); }

    Transform3D(const Transform3D &other) noexcept { std::memcpy(matrix, other.matrix, sizeof(matrix)); }

    Transform3D &operator=(const Transform3D &other) noexcept {
        if (this != &other) {
            std::memcpy(matrix, other.matrix, sizeof(matrix));
        }
        return *this;
    }

#pragma mark - 性能优化的变换操作
    OH_ALWAYS_INLINE void translate(float tx, float ty, float tz = 0.0f) noexcept {
        m41 = m11 * tx + m21 * ty + m31 * tz + m41;
        m42 = m12 * tx + m22 * ty + m32 * tz + m42;
        m43 = m13 * tx + m23 * ty + m33 * tz + m43;
        m44 = m14 * tx + m24 * ty + m34 * tz + m44;
    }

    OH_ALWAYS_INLINE void scale(float sx, float sy, float sz = 1.0f) noexcept {
        m11 *= sx;
        m12 *= sx;
        m13 *= sx;
        m14 *= sx;
        m21 *= sy;
        m22 *= sy;
        m23 *= sy;
        m24 *= sy;
        m31 *= sz;
        m32 *= sz;
        m33 *= sz;
        m34 *= sz;
    }

    OH_ALWAYS_INLINE void rotate(float degrees) noexcept {
        if (degrees == 0.0f)
            return;

        float rad = degrees * static_cast<float>(M_PI) / 180.0f;
        float cosA = std::cos(rad);
        float sinA = std::sin(rad);

        float a11 = m11, a12 = m12;
        float a21 = m21, a22 = m22;
        float a31 = m31, a32 = m32;
        float a41 = m41, a42 = m42;

        m11 = a11 * cosA + a21 * sinA;
        m12 = a12 * cosA + a22 * sinA;
        m13 = m13 * cosA + m23 * sinA;
        m14 = m14 * cosA + m24 * sinA;

        m21 = a11 * -sinA + a21 * cosA;
        m22 = a12 * -sinA + a22 * cosA;
        m23 = m13 * -sinA + m23 * cosA;
        m24 = m14 * -sinA + m24 * cosA;
    }

    OH_ALWAYS_INLINE void skew(float sx, float sy) noexcept {
        float a11 = m11, a12 = m12;
        float a21 = m21, a22 = m22;
        float a31 = m31, a32 = m32;
        float a41 = m41, a42 = m42;

        m11 = a11 + a21 * sy;
        m12 = a12 + a22 * sy;
        m13 = m13 + m23 * sy;
        m14 = m14 + m24 * sy;

        m21 = a11 * sx + a21;
        m22 = a12 * sx + a22;
        m23 = m13 * sx + m23;
        m24 = m14 * sx + m24;
    }
#pragma mark - 静态构造方法
    static OH_ALWAYS_INLINE Transform3D Identity() noexcept { return Transform3D(); }

    static OH_ALWAYS_INLINE Transform3D Translation(float tx, float ty, float tz = 0.0f) noexcept {
        Transform3D t;
        t.m41 = tx;
        t.m42 = ty;
        t.m43 = tz;
        return t;
    }

    static OH_ALWAYS_INLINE Transform3D Scale(float sx, float sy, float sz = 1.0f) noexcept {
        Transform3D t;
        t.m11 = sx;
        t.m22 = sy;
        t.m33 = sz;
        return t;
    }

    static OH_ALWAYS_INLINE Transform3D Rotation(float degrees) noexcept {
        Transform3D t;
        if (degrees != 0.0f) {
            float rad = degrees * static_cast<float>(M_PI) / 180.0f;
            float cosA = std::cos(rad);
            float sinA = std::sin(rad);
            t.m11 = cosA;
            t.m12 = -sinA;
            t.m21 = sinA;
            t.m22 = cosA;
        }
        return t;
    }

    static OH_ALWAYS_INLINE Transform3D Skew(float sx, float sy) noexcept {
        Transform3D t;
        t.m21 = sx;
        t.m12 = sy;
        return t;
    }

#pragma mark - 工具方法
    OH_ALWAYS_INLINE bool isIdentity() const noexcept {
        return m11 == 1.0f && m12 == 0.0f && m13 == 0.0f && m14 == 0.0f && m21 == 0.0f && m22 == 1.0f && m23 == 0.0f &&
               m24 == 0.0f && m31 == 0.0f && m32 == 0.0f && m33 == 1.0f && m34 == 0.0f && m41 == 0.0f && m42 == 0.0f &&
               m43 == 0.0f && m44 == 1.0f;
    }

    OH_ALWAYS_INLINE const float *data() const noexcept { return matrix; }

    OH_ALWAYS_INLINE float *data() noexcept { return matrix; }

    OH_ALWAYS_INLINE void transformPoint(float &x, float &y) const noexcept {
        float w = m14 * x + m24 * y + m34 + m44;
        if (w != 0.0f) {
            x = (m11 * x + m21 * y + m31 + m41) / w;
            y = (m12 * x + m22 * y + m32 + m42) / w;
        }
    }

    OH_ALWAYS_INLINE void reset() noexcept { *this = Transform3D(); }

    OH_ALWAYS_INLINE std::string toReadableString() const noexcept {
        char buffer[512];
        std::snprintf(buffer, sizeof(buffer),
                      "Transform3D(\n"
                      "  [%.3f, %.3f, %.3f, %.3f]\n"
                      "  [%.3f, %.3f, %.3f, %.3f]\n"
                      "  [%.3f, %.3f, %.3f, %.3f]\n"
                      "  [%.3f, %.3f, %.3f, %.3f]\n"
                      ")",
                      m11, m12, m13, m14, m21, m22, m23, m24, m31, m32, m33, m34, m41, m42, m43, m44);
        return std::string(buffer, sizeof(buffer));
    }

#pragma mark - 运算符重载
    Transform3D operator*(const Transform3D &other) const noexcept {
        Transform3D result;

        result.m11 = m11 * other.m11 + m12 * other.m21 + m13 * other.m31 + m14 * other.m41;
        result.m12 = m11 * other.m12 + m12 * other.m22 + m13 * other.m32 + m14 * other.m42;
        result.m13 = m11 * other.m13 + m12 * other.m23 + m13 * other.m33 + m14 * other.m43;
        result.m14 = m11 * other.m14 + m12 * other.m24 + m13 * other.m34 + m14 * other.m44;

        result.m21 = m21 * other.m11 + m22 * other.m21 + m23 * other.m31 + m24 * other.m41;
        result.m22 = m21 * other.m12 + m22 * other.m22 + m23 * other.m32 + m24 * other.m42;
        result.m23 = m21 * other.m13 + m22 * other.m23 + m23 * other.m33 + m24 * other.m43;
        result.m24 = m21 * other.m14 + m22 * other.m24 + m23 * other.m34 + m24 * other.m44;

        result.m31 = m31 * other.m11 + m32 * other.m21 + m33 * other.m31 + m34 * other.m41;
        result.m32 = m31 * other.m12 + m32 * other.m22 + m33 * other.m32 + m34 * other.m42;
        result.m33 = m31 * other.m13 + m32 * other.m23 + m33 * other.m33 + m34 * other.m43;
        result.m34 = m31 * other.m14 + m32 * other.m24 + m33 * other.m34 + m34 * other.m44;

        result.m41 = m41 * other.m11 + m42 * other.m21 + m43 * other.m31 + m44 * other.m41;
        result.m42 = m41 * other.m12 + m42 * other.m22 + m43 * other.m32 + m44 * other.m42;
        result.m43 = m41 * other.m13 + m42 * other.m23 + m43 * other.m33 + m44 * other.m43;
        result.m44 = m41 * other.m14 + m42 * other.m24 + m43 * other.m34 + m44 * other.m44;

        return result;
    }

    Transform3D &operator*=(const Transform3D &other) noexcept {
        *this = *this * other;
        return *this;
    }

    bool operator==(const Transform3D &other) const noexcept {
        return std::memcmp(matrix, other.matrix, sizeof(matrix)) == 0;
    }

    bool operator!=(const Transform3D &other) const noexcept { return !(*this == other); }
};

// 全局常量
constexpr Transform3D Transform3DIdentity;

} // namespace OH

#endif