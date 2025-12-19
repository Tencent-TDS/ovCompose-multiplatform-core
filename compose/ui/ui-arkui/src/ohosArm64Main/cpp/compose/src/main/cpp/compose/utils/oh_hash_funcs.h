#ifndef OH_HASH_FUNCS_H
#define OH_HASH_FUNCS_H

#include <cstddef>
#include <cstring>
#include <vector>

#include "../constants/oh_native_constants.h"
#include "../trace/oh_systrace_section.h"

namespace OH {

constexpr uint64_t kHashPrime = 0x9E3779B97F4A7C15ULL;
constexpr uint64_t kHashPrime2 = 0x6C8E9CF570932BABULL;
constexpr uint64_t kHashPrime3 = 0x41C3C4F712193BEFULL;
constexpr uint64_t kHashMixConstant1 = 0x62A9D9ED799705F5ULL;
constexpr uint64_t kHashMixConstant2 = 0x88D3E5F4F60DEDCDULL;
constexpr uint64_t kHashAddConstant = 0xAAAAAAAAAAAAAAAAULL;

OH_ALWAYS_INLINE uint64_t hashMerge(size_t a, size_t b) noexcept {
    const uint64_t ua = static_cast<uint64_t>(a);
    const uint64_t ub = static_cast<uint64_t>(b);
    const uint64_t base_hash = 31 * ua + ub;
    return base_hash ^ (ub * kHashPrime);
}

OH_ALWAYS_INLINE uint64_t hashMerge(const size_t a, const size_t b, const size_t c) noexcept {
    const uint64_t ua = static_cast<uint64_t>(a);
    const uint64_t ub = static_cast<uint64_t>(b);
    const uint64_t uc = static_cast<uint64_t>(c);

    uint64_t h = (ua * kHashPrime) ^ (ub * kHashPrime2) ^ (uc * kHashPrime3);

    h += kHashAddConstant;
    h ^= h >> 33;
    h *= kHashMixConstant1;
    h ^= h >> 29;
    h += (ub << 24) | (uc >> 40);

    h ^= h >> 32;
    h *= kHashMixConstant2;
    h ^= h >> 37;
    return h;
}

OH_ALWAYS_INLINE uint64_t hashMerge(size_t a, size_t b, size_t c, size_t d) noexcept {
    const uint64_t ua = static_cast<uint64_t>(a);
    const uint64_t ub = static_cast<uint64_t>(b);
    const uint64_t uc = static_cast<uint64_t>(c);
    const uint64_t ud = static_cast<uint64_t>(d);

    uint64_t h = hashMerge(ua, ub);
    h = hashMerge(h, uc);
    h = hashMerge(h, ud);

    // 额外的混合步骤
    h ^= h >> 31;
    h *= kHashPrime;
    h ^= h >> 27;

    return h;
}

template <typename... Args> OH_ALWAYS_INLINE uint64_t hashMergeVariadic(Args... args) noexcept {
    uint64_t result = 0;

    // 使用折叠表达式处理所有参数
    size_t index = 0;
    ((result ^= hashMerge(static_cast<uint64_t>(args), index++)), ...);

    // 最终混合
    result ^= result >> 33;
    result *= kHashPrime;
    result ^= result >> 29;

    return result;
}

OH_ALWAYS_INLINE uint64_t hashMergePointers(const void *ptr1, const void *ptr2) noexcept {
    const uint64_t p1 = reinterpret_cast<uint64_t>(ptr1);
    const uint64_t p2 = reinterpret_cast<uint64_t>(ptr2);
    return hashMerge(p1, p2);
}

OH_ALWAYS_INLINE uint64_t hashMergeFloats(float a, float b) noexcept {
    // 将浮点数按位解释为整数进行哈希
    static_assert(sizeof(float) == sizeof(uint32_t), "float size mismatch");

    uint32_t ua, ub;
    std::memcpy(&ua, &a, sizeof(float));
    std::memcpy(&ub, &b, sizeof(float));

    return hashMerge(ua, ub);
}

OH_ALWAYS_INLINE uint64_t FNVHash(const void *data, size_t len) noexcept {
    const uint8_t *bytes = static_cast<const uint8_t *>(data);
    uint64_t hash = 14695981039346656037ULL; // FNV偏移基数
    for (size_t i = 0; i < len; ++i) {
        hash ^= bytes[i];
        hash *= 1099511628211ULL; // FNV质数
    }
    return hash;
}

template <typename T> OH_ALWAYS_INLINE uint64_t FNVHashNumberArray(const std::vector<T> &data) noexcept {
    uint64_t hash = 14695981039346656037ULL;
    if (data.size() == 0) {
        return 0;
    }
    for (auto f : data) {
        hash ^= static_cast<uint64_t>(f);
        hash *= 1099511628211ULL;
    }
    return hash;
}

OH_ALWAYS_INLINE uint64_t hashArray(const void *data, size_t elementSize, size_t elementCount) noexcept {
    if (data == nullptr || elementCount == 0 || elementSize == 0) {
        return 0;
    }
    return FNVHash(data, elementSize * elementCount);
}

OH_ALWAYS_INLINE uint64_t hashMergeDoubles(double a, double b) noexcept {
    // 将双精度浮点数按位解释为整数进行哈希
    static_assert(sizeof(double) == sizeof(uint64_t), "double size mismatch");

    uint64_t ua, ub;
    std::memcpy(&ua, &a, sizeof(double));
    std::memcpy(&ub, &b, sizeof(double));

    return hashMerge(ua, ub);
}

OH_ALWAYS_INLINE uint64_t hashMergeWithSeed(uint64_t seed, size_t value) noexcept {
    const uint64_t uvalue = static_cast<uint64_t>(value);
    return seed ^ (uvalue + kHashPrime + (seed << 6) + (seed >> 2));
}

template <typename... Args> OH_ALWAYS_INLINE uint64_t hashCombineSequential(Args... args) noexcept {
//    OH::SystraceSection trace("hashCombineSequential");
    uint64_t seed = 0;

    // 顺序合并所有参数
    ((seed = hashMergeWithSeed(seed, args)), ...);

    return seed;
}

/// 从 Paint 上根据参数，计算出一个 hash
/// 参考 iOS 平台的 TMMNativeDataHashFromPaint 实现
/// - Parameter paint: OHComposeNativePaint*
template <typename PaintType> OH_ALWAYS_INLINE uint64_t nativeDataHashFromPaint(PaintType *paint) noexcept {
//    OH::SystraceSection trace("nativeDataHashFromPaint");
    if (paint == nullptr) {
        return 0;
    }

    // 获取 shader hash
    uint64_t shaderHash = 0;
    if (paint->shader != nullptr) {
        shaderHash = paint->shader->propertyHash();
    }

    // 获取 colorFilter hash
    uint64_t colorFilterHash = 0;
    if (paint->colorFilter != nullptr) {
        // 使用指针地址作为 hash（如果有更好的 hash 方法可以替换）
        colorFilterHash = reinterpret_cast<uint64_t>(paint->colorFilter);
    }

    // 将所有属性打包到数组中计算 hash
    const float floats[11] = {paint->alpha,
                              paint->strokeWidth,
                              paint->strokeMiterLimit,
                              static_cast<float>(paint->isAntiAlias),
                              static_cast<float>(paint->blendMode),
                              static_cast<float>(paint->style),
                              static_cast<float>(paint->strokeCap),
                              static_cast<float>(paint->strokeJoin),
                              static_cast<float>(paint->filterQuality),
                              static_cast<float>(shaderHash),
                              static_cast<float>(colorFilterHash)};

    // 使用 FNV hash 算法计算数组的 hash
    uint64_t hash = FNVHash(floats, sizeof(floats));

    // 混合颜色值
    hash ^= static_cast<uint64_t>(paint->color);
    hash *= 1099511628211ULL; // FNV质数

    return hash;
}

} // namespace OH

#endif // OH_HASH_FUNCS_H