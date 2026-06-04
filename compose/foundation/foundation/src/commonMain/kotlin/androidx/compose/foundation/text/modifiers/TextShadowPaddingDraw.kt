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

package androidx.compose.foundation.text.modifiers

import androidx.compose.foundation.FastTraceTag
import androidx.compose.foundation.OVMPFastLogPhase
import androidx.compose.foundation.fastLog
import androidx.compose.foundation.fastLogSetCurrentTraceId
import androidx.compose.foundation.nativePtr
import androidx.compose.foundation.utf16Head4AsLong
import androidx.compose.runtime.ComposeTabService
import androidx.compose.runtime.EnableIOSParagraph
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.style.TextDecoration

/**
 * Shadow-padding-aware draw implementation for [TextStringSimpleNode].
 *
 * This is an isolated draw path that expands the bitmap to accommodate shadow blur,
 * preventing shadow clipping on the iOS bitmap → CALayer rendering pipeline.
 *
 * The original draw logic in [TextStringSimpleNode] is left completely untouched.
 * This function is only invoked when [isTextShadowPaddingEnabled] returns true.
 */
internal fun ContentDrawScope.drawWithShadowPadding(node: TextStringSimpleNode) {

    

    val localParagraph = requireNotNull(node.layoutCache.paragraph) { "no paragraph" }

    // OC-side bitmap cache is keyed solely by paragraphHashCode (width/height are ignored).
    // When both the original path and the shadow-padding path render the same Text content,
    // they share the same paragraphHashCode, causing the shadow-padding path to hit the
    // original (clipped) cache entry. Mixing in a fixed salt differentiates the two.
    val shadowPaddingHashSalt = 0x5348_4457 // "SHDW" as hex
    val baseParagraphHashCode = node.paragraphHashCode()
    val paragraphHashCode = 31 * baseParagraphHashCode + shadowPaddingHashSalt

    drawIntoCanvas { canvas ->

        if (node.platformTextDelegate != null &&
            node.platformTextDelegate.enableTextAsyncPaint(canvas) &&
            !drawInSkia
        ) {
            asyncDrawIntoCanvasWithShadowPadding(node, localParagraph, canvas, paragraphHashCode)
            return
        }

        // Sync path
        var currentParagraphHashCode = 0
        val textHash = node.text.hashCode().toLong()
        val textLen = node.text.length.toLong()
        val nativePtr = canvas.nativePtr()
        val stringLongValue = node.text.utf16Head4AsLong()
        val traceId = node.traceId
        fastLogSetCurrentTraceId(traceId)

        if (drawInSkia || EnableIOSParagraph) {
            node.localCanvas = canvas
        } else {
            currentParagraphHashCode = paragraphHashCode

            // Calculate shadow padding early so expanded size is used for cache key
            val syncShadow = node.style.shadow ?: Shadow.None
            val syncShadowPad = calculateShadowPadding(syncShadow, true)
            val syncBitmapWidth = node.layoutCache.layoutSize.width + syncShadowPad.horizontalTotal
            val syncBitmapHeight = node.layoutCache.layoutSize.height + syncShadowPad.verticalTotal

            fastLog(
                FastTraceTag.Text,
                traceId,
                stringLongValue,
                OVMPFastLogPhase.TextOnKtSyncBegin.value,
                nativePtr,
                textHash,
                textLen,
                currentParagraphHashCode.toLong(),
            )

            if (ComposeTabService.composeIOSNullTextOptEnable && node.text.isEmpty() && node.platformTextDelegate != null) {
                node.platformTextDelegate.drawNullText(
                    nativeCanvas = canvas,
                    paragraphHashKey = currentParagraphHashCode,
                    width = syncBitmapWidth,
                    height = syncBitmapHeight
                )
                fastLog(
                    FastTraceTag.Text,
                    traceId,
                    stringLongValue,
                    OVMPFastLogPhase.TextOnKtSyncDrawNullTextEnd.value,
                    nativePtr,
                    textHash,
                    textLen,
                    currentParagraphHashCode.toLong()
                )
                return
            }

            if (node.platformTextDelegate?.needRedrawText(
                    nativeCanvas = canvas,
                    paragraphHashKey = currentParagraphHashCode,
                    width = syncBitmapWidth,
                    height = syncBitmapHeight
                ) == false
            ) {
                fastLog(
                    FastTraceTag.Text,
                    traceId,
                    stringLongValue,
                    OVMPFastLogPhase.TextOnKtSyncHitCacheEnd.value,
                    nativePtr,
                    textHash,
                    textLen,
                    currentParagraphHashCode.toLong(),
                )
                return
            }
            // Shadow padding: expand bitmap
            val newBitmap = ImageBitmap(syncBitmapWidth, syncBitmapHeight)
            node.localCanvas = Canvas(newBitmap)
            node.localBitmap = newBitmap
        }

        val renderCanvas = node.localCanvas!!
        val willClip = node.layoutCache.didOverflow

        // Shadow padding calculation
        val shadow = node.style.shadow ?: Shadow.None
        val shadowPad = calculateShadowPadding(shadow, true)
        val padLeft = shadowPad.left.toFloat()
        val padTop = shadowPad.top.toFloat()
        val bitmapWidth = node.layoutCache.layoutSize.width + shadowPad.horizontalTotal
        val bitmapHeight = node.layoutCache.layoutSize.height + shadowPad.verticalTotal

        // Translate render canvas to leave room for shadow on left/top
        if (padLeft > 0f || padTop > 0f) {
            renderCanvas.translate(padLeft, padTop)
        }

        if (willClip) {
            renderCanvas.save()
            renderCanvas.clipRect(
                Rect(
                    -padLeft, -padTop,
                    node.layoutCache.layoutSize.width.toFloat() + shadowPad.right.toFloat(),
                    node.layoutCache.layoutSize.height.toFloat() + shadowPad.bottom.toFloat()
                )
            )
        }
        try {
            val textDecoration = node.style.textDecoration ?: TextDecoration.None
            val drawStyle = node.style.drawStyle ?: Fill
            val brush = node.style.brush
            if (brush != null) {
                val alpha = node.style.alpha
                localParagraph.paint(
                    canvas = renderCanvas,
                    brush = brush,
                    alpha = alpha,
                    shadow = shadow,
                    drawStyle = drawStyle,
                    textDecoration = textDecoration
                )
            } else {
                val overrideColorVal = node.overrideColor?.invoke() ?: Color.Unspecified
                val color = if (overrideColorVal.isSpecified) {
                    overrideColorVal
                } else if (node.style.color.isSpecified) {
                    node.style.color
                } else {
                    Color.Black
                }
                localParagraph.paint(
                    canvas = renderCanvas,
                    color = color,
                    shadow = shadow,
                    drawStyle = drawStyle,
                    textDecoration = textDecoration
                )
            }
            if (!drawInSkia && !EnableIOSParagraph) {
                // Offset CALayer origin to compensate for shadow padding
                val hasShadowPad = padLeft > 0f || padTop > 0f
                if (hasShadowPad) {
                    canvas.save()
                    canvas.translate(-padLeft, -padTop)
                }
                node.platformTextDelegate?.renderTextImage(
                    imageBitmap = node.localBitmap,
                    width = bitmapWidth,
                    height = bitmapHeight,
                    paragraphHashCode = currentParagraphHashCode,
                    nativeCanvas = canvas
                )
                if (hasShadowPad) {
                    canvas.restore()
                }
                node.localBitmap = null
                node.localCanvas = null
            }
        } finally {
            if (willClip) {
                renderCanvas.restore()
            }
        }
    }
}

