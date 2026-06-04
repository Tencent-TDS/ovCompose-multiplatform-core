package androidx.compose.ui.platform.input.key


import androidx.compose.ui.annotation.InternalComposeApi
import androidx.compose.ui.napi.JsEnv
import androidx.compose.ui.napi.NapiValue
import androidx.compose.ui.napi.NapiValueImpl
import androidx.compose.ui.napi.asInt
import platform.ohos.napi_value

/**
 * 平台 KeyEvent
 *
 * @author gavinbaoliu
 * @since 2025/12/5
 */
@InternalComposeApi
interface PlatformKeyEvent {
    /**
     * 按键的 Unicode 码值。支持范围为非空格的基本拉丁字符：0x0021-0x007E，不支持字符为0。组合键场景下，返回当前keyEvent对应按键的Unicode码值。
     */
    val unicode: Int
}

/**
 * 构造平台 KeyEvent
 */
@InternalComposeApi
inline fun PlatformKeyEvent(value: napi_value): PlatformKeyEvent = PlatformKeyEventImpl(value)

@PublishedApi
@InternalComposeApi
internal class PlatformKeyEventImpl(value: napi_value) : PlatformKeyEvent, NapiValue by NapiValueImpl(value) {
    override val unicode: Int by lazy { JsEnv.getNamedProperty(rawValue, "unicode").asInt() ?: 0 }
}