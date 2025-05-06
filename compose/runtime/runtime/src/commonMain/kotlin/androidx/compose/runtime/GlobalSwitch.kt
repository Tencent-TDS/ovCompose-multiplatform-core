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

var EnableIosRenderLayerV2 = false
    set(value) {
        field = value
        GlobalSwitch.notifyObservers()
    }

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
            it(EnableIosRenderLayerV2)
        }
    }
}

object ComposeTabService {

    // 是否关闭 updateParentLayer 频率优化
    private const val COMPOSE_REDUCE_UPDATE_PARENT_DISABLE = "ios_compose_reduce_update_parent_disable"
    private const val COMPOSE_GESTURE_ENABLE = "ios_compose_gesture"
    private const val COMPOSE_ASYNC_TOUCH_ENABLE = "ios_compose_async_touch"
    private const val VIEW_PROXY_REUSE_ENABLE = "ios_compose_view_proxy_reuse_enable"
    private const val COMPOSE_IOS_SCROLL_ANIMATION_FIX_ENABLE = "ios_compose_scroll_animation_fix"
    private var tabService: ((tabKey: String, defaultValue: Boolean) -> Boolean)? = null
    private const val COMPOSE_HIT_TEST_LOG_ENABLE = "ios_compose_hit_test_log"
    private const val COMPOSE_HIT_TEST_LOG_TAG = "[compose][hit_test]"
    private var logService: ((tag: String, msg: String) -> Unit)? = null

    val composeIOSScrollAnimationFixUpEnable: Boolean by lazy {
        EnableIosRenderLayerV2 && (tabService?.invoke(COMPOSE_IOS_SCROLL_ANIMATION_FIX_ENABLE, false) ?: false)
    }

    val composeGestureEnable: Boolean by lazy {
        EnableIosRenderLayerV2 && (tabService?.invoke(COMPOSE_GESTURE_ENABLE, false) ?: false)
    }

    val composeIOSHitTestLogEnable: Boolean by lazy {
        EnableIosRenderLayerV2 && (tabService?.invoke(COMPOSE_HIT_TEST_LOG_ENABLE, false) ?: false)
    }

    /**
     * Reuse this key for dispatching render event asynchronously.
     */
    val sendTouchesAsynchronously: Boolean by lazy {
        // default to true, disable this if issue occurs with remote config.
        tabService?.invoke(COMPOSE_ASYNC_TOUCH_ENABLE, true) ?: true
    }

    val viewProxyReuseEnable: Boolean by lazy {
        if (ForceEnableViewProxyReuse) true else tabService?.invoke(VIEW_PROXY_REUSE_ENABLE, false)
            ?: false
    }

    val reduceUpdateParentLayer: Boolean by lazy {
        if (!EnableIosRenderLayerV2 || ForceDisableReduceUpdateParentLayer) {
            // 本地强制关闭，返回 false
            false
        } else {
            val currentTabService = tabService
            if (currentTabService != null) {
                // tab 返回为 true  则说明关闭了该优化
                !currentTabService.invoke(COMPOSE_REDUCE_UPDATE_PARENT_DISABLE, false)
            } else {
                true
            }
        }
    }

    // 已全量
    val textAsyncPaint: Boolean by lazy {
        if (EnableIosRenderLayerV2) {
            true
        } else {
            false
        }
    }

    fun injectTabService(service: (tabKey: String, defaultValue: Boolean) -> Boolean) {
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

object CrashReporter {
    var reportImpl: ((throwable: Throwable) -> Unit)? = null

    fun reportThrowable(throwable: Throwable) {
        reportImpl?.invoke(throwable)
    }
}