/**
 * Shadow-padding-aware version of asyncDrawIntoCanvas (path 1).
 */
private fun asyncDrawIntoCanvasWithShadowPadding(
    node: TextStringSimpleNode,
    localParagraph: Paragraph,
    canvas: Canvas,
    paragraphHashCode: Int
) {
    
    val currentParagraphHashCode = paragraphHashCode
    val textHash = node.text.hashCode().toLong()
    val textLen = node.text.length.toLong()
    val nativePtr = canvas.nativePtr()
    val stringLongValue = node.text.utf16Head4AsLong()
    val traceId = node.traceId
    fastLogSetCurrentTraceId(traceId)
    fastLog(
        FastTraceTag.Text,
        traceId,
        stringLongValue,
        OVMPFastLogPhase.TextOnKtAsync1Begin.value,
        nativePtr,
        textHash,
        textLen,
        currentParagraphHashCode.toLong(),
    )

    // Shadow padding — calculate early so expanded size is used for cache key
    val layoutSizeWidth = node.layoutCache.layoutSize.width
    val layoutSizeHeight = node.layoutCache.layoutSize.height
    val shadow = node.style.shadow ?: Shadow.None
    val shadowPad = calculateShadowPadding(shadow, true)
    val bitmapWidth = layoutSizeWidth + shadowPad.horizontalTotal
    val bitmapHeight = layoutSizeHeight + shadowPad.verticalTotal

    if (ComposeTabService.composeIOSNullTextOptEnable && node.text.isEmpty() && node.platformTextDelegate != null) {
        node.platformTextDelegate.drawNullText(
            nativeCanvas = canvas,
            paragraphHashKey = currentParagraphHashCode,
            width = bitmapWidth,
            height = bitmapHeight
        )
        fastLog(
            FastTraceTag.Text,
            traceId,
            stringLongValue,
            OVMPFastLogPhase.TextOnKtAsync1DrawNullTextEnd.value,
            nativePtr,
            textHash,
            textLen,
            currentParagraphHashCode.toLong(),
        )
        return
    }

    if (node.platformTextDelegate?.needRedrawText(
            nativeCanvas = canvas,
            paragraphHashKey = currentParagraphHashCode,
            width = bitmapWidth,
            height = bitmapHeight
        ) == false
    ) {
        fastLog(
            FastTraceTag.Text,
            traceId,
            stringLongValue,
            OVMPFastLogPhase.TextOnKtAsync1HitCacheEnd.value,
            nativePtr,
            textHash,
            textLen,
            currentParagraphHashCode.toLong()
        )
        return
    }

    val willClip = node.layoutCache.didOverflow
    val padLeft = shadowPad.left.toFloat()
    val padTop = shadowPad.top.toFloat()

    val globalTask: () -> Long = {
        val newBitmap = ImageBitmap(bitmapWidth, bitmapHeight)
        val renderCanvas = Canvas(newBitmap)
        // Translate so text draws at (padLeft, padTop), leaving room for shadow expansion
        if (padLeft > 0f || padTop > 0f) {
            renderCanvas.translate(padLeft, padTop)
        }
        if (willClip) {
            renderCanvas.save()
            renderCanvas.clipRect(
                Rect(
                    -padLeft, -padTop,
                    layoutSizeWidth.toFloat() + shadowPad.right.toFloat(),
                    layoutSizeHeight.toFloat() + shadowPad.bottom.toFloat()
                )
            )
        }
        try {
            val textDecoration = node.style.textDecoration ?: androidx.compose.ui.text.style.TextDecoration.None
            val drawStyle = node.style.drawStyle ?: Fill
            val brush = node.style.brush
            if (brush != null) {
                val alpha = node.style.alpha
                localParagraph.paint(
                    canvas = renderCanvas,
                    brush = brush,
                    alpha = alpha,
                    shadow = shadow,
                    drawStyle = drawStyle,
                    textDecoration = textDecoration
                )
            } else {
                val overrideColorVal = node.overrideColor?.invoke() ?: Color.Unspecified
                val color = if (overrideColorVal.isSpecified) {
                    overrideColorVal
                } else if (node.style.color.isSpecified) {
                    node.style.color
                } else {
                    Color.Black
                }
                localParagraph.paint(
                    canvas = renderCanvas,
                    color = color,
                    shadow = shadow,
                    drawStyle = drawStyle,
                    textDecoration = textDecoration
                )
            }
            node.platformTextDelegate?.imageFromImageBitmap(canvas, currentParagraphHashCode, newBitmap) ?: 0
        } finally {
            if (willClip) {
                renderCanvas.restore()
            }
        }
    }

    // Offset CALayer origin to compensate for shadow padding
    val hasShadowPad = padLeft > 0f || padTop > 0f
    if (hasShadowPad) {
        canvas.save()
        canvas.translate(-padLeft, -padTop)
    }

    node.platformTextDelegate?.asyncDrawIntoCanvas(
        canvas,
        globalTask,
        currentParagraphHashCode,
        bitmapWidth,
        bitmapHeight
    ) {}

    if (hasShadowPad) {
        canvas.restore()
    }
}
