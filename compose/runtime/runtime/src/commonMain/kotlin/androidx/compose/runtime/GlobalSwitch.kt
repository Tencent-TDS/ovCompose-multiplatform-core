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

var EnableLocaleListCachedHashCode = false

/* 强制开启 ViewProxy 复用 */
var ForceEnableViewProxyReuse = false

/* 本地开关强制不优化 updateParentLayer 的频率方便各场景快速回滚 */
var ForceDisableReduceUpdateParentLayer = false

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

    fun isOn(key: String, defaultValue: Boolean, needReport:Boolean = true): Boolean

    fun getConfigString(key: String): String?

    fun getConfigMap(key: String): Map<String, Any?>?

    fun getConfigGrayPolicyId(key: String, needReport: Boolean): Int

    fun getToggleGrayPolicyId(key: String, needReport: Boolean): Int
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

    private const val COMPOSE_IOS_DISABLE_NATIVE_VIEW_FORCE_TOUCH = "ios_compose_disable_native_view_force_touch"

    private const val COMPOSE_SEMANTIC_SORT_ENABLE = "ios_semantic_sort_enable"
    private const val COMPOSE_IOS_TEXT_FIELD_STATE_CLEAN_ENABLE = "ios_text_field_state_clean_enable"
    private const val COMPOSE_IOS_KEYBOARD_DICTATION_LAYOUT_ENABLE = "ios_keyboard_diction_layout_enable"
    private const val COMPOSE_IOS_SKIA_ANR_TEXT_LAYOUT_FIX_ENABLE = "enable_skia_text_layout_anr_fix"
    private const val COMPOSE_SKIA_ANR_ALL_FIX_DISABLE = "disable_skia_all_crash_fix"
    private const val COMPOSE_DOUBLE_TAP_CONFIG = "ios_compose_double_tap_config"
    private const val COMPOSE_IOS_VELOCITY_TRACKER_FIX_ENABLE = "ios_compose_velocity_track_fix_enable"
    private const val COMPOSE_IOS_VELOCITY_MINI_COUNT_FIX_ENABLE = "ios_compose_velocity_track_mini_count_fix_enable"
    private const val COMPOSE_VIEW_CONFIGURATION_TOUCH_DIRECTION_FACTOR = "compose_view_configuration_touch_direction_factor"
    private const val COMPOSE_NATIVE_A11Y_ENABLE = "ios_compose_a11y_native_enable"
    private const val COMPOSE_THREAD_PROTECTION_ENABLE = "ios_compose_thread_protection_enable"
    private const val COMPOSE_DURING_MEASURE_LAYOUT_CRASH_FIX_ENABLE = "ios_compose_during_measure_layout_enable"

    private const val COMPOSE_LOCK_OPTIMIZATION_ENABLED = "compose_lock_optimization_enabled"
    private const val COMPOSE_FORCE_ZERO_TOUCH_SIZE_ENABLED = "compose_force_zero_touch_enabled"

    private const val COMPOSE_HARMONY_KEY_EVENT_ENABLED = "compose_harmony_key_event_enabled"

    private const val COMPOSE_IOS_REPLACE_RANGE_FINISH_ENABLED = "ios_compose_replace_range_finish_enabled"
    private const val COMPOSE_IOS_CORRECT_COMPOSITION_ENABLED = "ios_compose_correct_composition_enabled"
    private const val COMPOSE_IOS_NULL_TEXT_OPT_ENABLED = "ios_compose_null_text_opt_enabled"

    private const val COMPOSE_IOS_TEXT_RECORDER_PAINT_ENABLED = "ios_compose_text_recorder_paint"
    private const val COMPOSE_IOS_CLOSE_ALL_ASYNC_PAINT_ENABLED = "ios_compose_close_all_async_paint_enable"

    private const val COMPOSE_ENABLE_FAST_LOG = "ios_compose_fast_log_enable"
    private const val COMPOSE_IOS_TEXT_LEAK_FIX_CONFIG = "ios_global_text_leak_fix_config"

    var tabService: TabService? = null
    var logService: ((tag: String, msg: String) -> Unit)? = null
    private val isIOSPlatForm: Boolean = CurrentPlatform == PlatformType.IOS

    val composeFastLogEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_ENABLE_FAST_LOG, true) ?: true)
    }

    val composeIOSScrollAnimationFixUpEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_SCROLL_ANIMATION_FIX_ENABLE, false) ?: false)
    }

    val composeTextRecorderPaintEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_TEXT_RECORDER_PAINT_ENABLED, false) ?: false)
    }

    val closeAsyncPaintEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_CLOSE_ALL_ASYNC_PAINT_ENABLED, false) ?: false)
    }

    val composeAccessibilityNativeEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_NATIVE_A11Y_ENABLE, true) ?: true)
    }

    val composeGestureEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_GESTURE_ENABLE, true) ?: true)
    }

    val skiaSwitchStateANRFixEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_SKIA_ANR_TEXT_LAYOUT_FIX_ENABLE, false)
            ?: false) && !(tabService?.isOn(COMPOSE_SKIA_ANR_ALL_FIX_DISABLE, false)
            ?: false)
    }

    val threadProtectionEnable by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_THREAD_PROTECTION_ENABLE, false)
            ?: false) && !(tabService?.isOn(COMPOSE_SKIA_ANR_ALL_FIX_DISABLE, false)
            ?: false)
    }

    val iosTextLeakFixType by lazy {
        var type = 0
        tabService?.getConfigMap(COMPOSE_IOS_TEXT_LEAK_FIX_CONFIG)?.let { map ->
            type = map["type"]?.toString()?.toIntOrNull()?.takeIf { it >= 0 } ?: 0
        }
        type
    }

    val duringMeasureLayoutCrashFixEnable by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_DURING_MEASURE_LAYOUT_CRASH_FIX_ENABLE, false)
            ?: false) && !(tabService?.isOn(COMPOSE_SKIA_ANR_ALL_FIX_DISABLE, false)
            ?: false)
    }

    val composeIOSHitTestLogEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_HIT_TEST_LOG_ENABLE, false) ?: false)
    }

    val composeIOSNativeViewDisableForceTouch: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_DISABLE_NATIVE_VIEW_FORCE_TOUCH, false) ?: false)
    }

    val composeIOSSemanticSortEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_SEMANTIC_SORT_ENABLE, true) ?: true)
    }

    val composeIOSTextFieldStateCleanEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_TEXT_FIELD_STATE_CLEAN_ENABLE, true) ?: true)
    }

    val composeIOSKeyboardDictationLayoutEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_KEYBOARD_DICTATION_LAYOUT_ENABLE, true) ?: true)
    }

    val composeIOSVelocityTrackerAddPointsFixEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_VELOCITY_TRACKER_FIX_ENABLE, false) ?: false)
    }

    val composeIOSVelocityTrackerMiniCountFixEnable: Boolean by lazy {
        isIOSPlatForm && (tabService?.isOn(COMPOSE_IOS_VELOCITY_MINI_COUNT_FIX_ENABLE, false) ?: false)
    }

    val composeForceZeroTouchSizeEnable: Boolean by lazy {
         tabService?.isOn(COMPOSE_FORCE_ZERO_TOUCH_SIZE_ENABLED, false) ?: false
    }

    val composeIOSNullTextOptEnable: Boolean by lazy {
        isIOSPlatForm && tabService?.isOn(COMPOSE_IOS_NULL_TEXT_OPT_ENABLED, false) ?: false
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

    val doubleTapConfig: DoubleTapConfig by lazy {
        val configMap = tabService?.getConfigMap(COMPOSE_DOUBLE_TAP_CONFIG) ?: return@lazy DoubleTapConfig()
        val enableMode = configMap["enableMode"]?.toString()?.toIntOrNull() ?: return@lazy DoubleTapConfig()
        // 需要额外标记位，标记为开启，才可以，防止意外情况
        if (enableMode == 1) {
            DoubleTapConfig(
                doubleTapTimeoutMillis = configMap["max"]?.toString()?.toLongOrNull() ?: 300L,
                doubleTapMinTimeMillis = configMap["min"]?.toString()?.toLongOrNull() ?: 40L,
            )
        } else {
            DoubleTapConfig()
        }
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

    /**
     * Factor to determine the direction of the scroll, the larger the value, the stricter the direction of the gesture,
     * for example, 0 means no limit, 1 means equalization.
     */
    val touchDirectionFactor: Float by lazy {
        if (!isIOSPlatForm) return@lazy 0f
        tabService?.getConfigString(COMPOSE_VIEW_CONFIGURATION_TOUCH_DIRECTION_FACTOR)
            ?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f
    }

    val lockOpt: Boolean by lazy {
        tabService?.isOn(COMPOSE_LOCK_OPTIMIZATION_ENABLED, false) ?: false
    }

    /**
     * 是否开启鸿蒙平台 KeyEvent 适配
     */
    val harmonyKeyEventEnabled: Boolean by lazy {
        tabService?.isOn(COMPOSE_HARMONY_KEY_EVENT_ENABLED, true) ?: true
    }

    val iosReplaceRangeEnabled: Boolean by lazy {
        tabService?.isOn(COMPOSE_IOS_REPLACE_RANGE_FINISH_ENABLED, false) ?: false
    }

    val iosUIKitInputCorrectCompositionEnabled: Boolean by lazy {
        tabService?.isOn(COMPOSE_IOS_CORRECT_COMPOSITION_ENABLED, false) ?: false
    }

    @Deprecated("Use injectTabService(TabService) instead.")
    fun injectTabService(service: (tabKey: String, defaultValue: Boolean) -> Boolean) {
        tabService = object: TabService {
            override fun isOn(key: String, defaultValue: Boolean): Boolean = service(key, defaultValue)
            override fun isOn(key: String, defaultValue: Boolean, needReport: Boolean) = service(key, defaultValue)
            override fun getConfigString(key: String): String? = null
            override fun getConfigMap(key: String): Map<String, Any?>? = null
            override fun getConfigGrayPolicyId(key: String, needReport: Boolean): Int = 0
            override fun getToggleGrayPolicyId(key: String, needReport: Boolean): Int = 0
        }
    }

    fun injectTabService(service: TabService) {
        tabService = service
    }

    fun injectTMMNativeTabService() {
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

class DoubleTapConfig(
    val doubleTapTimeoutMillis: Long = 300,
    val doubleTapMinTimeMillis: Long = 40
)

object CrashReporter {
    var reportImpl: ((throwable: Throwable) -> Unit)? = null

    fun reportThrowable(throwable: Throwable) {
        reportImpl?.invoke(throwable)
    }
}

