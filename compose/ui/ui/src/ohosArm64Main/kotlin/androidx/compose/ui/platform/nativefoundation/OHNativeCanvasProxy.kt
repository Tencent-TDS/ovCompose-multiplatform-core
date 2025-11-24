@file:Suppress("FunctionName")

package androidx.compose.ui.platform.nativefoundation

import androidx.compose.ui.annotation.InternalComposeApi
import androidx.compose.ui.arkui.utils.BaseRenderNode_Handle
import androidx.compose.ui.arkui.utils.Boolean
import androidx.compose.ui.arkui.utils.OHNativeCanvasProxy_Handle
import androidx.compose.ui.arkui.utils.OH_Drawing_Path_Handle
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_DisposeOHNativeCanvasProxy
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_applyTransformMatrix
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_asyncDrawIntoCanvas
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_beginDraw
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRect
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_finishDraw
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayer
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_attachToRootView
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRect
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipPath
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRoundRect
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clearClip
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_saveLayer
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_enableZ
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_disableZ
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawCircle
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawOval
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawArc
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawImageRect
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPath
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPoints
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLayerWithSubproxy
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setParent
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawLine
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawParagraph
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawRoundRect
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_markSelfAsNodeGroup
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_removeCanvasNodeFromParent
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_restore
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_save
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setBounds
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setOpacity
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPivot
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_setPosition
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_translate
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_scale
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_rotate
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_skew
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_concat
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMap
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMapWithPtr
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_needRedrawImageWithHashCode
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHNativeComposePixelMapFromImageBitmap
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_OHAsyncTaskRenderNode_updatePixelMapOnMainThread
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import platform.native.OH_Drawing_PointMode

/**
 * 封装 OHNativeCanvasProxy_Handle 结构体指针的 Kotlin 代理类
 * 提供类型安全和Kotlin风格的API访问Native方法
 */
