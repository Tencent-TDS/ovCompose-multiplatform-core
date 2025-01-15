/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.view

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.util.Size
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.pdf.PdfDocument
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Manages the loading of [Bitmap]s for a single PDF page. Keeps track of the current zoom level and
 * will switch between full page rendering to tiled rendering as dictated by [maxBitmapSizePx]
 */
internal class BitmapFetcher(
    private val pageNum: Int,
    private val pageSize: Point,
    private val pdfDocument: PdfDocument,
    private val backgroundScope: CoroutineScope,
    /**
     * The maximum size of a single bitmap in pixels. If the pageSize * current zoom exceeds this
     * threshold, we will start to use tiled rendering.
     */
    private val maxBitmapSizePx: Point,
    private val onPageUpdate: () -> Unit,
) {

    /**
     * The maximum size of a full page bitmap that is used as the background for a tiled rendering.
     * We draw a low-res bitmap behind tiles to avoid blank spaces in the page as high res tiles are
     * being loaded.
     */
    private val maxTileBackgroundSizePx = Point(maxBitmapSizePx.x / 2, maxBitmapSizePx.y / 2)

    var isActive: Boolean = false
        set(value) {
            // Debounce setting the field to the same value
            if (field == value) return
            field = value
            if (field) onActive() else onInactive()
        }

    @get:MainThread var pageContents: PageContents? = null

    private var bitmapSource: PdfDocument.BitmapSource? = null
    @VisibleForTesting var currentRenderingScale: Float? = null
    @VisibleForTesting var renderingJob: Job? = null

    /**
     * Notify this fetcher that the zoom level / scale factor of the UI has changed, and that it
     * ought to consider fetching new bitmaps
     */
    fun onScaleChanged(scale: Float) {
        if (!shouldRenderNewBitmaps(scale)) return

        currentRenderingScale = scale
        renderingJob?.cancel()
        renderingJob =
            if (needsTiling(scale)) {
                fetchTiles(scale)
            } else {
                fetchNewBitmap(scale)
            }
        renderingJob?.invokeOnCompletion { cause ->
            // We only want to reset these states when we completed naturally
            if (cause is CancellationException) return@invokeOnCompletion
            renderingJob = null
            currentRenderingScale = null
        }
    }

    private fun shouldRenderNewBitmaps(scale: Float): Boolean {
        val renderingAtCurrentScale =
            currentRenderingScale == scale && renderingJob?.isActive == true
        val renderedAtCurrentScale = pageContents?.let { it.renderedScale == scale } ?: false

        return !renderedAtCurrentScale && !renderingAtCurrentScale
    }

    /** Prepare to start fetching bitmaps */
    private fun onActive() {
        bitmapSource = pdfDocument.getPageBitmapSource(pageNum)
    }

    /**
     * Cancel ongoing work and release resources, including [Bitmap]s and [AutoCloseable]s held by
     * this fetcher
     */
    private fun onInactive() {
        currentRenderingScale = null
        pageContents = null
        renderingJob?.cancel()
        renderingJob = null
        bitmapSource?.close()
        bitmapSource = null
    }

    /** Fetch a [FullPageBitmap] */
    private fun fetchNewBitmap(scale: Float): Job {
        return backgroundScope.launch {
            val size = limitBitmapSize(scale, maxBitmapSizePx)
            // If our BitmapSource is null that means this fetcher is inactive and we should
            // stop what we're doing
            val bitmap = bitmapSource?.getBitmap(size) ?: return@launch
            ensureActive()
            pageContents = FullPageBitmap(bitmap, scale)
            onPageUpdate()
        }
    }

    /** Fetch a [TileBoard] */
    private fun fetchTiles(scale: Float): Job {
        val pageSizePx = Point((pageSize.x * scale).roundToInt(), (pageSize.y * scale).roundToInt())
        val tileBoard = TileBoard(tileSizePx, pageSizePx, scale)
        // Re-use an existing background bitmap if we have one to avoid unnecessary re-rendering
        // and jank
        val prevBackground = (tileBoard as? TileBoard)?.backgroundBitmap
        if (prevBackground != null) {
            tileBoard.backgroundBitmap = prevBackground
            pageContents = tileBoard
            onPageUpdate()
        }
        return backgroundScope.launch {
            // Render a new background bitmap if we must
            if (prevBackground == null) {
                // If our BitmapSource is null that means this fetcher is inactive and we should
                // stop what we're doing
                val backgroundSize = limitBitmapSize(scale, maxTileBackgroundSizePx)
                val bitmap = bitmapSource?.getBitmap(backgroundSize) ?: return@launch
                pageContents = tileBoard
                ensureActive()
                tileBoard.backgroundBitmap = bitmap
                onPageUpdate()
            }
            for (tile in tileBoard.tiles) {
                renderBitmap(tile, coroutineContext.job, scale)
            }
        }
    }

    /** Render a [Bitmap] for this [TileBoard.Tile] */
    private suspend fun renderBitmap(tile: TileBoard.Tile, thisJob: Job, scale: Float) {
        thisJob.ensureActive()
        val left = tile.offsetPx.x
        val top = tile.offsetPx.y
        val tileRect = Rect(left, top, left + tile.exactSizePx.x, top + tile.exactSizePx.y)
        // If our BitmapSource is null that means this fetcher is inactive and we should
        // stop what we're doing
        val bitmap =
            bitmapSource?.getBitmap(
                Size((pageSize.x * scale).roundToInt(), (pageSize.y * scale).roundToInt()),
                tileRect
            ) ?: return
        thisJob.ensureActive()
        tile.bitmap = bitmap
        onPageUpdate()
    }

    /** True if the [pageSize] * [scale] exceeds [maxBitmapSizePx] */
    private fun needsTiling(scale: Float): Boolean {
        return ((pageSize.x * scale) >= maxBitmapSizePx.x) ||
            ((pageSize.y * scale) >= maxBitmapSizePx.y)
    }

    /**
     * Returns a size that is as near as possible to [pageSize] * [requestedScale] while being
     * smaller than [maxSize] in both dimensions
     */
    private fun limitBitmapSize(requestedScale: Float, maxSize: Point): Size {
        val finalSize = PointF(pageSize.x * requestedScale, pageSize.y * requestedScale)
        // Reduce final size by 10% in each dimension until the constraints are satisfied
        while (finalSize.x > maxSize.x || finalSize.y > maxSize.y) {
            finalSize.x *= 0.9f
            finalSize.y *= 0.9f
        }
        return Size(finalSize.x.roundToInt(), finalSize.y.roundToInt())
    }

    companion object {
        /** The size of a single tile in pixels, when tiling is used */
        @VisibleForTesting internal val tileSizePx = Point(800, 800)
    }
}

