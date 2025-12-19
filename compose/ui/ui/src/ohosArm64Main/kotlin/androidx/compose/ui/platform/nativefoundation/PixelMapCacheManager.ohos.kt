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

package androidx.compose.ui.platform.nativefoundation

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.ui.SynchronizedObject
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_releaseNativePixelMap
import androidx.compose.ui.synchronized
import kotlinx.cinterop.COpaquePointer
import kotlin.concurrent.Volatile
import kotlin.native.ref.WeakReference

/**
 * PixelMap持有者基类
 * 
 * 继承NativeResourceHolder，利用cleaner机制和NativeResourceReleaser
 * 确保PixelMap在主线程正确释放。
 */
sealed class PixelMapHolder : NativeResourceHolder<COpaquePointer> {
    constructor(
        pixelMapPtr: COpaquePointer,
        releaseFunction: (COpaquePointer) -> Unit
    ) : super(pixelMapPtr, releaseFunction)
}

/**
 * 临时PixelMap持有者
 * 
 * 用于普通图片绘制，生命周期跟随ImageBitmap。
 * 当ImageBitmap被GC时，通过WeakHashMap自动释放。
 */
private class TransientPixelMapHolder(
    pixelMapPtr: COpaquePointer
) : PixelMapHolder(
    pixelMapPtr,
    releaseFunction = { ptr ->
        // NativeResourceReleaser确保在主线程调用
        releaseTransientPixelMap(ptr)
    }
)

/**
 * 持久PixelMap持有者
 * 
 * 用于文本渲染缓存，支持引用计数，独立生命周期。
 * 即使原始ImageBitmap被GC，缓存仍然有效，避免重复的昂贵文本渲染。
 */
private class PersistentPixelMapHolder(
    pixelMapPtr: COpaquePointer,
    val paragraphHashCode: Int
) : PixelMapHolder(
    pixelMapPtr,
    releaseFunction = { ptr ->
        // NativeResourceReleaser确保在主线程调用
        releasePersistentPixelMap(ptr, paragraphHashCode)
    }
) {
    private val refCount = kotlinx.atomicfu.atomic(1)
    
    /**
     * 增加引用计数
     * @return this for链式调用
     */
    fun retain(): PersistentPixelMapHolder {
        refCount.incrementAndGet()
        LogPrintUtil.verbose {
            "PersistentPixelMapHolder.retain: pixelMap=$handle, refCount=${refCount.value}, hashCode=$paragraphHashCode"
        }
        return this
    }
    
    /**
     * 减少引用计数
     * @return true if引用计数归零，应该释放
     */
    fun releaseRef(): Boolean {
        val newCount = refCount.decrementAndGet()
        LogPrintUtil.verbose {
            "PersistentPixelMapHolder.releaseRef: pixelMap=$handle, refCount=$newCount, hashCode=$paragraphHashCode"
        }
        return newCount == 0
    }
    
    /**
     * 获取当前引用计数（用于调试）
     */
    fun getRefCount(): Int = refCount.value
}

/**
 * 临时PixelMap释放函数（在主线程调用）
 */
private fun releaseTransientPixelMap(pixelMapPtr: COpaquePointer) {
    try {
        // 调用C++ API释放PixelMap
        // C++侧会清理OHDrawingPixelMapCache
        androidx_compose_ui_arkui_utils_releaseNativePixelMap(pixelMapPtr)
        LogPrintUtil.verbose { 
            "releaseTransientPixelMapOnMainThread: released $pixelMapPtr (main thread)" 
        }
    } catch (e: Exception) {
        LogPrintUtil.verbose { 
            "releaseTransientPixelMapOnMainThread: error $pixelMapPtr, ${e.message}" 
        }
    }
}

/**
 * 持久PixelMap释放函数（在主线程调用）
 */
private fun releasePersistentPixelMap(
    pixelMapPtr: COpaquePointer,
    paragraphHashCode: Int
) {
    try {
        // 调用C++ API释放PixelMap
        androidx_compose_ui_arkui_utils_releaseNativePixelMap(pixelMapPtr)
        LogPrintUtil.verbose { 
            "releasePersistentPixelMap: released $pixelMapPtr for hashCode=$paragraphHashCode (main thread)"
        }
    } catch (e: Exception) {
        LogPrintUtil.verbose { 
            "releasePersistentPixelMap: error $pixelMapPtr, ${e.message}"
        }
    }
}

