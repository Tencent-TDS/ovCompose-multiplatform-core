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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.JsFunction
import androidx.compose.ui.napi.JsObject
import androidx.compose.ui.napi.asInt
import androidx.compose.ui.napi.asJsObject
import androidx.compose.ui.napi.jsFunction
import androidx.compose.ui.platform.AvoidAreaType.TYPE_CUTOUT
import androidx.compose.ui.platform.AvoidAreaType.TYPE_NAVIGATION_INDICATOR
import androidx.compose.ui.platform.AvoidAreaType.TYPE_SYSTEM
import androidx.compose.ui.platform.AvoidAreaType.TYPE_SYSTEM_GESTURE
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlin.reflect.KProperty

/**
 * Composition local for SafeArea of ComposeUIViewController
 */
@InternalComposeApi
val LocalSafeArea = staticCompositionLocalOf { PlatformInsets.Zero }

/**
 * Composition local for layoutMargins of ComposeUIViewController
 */
@InternalComposeApi
val LocalLayoutMargins = staticCompositionLocalOf { PlatformInsets.Zero }

@OptIn(InternalComposeApi::class)
private object SafeAreaInsetsConfig : InsetsConfig {
    override val safeInsets: PlatformInsets
        @Composable get() = LocalSafeArea.current

    @Composable
    override fun excludeSafeInsets(content: @Composable () -> Unit) {
        val safeArea = LocalSafeArea.current
        val layoutMargins = LocalLayoutMargins.current
        CompositionLocalProvider(
            LocalSafeArea provides PlatformInsets(),
            LocalLayoutMargins provides layoutMargins.exclude(safeArea),
            content = content
        )
    }
}

internal actual var PlatformInsetsConfig: InsetsConfig = SafeAreaInsetsConfig

@InternalComposeApi
val LocalPlatformInsetsHolder = staticCompositionLocalOf<PlatformInsetsHolder> {
    error("No PlatformInsetsHolder provided")
}

@InternalComposeApi
class PlatformInsetsHolder(context: Context, lifecycle: Lifecycle) {
    val statusBars by LazyStateHolder { initInsetsValue(context, TYPE_SYSTEM) }
    val displayCutout by LazyStateHolder { initInsetsValue(context, TYPE_CUTOUT) }
    val systemGestures by LazyStateHolder { initInsetsValue(context, TYPE_SYSTEM_GESTURE) }
    val navigationBars by LazyStateHolder { initInsetsValue(context, TYPE_NAVIGATION_INDICATOR) }

    private var callback: JsFunction<PlatformInsetsHolder, Unit>? = null