/** Represents the [Bitmap] or [Bitmap]s used to render this page */
internal sealed interface PageContents {
    val renderedScale: Float
}

/** A singular [Bitmap] depicting the full page, when full page rendering is used */
internal class FullPageBitmap(val bitmap: Bitmap, override val renderedScale: Float) : PageContents

/**
 * A set of [Bitmap]s that depict the full page as a rectangular grid of individual bitmap tiles.
 * This [PageContents] is mutable; it's updated with new Bitmaps as tiles are loaded incrementally
 */
internal class TileBoard(
    val tileSizePx: Point,
    val pageSizePx: Point,
    override val renderedScale: Float
) : PageContents {

    /** The low res background [Bitmap] for this [TileBoard] */
    var backgroundBitmap: Bitmap? = null

    /** The number of rows in the current tiling */
    private val numRows
        get() = (1 + (pageSizePx.y - 1) / tileSizePx.y)

    /** The number of columns in the current tiling */
    private val numCols
        get() = (1 + (pageSizePx.x - 1) / tileSizePx.x)

    /** The [Tile]s in this board */
    val tiles = Array(numRows * numCols) { index -> Tile(index) }

    /** An individual [Tile] in this [TileBoard] */
    inner class Tile(index: Int) {
        /** The x position of this tile in the tile board */
        private val rowIdx = index / numCols

        /** The y position of this tile in the tile board */
        private val colIdx = index % numCols

        /**
         * The offset of this [Tile] from the origin of the page in pixels, used in computations
         * where an exact pixel size is expected, e.g. rendering bitmaps
         */
        val offsetPx = Point(colIdx * tileSizePx.x, rowIdx * tileSizePx.y)

        /** The size of this [Tile] in pixels */
        val exactSizePx =
            Point(
                minOf(tileSizePx.x, pageSizePx.x - offsetPx.x),
                minOf(tileSizePx.y, pageSizePx.y - offsetPx.y),
            )

        /** The high res [Bitmap] for this [Tile] */
        var bitmap: Bitmap? = null
    }
}
