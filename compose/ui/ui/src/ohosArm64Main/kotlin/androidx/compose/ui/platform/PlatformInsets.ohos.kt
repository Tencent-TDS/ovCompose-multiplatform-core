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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.arkui.ArkUIViewController
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.JsFunction
import androidx.compose.ui.napi.JsObject
import androidx.compose.ui.napi.asInt
import androidx.compose.ui.napi.asJsObject
import androidx.compose.ui.napi.jsFunction
import androidx.compose.ui.platform.AvoidAreaType.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

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
class PlatformInsetsHolder internal constructor(owner: ArkUIViewController) {

    var displayCutout by mutableStateOf(initInsetsValue(owner.context, TYPE_CUTOUT))
    var ime by mutableStateOf(initInsetsValue(owner.context, TYPE_KEYBOARD))
    var navigationBars by mutableStateOf(initInsetsValue(owner.context,TYPE_NAVIGATION_INDICATOR))
    var statusBars by mutableStateOf(initInsetsValue(owner.context, TYPE_SYSTEM))
    var systemGestures by mutableStateOf(initInsetsValue(owner.context, TYPE_SYSTEM_GESTURE))

    private var callback: JsFunction<PlatformInsetsHolder, Unit>? = null

    init {
        owner.lifecycle.addObserver(
            object : DefaultLifecycleObserver{
                override fun onCreate(owner: LifecycleOwner) {
                    if (owner is ArkUIViewController) {
                        bindListener(owner.context)
                    }
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    if (owner is ArkUIViewController) {
                        unbindListener(owner.context)
                    }
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

            val area = data["area"].asJsObject()
            val insets = platformInsets(area)

            when (type) {
                TYPE_SYSTEM -> statusBars = insets
                TYPE_CUTOUT -> displayCutout = insets
                TYPE_SYSTEM_GESTURE -> systemGestures = insets
                TYPE_KEYBOARD -> ime = insets
                TYPE_NAVIGATION_INDICATOR -> navigationBars = insets
            }
        }

        context.rawValue.asJsObject()
            .get("windowStage").asJsObject()
            .call("getMainWindowSync").asJsObject()
            .call("on", JsEnv.createStringUtf8("avoidAreaChange"), callback.jsValue)

        this.callback = callback
    }

    private fun unbindListener(context: Context) {

        val callback = callback ?: return

        context.rawValue.asJsObject()
            .get("windowStage").asJsObject()
            .call("getMainWindowSync").asJsObject()
            .call("off", JsEnv.createStringUtf8("avoidAreaChange"), callback.jsValue)

        callback.dispose()
        this.callback = null
    }

    private fun initInsetsValue(context: Context, type: AvoidAreaType): PlatformInsetsValues {

        val avoidArea = context.rawValue.asJsObject()
            .get("windowStage").asJsObject()
            .call("getMainWindowSync").asJsObject()
            .call("getWindowAvoidArea", JsEnv.createInt32(type.ordinal)).asJsObject()

        return platformInsets(avoidArea)

    }

    private fun platformInsets(area: JsObject) = PlatformInsetsValues(
        left = area["leftRect"].asJsObject()["width"].asInt() ?: 0,
        top = area["topRect"].asJsObject()["height"].asInt() ?: 0,
        right = area["rightRect"].asJsObject()["width"].asInt() ?: 0,
        bottom = area["bottomRect"].asJsObject()["height"].asInt() ?: 0
    )

    companion object {

        @Composable
        fun current(): PlatformInsetsHolder {
            return LocalPlatformInsetsHolder.current
        }

    }
}

@InternalComposeApi
@Immutable
class PlatformInsetsValues(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    companion object {
        val Zero = PlatformInsetsValues(0, 0, 0, 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is PlatformInsetsValues) {
            return false
        }

        return left == other.left &&
            top == other.top &&
            right == other.right &&
            bottom == other.bottom
    }

    override fun hashCode(): Int {
        var result = left
        result = 31 * result + top
        result = 31 * result + right
        result = 31 * result + bottom
        return result
    }

    override fun toString(): String =
        "PlatformInsetsValues(left=$left, top=$top, right=$right, bottom=$bottom)"
}

// as @ohos/window#AvoidAreaType
private enum class AvoidAreaType {
    TYPE_SYSTEM, // actually is statusBars
    TYPE_CUTOUT,
    TYPE_SYSTEM_GESTURE,
    TYPE_KEYBOARD,
    TYPE_NAVIGATION_INDICATOR
}