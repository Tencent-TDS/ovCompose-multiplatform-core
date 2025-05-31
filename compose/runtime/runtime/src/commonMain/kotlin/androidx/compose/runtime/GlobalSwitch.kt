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

package androidx.compose.runtime

enum class PlatformType {
    IOS,
    ANDROID,
    HARMONY,
    UNKNOWN,
}

expect val CurrentPlatform: PlatformType

/** IOSParagraph开关 */
var EnableIOSParagraph = false

var EnableSkiaBackedCanvasLog = false

var DeleteRedundantGraphicsLayer = false

var EnableLocaleListCachedHashCode = false

/* 强制开启 ViewProxy 复用 */
var ForceEnableViewProxyReuse = false

/* 本地开关强制不优化 updateParentLayer 的频率方便各场景快速回滚 */
var ForceDisableReduceUpdateParentLayer = false

var ForceEnablePictureRecorderV2 = true

object GlobalSwitch {
    private val observers = mutableListOf<(Boolean) -> Unit>()

    fun addObserver(observer: (Boolean) -> Unit) {
        observers.add(observer)
    }

    fun removeObserver(observer: (Boolean) -> Unit) {
        observers.remove(observer)
    }

    fun notifyObservers() {
        observers.forEach {
            it(CurrentPlatform == PlatformType.IOS)
        }
    }
}

interface TabService {
    fun isOn(key: String, defaultValue: Boolean): Boolean

    fun getConfigString(key: String): String?

    fun getConfigMap(key: String): Map<String, Any?>?
}

object ComposeTabService {

    // 是否关闭 updateParentLayer 频率优化
    private const val COMPOSE_REDUCE_UPDATE_PARENT_DISABLE = "ios_compose_reduce_update_parent_disable"
    private const val COMPOSE_GESTURE_ENABLE = "ios_compose_gesture"
    private const val COMPOSE_ASYNC_TOUCH_ENABLE = "ios_compose_async_touch"
    private const val VIEW_PROXY_REUSE_ENABLE = "ios_compose_view_proxy_reuse_enable"
    private const val COMPOSE_IOS_SCROLL_ANIMATION_FIX_ENABLE = "ios_compose_scroll_animation_fix"
    private const val COMPOSE_HIT_TEST_LOG_ENABLE = "ios_compose_hit_test_log"
    private const val COMPOSE_HIT_TEST_LOG_TAG = "[compose][hit_test]"
    private const val COMPOSE_IOS_FLING_CONFIG = "ios_compose_fling_config"

    private var tabService: TabService? = null
    private var logService: ((tag: String, msg: String) -> Unit)? = null
    private val isIOSPlatForm: Boolean = CurrentPlatform == PlatformType.IOS

    val composeIOSScrollAnimationFixUpEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_SCROLL_ANIMATION_FIX_ENABLE, false) ?: false)
    }

    val composeGestureEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_GESTURE_ENABLE, true) ?: true)
    }

    val composeIOSHitTestLogEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_HIT_TEST_LOG_ENABLE, false) ?: false)
    }

    /**
     * Reuse this key for dispatching render event asynchronously.
     */
    val sendTouchesAsynchronously: Boolean by lazy {
        // default to true, disable this if issue occurs with remote config.
        tabService?.isOn(COMPOSE_ASYNC_TOUCH_ENABLE, true) ?: true
    }

    val viewProxyReuseEnable: Boolean by lazy {
        if (ForceEnableViewProxyReuse) true else tabService?.isOn(VIEW_PROXY_REUSE_ENABLE, false)
            ?: false
    }

    val reduceUpdateParentLayer: Boolean by lazy {
        if (!isIOSPlatForm || ForceDisableReduceUpdateParentLayer) {
            // 本地强制关闭，返回 false
            false
        } else {
            val currentTabService = tabService
            if (currentTabService != null) {
                // tab 返回为 true  则说明关闭了该优化
                !currentTabService.isOn(COMPOSE_REDUCE_UPDATE_PARENT_DISABLE, false)
            } else {
                true
            }
        }
    }

    // 异步绘制可能导致文本偏移,暂时屏蔽
    val textAsyncPaint: Boolean by lazy {
        false
//        if (CurrentPlatform == PlatformType.IOS) {
//            true
//        } else {
//            false
//        }
    }

    val iOSFlingConfig: IOSFlingConfig by lazy {
        tabService?.getConfigMap(COMPOSE_IOS_FLING_CONFIG)?.let { map ->
            IOSFlingConfig(
                maximumVelocity = map["maximumVelocity"]?.toString()?.toFloatOrNull()?.takeIf { it > 2000f } ?: Float.MAX_VALUE,
                decelerationRate = map["decelerationRate"]?.toString()?.toFloatOrNull(),
                alwaysUseDataPoints = map["alwaysUseDataPoints"]?.toString()?.toBooleanStrictOrNull() ?: false
            )
        } ?: IOSFlingConfig()
    }

    @Deprecated("Use injectTabService(TabService) instead.")
    fun injectTabService(service: (tabKey: String, defaultValue: Boolean) -> Boolean) {
        tabService = object: TabService {
            override fun isOn(key: String, defaultValue: Boolean) = service(key, defaultValue)
            override fun getConfigString(key: String): String? = null
            override fun getConfigMap(key: String): Map<String, Any?>? = null
        }
    }

    fun injectTabService(service: TabService) {
        tabService = service
    }

    fun injectLogService(service: (tag: String, msg: String) -> Unit) {
        logService = service
    }

    fun composeHitTestLog(msg: String) {
        if (composeIOSHitTestLogEnable) {
            logService?.invoke(COMPOSE_HIT_TEST_LOG_TAG, msg)
        }
    }
}

class IOSFlingConfig(
    val maximumVelocity: Float = Float.MAX_VALUE,
    val decelerationRate: Float? = null,
    val alwaysUseDataPoints: Boolean = false
)

object CrashReporter {
    var reportImpl: ((throwable: Throwable) -> Unit)? = null

    fun reportThrowable(throwable: Throwable) {
        reportImpl?.invoke(throwable)
    }
}

