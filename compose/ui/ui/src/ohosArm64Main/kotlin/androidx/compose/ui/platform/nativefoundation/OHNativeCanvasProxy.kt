package androidx.compose.ui.platform.nativefoundation

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.ui.arkui.utils.Boolean
import androidx.compose.ui.arkui.utils.OHNativeCanvasProxy_Handle
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRect
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_finishDraw
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayer
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_attachToRootView
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setParent
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Paint
import kotlinx.cinterop.*


/**
 * 封装 OHNativeCanvasProxy_Handle 结构体指针的 Kotlin 代理类
 * 提供类型安全和Kotlin风格的API访问Native方法
 */
class OHNativeCanvasProxy(private val handle: OHNativeCanvasProxy_Handle?) {
    /**
     * 画布宽度属性
     */
//    val width: Float
//        get() = handle?.let { OHNativeCanvasProxy_getWidth(it) } ?: 0f

    /**
     * 画布高度属性
     */
//    val height: Float
//        get() = handle?.let { OHNativeCanvasProxy_getHeight(it) } ?: 0f

    /**
     * 透明度属性（可读写）
     */
//    var alpha: Float
//        get() = handle?.let { OHNativeCanvasProxy_getAlpha(it) } ?: 1f
//        set(value) { handle?.let { OHNativeCanvasProxy_setAlpha(it, value) } }

    /**
     * 开始绘制
     */
    fun beginDraw() {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw(it) }
    }

    /**
     * 结束绘制
     */
    fun finishDraw() {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_finishDraw(it) }
    }

    /**
     * 绘制图层
     */
    fun drawLayer() {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayer(it) }
    }

    /**
     * 使用子代理绘制图层
     */
    fun drawLayerWithSubproxy(subproxy: OHNativeCanvasProxy_Handle?) {
//        handle?.let { OHNativeCanvasProxy_drawLayerWithSubproxy(it, subproxy) }
    }

    /**
     * 绘制矩形
     */
    fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRect(it, left, top, right, bottom) }
    }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint) {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine(it, x1, y1, x2, y2) }
    }


    fun setParent(parentProxy: OHNativeCanvasProxy) {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setParent(it, parentProxy.handle) }
    }

    fun attachToRootView() {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_attachToRootView(it) }
    }

    /**
     * 应用变换矩阵
     */
    fun applyTransformMatrix(
        rotationX: Float,
        rotationY: Float,
        rotationZ: Float,
        scaleX: Float,
        scaleY: Float,
        translationX: Float,
        translationY: Float,
        m34Transform: Double
    ) {
//        handle?.let {
//            OHNativeCanvasProxy_applyTransformMatrix(
//                it, rotationX, rotationY, rotationZ,
//                scaleX, scaleY, translationX, translationY, m34Transform
//            )
//        }
    }

    /**
     * 获取快照图像
     */
    fun getSnapshotImage(): Long {
//        return handle?.let { OHNativeCanvasProxy_getSnapshotImage(it) } ?: 0L
        return 0L
    }

    /**
     * 获取指定尺寸的快照图像
     */
    fun getSnapshotImageWithWidth(width: Int, height: Int): Long {
//        return handle?.let { OHNativeCanvasProxy_getSnapshotImageWithWidth(it, width, height) } ?: 0L
        return 0L
    }

    /**
     * 检查是否需要重绘图像
     */
    fun needRedrawImageWithHashCode(hashCode: Int, width: Int, height: Int): Boolean {
//        return handle?.let { OHNativeCanvasProxy_needRedrawImageWithHashCode(it, hashCode, width, height) } ?: false
        return true
    }
}