class OHNativeCanvasProxy(handle: OHNativeCanvasProxy_Handle?) :
    NativeResourceHolder<OHNativeCanvasProxy_Handle>(
        handle,
        ::androidx_compose_ui_arkui_utils_DisposeOHNativeCanvasProxy
    ) {
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

    fun scale(sx: Float, sy: Float) {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_scale(it, sx, sy) }
    }

    fun rotate(degrees: Float) {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_rotate(it, degrees) }
    }

    fun skew(sx: Float, sy: Float) {
        handle?.let { androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_skew(it, sx, sy) }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun concat(matrix: FloatArray) {
        require(matrix.size == 16) { "Matrix must have 16 elements" }
        handle?.let {
            matrix.usePinned { pinned ->
                androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_concat(
                    it,
                    pinned.addressOf(0)
                )
            }
        }
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

    fun clipPath(path: OH_Drawing_Path_Handle?, clipOp: UInt) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipPath(it, path, clipOp)
        }
    }

    fun clipRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        radiusX: Float,
        radiusY: Float,
        clipOp: UInt
    ) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clipRoundRect(
                it, left, top, right, bottom, radiusX, radiusY, clipOp
            )
        }
    }

    fun clearClip() {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_clearClip(it)
        }
    }

    fun saveLayer(left: Float, top: Float, right: Float, bottom: Float, nativePaint: OHComposeNativePaint) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_saveLayer(
                it, left, top, right, bottom, nativePaint.handle
            )
        }
    }

    fun enableZ() {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_enableZ(it)
        }
    }

    fun disableZ() {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_disableZ(it)
        }
    }

    fun markSelfAsNodeGroup() {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_markSelfAsNodeGroup(it)
        }
    }

    fun removeCanvasNodeFromParent() {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_removeCanvasNodeFromParent(it)
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

    fun drawCircle(
        centerX: Float,
        centerY: Float,
        radius: Float,
        nativePaint: OHComposeNativePaint
    ) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawCircle(
                proxy = it,
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                paint = nativePaint.handle
            )
        }
    }

    fun drawOval(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        nativePaint: OHComposeNativePaint
    ) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawOval(
                proxy = it,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                paint = nativePaint.handle
            )
        }
    }

    fun drawArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        nativePaint: OHComposeNativePaint
    ) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawArc(
                proxy = it,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = useCenter,
                paint = nativePaint.handle
            )
        }
    }

    fun drawPath(path: OH_Drawing_Path_Handle, nativePaint: OHComposeNativePaint) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPath(
                proxy = it,
                path = path,
                paint = nativePaint.handle
            )
        }
    }

    fun drawImageRect(
        pixelMap: COpaquePointer?,
        srcX: Int,
        srcY: Int,
        srcWidth: Int,
        srcHeight: Int,
        dstX: Int,
        dstY: Int,
        dstWidth: Int,
        dstHeight: Int,
        nativePaint: OHComposeNativePaint
    ) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawImageRect(
                proxy = it,
                pixelMap = pixelMap,
                srcX = srcX,
                srcY = srcY,
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstX = dstX,
                dstY = dstY,
                dstWidth = dstWidth,
                dstHeight = dstHeight,
                paint = nativePaint.handle
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun drawPoints(
        pointMode: OH_Drawing_PointMode,
        points: FloatArray,
        nativePaint: OHComposeNativePaint
    ) {
        handle?.let {
            points.usePinned { pinned ->
                androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawPoints(
                    proxy = it,
                    pointMode = pointMode.value.toUInt(),
                    points = pinned.addressOf(0),
                    pointCount = (points.size / 2).toULong(),
                    paint = nativePaint.handle
                )
            }
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
        return handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_needRedrawImageWithHashCode(
                proxy = it,
                hashCode = hashCode,
                width = width,
                height = height
            )
        } ?: true
    }

    /**
     * 绘制文本图像
     */
    @OptIn(ExperimentalForeignApi::class)
    fun drawTextPixelMap(
        pixelMap: COpaquePointer?,
        cacheKey: Int,
        width: Int,
        height: Int
    ) {
        handle?.let {
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMap(
                proxy = it,
                pixelMapNative = pixelMap,
                cacheKey = cacheKey,
                width = width,
                height = height
            )
        }
    }

    /**
     * 使用缓存的指针绘制文本图像
     */
    @OptIn(ExperimentalForeignApi::class)
    fun drawTextPixelMapWithPtr(
        pixelMapPtr: Long,
        width: Int,
        height: Int
    ) {
        handle?.let {
            // 将 Long 转换为 COpaquePointer
            // 在 Kotlin/Native 中，Long 可以直接转换为指针地址
            val pixelMapPointer: COpaquePointer? = if (pixelMapPtr != 0L) {
                pixelMapPtr.toCPointer<COpaquePointerVar>()
            } else {
                null
            }
            if (pixelMapPointer != null) {
                androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_drawTextPixelMapWithPtr(
                    proxy = it,
                    pixelMapPtr = pixelMapPointer,
                    width = width,
                    height = height
                )
            }
        }
    }

    /**
     * 异步绘制文本到Canvas
     * 
     * 线程模型：
     * 1. globalTask 在后台线程（C++ AsyncPaintQueue）执行
     * 2. 执行完成后，onMainThreadUpdate 回调会post到Kotlin主线程
     * 3. 主线程调用JNI更新RenderNode
     * 
     * 注意：由于Kotlin/Native的lambda不能直接传递到C++，我们采用以下策略：
     * 1. 使用StableRef将Kotlin lambda包装成指针
     * 2. C++侧通过回调机制异步执行任务
     * 3. 完成后通过Dispatchers.Main切换到主线程
     * 4. 在主线程更新RenderNode并释放StableRef
     */
    @OptIn(ExperimentalForeignApi::class, DelicateCoroutinesApi::class)
    fun asyncDrawIntoCanvas(
        globalTask: () -> Long,
        paragraphHashCode: Int,
        width: Int,
        height: Int
    ) {
        handle?.let {
            // 使用StableRef包装globalTask lambda
            val taskRef = StableRef.create(globalTask)
            
            // 创建主线程回调：(renderNodePtr: Long, pixelMapPtr: Long) -> Unit
            val onMainThreadUpdate: (COpaquePointer, Long) -> Unit = { renderNodePtr, pixelMapPtr ->
                // 此lambda会在C++后台线程调用，但内部会post到主线程
                // 参考iOS的 dispatch_async(dispatch_get_main_queue())
                GlobalScope.launch(Dispatchers.Main) {
                    // 在主线程调用JNI更新RenderNode
                    androidx_compose_ui_arkui_utils_OHAsyncTaskRenderNode_updatePixelMapOnMainThread(
                        renderNodePtr = renderNodePtr,
                        pixelMapPtr = pixelMapPtr
                    )
                }
            }
            
            val updateCallbackRef = StableRef.create(onMainThreadUpdate)
            
            androidx_compose_ui_arkui_utils_OHNativeCanvasProxy_asyncDrawIntoCanvas(
                proxy = it,
                globalTaskPtr = taskRef.asCPointer().rawValue.toLong(),
                paragraphHashCode = paragraphHashCode,
                width = width,
                height = height,
                onMainThreadUpdatePtr = updateCallbackRef.asCPointer().rawValue.toLong()
            )
            // 注意：taskRef 和 updateCallbackRef 都会在C++侧执行完后释放
        }
    }

    /**
     * 从 ImageBitmap 创建并缓存 PixelMap
     */
    @OptIn(ExperimentalForeignApi::class)
    fun imageFromImageBitmap(pixelMap: COpaquePointer?, paragraphHashCode: Int): Long {
        return pixelMap?.let {
            androidx_compose_ui_arkui_utils_OHNativeComposePixelMapFromImageBitmap(
                pixelMapNative = it,
                cacheKey = paragraphHashCode
            )
        } ?: 0L
    }
}

