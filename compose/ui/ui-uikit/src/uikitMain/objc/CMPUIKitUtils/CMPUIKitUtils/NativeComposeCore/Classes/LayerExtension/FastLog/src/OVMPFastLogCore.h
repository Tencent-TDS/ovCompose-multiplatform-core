/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * OVMPFastLog 核心实现（C++ 头文件）
 * 
 * 多线程写入（MPSC）环形缓冲区，零堆内存分配。
 */

#import "OVMPFastLogCAPI.h"

#ifdef __cplusplus

#import <mach/mach_time.h>
#import <atomic>

namespace ovmp_fastlog {

// 默认容量（必须是2的幂）
constexpr uint32_t kDefaultCapacity = 2048;
constexpr uint32_t kMaxCapacity = 65536;

// 自动排空（写入侧唤醒）参数
constexpr uint32_t kAutoDumpWakeupThreshold = 512;
constexpr uint32_t kAutoDumpHighWaterPermille = 800; // 80%

// 写入侧只做"请求排空"，真正 drain/打印在 OVMPLogPrinter.queue 上完成。
void OVMPFastLogRequestAsyncDrain();

// 缓存行大小，用于对齐
constexpr size_t kCacheLineSize = 64;

static inline uint64_t OVMPFastLogMachToUs(uint64_t machTicks) {
    static mach_timebase_info_data_t timebase = {0};
    if (timebase.denom == 0) {
        mach_timebase_info(&timebase);
    }
    return (machTicks * timebase.numer) / (timebase.denom * 1000);
}

static inline uint64_t OVMPFastLogNowUs() {
    return OVMPFastLogMachToUs(mach_absolute_time());
}

/**
 * 环形缓冲区（MPSC，多写单读）
 * 
 * 优化目标:
 * - 多线程写入安全（MPSC）
 * - 零内存分配（静态缓冲区）
 * - 极低开销（无堆内存分配）
 */
class OVMPFastLogRingBuffer {
public:
    OVMPFastLogRingBuffer() = default;
    ~OVMPFastLogRingBuffer() = default;
    
    // 禁止拷贝和移动
    OVMPFastLogRingBuffer(const OVMPFastLogRingBuffer&) = delete;
    OVMPFastLogRingBuffer& operator=(const OVMPFastLogRingBuffer&) = delete;
    
    /**
     * 使用默认静态缓冲区初始化（多线程写入安全：MPSC）。
     * @param capacity 条目数量（必须是2的幂，且 <= kDefaultCapacity）
     */
    OVMPFastLogStatus initStatic(int32_t capacity);
    
    /**
     * 关闭并重置状态。
     */
    void shutdown();
    
    /**
     * 检查是否已初始化。
     */
    bool isInitialized() const { return buffer_ != nullptr; }
    
    // ========== 日志记录（单生产者）==========
    /**
     * 记录6个参数的条目。
     * 性能关键路径 - 内联。
     */
    inline void log7(int32_t tag,
                     int64_t arg0, int64_t arg1, int64_t arg2,
                     int64_t arg3, int64_t arg4, int64_t arg5, int64_t arg6) {
        log8(tag, arg0, arg1, arg2, arg3, arg4, arg5, arg6, 0);
    }
    
