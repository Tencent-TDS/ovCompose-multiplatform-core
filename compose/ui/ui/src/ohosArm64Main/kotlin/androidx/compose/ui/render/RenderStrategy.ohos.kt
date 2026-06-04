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

package androidx.compose.ui.render

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_xcomponent_finishDraw
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_xcomponent_prepareDraw
import androidx.compose.ui.arkui.utils.androidx_compose_ui_arkui_utils_xcomponent_makeCurrentNull
import kotlinx.cinterop.COpaquePointer
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.PixelGeometry
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.SurfaceProps


internal abstract class RenderStrategy {

	open var render: COpaquePointer? = null
	val config = RenderStrategyFactory.StrategyConfig()

	var width: Int = 0
	var height: Int = 0
	protected var surface: Surface? = null
	protected var surfaceCanvas: Canvas? = null
	protected var pictureRecorder: PictureRecorder? = null
	protected var renderRect = Rect(0f, 0f, 0f, 0f)
	private var directContext: DirectContext? = null
	private var renderTarget: BackendRenderTarget? = null

	abstract fun prepareRender()

	abstract fun render(timestamp: Long, onDraw: (canvas: Canvas, timestamp: Long) -> Unit)

	abstract fun finishRender()

	open fun onSystemVsync(timestamp: Long) = Unit

	open fun close() {
		clearSurface()
		clearRecorder()
		disposeDirectContext()
	}

	fun setSize(width: Int, height: Int) {
		if (this.width != width || this.height != height) {
			this.width = width
			this.height = height
			this.renderRect = Rect(0f, 0f, width.toFloat(), height.toFloat())
			clearSurface()
			clearRecorder()
		}
	}

	protected fun ensureSurface() {
		if (directContext == null) {
			directContext = DirectContext.makeGL()
		}

		if (renderTarget == null) {
			if (width <= 0 || height <= 0) return
			renderTarget = BackendRenderTarget.makeGL(
				width,
				height,
				SAMPLE_COUNT,
				STENCIL_BITS,
				FRAMEBUFFER_ID,
				GL_RGBA8
			)
		}

		if (surface != null) return
		val context = directContext ?: return
		val target = renderTarget ?: return
		surface = Surface.makeFromBackendRenderTarget(
			context,
			target,
			SurfaceOrigin.TOP_LEFT,
			SurfaceColorFormat.RGBA_8888,
			ColorSpace.sRGB,
			SurfaceProps(pixelGeometry = PixelGeometry.UNKNOWN)
		)

		surfaceCanvas = surface?.canvas?.asComposeCanvas()

		if (surfaceCanvas == null) {
			clearSurface()
			disposeDirectContext()
			logger.log { "ensureSurface surfaceCanvas null" }
		}
	}

	private fun disposeDirectContext() {
		directContext?.abandon()
		directContext = null
	}

	protected fun flush() {
		directContext?.flush()
	}

	protected fun prepareDraw(): Boolean {
		if (render == null) {
			return false
		}
		return androidx_compose_ui_arkui_utils_xcomponent_prepareDraw(render)
	}

	protected fun makeCurrentNull(): Boolean {
		if (render == null) {
			return false
		}
		return androidx_compose_ui_arkui_utils_xcomponent_makeCurrentNull(render)
	}

	protected fun finishDraw(): Boolean {
		if (render == null) {
			return false
		}
		return androidx_compose_ui_arkui_utils_xcomponent_finishDraw(render)
	}

	protected fun ensureRecorder(): PictureRecorder {
		return pictureRecorder ?: PictureRecorder().also { pictureRecorder = it }
	}

	private fun clearSurface() {
		renderTarget?.close()
		renderTarget = null
		surface?.close()
		surface = null
		surfaceCanvas = null
	}

	private fun clearRecorder() {
		pictureRecorder?.close()
		pictureRecorder = null
	}

	internal inline fun dumpSkia(timestamp: Long) {
		directContext?.let {
			SkiaMemoryDumper.dump(it, timestamp)
		}
	}

	companion object {
		const val TRACE_RENDER_PICTURE = "RenderPicture"
		const val TRACE_DRAW_PICTURE = "DrawPicture"
		const val TRACE_PREPARE_DRAW = "PrepareDraw"
		const val TRACE_DRAW = "Draw"
		const val TRACE_DRAW_PICTURE_ASYNC = "DrawPictureAsync"
		const val TRACE_PROCESS_INTEROP_ACTIONS = "ProcessInteropActions"
		const val TRACE_FLUSH = "Flush"
		const val TRACE_FINISH_DRAW = "FinishDraw"

		const val SAMPLE_COUNT = 1
		const val STENCIL_BITS = 8
		const val FRAMEBUFFER_ID = 0
		const val GL_RGBA8 = 0x8058 // OpenGL 常量，代表 RGBA8 格式

		val logger = RenderLog.getLogger("RenderStrategy")
	}
}