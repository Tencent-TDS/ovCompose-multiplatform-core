#include <algorithm>
#include "oh_cache_manager.h"

namespace OH {

// ========== LineMetricsCacheManager 实现 ==========

LineMetricsCacheManager::LineMetricsCacheManager(CacheBuilder builder) :
    builder_(std::move(builder)),
    isValid_(false) {
}

const LineMetrics *LineMetricsCacheManager::getLineMetrics(uint32_t lineIndex) {
    if (!isValid_) {
        buildCache();
    }

    if (lineIndex >= cache_.size()) {
        return nullptr;
    }

    return &cache_[lineIndex];
}

const std::vector<LineMetrics> &LineMetricsCacheManager::getAllLineMetrics() {
    if (!isValid_) {
        buildCache();
    }

    return cache_;
}

void LineMetricsCacheManager::invalidate() {
    isValid_ = false;
    notifyObservers();
}

uint32_t LineMetricsCacheManager::getCachedLineCount() const {
    return cache_.size();
}

void LineMetricsCacheManager::addObserver(std::shared_ptr<ICacheObserver> observer) {
    if (observer) {
        observers_.push_back(observer);
    }
}

void LineMetricsCacheManager::removeObserver(ICacheObserver *observer) {
    observers_.erase(
        std::remove_if(observers_.begin(), observers_.end(),
                       [observer](const std::shared_ptr<ICacheObserver> &obs) {
                           return obs.get() == observer;
                       }),
        observers_.end());
}

void LineMetricsCacheManager::reserve(uint32_t capacity) {
    cache_.reserve(capacity);
}

void LineMetricsCacheManager::buildCache() {
    if (builder_) {
        cache_ = builder_();
        isValid_ = true;
    }
}

void LineMetricsCacheManager::notifyObservers() {
    for (auto &observer : observers_) {
        if (observer) {
            observer->onCacheInvalidated();
        }
    }
}

// ========== MetricsCacheManager 实现 ==========

MetricsCacheManager::MetricsCacheManager(MetricsProvider provider) :
    provider_(std::move(provider)),
    isValid_(false) {
}

const ParagraphMetrics &MetricsCacheManager::getMetrics() {
    if (!isValid_ && provider_) {
        cachedMetrics_ = provider_();
        isValid_ = true;
    }

    return cachedMetrics_;
}

void MetricsCacheManager::invalidate() {
    isValid_ = false;
}
} // namespace OH
