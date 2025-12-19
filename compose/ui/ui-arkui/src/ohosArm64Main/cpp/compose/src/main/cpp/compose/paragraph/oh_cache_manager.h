#ifndef CACHE_MANAGER_H
#define CACHE_MANAGER_H

#include <functional>
#include <memory>
#include <vector>

#include "oh_native_paragraph_types.h"

namespace OH {

/**
 * 缓存观察者接口（观察者模式）
 *
 * 设计目的：
 * 1. 当缓存失效时通知相关组件
 * 2. 解耦缓存管理和使用者
 */
class ICacheObserver {
public:
    virtual ~ICacheObserver() = default;
    virtual void onCacheInvalidated() = 0;
};

/**
 * 行信息缓存管理器
 *
 * 设计模式：
 * - 观察者模式：缓存失效时通知订阅者
 * - 单例模式：每个Paragraph一个缓存管理器
 * - 懒加载：按需构建缓存
 *
 * 性能优化：
 * - 预分配内存避免频繁扩容
 * - 缓存有效性标记避免重复构建
 * - 支持部分失效策略
 */
class LineMetricsCacheManager {
public:
    using CacheBuilder = std::function<std::vector<LineMetrics>()>;

    explicit LineMetricsCacheManager(CacheBuilder builder);
    ~LineMetricsCacheManager() = default;

    // 禁止拷贝和赋值
    LineMetricsCacheManager(const LineMetricsCacheManager &) = delete;
    LineMetricsCacheManager &operator=(const LineMetricsCacheManager &) = delete;

    /**
     * 获取行信息（懒加载）
     */
    const LineMetrics *getLineMetrics(uint32_t lineIndex);

    /**
     * 获取所有行信息
     */
    const std::vector<LineMetrics> &getAllLineMetrics();

    /**
     * 使缓存失效
     */
    void invalidate();

    /**
     * 检查缓存是否有效
     */
    bool isValid() const { return isValid_; }

    /**
     * 获取缓存的行数
     */
    uint32_t getCachedLineCount() const;

    /**
     * 添加观察者
     */
    void addObserver(const std::shared_ptr<ICacheObserver> &observer);

    /**
     * 移除观察者
     */
    void removeObserver(ICacheObserver *observer);

    /**
     * 预分配缓存空间
     */
    void reserve(uint32_t capacity);

private:
    void buildCache();
    void notifyObservers() const;

    CacheBuilder builder_;
    std::vector<LineMetrics> cache_;
    bool isValid_;
    std::vector<std::shared_ptr<ICacheObserver>> observers_;
};

/**
 * 度量信息缓存（值对象缓存）
 */
class MetricsCacheManager {
public:
    using MetricsProvider = std::function<ParagraphMetrics()>;

    explicit MetricsCacheManager(MetricsProvider provider);

    /**
     * 获取度量信息（带缓存）
     */
    const ParagraphMetrics &getMetrics();

    /**
     * 使缓存失效
     */
    void invalidate();

    /**
     * 检查缓存是否有效
     */
    bool isValid() const { return isValid_; }

private:
    MetricsProvider provider_;
    ParagraphMetrics cachedMetrics_;
    bool isValid_;
};
} // namespace OH

#endif // CACHE_MANAGER_H
