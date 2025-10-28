#ifndef RESOURCE_MANAGER_H
#define RESOURCE_MANAGER_H

#include <native_drawing/drawing_text_typography.h>
#include <native_drawing/drawing_text_declaration.h>
#include <functional>

namespace OH {

/**
 * RAII资源管理器模板类
 *
 * 设计目的：
 * 1. 自动管理OHOS资源的生命周期
 * 2. 异常安全（RAII保证）
 * 3. 避免内存泄漏和double-free
 * 4. 统一资源管理接口
 */
template <typename T>
class ResourceHandle {
public:
    using Deleter = std::function<void(T *)>;

    /**
     * 构造函数：接管资源所有权
     */
    ResourceHandle(T *resource, Deleter deleter) : resource_(resource), deleter_(deleter) {
    }

    /**
     * 移动构造（支持转移所有权）
     */
    ResourceHandle(ResourceHandle &&other) noexcept
        : resource_(other.resource_), deleter_(std::move(other.deleter_)) {
        other.resource_ = nullptr;
    }

    /**
     * 移动赋值
     */
    ResourceHandle &operator=(ResourceHandle &&other) noexcept {
        if (this != &other) {
            reset();
            resource_ = other.resource_;
            deleter_ = std::move(other.deleter_);
            other.resource_ = nullptr;
        }
        return *this;
    }

    /**
     * 禁止拷贝（唯一所有权）
     */
    ResourceHandle(const ResourceHandle &) = delete;
    ResourceHandle &operator=(const ResourceHandle &) = delete;

    /**
     * 析构函数：自动释放资源
     */
    ~ResourceHandle() {
        reset();
    }

    /**
     * 获取原始指针
     */
    T *get() const {
        return resource_;
    }

    /**
     * 检查是否有效
     */
    bool isValid() const {
        return resource_ != nullptr;
    }

    /**
     * 显式类型转换
     */
    explicit operator bool() const {
        return isValid();
    }

    /**
     * 释放资源
     */
    void reset() {
        if (resource_ && deleter_) {
            deleter_(resource_);
            resource_ = nullptr;
        }
    }

    /**
     * 释放所有权（不调用deleter）
     */
    T *release() {
        T *temp = resource_;
        resource_ = nullptr;
        return temp;
    }

private:
    T *resource_;
    Deleter deleter_;
};

/**
 * 资源管理器工厂类（工厂模式）
 *
 * 提供统一的资源创建接口
 */
class DrawingResourceFactory {
public:
    /**
     * 创建FontCollection资源句柄
     */
    static ResourceHandle<OH_Drawing_FontCollection> createFontCollection();

    /**
     * 创建TypographyStyle资源句柄
     */
    static ResourceHandle<OH_Drawing_TypographyStyle> createTypographyStyle();

    /**
     * 创建TextStyle资源句柄
     */
    static ResourceHandle<OH_Drawing_TextStyle> createTextStyle();

    /**
     * 创建TypographyCreate资源句柄
     */
    static ResourceHandle<OH_Drawing_TypographyCreate> createTypographyHandler(
        OH_Drawing_TypographyStyle *typoStyle,
        OH_Drawing_FontCollection *fontCollection);

    /**
     * 创建Typography资源句柄
     */
    static ResourceHandle<OH_Drawing_Typography> createTypography(
        OH_Drawing_TypographyCreate *handler);
};

/**
 * 智能指针别名（便于使用）
 */
using FontCollectionPtr = ResourceHandle<OH_Drawing_FontCollection>;
using TypographyStylePtr = ResourceHandle<OH_Drawing_TypographyStyle>;
using TextStylePtr = ResourceHandle<OH_Drawing_TextStyle>;
using TypographyHandlerPtr = ResourceHandle<OH_Drawing_TypographyCreate>;
using TypographyPtr = ResourceHandle<OH_Drawing_Typography>;

} // namespace OH

#endif // RESOURCE_MANAGER_H
