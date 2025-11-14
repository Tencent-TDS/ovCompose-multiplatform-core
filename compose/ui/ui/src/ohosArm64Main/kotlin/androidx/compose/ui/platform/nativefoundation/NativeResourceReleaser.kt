/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

package androidx.compose.ui.platform.nativefoundation

import kotlinx.atomicfu.atomic

/**
 * 统一的资源释放任务类型
 * 所有Native资源的释放都封装为一个无参数的函数
 */
private typealias ReleaseTask = () -> Unit

/**
 * 统一的Native资源释放管理器
 * 
 * 设计理念：
 * - 使用函数式接口 `() -> Unit` 作为统一的释放任务类型，所有资源类型共享同一套机制
 * - 使用链表结构避免数组频繁扩容，提高性能
 * - 线程安全的无锁队列（使用atomic CAS操作）
 * - 统一的flush接口，在主线程批量执行所有待释放任务
 * 
 * 使用策略：
 * - cleaner回调线程只做enqueue，不做真正的资源释放（避免跨线程UI/渲染崩溃）
 * - 在主线程合适的时机（如每帧/Composition结束/显式调用）执行flush()
 */
internal object NativeResourceReleaser {

    /**
     * 链表节点，用于构建无锁队列
     * 使用链表而不是数组，避免频繁扩容和内存拷贝
     */
    private class Node(val task: ReleaseTask, var next: Node? = null)

    /**
     * 原子引用指向链表头节点
     * 使用atomic保证线程安全，无需额外的锁
     */
    private val head = atomic<Node?>(null)

    /**
     * 将释放任务入队
     * 
     * 线程安全：可以在任何线程调用（包括cleaner回调线程）
     * 性能：O(1)时间复杂度，无锁操作
     * 
     * @param releaseTask 释放任务，通常是一个lambda，包含实际的资源释放逻辑
     */
    fun enqueue(releaseTask: ReleaseTask) {
        val newNode = Node(releaseTask)
        while (true) {
            val currentHead = head.value
            newNode.next = currentHead
            // CAS操作：原子性地更新链表头
            if (head.compareAndSet(currentHead, newNode)) {
                break
            }
        }
    }

    /**
     * 批量执行所有待释放的任务
     * 
     * **必须在主线程调用**，否则会导致"Render node function doesn't run on UI thread"崩溃
     * 
     * 执行策略：
     * 1. 原子性地获取整个链表（通过getAndSet(null)）
     * 2. 从链表尾部到头部执行任务（因为链表是倒序插入的）
     * 3. 每个任务执行时捕获异常，避免单个任务失败影响其他任务
     * 
     * 性能：O(n)时间复杂度，n为待释放任务数量
     */
    fun flush() {
        // 原子性地获取整个链表并清空队列
        val node = head.getAndSet(null) ?: return

        // 先反转链表，因为任务是倒序插入的，我们需要按插入顺序执行
        // 这样可以保证先入队的任务先执行（FIFO）
        val reversed = reverseList(node)

        // 执行所有任务
        var current: Node? = reversed
        while (current != null) {
            try {
                current.task()
            } catch (e: Throwable) {
                // 捕获异常，避免单个任务失败影响其他任务
                // 在release模式下可以记录日志，但不应抛出异常
            }
            current = current.next
        }
    }

    /**
     * 反转链表
     * 由于任务是从头部插入的，需要反转才能按FIFO顺序执行
     */
    private fun reverseList(head: Node): Node {
        var prev: Node? = null
        var current: Node? = head
        while (current != null) {
            val next = current.next
            current.next = prev
            prev = current
            current = next
        }
        return prev ?: head
    }
}

/**
 * 对外暴露的便捷函数：在主线程周期性调用以确保及时释放Native资源
 * 
 * 可在一个统一的帧调度或某个CompositionEffect中循环调用
 * 建议在每帧绘制后调用，确保资源及时释放
 */
internal fun flushNativeResourcesOnMainThread() {
    NativeResourceReleaser.flush()
}

