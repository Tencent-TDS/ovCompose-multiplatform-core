@file:Suppress("FunctionName")

package androidx.compose.export.ui.arkui

import androidx.compose.export.annotation.InternalExportApi
import androidx.compose.ui.platform.nativefoundation._invokeKotlinAsyncTask
import androidx.compose.ui.platform.nativefoundation._invokeKotlinMainThreadCallback


/**
 * C++回调函数，用于异步执行Kotlin lambda
 * 这个函数会被C++侧调用（在后台线程）
 *
 * @param stableRefPtr StableRef指针，指向Kotlin lambda
 * @return 执行结果（PixelMap指针）
 */
@InternalExportApi
@CName("invokeKotlinAsyncTask")
fun _Export_invokeKotlinAsyncTask(stableRefPtr: Long) = _invokeKotlinAsyncTask(stableRefPtr)

@InternalExportApi
@CName("invokeKotlinMainThreadCallback")
fun _Export_invokeKotlinMainThreadCallback(
    stableRefPtr: Long,
    renderNodePtr: Long,
    pixelMapPtr: Long
) =
    _invokeKotlinMainThreadCallback(stableRefPtr, renderNodePtr, pixelMapPtr)


