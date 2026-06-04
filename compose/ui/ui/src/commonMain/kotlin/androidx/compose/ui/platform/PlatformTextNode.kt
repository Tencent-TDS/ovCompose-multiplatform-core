package androidx.compose.ui.platform

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.NativeCanvas

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

/*  iOS 平台使用的 文本的 PictureRecorder */
interface TextPictureRecorder {
    /* 开始录制 */
    fun beginRecording(width:Float, height:Float) : Canvas

    /* 结束录制 */
    fun finishRecordingAsPicture() : TextPicture

    /* 关闭 */
    fun close()
}

/*  iOS 平台使用的 文本的 Picture */
interface TextPicture {

    /* 绘制在 native canvas 上 */
    fun drawNativeCanvas(nativeCanvas: NativeCanvas)

    /* 关闭 */
    fun close()
}

/** 文本平台的代理节点，用于替代Skiko的文本测量、渲染等逻辑 */
interface PlatformTextNode {

    fun createTextPictureRecorder() : TextPictureRecorder?

    /** 查询是否需要渲染Text */
    fun needRedrawText(
        nativeCanvas: Canvas,
        paragraphHashKey: Int,
        width: Int,
        height: Int,
    ): Boolean

    /** bitmap渲染成Text */
    fun renderTextImage(
        imageBitmap: ImageBitmap?,
        width: Int,
        height: Int,
        paragraphHashCode: Int,
        nativeCanvas: Canvas
    )

    /** 空文本绘制 */
    fun drawNullText(
        nativeCanvas: Canvas,
        paragraphHashKey: Int,
        width: Int,
        height: Int,
    )

    fun imageFromImageBitmap(nativeCanvas: Canvas, paragraphHashCode: Int, imageBitmap: ImageBitmap): Long

    fun asyncDrawIntoCanvas(
        nativeCanvas: Canvas,
        asyncTask: () -> Long,
        paragraphHashCode: Int,
        width: Int,
        height: Int,
        drawCompleted: () -> Unit
    )

    fun enableTextAsyncPaint(nativeCanvas: Canvas): Boolean
}