/**
 * 统一的PixelMap缓存管理器
 * 
 * 双模式设计：
 * 1. 临时模式（Transient）：用于普通图片绘制，生命周期跟随ImageBitmap
 * 2. 持久模式（Persistent）：用于文本渲染缓存，独立生命周期，LRU管理
 * 
 * 核心优势：
 * - 临时缓存：避免重复的asNativePixelMap调用（1.4MB临时内存 + 120k计算）
 * - 持久缓存：避免重复的文本渲染（8-35ms的Paragraph layout + paint）
 * - 主线程释放：NativeResourceHolder + NativeResourceReleaser保证
 * - 零内存泄漏：cleaner机制 + 引用计数
 */

/**
 * WeakKey封装WeakReference，提供正确的equals/hashCode
 */
private class WeakKey(
    val ref: WeakReference<ImageBitmap>,
    val hash: Int
) {
    constructor(obj: ImageBitmap) : this(WeakReference(obj), obj.hashCode())
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WeakKey) return false
        val thisRef = ref.get()
        val otherRef = other.ref.get()
        if (thisRef != null && otherRef != null) {
            return thisRef === otherRef
        }
        return false 
    }
    
    override fun hashCode(): Int = hash
    
    fun get(): ImageBitmap? = ref.get()
}

object PixelMapCacheManager {
    
    // ========== 临时缓存（跟随ImageBitmap） ==========
    
    private val transientCacheLock = SynchronizedObject()
    // 使用WeakKey作为键，避免强引用ImageBitmap
    private val transientCache = mutableMapOf<WeakKey, TransientPixelMapHolder>()
    
    // 清理阈值
    private const val CLEANUP_THRESHOLD = 100
    @Volatile
    private var accessCount = 0
    
    @Volatile
    private var transientHits: Long = 0
    
    @Volatile
    private var transientMisses: Long = 0
    
    /**
     * 获取临时PixelMap（用于普通图片绘制）
     * 
     * 生命周期跟随ImageBitmap，ImageBitmap GC时自动释放。
     * 
     * @param imageBitmap 要转换的ImageBitmap
     * @return PixelMap指针，失败返回null
     */
    fun getTransientPixelMap(imageBitmap: ImageBitmap): COpaquePointer? {
        synchronized(transientCacheLock) {
            // 定期清理失效的键值对
            accessCount++
            if (accessCount >= CLEANUP_THRESHOLD) {
                cleanUpTransientCache()
                accessCount = 0
            }
            
            // 构造查找用的Key
            // 注意：这里不仅是为了查找，如果需要插入，这个Key也会被存储
            val lookupKey = WeakKey(imageBitmap)
            
            // 检查缓存
            transientCache[lookupKey]?.let { holder ->
                transientHits++
                LogPrintUtil.verbose {
                    "PixelMapCacheManager.Transient: HIT for ImageBitmap@${imageBitmap.hashCode()}, " +
                    "pixelMap=${holder.handle} (hits=$transientHits, misses=$transientMisses)"
                }
                return holder.handle
            }
            
            // 缓存未命中，创建新的
            transientMisses++
            LogPrintUtil.verbose {
                "PixelMapCacheManager.Transient: MISS for ImageBitmap@${imageBitmap.hashCode()}"
            }
            
            val pixelMapPtr = imageBitmap.asNativePixelMap() ?: return null
            val holder = TransientPixelMapHolder(pixelMapPtr)
            transientCache[lookupKey] = holder
            
            LogPrintUtil.verbose {
                "PixelMapCacheManager.Transient: created pixelMap=$pixelMapPtr, cache size=${transientCache.size}"
            }
            
            return pixelMapPtr
        }
    }
    
