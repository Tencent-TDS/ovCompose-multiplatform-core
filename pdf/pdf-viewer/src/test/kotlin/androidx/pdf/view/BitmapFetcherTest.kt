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

import android.graphics.Point
import androidx.pdf.PdfDocument
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapFetcherTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val pdfDocument =
        mock<PdfDocument> {
            on { getPageBitmapSource(any()) } doAnswer
                { invocation ->
                    FakeBitmapSource(invocation.getArgument(0))
                }
        }

    private var invalidationCounter = 0
    private val invalidationTracker: () -> Unit = { invalidationCounter++ }

    private val maxBitmapSizePx = Point(2048, 2048)
    private val pageSize = Point(512, 512)

    private lateinit var bitmapFetcher: BitmapFetcher
    private lateinit var tileSizePx: Point

    @Before
    fun setup() {
        testDispatcher.cancelChildren()
        invalidationCounter = 0

        bitmapFetcher =
            BitmapFetcher(
                pageNum = 0,
                pageSize,
                pdfDocument,
                testScope,
                maxBitmapSizePx,
                invalidationTracker,
            )
        bitmapFetcher.isActive = true
        tileSizePx = BitmapFetcher.tileSizePx
    }

    @Test
    fun setInactive_cancelsWorkAndFreesBitmaps() {
        bitmapFetcher.onScaleChanged(1.5f)
        assertThat(bitmapFetcher.renderingJob?.isActive).isTrue()

        bitmapFetcher.isActive = false
        assertThat(bitmapFetcher.renderingJob).isNull()
        assertThat(bitmapFetcher.pageContents).isNull()
    }

    @Test
    fun setScale_rendersFullPageBitmap() {
        bitmapFetcher.onScaleChanged(1.5f)

        testDispatcher.scheduler.runCurrent()

        val pageBitmaps = bitmapFetcher.pageContents
        assertThat(pageBitmaps).isInstanceOf(FullPageBitmap::class.java)
        assertThat(pageBitmaps?.renderedScale).isEqualTo(1.5f)
        pageBitmaps as FullPageBitmap // Make smartcast work nicely below
        assertThat(pageBitmaps.bitmap.width).isEqualTo((pageSize.x * 1.5f).roundToInt())
        assertThat(pageBitmaps.bitmap.height).isEqualTo((pageSize.y * 1.5f).roundToInt())
        assertThat(invalidationCounter).isEqualTo(1)
    }

    @Test
    fun setScale_rendersTileBoard() {
        bitmapFetcher.onScaleChanged(5.0f)

        testDispatcher.scheduler.runCurrent()

        val pageBitmaps = bitmapFetcher.pageContents
        assertThat(pageBitmaps).isInstanceOf(TileBoard::class.java)
        assertThat(pageBitmaps?.renderedScale).isEqualTo(5.0f)
        pageBitmaps as TileBoard // Make smartcast work nicely below

        // Check the properties of an arbitrary full-size tile
        val row0Col2 = pageBitmaps.tiles[2]
        assertThat(row0Col2.bitmap?.width).isEqualTo(tileSizePx.x)
        assertThat(row0Col2.bitmap?.height).isEqualTo(tileSizePx.y)
        assertThat(row0Col2.offsetPx).isEqualTo(Point(tileSizePx.x * 2, 0))
        assertThat(row0Col2.exactSizePx).isEqualTo(Point(tileSizePx.x, tileSizePx.y))

        // Check the properties of the last tile in the bottom right corner
        val row3Col3 = pageBitmaps.tiles[15]
        val pageSizePx = Point(pageSize.x * 5, pageSize.y * 5)
        // This tile is not the same size as the others b/c it's cut off on both edges
        val row3Col3Size = Point(pageSizePx.x - tileSizePx.x * 3, pageSizePx.y - tileSizePx.y * 3)
        assertThat(row3Col3.bitmap?.width).isEqualTo(row3Col3Size.x)
        assertThat(row3Col3.bitmap?.height).isEqualTo(row3Col3Size.y)
        assertThat(row3Col3.offsetPx).isEqualTo(Point(tileSizePx.x * 3, tileSizePx.y * 3))
        assertThat(row3Col3.exactSizePx).isEqualTo(row3Col3Size)

        // 1 invalidation for the low-res background, 1 for each tile * 16 tiles
        assertThat(invalidationCounter).isEqualTo(17)
    }

    @Test
    fun setScale_toRenderedValue_noNewWork() {
        bitmapFetcher.isActive = true

        bitmapFetcher.onScaleChanged(1.5f)
        testDispatcher.scheduler.runCurrent()
        val firstBitmaps = bitmapFetcher.pageContents
        bitmapFetcher.onScaleChanged(1.5f)

        // We shouldn't have started a new Job the second time onScaleChanged to the same value
        assertThat(bitmapFetcher.renderingJob).isNull()
        // And we should still have the same bitmaps
        assertThat(bitmapFetcher.pageContents).isEqualTo(firstBitmaps)
        // 1 total invalidation
        assertThat(invalidationCounter).isEqualTo(1)
    }

    @Test
    fun setScale_toRenderingValue_noNewWork() {
        bitmapFetcher.onScaleChanged(1.5f)
        val firstJob = bitmapFetcher.renderingJob
        bitmapFetcher.onScaleChanged(1.5f)

        // This should be the same Job we started the first time onScaleChanged
        assertThat(bitmapFetcher.renderingJob).isEqualTo(firstJob)
        // 0 invalidations because we're still rendering
        assertThat(invalidationCounter).isEqualTo(0)
    }

    @Test
    fun setScale_afterInactive_rendersNewBitmaps() {
        bitmapFetcher.onScaleChanged(1.5f)
        testDispatcher.scheduler.runCurrent()
        assertThat(bitmapFetcher.pageContents).isNotNull()
        assertThat(invalidationCounter).isEqualTo(1)

        bitmapFetcher.isActive = false
        assertThat(bitmapFetcher.pageContents).isNull()

        bitmapFetcher.isActive = true
        bitmapFetcher.onScaleChanged(1.5f)
        testDispatcher.scheduler.runCurrent()
        assertThat(bitmapFetcher.pageContents).isNotNull()
        assertThat(invalidationCounter).isEqualTo(2)
    }

    @Test
    fun setScale_fromFullPage_toTiled() {
        bitmapFetcher.onScaleChanged(1.5f)
        testDispatcher.scheduler.runCurrent()
        val fullPageBitmap = bitmapFetcher.pageContents
        assertThat(fullPageBitmap).isInstanceOf(FullPageBitmap::class.java)
        assertThat(fullPageBitmap?.renderedScale).isEqualTo(1.5f)
        assertThat(invalidationCounter).isEqualTo(1)

        bitmapFetcher.onScaleChanged(5.0f)
        testDispatcher.scheduler.runCurrent()
        val tileBoard = bitmapFetcher.pageContents
        assertThat(tileBoard).isInstanceOf(TileBoard::class.java)
        assertThat(tileBoard?.renderedScale).isEqualTo(5.0f)
        // 1 invalidation for the previous full page bitmap + 1 for the low res background
        // + (1 for each tile * 16 tiles)
        assertThat(invalidationCounter).isEqualTo(18)
    }

    @Test
    fun setScale_fromTiled_toFullPage() {
        bitmapFetcher.onScaleChanged(5.0f)
        testDispatcher.scheduler.runCurrent()
        val tileBoard = bitmapFetcher.pageContents
        assertThat(tileBoard).isInstanceOf(TileBoard::class.java)
        assertThat(tileBoard?.renderedScale).isEqualTo(5.0f)
        // 1 invalidation for the low res background + (1 for each tile * 16 tiles)
        assertThat(invalidationCounter).isEqualTo(17)

        bitmapFetcher.onScaleChanged(1.5f)
        testDispatcher.scheduler.runCurrent()
        val fullPageBitmap = bitmapFetcher.pageContents
        assertThat(fullPageBitmap).isInstanceOf(FullPageBitmap::class.java)
        assertThat(fullPageBitmap?.renderedScale).isEqualTo(1.5f)
        // 1 additional invalidation for the new full page bitmap
        assertThat(invalidationCounter).isEqualTo(18)
    }
}