    init {
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    bindListener(context)
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    unbindListener(context)
                }
            }
        )
    }

    private fun bindListener(context: Context) {

        if (callback != null) {
            unbindListener(context)
        }

        val callback = jsFunction(this) { data: JsObject? ->

            data ?: return@jsFunction

            val typeOrdinal = data["type"].asInt() ?: return@jsFunction
            val type = AvoidAreaType.entries[typeOrdinal]

            // 键盘已在ets侧实现
            if (type == AvoidAreaType.TYPE_KEYBOARD) {
                return@jsFunction
            }
            val area = data["area"].asJsObject()
            when (type) {
                TYPE_SYSTEM -> statusBars.value = platformInsets(area)
                TYPE_CUTOUT -> displayCutout.value = cutoutInsets(area, context)
                TYPE_SYSTEM_GESTURE -> systemGestures.value = platformInsets(area)
                TYPE_NAVIGATION_INDICATOR -> navigationBars.value = platformInsets(area)
                else -> {}
            }
        }
        getMainWindowSync(context).call(
            "on",
            JsEnv.createStringUtf8("avoidAreaChange"),
            callback.jsValue
        )
        this.callback = callback
    }

    private fun unbindListener(context: Context) {
        val callback = callback ?: return
        getMainWindowSync(context).call(
            "off",
            JsEnv.createStringUtf8("avoidAreaChange"),
            callback.jsValue
        )
        callback.dispose()
        this.callback = null
    }

    private fun initInsetsValue(context: Context, type: AvoidAreaType): PlatformInsetsValues {
        val avoidArea = getMainWindowSync(context)
            .call("getWindowAvoidArea", JsEnv.createInt32(type.ordinal))
            .asJsObject()
        return if (type == TYPE_CUTOUT) cutoutInsets(avoidArea, context) else platformInsets(
            avoidArea
        )
    }

    private fun platformInsets(area: JsObject) = PlatformInsetsValues(
        left = area["leftRect"].asJsObject()["width"].asInt() ?: 0,
        top = area["topRect"].asJsObject()["height"].asInt() ?: 0,
        right = area["rightRect"].asJsObject()["width"].asInt() ?: 0,
        bottom = area["bottomRect"].asJsObject()["height"].asInt() ?: 0
    )

    /**
     * 单独适配刘海
     *
     * 1、在左侧: leftRect.left + leftRect.width
     * 2、在顶部: topRect.top + topRect.height
     * 3、在右侧: screenWidth - rightRect.left
     * 4、在底部: screenHeight - bottomRect.top
     * */
    private fun cutoutInsets(area: JsObject, context: Context): PlatformInsetsValues {
        val leftRect = area["leftRect"].asJsObject()
        val left = (leftRect["width"].asInt() ?: 0) + (leftRect["left"].asInt() ?: 0)

        val topRect = area["topRect"].asJsObject()
        val top = (topRect["top"].asInt() ?: 0) + (topRect["height"].asInt() ?: 0)

        val rightRect = area["rightRect"].asJsObject()
        //刘海在屏幕右侧
        val right = if ((rightRect["width"].asInt() ?: 0) > 0) {
            getWindowWidth(context) - (rightRect["left"].asInt() ?: 0)
        } else {
            0
        }

        val bottomRect = area["bottomRect"].asJsObject()
        //刘海在屏幕底部
        val bottom = if ((bottomRect["height"].asInt() ?: 0) > 0) {
            getWindowHeight(context) - (bottomRect["top"].asInt() ?: 0)
        } else {
            0
        }
        return PlatformInsetsValues(left = left, top = top, right = right, bottom = bottom)
    }

    private fun getWindowWidth(context: Context): Int {
        return getWindowRect(context).asJsObject()["width"].asInt() ?: 0
    }

    private fun getWindowHeight(context: Context): Int {
        return getWindowRect(context).asJsObject()["height"].asInt() ?: 0
    }

    private fun getWindowRect(context: Context) = getMainWindowSync(context)
        .call("getWindowProperties")
        .asJsObject()["windowRect"]

    private fun getMainWindowSync(context: Context) =
        context.rawValue.asJsObject()["windowStage"].asJsObject().call("getMainWindowSync")
            .asJsObject()

    companion object {
        @Composable
        inline fun current(): PlatformInsetsHolder {
            return LocalPlatformInsetsHolder.current
        }

    }
}

class LazyStateHolder<T>(private val initBlock: () -> T) {
    private var cachedValue: T? = null
    private var state: MutableState<T>? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): MutableState<T> {
        if (state == null) {
            state = mutableStateOf(cachedValue ?: initBlock())
        }
        return state!!
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (state == null) {
            cachedValue = value
        } else {
            state!!.value = value
        }
    }

}

@Stable
data class PlatformInsetsValues(val left: Int, val top: Int, val right: Int, val bottom: Int)

// as @ohos/window#AvoidAreaType
private enum class AvoidAreaType {
    TYPE_SYSTEM, // 状态栏
    TYPE_CUTOUT, // 刘海
    TYPE_SYSTEM_GESTURE, // 系统手势区域
    TYPE_KEYBOARD, // 键盘区域--已在ets侧实现
    TYPE_NAVIGATION_INDICATOR // 导航条区域
}