    /**
     * 清理失效的WeakReference条目
     */
    private fun cleanUpTransientCache() {
        val iterator = transientCache.iterator()
        var removedCount = 0
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key.get() == null) {
                // ImageBitmap已被GC，holder也会随之被移除引用
                // TransientPixelMapHolder的cleaner机制会负责释放Native资源
                iterator.remove()
                removedCount++
            }
        }
        if (removedCount > 0) {
            LogPrintUtil.verbose {
                "PixelMapCacheManager.Transient: Cleanup removed $removedCount stale entries"
            }
        }
    }
    
    // ========== 持久缓存（文本渲染，独立生命周期） ==========
    
    private val persistentCacheLock = SynchronizedObject()
    private val persistentCache = mutableMapOf<Int, PersistentPixelMapHolder>()
    
    private const val MAX_PERSISTENT_CACHE_SIZE = 500
    
    @Volatile
    private var persistentHits: Long = 0
    
    @Volatile
    private var persistentMisses: Long = 0
    
    /**
     * 缓存持久PixelMap（用于文本渲染）
     * 
     * 创建独立的PixelMap，不与临时缓存共享。
     * 使用LRU策略管理，上限50个条目。
     * 
     * @param paragraphHashCode 文本段落哈希码
     * @param imageBitmap 渲染结果ImageBitmap
     * @return PixelMap指针，失败返回null
     */
    fun cachePersistentPixelMap(
        paragraphHashCode: Int,
        imageBitmap: ImageBitmap
    ): COpaquePointer? {
        synchronized(persistentCacheLock) {
            // 检查是否已缓存
            persistentCache[paragraphHashCode]?.let { holder ->
                persistentHits++
                LogPrintUtil.verbose {
                    "PixelMapCacheManager.Persistent: HIT for hashCode=$paragraphHashCode, " +
                    "pixelMap=${holder.handle}, refCount=${holder.getRefCount()}"
                }
                return holder.handle
            }
            
            // 缓存未命中，创建新的PixelMap
            persistentMisses++
            LogPrintUtil.verbose {
                "PixelMapCacheManager.Persistent: MISS for hashCode=$paragraphHashCode, creating..."
            }
            
            // 创建新的PixelMap（独立于transientCache）
            val pixelMapPtr = imageBitmap.asNativePixelMap() ?: return null
            val holder = PersistentPixelMapHolder(pixelMapPtr, paragraphHashCode)
            
            // LRU驱逐策略
            if (persistentCache.size >= MAX_PERSISTENT_CACHE_SIZE) {
                evictLRU()
            }
            
            persistentCache[paragraphHashCode] = holder
            
            LogPrintUtil.verbose {
                "PixelMapCacheManager.Persistent: created pixelMap=$pixelMapPtr for hashCode=$paragraphHashCode, " +
                "cache size=${persistentCache.size}"
            }
            
            return pixelMapPtr
        }
    }
    
    /**
     * 获取持久PixelMap
     * 
     * @param paragraphHashCode 文本段落哈希码
     * @return PixelMap指针if存在，否则null
     */
    fun getPersistentPixelMap(paragraphHashCode: Int): COpaquePointer? {
        synchronized(persistentCacheLock) {
            val holder = persistentCache[paragraphHashCode]
            if (holder != null) {
                persistentHits++
                LogPrintUtil.verbose {
                    "PixelMapCacheManager.Persistent: GET for hashCode=$paragraphHashCode, pixelMap=${holder.handle}"
                }
                return holder.handle
            }
            return null
        }
    }
    
    /**
     * LRU驱逐策略（简化版）
     * 
     * 当缓存达到上限时，移除第一个条目。
     * TODO: 实现基于lastAccessTime的真正LRU
     */
    private fun evictLRU() {
        synchronized(persistentCacheLock) {
            if (persistentCache.isEmpty()) return
            
            // 简化：移除第一个
            val (hashCode, holder) = persistentCache.entries.first()
            persistentCache.remove(hashCode)
            
            // 减少引用计数
            if (holder.releaseRef()) {
                holder.dispose()  // 引用计数归零，释放native资源
            }
            
            LogPrintUtil.verbose {
                "PixelMapCacheManager.Persistent: LRU evicted hashCode=$hashCode, cache size=${persistentCache.size}"
            }
        }
    }
    
    // ========== 统一清理接口 ==========
    
    /**
     * 清空所有缓存
     */
    fun clearAll() {
        clearTransientCache()
        clearPersistentCache()
    }
    
    /**
     * 清空临时缓存
     */
    fun clearTransientCache() {
        synchronized(transientCacheLock) {
            val oldSize = transientCache.size
            transientCache.values.forEach { it.dispose() }
            transientCache.clear()
            
            LogPrintUtil.verbose {
                "PixelMapCacheManager: cleared transient cache (released $oldSize entries)"
            }
        }
    }
    
    /**
     * 清空持久缓存
     */
    fun clearPersistentCache() {
        synchronized(persistentCacheLock) {
            val oldSize = persistentCache.size
            persistentCache.values.forEach { holder ->
                if (holder.releaseRef()) {
                    holder.dispose()
                }
            }
            persistentCache.clear()
            
            LogPrintUtil.verbose {
                "PixelMapCacheManager: cleared persistent cache (released $oldSize entries)"
            }
        }
    }
    
    // ========== 统计和监控 ==========
    
    /**
     * 获取详细统计信息
     */
    fun getDetailedStats(): Map<String, Any> {
        synchronized(transientCacheLock) {
            synchronized(persistentCacheLock) {
                val transientHitRate = if (transientHits + transientMisses > 0) {
                    (transientHits.toFloat() / (transientHits + transientMisses) * 100f)
                } else {
                    0f
                }
                
                val persistentHitRate = if (persistentHits + persistentMisses > 0) {
                    (persistentHits.toFloat() / (persistentHits + persistentMisses) * 100f)
                } else {
                    0f
                }
                
                return mapOf(
                    "transient" to mapOf(
                        "hits" to transientHits,
                        "misses" to transientMisses,
                        "hitRate" to transientHitRate,
                        "cacheSize" to transientCache.size
                    ),
                    "persistent" to mapOf(
                        "hits" to persistentHits,
                        "misses" to persistentMisses,
                        "hitRate" to persistentHitRate,
                        "cacheSize" to persistentCache.size,
                        "maxSize" to MAX_PERSISTENT_CACHE_SIZE
                    )
                )
            }
        }
    }
    
    /**
     * 打印统计信息
     */
    fun printStats() {
        val stats = getDetailedStats()
        val transient = stats["transient"] as Map<*, *>
        val persistent = stats["persistent"] as Map<*, *>
        
        LogPrintUtil.verbose {
            "PixelMapCacheManager Statistics:\n" +
            "  Transient Cache (普通图片):\n" +
            "    ├─ Hits: ${transient["hits"]}\n" +
            "    ├─ Misses: ${transient["misses"]}\n" +
            "    ├─ Hit Rate: ${transient["hitRate"]}%\n" +
            "    └─ Size: ${transient["cacheSize"]}\n" +
            "  Persistent Cache (文本渲染):\n" +
            "    ├─ Hits: ${persistent["hits"]}\n" +
            "    ├─ Misses: ${persistent["misses"]}\n" +
            "    ├─ Hit Rate: ${persistent["hitRate"]}%\n" +
            "    ├─ Size: ${persistent["cacheSize"]} / ${persistent["maxSize"]}\n" +
            "    └─ Status: ${if ((persistent["cacheSize"] as Int) < MAX_PERSISTENT_CACHE_SIZE) "✅" else "⚠️ FULL"}"
        }
    }
    
    /**
     * 重置统计信息
     */
    fun resetStats() {
        transientHits = 0
        transientMisses = 0
        persistentHits = 0
        persistentMisses = 0
        LogPrintUtil.verbose { "PixelMapCacheManager: Statistics reset" }
    }
}

/**
 * ImageBitmap扩展函数：获取缓存的PixelMap（临时模式）
 * 
 * 用于普通图片绘制，推荐使用方式。
 * 
 * @return PixelMap指针，失败返回null
 */
internal fun ImageBitmap.asNativePixelMapCached(): COpaquePointer? {
    return PixelMapCacheManager.getTransientPixelMap(this)
}