    inline void log8(int32_t tag,
                     int64_t arg0, int64_t arg1, int64_t arg2,
                     int64_t arg3, int64_t arg4, int64_t arg5, int64_t arg6, int64_t arg7) {
        if (__builtin_expect(buffer_ == nullptr, 0)) return;
        mpscEnqueue(tag, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    }
    
    inline void log4(int32_t tag,
                     int64_t arg0, int64_t arg1, int64_t arg2, int64_t arg3) {
        log7(tag, arg0, arg1, arg2, arg3, 0, 0, 0);
    }
    
    inline void log2(int32_t tag, int64_t arg0, int64_t arg1) {
        log7(tag, arg0, arg1, 0, 0, 0, 0, 0);
    }
    
    /**
     * 导出条目到外部缓冲区。
     * @param outBuffer  目标缓冲区
     * @param maxCount   最大拷贝条数
     * @return 实际拷贝的条数
     */
    int32_t drain(OVMPFastLogEntry* outBuffer, int32_t maxCount);
    
    /**
     * 查看条目但不消费。
     */
    int32_t peek(OVMPFastLogEntry* outBuffer, int32_t maxCount, int32_t offset) const;
    
    /**
     * 清空所有条目。
     */
    void clear();
    
    OVMPFastLogStats stats() const;
    
    uint32_t readableCount() const {
        const uint32_t head = enqueuePos_.load(std::memory_order_relaxed);
        const uint32_t tail = dequeuePos_.load(std::memory_order_relaxed);
        return (head >= tail) ? (head - tail) : 0;
    }

private:
    inline void resetAllState() {
        enqueuePos_.store(0, std::memory_order_relaxed);
        dequeuePos_.store(0, std::memory_order_relaxed);
        droppedCount_.store(0, std::memory_order_relaxed);
    }

    inline void mpscEnqueue(int32_t tag,
                            int64_t arg0, int64_t arg1, int64_t arg2,
                            int64_t arg3, int64_t arg4, int64_t arg5, int64_t arg6, int64_t arg7) {
        // init 只在 set_enabled(true) 调一次，shutdown 在 app 内不会调用，直接用普通变量。
        OVMPFastLogEntry* const buffer = buffer_;
        std::atomic<uint32_t>* const seq = seq_;
        const uint32_t capacityMask = capacityMask_;

        // Vyukov bounded MPMC queue（这里用于 MPSC）：满则直接丢弃
        uint32_t pos = enqueuePos_.load(std::memory_order_relaxed);
        for (;;) {
            std::atomic<uint32_t>& seqCell = seq[pos & capacityMask];
            const uint32_t s = seqCell.load(std::memory_order_acquire);
            const int32_t dif = static_cast<int32_t>(s) - static_cast<int32_t>(pos);
            if (dif == 0) {
                if (enqueuePos_.compare_exchange_weak(pos, pos + 1,
                                                     std::memory_order_relaxed,
                                                     std::memory_order_relaxed)) {
                    break;
                }
            } else if (dif < 0) {
                droppedCount_.fetch_add(1, std::memory_order_relaxed);
                return;
            } else {
                pos = enqueuePos_.load(std::memory_order_relaxed);
            }
        }

        const uint32_t idx = pos & capacityMask;

        // [极致优化] 写入侧直接存 mach_absolute_time()，消费侧基于 anchor 换算绝对时间
        OVMPFastLogEntry& entry = buffer[idx];
        entry.mach_ticks = static_cast<int64_t>(mach_absolute_time());
        entry.tag = tag;
        entry.arg0 = arg0;
        entry.arg1 = arg1;
        entry.arg2 = arg2;
        entry.arg3 = arg3;
        entry.arg4 = arg4;
        entry.arg5 = arg5;
        entry.arg6 = arg6;
        entry.arg7 = arg7;

        // 发布提交：消费者通过 seq[idx] 的 release 读到完整 entry。
        seq[idx].store(pos + 1, std::memory_order_release);

        // 写入侧触发 drain 请求：每 64 条检查一次（低频采样）
        const uint32_t head = pos + 1;
        if ((head & 0x3F) == 0) {
            const uint32_t tail = dequeuePos_.load(std::memory_order_relaxed);
            const uint32_t readable = (head >= tail) ? (head - tail) : 0;
            if (readable >= kAutoDumpWakeupThreshold) {
                OVMPFastLogRequestAsyncDrain();
            }
        }
    }

    // 缓冲区和容量（init 只调一次，shutdown 不会调用，用普通变量）
    OVMPFastLogEntry* buffer_ = nullptr;
    std::atomic<uint32_t>* seq_ = nullptr;
    uint32_t capacity_ = 0;
    uint32_t capacityMask_ = 0;

    // ========== 默认静态缓冲区模式（MPSC，多写单读）==========
    std::atomic<uint32_t> enqueuePos_{0};
    std::atomic<uint32_t> dequeuePos_{0};
    std::atomic<uint32_t> droppedCount_{0};

    // 帧跟踪（多线程可读）
    std::atomic<int64_t> currentFrameId_{0};
};


/**
 * 默认初始化使用的静态缓冲区。
 * 完全避免堆分配。
 */
struct OVMPStaticBuffer {
    // 对齐到缓存行以获得更好性能
    alignas(64) OVMPFastLogEntry entries[kDefaultCapacity];
    alignas(64) std::atomic<uint32_t> seq[kDefaultCapacity];
};

} // namespace ovmp_fastlog

#endif /* OVMP_FASTLOG_CORE_HPP */