/**
 * C++回调函数，用于异步执行Kotlin lambda
 * 这个函数会被C++侧调用（在后台线程）
 *
 * @param stableRefPtr StableRef指针，指向Kotlin lambda
 * @return 执行结果（PixelMap指针）
 */

@InternalComposeApi
fun _invokeKotlinAsyncTask(stableRefPtr: Long): Long {
    return try {
        // 将指针转换回StableRef
        val stableRef = stableRefPtr.toCPointer<COpaquePointerVar>()!!.asStableRef<() -> Long>()

        // 执行lambda
        val result = stableRef.get().invoke()

        // 释放StableRef
        stableRef.dispose()

        result
    } catch (e: Exception) {
        // 处理异常，记录日志
        println("invokeKotlinAsyncTask error: ${e.message}")
        0L
    }
}

/**
 * C++回调函数，用于在主线程更新AsyncTaskRenderNode
 * 这个函数会被C++侧调用（在后台线程），但lambda内部会post到主线程
 * 
 * 线程模型：
 * 1. C++后台线程调用此函数
 * 2. 此函数解包StableRef并执行lambda
 * 3. lambda内部使用Dispatchers.Main切换到主线程
 * 4. 主线程调用JNI更新RenderNode
 * 5. 释放StableRef
 * 
 * @param stableRefPtr StableRef指针，指向Kotlin lambda (Long, Long) -> Unit
 * @param renderNodePtr AsyncTaskRenderNode指针
 * @param pixelMapPtr PixelMap指针（后台任务的执行结果）
 */
@InternalComposeApi
fun _invokeKotlinMainThreadCallback(stableRefPtr: Long, renderNodePtr: Long, pixelMapPtr: Long) {
    try {
        // 将指针转换回StableRef
        val stableRef = stableRefPtr.toCPointer<COpaquePointerVar>()!!.asStableRef<(Long, Long) -> Unit>()
        
        // 执行lambda（lambda内部会使用Dispatchers.Main切换到主线程）
        stableRef.get().invoke(renderNodePtr, pixelMapPtr)
        
        // 释放StableRef
        stableRef.dispose()
    } catch (e: Exception) {
        // 处理异常，记录日志
        println("invokeKotlinMainThreadCallback error: ${e.message}")
    }
}