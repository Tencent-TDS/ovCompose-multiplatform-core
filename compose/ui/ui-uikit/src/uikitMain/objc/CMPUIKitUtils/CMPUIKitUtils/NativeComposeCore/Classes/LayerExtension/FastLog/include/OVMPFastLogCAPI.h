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
 * OVMPFastLog - 超高性能环形缓冲日志器
 * 
 * 多线程写入（MPSC）、无锁、零堆内存分配的高频事件日志记录。
 * 专为 Compose iOS 文本渲染诊断设计。
 */

#ifndef OVMP_FASTLOG_C_API_H
#define OVMP_FASTLOG_C_API_H

#import "OVMPFastLogPhase.h"

#ifdef __cplusplus
extern "C" {
#endif

/* ============================================================================
 * 状态码
 * ============================================================================ */

typedef enum OVMPFastLogStatus {
    OVMP_FASTLOG_OK                  = 0,    // 成功
    OVMP_FASTLOG_ERR_NOT_INIT        = 1,    // 未初始化
    OVMP_FASTLOG_ERR_ALREADY_INIT    = 2,    // 已初始化（重复调用）
    OVMP_FASTLOG_ERR_INVALID_ARG     = 3,    // 参数无效
    OVMP_FASTLOG_ERR_BUFFER_FULL     = 4,    // 缓冲区满（仅提示，覆盖模式下非错误）
    OVMP_FASTLOG_ERR_NOT_ENABLE      = 5,    // 已废弃（保留枚举值兼容；当前实现不再返回该错误）
} OVMPFastLogStatus;


/* ============================================================================
 * 日志条目结构（消费者只读）
 * ============================================================================ */

typedef struct OVMPFastLogEntry {
    int64_t mach_ticks;     // 日志记录时的 mach_absolute_time()（消费侧基于 anchor 换算绝对时间）
    int32_t tag;            // 事件类型标签
    int64_t arg0;           // arg0 一般固定为 taskId
    int64_t arg1;
    int64_t arg2;
    int64_t arg3;
    int64_t arg4;
    int64_t arg5;
    int64_t arg6;
    int64_t arg7;
} OVMPFastLogEntry;

/* ============================================================================
 * 统计信息结构
 * ============================================================================ */

typedef struct OVMPFastLogStats {
    int32_t capacity;          // 缓冲区总容量
    int32_t written_count;     // 已写入条数（可能环绕）
    int32_t readable_count;    // 可读取条数
    int32_t dropped_count;     // 被覆盖的条数（未被消费即被覆盖）
    int64_t current_frame_id;  // 当前帧标识符
    int64_t frame_time_base;   // 当前帧基准时间戳（mach_absolute_time）
} OVMPFastLogStats;

/* ============================================================================
 * 预定义标签
 * ============================================================================ */

typedef enum OVMPFastLogTag {
    OVMP_FASTLOG_TAG_FRAME_BEGIN = 666, // 帧开始
    OVMP_FASTLOG_TAG_FRAME_END   = 667, // 帧结束
    OVMP_FASTLOG_TAG_TEXT        = 668, // 文本渲染统一标签（阶段/状态放入参数）
    OVMP_FASTLOG_TAG_IMAGE       = 669, // 图片绘制
} OVMPFastLogTag;


/* ============================================================================
 * 初始化与关闭
 * ============================================================================ */

/**
 * 关闭 OVMPFastLog 并重置状态。
 * 可安全多次调用。
 */
void ovmp_fastlog_shutdown(void);

/**
 * 检查 OVMPFastLog 是否已初始化。
 */
bool ovmp_fastlog_is_initialized(void);

/* ============================================================================
 * 日志记录函数（多线程写入安全：MPSC；单消费者读取）
 * ============================================================================ */

/**
 * 记录一个事件，最多 6 个 64 位整型/指针参数。
 *
 * @param tag       事件类型标签（使用预定义的 OVMP_FASTLOG_TAG_* 或自定义值）
 * @param arg0-5    整型或指针载荷值（64 位）
 *
 * 性能: 极低开销（无堆内存分配）。
 * delta_ticks 由内部根据 begin_frame 记录的时间基准自动计算（消费侧换算为 us）。
 *
 * 文本渲染示例:
 *   ovmp_fastlog_log6(OVMP_FASTLOG_TAG_TEXT_CACHE_HIT,
 *                textHash, textLen, textHead, paragraphHashCode, viewId, cacheHit);
 */
void ovmp_fastlog_log6(int32_t tag,
                  int64_t arg0, int64_t arg1, int64_t arg2,
                  int64_t arg3, int64_t arg4, int64_t arg5);


void ovmp_fastlog_log7(int32_t tag,
                  int64_t arg0, int64_t arg1, int64_t arg2,
                  int64_t arg3, int64_t arg4, int64_t arg5,int64_t arg6);

void ovmp_fastlog_log8(int32_t tag,
                  int64_t arg0, int64_t arg1, int64_t arg2,
                       int64_t arg3, int64_t arg4, int64_t arg5,int64_t arg6, int64_t arg7);

/**
 * 便捷函数: 记录4个参数（arg4, arg5 = 0）
 */
void ovmp_fastlog_log4(int32_t tag,
                  int64_t arg0, int64_t arg1, int64_t arg2, int64_t arg3);

/* ============================================================================
 * 消费 / 导出
 * ============================================================================ */

/**
 * 从环形缓冲区导出（拷贝）条目到外部缓冲区。
 * 
 * @param out_buffer  目标缓冲区（调用者提供）
 * @param max_count   最大拷贝条数
 * @param out_written 实际写入条数（输出参数）
 * @return 成功返回 OVMP_FASTLOG_OK
 * 
 * 注意: 供消费者线程使用。从后台/诊断线程调用。
 */
OVMPFastLogStatus ovmp_fastlog_drain(OVMPFastLogEntry* out_buffer, int32_t max_count, int32_t* out_written);

/**
 * 查看条目但不消费。
 * 
 * @param out_buffer  目标缓冲区
 * @param max_count   最大拷贝条数
 * @param out_written 实际写入条数
 * @param offset      从最旧可读条目开始的偏移量
 */
OVMPFastLogStatus ovmp_fastlog_peek(OVMPFastLogEntry* out_buffer, int32_t max_count,
                           int32_t* out_written, int32_t offset);

/**
 * 清空所有条目（重置读写指针）。
 */
void ovmp_fastlog_clear(void);

/* ============================================================================
 * 诊断
 * ============================================================================ */

/**
 * 获取当前统计信息。
 */
OVMPFastLogStats ovmp_fastlog_stats(void);

/**
 * 启用/禁用日志写入（运行时开关）。
 *
 * - enabled = true：若尚未初始化，会使用默认静态缓冲区（2048 条目）进行懒初始化，然后开始写入。
 * - enabled = false：仅停止写入（写入类接口为空操作），不会自动 shutdown。
 */
void ovmp_fastlog_set_enabled(bool enabled);
bool ovmp_fastlog_is_enabled(void);

/* ============================================================================
 * 时间锚点（挂钟时间 + 单调时钟）
 * ============================================================================ */
/**
 * 记录一个挂钟时间锚点：
 *  - wall_clock_seconds：使用 CFAbsoluteTimeGetCurrent + kCFAbsoluteTimeIntervalSince1970（秒）
 *  - mach_ticks：mach_absolute_time() 当前刻度
 * 调用后即可在 OC 侧用 (mach_now - mach_ticks) 换算相对时间，再叠加 wall_clock_seconds 还原年月日时分秒。
 */
void ovmp_fastlog_set_wall_clock_anchor(void);

/**
 * 读取最近一次设置的挂钟锚点；若未设置过，返回 false。
 * @param out_wall_seconds  可为 NULL，返回秒（Epoch）
 * @param out_mach_ticks    可为 NULL，返回 mach_absolute_time 刻度
 * @return 是否已有有效锚点
 */
bool ovmp_fastlog_get_wall_clock_anchor(double* out_wall_seconds, int64_t* out_mach_ticks);

// 统一的 traceId 管理（Text/Image 共用）
int64_t ovmp_alloc_trace_id(void);
void ovmp_set_current_trace_id(int64_t traceId);
int64_t ovmp_get_current_trace_id(void);

#ifdef __cplusplus
}
#endif

#endif /* OVMP_FASTLOG_C_API_H */
