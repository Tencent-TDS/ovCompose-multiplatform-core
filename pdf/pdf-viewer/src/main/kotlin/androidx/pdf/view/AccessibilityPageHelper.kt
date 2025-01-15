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

import android.content.Context
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import androidx.pdf.R
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.pdf.util.ExternalLinks

/**
 * Accessibility delegate for PdfView that provides a virtual view hierarchy for pages.
 *
 * This helper class allows accessibility services to interact with individual pages as virtual
 * views, enabling navigation and content exploration.
 */
internal class AccessibilityPageHelper(
    private val pdfView: PdfView,
    private val pageLayoutManager: PageLayoutManager,
    private val pageManager: PageManager
) : ExploreByTouchHelper(pdfView) {

    private var gotoLinks: MutableList<PdfPageGotoLinkContent> = mutableListOf()
    private var urlLinks: MutableList<PdfPageLinkContent> = mutableListOf()
    private val totalPages = pdfView.pdfDocument?.pageCount ?: 0
    private var isLinksLoaded = false

    public override fun getVirtualViewAt(x: Float, y: Float): Int {
        val visiblePages = pageLayoutManager.visiblePages.value

        val contentX = pdfView.toContentX(x).toInt()
        val contentY = pdfView.toContentY(y).toInt()

        if (!isLinksLoaded) {
            loadPageLinks()
        }

        // Check if the coordinates fall within any of the gotoLinks bounds
        gotoLinks.forEachIndexed { index, gotoLink ->
            if (gotoLink.bounds.any { it.contains(contentX.toFloat(), contentY.toFloat()) }) {
                return index + totalPages
            }
        }

        // Check if the coordinates fall within any of the urlLinks bounds
        urlLinks.forEachIndexed { index, urlLink ->
            if (urlLink.bounds.any { it.contains(contentX.toFloat(), contentY.toFloat()) }) {
                return index + totalPages + gotoLinks.size
            }
        }

        // Check if the coordinates fall within the visible page bounds
        return (visiblePages.lower..visiblePages.upper).firstOrNull { page ->
            pageLayoutManager
                .getPageLocation(page, pdfView.getVisibleAreaInContentCoords())
                .contains(contentX, contentY)
        } ?: HOST_ID
    }

    public override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
        val visiblePages = pageLayoutManager.visiblePages.value
        virtualViewIds.addAll(visiblePages.lower..visiblePages.upper)

        loadPageLinks()

        gotoLinks.forEachIndexed { index, _ -> virtualViewIds.add(totalPages + index) }

        urlLinks.forEachIndexed { index, _ ->
            virtualViewIds.add(totalPages + gotoLinks.size + index)
        }
    }

    public override fun onPopulateNodeForVirtualView(
        virtualViewId: Int,
        @NonNull node: AccessibilityNodeInfoCompat
    ) {
        if (!isLinksLoaded) loadPageLinks()

        if (virtualViewId < totalPages) {
            populateNodeForPage(virtualViewId, node)
        } else {
            populateNodeForLink(virtualViewId, node)
        }
    }

    override fun onPerformActionForVirtualView(
        virtualViewId: Int,
        action: Int,
        arguments: Bundle?
    ): Boolean {
        // This view does not handle any actions.
        return false
    }

    private fun populateNodeForPage(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
        val pageText = pageManager.pages[virtualViewId]?.pageText
        val pageBounds =
            pageLayoutManager.getPageLocation(
                virtualViewId,
                pdfView.getVisibleAreaInContentCoords()
            )

        node.apply {
            contentDescription =
                pageText?.let { getContentDescriptionForPage(pdfView.context, virtualViewId, it) }
                    ?: getDefaultDesc(pdfView.context, virtualViewId)

            setBoundsInScreenFromBoundsInParent(
                node,
                scalePageBounds(RectF(pageBounds), pdfView.zoom)
            )
            isFocusable = true
        }
    }

    private fun populateNodeForLink(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
        val adjustedId = virtualViewId - totalPages
        if (adjustedId < gotoLinks.size) {
            populateGotoLinkNode(adjustedId, node)
        } else {
            populateUrlLinkNode(adjustedId - gotoLinks.size, node)
        }
    }

    private fun populateGotoLinkNode(linkIndex: Int, node: AccessibilityNodeInfoCompat) {
        val gotoLink = gotoLinks[linkIndex]
        val bounds = scalePageBounds(gotoLink.bounds.first(), pdfView.zoom)
        node.apply {
            contentDescription =
                pdfView.context.getString(
                    R.string.desc_goto_link,
                    gotoLink.destination.pageNumber + 1
                )
            setBoundsInScreenFromBoundsInParent(node, bounds)
            isFocusable = true
        }
    }

    private fun populateUrlLinkNode(linkIndex: Int, node: AccessibilityNodeInfoCompat) {
        val urlLink = urlLinks[linkIndex]
        val bounds = scalePageBounds(urlLink.bounds.first(), pdfView.zoom)
        node.apply {
            contentDescription =
                ExternalLinks.getDescription(urlLink.uri.toString(), pdfView.context)
            setBoundsInScreenFromBoundsInParent(node, bounds)
            isFocusable = true
        }
    }

    @VisibleForTesting
    fun scalePageBounds(bounds: RectF, zoom: Float): Rect {
        return Rect(
            (bounds.left * zoom).toInt(),
            (bounds.top * zoom).toInt(),
            (bounds.right * zoom).toInt(),
            (bounds.bottom * zoom).toInt()
        )
    }

    fun loadPageLinks() {
        val visiblePages = pageLayoutManager.visiblePages.value

        // Clear existing links and fetch new ones for the visible pages
        gotoLinks.clear()
        urlLinks.clear()

        (visiblePages.lower..visiblePages.upper).forEach { pageIndex ->
            pageManager.pages[pageIndex]?.links?.let { links ->
                links.gotoLinks.let { gotoLinks.addAll(it) }
                links.externalLinks.let { urlLinks.addAll(it) }
            }
        }
        isLinksLoaded = true
    }

    /**
     * Updates accessibility node with extracted page text when ready.
     *
     * @param pageNum 0-indexed page number.
     */
    fun onPageTextReady(pageNum: Int) {
        val pageText = pageManager.pages.get(pageNum)?.pageText

        if (pageText != null) {
            // Update accessibility node with new text.
            invalidateVirtualView(pageNum)
        }
    }

    companion object {

        /**
         * Builds the content description for a page.
         *
         * @param context The context for accessing resources.
         * @param pageText The extracted text content of the page, or null if not loaded.
         * @param pageNum The 0-indexed page number.
         * @return The content description string.
         */
        private fun getContentDescriptionForPage(
            context: Context,
            pageNum: Int,
            pageText: String?
        ): String {
            return when {
                pageText == null -> getDefaultDesc(context, pageNum)
                pageText.trim().isEmpty() -> context.getString(R.string.desc_empty_page)
                else -> context.getString(R.string.desc_page_with_text, pageNum + 1, pageText)
            }
        }

        /**
         * Gets the default content description for a page. This is used as a placeholder while the
         * actual content description is loading.
         *
         * @param context The context for accessing resources.
         * @param pageNum The 0-indexed page number.
         * @return The default content description string.
         */
        private fun getDefaultDesc(context: Context, pageNum: Int): String {
            return context.getString(R.string.desc_page, pageNum + 1)
        }
    }
}
