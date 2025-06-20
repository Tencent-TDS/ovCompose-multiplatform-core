/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text

import org.jetbrains.skia.Rect as SkRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.platform.SkiaParagraphIntrinsics
import androidx.compose.ui.text.platform.cursorHorizontalPosition
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.isUnspecified
import kotlin.math.floor
import kotlin.math.roundToInt
import org.jetbrains.skia.FontMetrics
import org.jetbrains.skia.IRange
import org.jetbrains.skia.paragraph.*

internal class SkiaParagraph(
    intrinsics: ParagraphIntrinsics,
    val maxLines: Int,
    val ellipsis: Boolean,
    val constraints: Constraints
) : Paragraph {

    private val ellipsisChar = if (ellipsis) "\u2026" else ""

    private val paragraphIntrinsics = intrinsics as SkiaParagraphIntrinsics

    private val layouter = paragraphIntrinsics.layouter().apply {
        setParagraphStyle(
            maxLines = maxLines,
            ellipsis = ellipsisChar
        )
    }

    internal val defaultFont
        get() = layouter.defaultFont

    /**
     * Paragraph isn't always immutable, it could be changed via [paint] method without
     * rerunning layout
     */
    private var paragraph = layouter.layoutParagraph(
        width = width
    )
        set(value) {
            field = value
            _lineMetrics = null
        }

    init {
        paragraph.layout(width)
    }

    private val text: String
        get() = paragraphIntrinsics.text

    override val width: Float
        get() = constraints.maxWidth.toFloat()

    override val height: Float
        get() = paragraph.height

    override val minIntrinsicWidth: Float
        get() = paragraphIntrinsics.minIntrinsicWidth

    override val maxIntrinsicWidth: Float
        get() = paragraphIntrinsics.maxIntrinsicWidth

    override val firstBaseline: Float
        get() = lineMetrics.firstOrNull()?.run { baseline.toFloat() } ?: 0f

    override val lastBaseline: Float
        get() = lineMetrics.lastOrNull()?.run { baseline.toFloat() } ?: 0f

    override val didExceedMaxLines: Boolean
        get() = paragraph.didExceedMaxLines()

    override val lineCount: Int
        // workaround for https://bugs.chromium.org/p/skia/issues/detail?id=11321
        // workaround for invalid paragraph layout result
        get() = if (text == "" || paragraph.lineNumber < 1) {
            1
        } else {
            paragraph.lineNumber
        }

    override val placeholderRects: List<Rect?>
        get() =
            paragraph.rectsForPlaceholders.map {
                it.rect.toComposeRect()
            }

    override fun getPathForRange(start: Int, end: Int): Path {
        val boxes = paragraph.getRectsForRange(
            start,
            end,
            RectHeightMode.MAX,
            RectWidthMode.TIGHT
        )
        val path = LocalPath()
        for (b in boxes) {
            path.asSkiaPath().addRect(b.rect)
        }
        return path
    }

    override fun getCursorRect(offset: Int): Rect {
        val horizontal = getHorizontalPosition(offset, true)
        val line = lineMetricsForOffset(offset)!!

        // workaround for https://bugs.chromium.org/p/skia/issues/detail?id=11321 :(
        // Otherwise it shows a big cursor on a new empty line https://github.com/JetBrains/compose-jb/issues/1895
        val isNewEmptyLine = offset - 1 == line.startIndex && offset == text.length
        val metrics = defaultFont.metrics

        val asc = line.ascent.let {
            if (isNewEmptyLine) {
                val ascent = -metrics.ascent.toDouble()
                it.coerceAtMost(ascent)
            } else {
                it
            }
        }
        val desc = line.descent.let {
            if (isNewEmptyLine) {
                val descent = metrics.descent.toDouble()
                it.coerceAtMost(descent)
            } else {
                it
            }
        }

        return Rect(
            horizontal,
            (line.baseline - asc).toFloat(),
            horizontal,
            (line.baseline + desc).toFloat()
        )
    }

    override fun getLineLeft(lineIndex: Int): Float =
        lineMetrics.getOrNull(lineIndex)?.left?.toFloat() ?: 0f

    override fun getLineRight(lineIndex: Int): Float =
        lineMetrics.getOrNull(lineIndex)?.right?.toFloat() ?: 0f

    override fun getLineTop(lineIndex: Int) =
        lineMetrics.getOrNull(lineIndex)?.let { line ->
            floor((line.baseline - line.ascent).toFloat())
        } ?: 0f

    override fun getLineBottom(lineIndex: Int) =
        lineMetrics.getOrNull(lineIndex)?.let { line ->
            floor((line.baseline + line.descent).toFloat())
        } ?: 0f

    internal fun getLineAscent(lineIndex: Int): Int =
        -(lineMetrics.getOrNull(lineIndex)?.ascent?.roundToInt() ?: 0)

    internal fun getLineBaseline(lineIndex: Int): Int =
        lineMetrics.getOrNull(lineIndex)?.baseline?.roundToInt() ?: 0

    internal fun getLineDescent(lineIndex: Int): Int =
        lineMetrics.getOrNull(lineIndex)?.descent?.roundToInt() ?: 0

    private fun lineMetricsForOffset(offset: Int): LineMetrics? {
        checkOffsetIsValid(offset)

        return lineMetrics.binarySearchFirstMatchingOrLast {
            offset < it.endIncludingNewline
        }
    }

    override fun getLineHeight(lineIndex: Int) =
        lineMetrics.getOrNull(lineIndex)?.height?.toFloat() ?: 0f

    override fun getLineWidth(lineIndex: Int) =
        lineMetrics.getOrNull(lineIndex)?.width?.toFloat() ?: 0f

    override fun getLineStart(lineIndex: Int) =
        lineMetrics.getOrNull(lineIndex)?.startIndex ?: 0

    override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int {
        val metrics = lineMetrics.getOrNull(lineIndex) ?: return 0
        return if (visibleEnd) {
            // workarounds for https://bugs.chromium.org/p/skia/issues/detail?id=11321 :(
            // we are waiting for fixes
            if (lineIndex > 0 && metrics.startIndex < lineMetrics[lineIndex - 1].endIndex) {
                metrics.endIndex
            } else if (
                metrics.startIndex < text.length &&
                text[metrics.startIndex] == '\n'
            ) {
                metrics.startIndex
            } else {
                metrics.endExcludingWhitespaces
            }
        } else {
            metrics.endIndex
        }
    }

    override fun isLineEllipsized(lineIndex: Int) = false

    override fun getLineForOffset(offset: Int) =
        lineMetricsForOffset(offset)?.lineNumber ?: 0

    override fun getLineForVerticalPosition(vertical: Float): Int {
        return getLineMetricsForVerticalPosition(vertical)?.lineNumber ?: 0
    }

    private fun getLineMetricsForVerticalPosition(vertical: Float): LineMetrics? {
        return lineMetrics.binarySearchFirstMatchingOrLast { vertical < it.baseline + it.descent }
    }

    override fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float {
        val prevBox = getBoxBackwardByOffset(offset)
        val nextBox = getBoxForwardByOffset(offset)
        val isRtl = paragraphIntrinsics.textDirection == ResolvedTextDirection.Rtl
        val isLtr = !isRtl
        return when {
            prevBox == null && nextBox == null -> if (isRtl) width else 0f
            prevBox == null -> nextBox!!.cursorHorizontalPosition(true)
            nextBox == null -> prevBox.cursorHorizontalPosition()
            nextBox.direction == prevBox.direction -> nextBox.cursorHorizontalPosition(true)
            isLtr && prevBox.direction == Direction.LTR -> nextBox.cursorHorizontalPosition(opposite = true)
            isRtl && prevBox.direction == Direction.RTL -> nextBox.cursorHorizontalPosition(opposite = true)
            // BiDi transition offset, we need to resolve ambiguity with usePrimaryDirection
            // for details see comment for MultiParagraph.getHorizontalPosition
            usePrimaryDirection -> prevBox.cursorHorizontalPosition()
            else -> nextBox.cursorHorizontalPosition(true)
        }
    }

    private var _lineMetrics: Array<LineMetrics>? = null
    private val lineMetrics: Array<LineMetrics>
        get() {
            val lineMetrics = _lineMetrics ?: receiveLineMetrics().also {
                _lineMetrics = it
            }
            return lineMetrics
        }

    private fun receiveLineMetrics(): Array<LineMetrics> {
        val lineMetrics = if (text.isEmpty()) {
            layouter.emptyLineMetrics(paragraph)
        } else {
            // This creates a new objects every time
            paragraph.lineMetrics
        }

        val fontMetrics = defaultFont.metrics
        if (lineMetrics.isNotEmpty()) {
            lineMetrics[0] = lineMetrics[0]
                .trimFirstAscent(fontMetrics, layouter.textStyle)
            lineMetrics[lineMetrics.size - 1] = lineMetrics[lineMetrics.size - 1]
                .trimLastDescent(fontMetrics, layouter.textStyle)
        }

        return lineMetrics
    }

    private fun getBoxForwardByOffset(offset: Int): TextBox? {
        checkOffsetIsValid(offset)
        var to = offset + 1 // TODO: Use unicode code points (CodePoint.charCount() instead of +1)
        while (to <= text.length) {
            val box = paragraph.getRectsForRange(
                offset, to,
                RectHeightMode.STRUT, RectWidthMode.TIGHT
            ).firstOrNull()
            if (box != null) {
                return box
            }
            to += 1 // TODO: Use unicode code points (CodePoint.charCount() instead of +1)
        }
        return null
    }

    private fun getBoxBackwardByOffset(offset: Int, end: Int = offset): TextBox? {
        checkOffsetIsValid(offset)
        var from = offset - 1
        val isRtl = paragraphIntrinsics.textDirection == ResolvedTextDirection.Rtl
        while (from >= 0) {
            val box = paragraph.getRectsForRange(
                from, end,
                RectHeightMode.STRUT, RectWidthMode.TIGHT
            ).firstOrNull()
            when {
                (box == null) -> from -= 1
                (text[from] == '\n') -> {
                    return if (!isRtl) {
                        val bottom = box.rect.bottom + box.rect.bottom - box.rect.top
                        val rect = SkRect(0f, box.rect.bottom, 0f, bottom)
                        return TextBox(rect, box.direction)
                    } else {
                        // For RTL:
                        // When cursor changes its position across lines, we apply the following rules:

                        // if '\n' is the last character, then the box should be aligned to the right:
                        // _________________abc   <- '\n' new line here
                        // ___________________|   <- cursor is in the end of the next line

                        // if '\n' is not the last, then the box should be be aligned to the left of the following box:
                        // _________________abc   <- '\n' new line here
                        // _________________|qw   <- cursor is before the box ('q') following the new line

                        if (from == text.lastIndex) {
                            val bottom = box.rect.bottom + box.rect.bottom - box.rect.top
                            val rect = SkRect(width, box.rect.bottom, width, bottom)
                            TextBox(rect, box.direction)
                        } else {
                            // TODO: Use unicode code points (CodePoint.charCount() instead of +1)
                            val nextBox =  paragraph.getRectsForRange(
                                offset, offset + 1,
                                RectHeightMode.STRUT, RectWidthMode.TIGHT
                            ).first()
                            val rect = SkRect(
                                nextBox.rect.left, nextBox.rect.top,
                                nextBox.rect.left, nextBox.rect.bottom
                            )
                            TextBox(rect, nextBox.direction)
                        }
                    }
                }
                else -> return box
            }
        }
        return null
    }

    override fun getParagraphDirection(offset: Int): ResolvedTextDirection =
        // TODO: It should be position based (direction isolate for example)
        paragraphIntrinsics.textDirection

    override fun getBidiRunDirection(offset: Int): ResolvedTextDirection =
        when (getBoxForwardByOffset(offset)?.direction) {
            Direction.RTL -> ResolvedTextDirection.Rtl
            Direction.LTR -> ResolvedTextDirection.Ltr
            null -> ResolvedTextDirection.Ltr
        }

    override fun getOffsetForPosition(position: Offset): Int {
        val glyphPosition = paragraph.getGlyphPositionAtCoordinate(position.x, position.y).position

        // Below we apply a workaround for skiko/skia issue:
        //
        // It's expected that this method should return the glyph position that lays on the line at `position.y`.
        // When the `position` is not within the text line, glyphPosition will reference a wrong glyph (for example, the first glyph on a next line).
        // This will make the cursor go to the wrong position, not according to the coordinates of a click.
        //
        // When position.x lays beyond the left or right side of a text line,
        // `getGlyphPositionAtCoordinate` returns a wrong value.
        // This happens:
        // - in multiline text when a text block has an opposite direction than the primary paragraph direction
        // - in text with line-breaks, when clicking to the right of a text line
        //
        // Therefore, when the position.x is not within the line's left or right side,
        // we call getGlyphPositionAtCoordinate with `x` value closest to the corresponding side.
        //
        // TODO: consider fixing it in skiko

        // expectedLine is the line which lays at position.y
        val expectedLine = getLineMetricsForVerticalPosition(position.y) ?: return glyphPosition
        val isNotEmptyLine = expectedLine.startIndex < expectedLine.endIndex // a line with only whitespaces considered to be not empty

        // No need to apply the workaround if the clicked position is within the line bounds (but doesn't include whitespaces)
        if (position.x > expectedLine.left && position.x < expectedLine.right) {
            return glyphPosition
        }

        val rects = if (isNotEmptyLine) {
            // expectedLine width doesn't include whitespaces. Therefore we look at the Rectangle representing the line
            paragraph.getRectsForRange(
                start = expectedLine.startIndex,
                end = if (expectedLine.isHardBreak) expectedLine.endIndex else expectedLine.endIndex - 1,
                rectHeightMode = RectHeightMode.STRUT,
                rectWidthMode = RectWidthMode.TIGHT
            )
        } else { // the array of rects should be empty for an empty line, so no need to call `getRectsForRange`
            null
        }

        val leftX = rects?.firstOrNull()?.rect?.left ?: expectedLine.left.toFloat()
        val rightX = rects?.lastOrNull()?.rect?.right ?: expectedLine.right.toFloat()

        if (leftX == rightX) {
            return glyphPosition
        }

        var correctedGlyphPosition = glyphPosition

        if (position.x <= leftX) { // when clicked to the left of a text line
            correctedGlyphPosition = paragraph.getGlyphPositionAtCoordinate(leftX + 1f, position.y).position
        } else if (position.x >= rightX) { // when clicked to the right of a text line
            correctedGlyphPosition = paragraph.getGlyphPositionAtCoordinate(rightX - 1f, position.y).position
            val isNeutralChar = if (correctedGlyphPosition in text.indices) {
                text.codePointAt(correctedGlyphPosition).isNeutralDirection()
            } else false

            // For RTL blocks, the position is still not correct, so we have to subtract 1 from the returned result
            if (!isNeutralChar && getBoxBackwardByOffset(correctedGlyphPosition)?.direction == Direction.RTL) {
                correctedGlyphPosition -= 1 // TODO Check if it should be CodePoint.charCount()
            }
        }

        return correctedGlyphPosition
    }

    override fun getBoundingBox(offset: Int): Rect {
        val box = getBoxForwardByOffset(offset) ?: getBoxBackwardByOffset(offset, text.length)!!
        return box.rect.toComposeRect()
    }

    override fun fillBoundingBoxes(
        range: TextRange,
        array: FloatArray,
        arrayStart: Int
    ) {
        println("Compose Multiplatform doesn't support fillBoundingBoxes` yet. " +
            "Follow https://github.com/JetBrains/compose-multiplatform/issues/4236")
        // TODO(https://youtrack.jetbrains.com/issue/COMPOSE-720/Implement-Paragraph.fillBoundingBoxes) implement fillBoundingBoxes
    }

    override fun getWordBoundary(offset: Int): TextRange {
        checkOffsetIsValid(offset)

        // To match with Android implementation it should return:
        // - empty range if offset is between spaces
        // - previous word if offset right after that
        // - TODO: punctuation doesn't divide words
        //   NOTE: Android punctuation handling has some issues at the moment, so it doesn't clear
        //         what expected behavior is.

        // Android uses `isLetterOrDigit` for codepoints, but we have it only for chars.
        // Using `Char.isWhitespace` instead because whitespaces are not supplementary code units.
        // TODO: Replace chars to code units to make this code future proof.
        if (offset < text.length && text[offset].isWhitespace() || offset == text.length) {
            // If it's whitespace, we're sure that it's not surrogate.
            return if (offset > 0 && !text[offset - 1].isWhitespace()) {
                paragraph.getWordBoundary(offset - 1).toTextRange()
            } else TextRange(offset, offset)
        }

        // Skia paragraph should handle unicode correctly, but it doesn't match Android behavior.
        // It uses ICU's BreakIterator under the hood, so we can use
        // `BreakIterator.makeWordInstance()` without calling skia's `getWordBoundary` at all.
        return paragraph.getWordBoundary(offset).toTextRange()
    }

    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        paragraph = with(layouter) {
            setTextStyle(
                color = color,
                shadow = shadow,
                textDecoration = textDecoration
            )
            layoutParagraph(
                width = width
            )
        }
        paragraph.paint(canvas.nativeCanvas, 0.0f, 0.0f)
    }

    @ExperimentalTextApi
    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        paragraph = with(layouter) {
            setTextStyle(
                color = color,
                shadow = shadow,
                textDecoration = textDecoration
            )
            setDrawStyle(drawStyle)
            setBlendMode(blendMode)
            layoutParagraph(
                width = width
            )
        }
        paragraph.paint(canvas.nativeCanvas, 0.0f, 0.0f)
    }

    @ExperimentalTextApi
    override fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        paragraph = with(layouter) {
            setTextStyle(
                brush = brush,
                brushSize = Size(width, height),
                alpha = alpha,
                shadow = shadow,
                textDecoration = textDecoration
            )
            setDrawStyle(drawStyle)
            setBlendMode(blendMode)
            layoutParagraph(
                width = width
            )
        }
        paragraph.paint(canvas.nativeCanvas, 0.0f, 0.0f)
    }

    /**
     * Check if the given offset is in the given range.
     */
    private inline fun checkOffsetIsValid(offset: Int) {
        require(offset in 0..text.length) {
            ("Invalid offset: $offset. Valid range is [0, ${text.length}]")
        }
    }
}

