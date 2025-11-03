package androidx.compose.ui.platform.nativefoundation

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.ui.arkui.utils.BaseRenderNode_Handle
import androidx.compose.ui.arkui.utils.Boolean
import androidx.compose.ui.arkui.utils.OHNativeCanvasProxy_Handle
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_DisposeOHNativeCanvasProxy
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_applyTransformMatrix
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRect
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_finishDraw
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayer
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_attachToRootView
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRect
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayerWithSubproxy
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setParent
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawParagraph
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRoundRect
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_markSelfAsNodeGroup
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_restore
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_save
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setBounds
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setOpacity
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPivot
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPosition
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_translate
import kotlin.native.ref.createCleaner

/**
 * 封装 OHNativeCanvasProxy_Handle 结构体指针的 Kotlin 代理类
 * 提供类型安全和Kotlin风格的API访问Native方法
 */
class OHNativeCanvasProxy(private val handle: OHNativeCanvasProxy_Handle?) {

    /**
     * 自动资源清理器
     * 使用createCleaner确保Native资源在对象被GC时自动释放
     */
    @Suppress("unused")
    private val cleaner = createCleaner(handle) { ptr ->
        androidx_compose_ui_arkui_utils_DisposeOHNativeCanvasProxy(ptr)
    }
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

    fun save() {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_save(it) }
    }

    fun restore() {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_restore(it) }
    }

    fun translate(dx: Float, dy: Float) {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_translate(it, dx, dy) }
    }

    fun setPosition(positionX: Int, positionY: Int) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPosition(
                proxy = it,
                x = positionX,
                y = positionY
            )
        }
    }

    fun setBounds(originX: Int, originY: Int, boundsWidth: Int, boundsHeight: Int) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setBounds(
                proxy = it,
                originX = originX,
                originY = originY,
                boundsWidth = boundsWidth,
                boundsHeight = boundsHeight
            )
        }
    }

    fun setPivot(pivotFractionX: Float, pivotFractionY: Float) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPivot(
                proxy = it,
                px = pivotFractionX,
                py = pivotFractionY
            )
        }
    }

    fun setOpacity(opacity: Float) {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setOpacity(it, opacity) }
    }

    fun clipRect(left: Float, top: Float, right: Float, bottom: Float, clipOp: UInt) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRect(
                proxy = it,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                clipOp = clipOp
            )
        }
    }

    fun markSelfAsNodeGroup() {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_markSelfAsNodeGroup(it)
        }
    }

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
    fun drawLayer(renderNodeHandle: BaseRenderNode_Handle) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayer(
                proxy = it,
                renderNodeHandle = renderNodeHandle
            )
        }
    }

    /**
     * 绘制段落
     */
    fun drawParagraph(pargraphHandle: BaseRenderNode_Handle) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawParagraph(
                proxy = it,
                paragraphHandle = pargraphHandle
            )
        }
    }

    /**
     * 使用子代理绘制图层
     */
    fun drawLayerWithSubproxy(subproxy: OHNativeCanvasProxy) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayerWithSubproxy(
                it,
                subproxy.handle
            )
        }
    }

    /**
     * 绘制矩形
     */
    fun drawRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        nativePaint: OHComposeNativePaint
    ) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRect(
                proxy = it,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                paint = nativePaint.handle
            )
        }
    }

    /**
     * 绘制圆角矩形
     */
    fun drawRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        nativePaint: OHComposeNativePaint
    ) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRoundRect(
                proxy = it,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                radiusX = radiusX,
                radiusY = radiusY,
                paint = nativePaint.handle
            )
        }
    }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, nativePaint: OHComposeNativePaint) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine(
                proxy = it,
                x1 = x1,
                y1 = y1,
                x2 = x2,
                y2 = y2,
                paint = nativePaint.handle
            )
        }
    }


    fun setParent(parentProxy: OHNativeCanvasProxy) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setParent(
                proxy = it,
                parentProxy = parentProxy.handle
            )
        }
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
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_applyTransformMatrix(
                it, rotationX, rotationY, rotationZ,
                scaleX, scaleY, translationX, translationY, m34Transform
            )
        }

        // 临时日志：帮助验证方法是否被调用
        LogPrintUtil.verbose(
            "[OHOS] applyTransformMatrix called but NOT IMPLEMENTED! " +
                    "translation=($translationX, $translationY), " +
                    "scale=($scaleX, $scaleY), " +
                    "rotation=($rotationX, $rotationY, $rotationZ)"
        )
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