private fun LineMetrics.trimFirstAscent(
    fontMetrics: FontMetrics,
    textStyle: TextStyle
): LineMetrics {
    if (textStyle.lineHeight.isUnspecified) return this
    val style = textStyle.lineHeightStyle ?: LineHeightStyle.Default
    val ascent = if (style.trim.isTrimFirstLineTop()) {
        -fontMetrics.ascent.toDouble()
    } else {
        ascent
    }
    return copy(ascent = ascent)
}

private fun LineMetrics.trimLastDescent(
    fontMetrics: FontMetrics,
    textStyle: TextStyle
): LineMetrics {
    if (textStyle.lineHeight.isUnspecified) return this
    val style = textStyle.lineHeightStyle ?: LineHeightStyle.Default
    val descent = if (style.trim.isTrimLastLineBottom()) {
        fontMetrics.descent.toDouble()
    } else {
        descent
    }
    return copy(descent = descent)
}

private fun LineMetrics.copy(
    startIndex: Int = this.startIndex,
    endIndex: Int = this.endIndex,
    endExcludingWhitespaces: Int = this.endExcludingWhitespaces,
    endIncludingNewline: Int = this.endIncludingNewline,
    isHardBreak: Boolean = this.isHardBreak,
    ascent: Double = this.ascent,
    descent: Double = this.descent,
    unscaledAscent: Double = this.unscaledAscent,
    height: Double = this.height,
    width: Double = this.width,
    left: Double = this.left,
    baseline: Double = this.baseline,
    lineNumber: Int = this.lineNumber
) = LineMetrics(
    startIndex = startIndex,
    endIndex = endIndex,
    endExcludingWhitespaces = endExcludingWhitespaces,
    endIncludingNewline = endIncludingNewline,
    isHardBreak = isHardBreak,
    ascent = ascent,
    descent = descent,
    unscaledAscent = unscaledAscent,
    height = height,
    width = width,
    left = left,
    baseline = baseline,
    lineNumber = lineNumber
)

private fun IRange.toTextRange() = TextRange(start, end)

/**
 * Returns the first item satisfying [predicate], or the last item in the array if none satisfy it.
 *
 * Returns `null` if the array is empty.
 */
private inline fun <T> Array<out T>.binarySearchFirstMatchingOrLast(
    crossinline predicate: (T) -> Boolean): T?
{
    if (this.isEmpty()) {
        return null
    }

    val index = this.asList().binarySearch {
        if (predicate(it)) 1 else -1
    }

    // The search will always return a negative value because the comparison never returns 0
    return this[(-index - 1).coerceAtMost(this.lastIndex)]
}
