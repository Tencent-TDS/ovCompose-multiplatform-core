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

package androidx.pdf

import android.content.ContentResolver
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.fragment.app.Fragment
import androidx.pdf.data.DisplayData
import androidx.pdf.data.FutureValue
import androidx.pdf.data.Openable
import androidx.pdf.fetcher.Fetcher
import androidx.pdf.find.FindInFileView
import androidx.pdf.models.PageSelection
import androidx.pdf.util.ObservableValue.ValueObserver
import androidx.pdf.util.Observables
import androidx.pdf.util.Observables.ExposedValue
import androidx.pdf.util.Preconditions
import androidx.pdf.util.TileBoard
import androidx.pdf.util.Uris
import androidx.pdf.viewer.FastScrollPositionValueObserver
import androidx.pdf.viewer.LayoutHandler
import androidx.pdf.viewer.LoadingView
import androidx.pdf.viewer.PageIndicator
import androidx.pdf.viewer.PageSelectionValueObserver
import androidx.pdf.viewer.PageViewFactory
import androidx.pdf.viewer.PaginatedView
import androidx.pdf.viewer.PaginationModel
import androidx.pdf.viewer.PdfSelectionHandles
import androidx.pdf.viewer.PdfSelectionModel
import androidx.pdf.viewer.SearchModel
import androidx.pdf.viewer.SingleTapHandler
import androidx.pdf.viewer.ZoomScrollValueObserver
import androidx.pdf.viewer.loader.PdfLoader
import androidx.pdf.viewer.loader.PdfLoaderCallbacksImpl
import androidx.pdf.widget.FastScrollContentModel
import androidx.pdf.widget.FastScrollContentModelImpl
import androidx.pdf.widget.FastScrollView
import androidx.pdf.widget.ZoomView
import androidx.pdf.widget.ZoomView.ContentResizedMode
import androidx.pdf.widget.ZoomView.InitialZoomMode
import androidx.pdf.widget.ZoomView.RotateMode
import androidx.pdf.widget.ZoomView.ZoomScroll
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * A Fragment that renders a PDF document.
 *
 * <p>A [PdfViewerFragment] that can display paginated PDFs. The viewer includes a FAB for
 * annotation support and a search menu. Each page is rendered in its own View. Upon creation, this
 * fragment displays a loading spinner.
 *
 * <p>Rendering is done in 2 passes:
 * <ol>
 * <li>Layout: Request the page data, get the dimensions and set them as measure for the image view.
 * <li>Render: Create bitmap(s) at adequate dimensions and attach them to the page view.
 * </ol>
 *
 * <p>The layout pass is progressive: starts with a few first pages of the document, then reach
 * further as the user scrolls down (and ultimately spans the whole document). The rendering pass is
 * tightly limited to the currently visible pages. Pages that are scrolled past (become not visible)
 * have their bitmaps released to free up memory.
 *
 * @see documentUri
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
open class PdfViewerFragment : Fragment() {

    /** Single access to the PDF document: loads contents asynchronously (bitmaps, text,...) */
    private var pdfLoader: PdfLoader? = null

    /** True when this Fragment's life-cycle is between [.onStart] and [.onStop]. */
    private var started = false

    /**
     * True when this Viewer is on-screen (but independent on whether it is actually started, so it
     * could be invisible, because obscured by another app). This value is controlled by
     * [.postEnter] and [.exit].
     */
    private var onScreen = false

    /** Marks that [.onEnter] must be run after [.onCreateView]. */
    private var delayedEnter = false
    private var hasContents = false

    private var container: ViewGroup? = null
    private var viewState: ExposedValue<ViewState> =
        Observables.newExposedValueWithInitialValue(ViewState.NO_VIEW)
    private var zoomView: ZoomView? = null
    private var paginatedView: PaginatedView? = null
    private var pageIndicator: PageIndicator? = null
    private var fastScrollView: FastScrollView? = null
    private var fastScrollContentModel: FastScrollContentModel? = null
    private var selectionObserver: ValueObserver<PageSelection>? = null

    private var fetcher: Fetcher? = null
    private var zoomScrollObserver: ValueObserver<ZoomScroll>? = null
    private var fastscrollerPositionObserver: ValueObserver<Int>? = null
    private var fastscrollerPositionObserverKey: Any? = null
    private var pdfViewer: FrameLayout? = null
    private var findInFileView: FindInFileView? = null
    private var singleTapHandler: SingleTapHandler? = null

    /** Callbacks of PDF loading asynchronous tasks. */
    private var pdfLoaderCallbacks: PdfLoaderCallbacksImpl? = null

    /** A saved [.onContentsAvailable] runnable to be run after [.onCreateView]. */
    private var delayedContentsAvailable: Runnable? = null
    private var loadingView: LoadingView? = null
    private var paginationModel: PaginationModel? = null
    private var layoutHandler: LayoutHandler? = null
    private var pageViewFactory: PageViewFactory? = null
    private var selectionHandles: PdfSelectionHandles? = null
    private var annotationButton: FloatingActionButton? = null
    private var fileData: DisplayData? = null

    internal var shouldRedrawOnDocumentLoaded = false
    internal var isAnnotationIntentResolvable = false
    internal var documentLoaded = false

    /**
     * The URI of the PDF document to display defaulting to `null`.
     *
     * When this property is set, the fragment begins loading the PDF. A loading spinner is
     * displayed until the document is fully loaded. If an error occurs during loading, an error
     * message is displayed, and the detailed exception can be captured by overriding
     * [onLoadDocumentError].
     */
    var documentUri: Uri? = null
        set(value) {
            field = value
            if (value != null) {
                loadFile(value)
            }
        }

    /**
     * Controls the visibility of the "find in file" menu. Defaults to `false`.
     *
     * Set to `true` to display the menu, or `false` to hide it.
     */
    var isTextSearchActive: Boolean = false

    /**
     * Callback invoked when an error occurs while loading the PDF document.
     *
     * Override this method to handle document loading errors. The default implementation displays a
     * generic error message in the loading view.
     *
     * @param throwable [Throwable] that occurred during document loading.
     */
    @Suppress("UNUSED_PARAMETER") fun onLoadDocumentError(throwable: Throwable) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fetcher = Fetcher.build(requireContext(), 1)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        this.container = container

        if (!hasContents && delayedContentsAvailable == null) {
            if (savedInstanceState != null) {
                restoreContents(savedInstanceState)
            }
        }

        pdfViewer = inflater.inflate(R.layout.pdf_viewer_container, container, false) as FrameLayout
        findInFileView = pdfViewer?.findViewById(R.id.search)
        fastScrollView = pdfViewer?.findViewById(R.id.fast_scroll_view)
        loadingView = pdfViewer?.findViewById(R.id.loadingView)
        paginatedView = fastScrollView?.findViewById(R.id.pdf_view)

        zoomView = fastScrollView?.findViewById(R.id.zoom_view)

        zoomView?.let {
            it.setStraightenVerticalScroll(true)

            it.setFitMode(ZoomView.FitMode.FIT_TO_WIDTH)
                .setInitialZoomMode(InitialZoomMode.ZOOM_TO_FIT)
                .setRotateMode(RotateMode.KEEP_SAME_VIEWPORT_WIDTH)
                .setContentResizedModeX(ContentResizedMode.KEEP_SAME_RELATIVE)

            // Setting an id so that the View can restore itself. The Id has to be unique and
            // predictable. An alternative that doesn't require id is to rely on this Fragment's
            // onSaveInstanceState().
            it.id = id * 100
            it.adjustZoomViewMargins()
            // The view system requires the document loaded in order to be properly initialized, so
            // we delay anything view-related until ViewState.VIEW_READY.
            it.visibility = View.GONE
        }

        zoomView?.adjustZoomViewMargins()

        annotationButton = pdfViewer?.findViewById(R.id.edit_fab)

        // All views are inflated, update the view state.
        if (viewState.get() == ViewState.NO_VIEW || viewState.get() == ViewState.ERROR) {
            viewState.set(ViewState.VIEW_CREATED)
            // View Inflated, show loading view
            loadingView?.showLoadingView()
        }

        pageIndicator = PageIndicator(requireActivity(), fastScrollView!!)

        pdfLoaderCallbacks =
            PdfLoaderCallbacksImpl(
                this,
                fastScrollView!!,
                zoomView!!,
                paginatedView!!,
                loadingView!!,
                annotationButton!!,
                pageIndicator!!,
                viewState
            )

        return pdfViewer
    }

    override fun onStart() {
        delayedContentsAvailable?.run()
        super.onStart()
        started = true
        if (delayedEnter || onScreen) {
            onEnter()
            delayedEnter = false
        }
    }

    override fun onStop() {
        if (onScreen) {
            onExit()
        }
        onScreen = false
        started = false
        super.onStop()
    }

    /** Called after this viewer enters the screen and becomes visible. */
    private fun onEnter() {
        participateInAccessibility(true)

        // This is necessary for password protected PDF documents. If the user failed to produce the
        // correct password, we want to prompt for the correct password every time the film strip
        // comes back to this viewer.
        if (!documentLoaded) {
            pdfLoader?.reconnect()
        }

        if (paginatedView != null && paginatedView?.childCount!! > 0) {
            pdfLoaderCallbacks?.loadPageAssets(zoomView?.zoomScroll()?.get()!!)
        }
    }

    /** Called after this viewer exits the screen and becomes invisible to the user. */
    protected fun onExit() {
        participateInAccessibility(false)
        if (!documentLoaded) {
            // e.g. a password-protected pdf that wasn't loaded.
            pdfLoader?.disconnect()
        }
    }

    /**
     * Notifies this Viewer goes on-screen. Guarantees that [.onEnter] will be called now or when
     * the Viewer is started.
     */
    private fun postEnter() {
        onScreen = true
        if (started) {
            onEnter()
        } else {
            delayedEnter = true
        }
    }

    /**
     * Returns true when this Viewer is on-screen (= entered but not exited) and active (i.e. the
     * Activity is resumed).
     */
    private fun isShowing(): Boolean {
        return isResumed && onScreen
    }

    private fun isStarted(): Boolean {
        return started
    }

    /**
     * Posts a [.onContentsAvailable] method to be run as soon as permitted (when this Viewer has
     * its view hierarchy built up and [.onCreateView] has finished). It might run right now if the
     * Viewer is currently started.
     */
    protected fun postContentsAvailable(contents: DisplayData, savedState: Bundle?) {
        Preconditions.checkState(delayedContentsAvailable == null, "Already waits for contents")

        if (isStarted()) {
            onContentsAvailable(contents, savedState)
            hasContents = true
        } else {
            delayedContentsAvailable = Runnable {
                Preconditions.checkState(
                    !hasContents,
                    "Received contents while restoring another copy"
                )
                onContentsAvailable(contents, savedState)
                delayedContentsAvailable = null
                hasContents = true
            }
        }
    }

    private fun onContentsAvailable(contents: DisplayData, savedState: Bundle?) {
        fileData = contents

        if (pdfLoader != null) {
            destroyContentModel()
        }
        if (paginatedView?.childCount!! > 0) {
            paginatedView?.removeAllViews()
        }

        createContentModel(
            PdfLoader.create(
                requireActivity().applicationContext,
                contents,
                TileBoard.DEFAULT_RECYCLER,
                pdfLoaderCallbacks!!,
                false
            )
        )

        layoutHandler = LayoutHandler(pdfLoader!!)
        pdfLoaderCallbacks!!.layoutHandler = layoutHandler
        zoomView?.setPdfSelectionModel(pdfLoaderCallbacks?.selectionModel!!)
        paginatedView?.selectionModel = pdfLoaderCallbacks?.selectionModel!!
        paginatedView?.searchModel = pdfLoaderCallbacks?.searchModel!!
        paginatedView?.setPdfLoader(pdfLoader!!)

        fastscrollerPositionObserver =
            FastScrollPositionValueObserver(fastScrollView!!, pageIndicator!!)
        fastscrollerPositionObserver?.onChange(null, fastScrollView!!.scrollerPositionY.get())
        fastscrollerPositionObserverKey =
            fastScrollView!!.scrollerPositionY.addObserver(fastscrollerPositionObserver)

        zoomScrollObserver =
            ZoomScrollValueObserver(
                zoomView!!,
                paginatedView!!,
                layoutHandler!!,
                annotationButton,
                findInFileView,
                pageIndicator!!,
                fastScrollView!!,
                isAnnotationIntentResolvable,
                viewState
            )
        zoomView?.zoomScroll()?.addObserver(zoomScrollObserver)

        singleTapHandler =
            SingleTapHandler(
                requireContext(),
                annotationButton!!,
                findInFileView!!,
                zoomView!!,
                pdfLoaderCallbacks?.selectionModel!!,
                paginationModel!!,
                layoutHandler!!
            )
        pageViewFactory =
            PageViewFactory(
                requireContext(),
                pdfLoader!!,
                paginatedView!!,
                zoomView!!,
                singleTapHandler!!
            )
        pdfLoaderCallbacks?.pageViewFactory = pageViewFactory!!
        paginatedView?.pageViewFactory = pageViewFactory!!

        selectionObserver =
            PageSelectionValueObserver(
                paginatedView!!,
                paginationModel!!,
                pageViewFactory!!,
                requireContext()
            )
        pdfLoaderCallbacks?.selectionModel?.selection()?.addObserver(selectionObserver)

        savedState?.containsKey(KEY_LAYOUT_REACH)?.let {
            val layoutReach = savedState.getInt(KEY_LAYOUT_REACH)
            layoutHandler?.setInitialPageLayoutReachWithMax(layoutReach)
        }
    }

    private fun createContentModel(pdfLoader: PdfLoader) {
        this.pdfLoader = pdfLoader

        paginationModel = paginatedView!!.initPaginationModelAndPageRangeHandler(requireContext())
        fastScrollContentModel = FastScrollContentModelImpl(paginationModel!!, zoomView!!)

        if (fastScrollView != null) {
            fastScrollView?.setScrollable(fastScrollContentModel!!)
            fastScrollView?.id = id * 10
        }
        pdfLoaderCallbacks?.searchModel = SearchModel(pdfLoader)
        pdfLoaderCallbacks?.selectionModel = PdfSelectionModel(pdfLoader)
        selectionHandles =
            PdfSelectionHandles(pdfLoaderCallbacks?.selectionModel!!, zoomView!!, paginatedView!!)
    }

    /** Restores the contents of this Viewer when it is automatically restored by android. */
    private fun restoreContents(savedState: Bundle?) {
        val dataBundle = savedState?.getBundle(KEY_DATA)
        if (dataBundle != null) {
            try {
                val restoredData = DisplayData.fromBundle(dataBundle)
                postContentsAvailable(restoredData, savedState)
            } catch (e: Exception) {
                // This can happen if the data is an instance of StreamOpenable, and the client
                // app that owns it has been killed by the system. We will still recover,
                // but log this.
                viewState.set(ViewState.ERROR)
            }
        }
    }

    private fun destroyContentModel() {

        paginationModel = null
        fastScrollContentModel = null

        selectionHandles?.destroy()
        selectionHandles = null

        pdfLoaderCallbacks?.selectionModel = null

        pdfLoaderCallbacks?.searchModel = null

        pdfLoader?.disconnect()
        pdfLoader = null
        documentLoaded = false
    }

    private fun destroyView() {
        if (zoomView != null) {
            zoomView?.zoomScroll()?.removeObserver(zoomScrollObserver!!)
            zoomView = null
        }

        if (paginatedView != null) {
            paginatedView?.removeAllViews()
            paginationModel?.removeObserver(paginatedView!!)
            paginatedView = null
        }

        pdfLoader?.cancelAll()
        pdfLoader?.disconnect()
        documentLoaded = false
        zoomView?.zoomViewBasePadding = Rect()
        zoomView?.isZoomViewBasePaddingSaved = false
        if (viewState.get() !== ViewState.NO_VIEW) {
            viewState.set(ViewState.NO_VIEW)
        }
        if (container != null && view != null && container === requireView().parent) {
            // Some viewers add extra views to their container, e.g. toasts. Remove them all.
            // Do not remove what's under it though.
            val count = container?.childCount
            var child: View
            if (count != null) {
                for (i in count - 1 downTo 1) {
                    child = container!!.getChildAt(i)
                    container?.removeView(child)
                    if (child === view) {
                        break
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        destroyView()
        container = null
        super.onDestroyView()

        fastScrollView!!.scrollerPositionY.removeObserver(fastscrollerPositionObserverKey!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (pdfLoader != null) {
            destroyContentModel()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_DATA, fileData?.asBundle())
        layoutHandler?.let { outState.putInt(KEY_LAYOUT_REACH, it.pageLayoutReach) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        zoomView?.adjustZoomViewMargins()
    }

    private fun loadFile(fileUri: Uri) {
        Preconditions.checkNotNull(fileUri)
        try {
            validateFileUri(fileUri)
            fetchFile(fileUri)
        } catch (e: SecurityException) {
            onLoadDocumentError(e)
        }
    }

    private fun validateFileUri(fileUri: Uri) {
        if (!Uris.isContentUri(fileUri) && !Uris.isFileUri(fileUri)) {
            throw IllegalArgumentException("Only content and file uri is supported")
        }
    }

    private fun fetchFile(fileUri: Uri) {
        Preconditions.checkNotNull(fileUri)
        val fileName: String = getFileName(fileUri)
        val openable: FutureValue<Openable> = fetcher?.loadLocal(fileUri)!!

        openable[
            object : FutureValue.Callback<Openable> {
                override fun available(value: Openable) {
                    viewerAvailable(fileUri, fileName, value)
                }

                override fun failed(thrown: Throwable) {
                    finishActivity()
                }

                override fun progress(progress: Float) {}
            }]
    }

    private fun finishActivity() {
        if (activity != null) {
            requireActivity().finish()
        }
    }

    private fun getFileName(fileUri: Uri): String {
        val resolver: ContentResolver? = getResolver()
        return if (resolver != null) Uris.extractName(fileUri, resolver)
        else Uris.extractFileName(fileUri)
    }

    private fun getResolver(): ContentResolver? {
        if (activity != null) {
            return requireActivity().contentResolver
        }
        return null
    }

    private fun viewerAvailable(fileUri: Uri, fileName: String, openable: Openable) {
        val contents = DisplayData(fileUri, fileName, openable)

        startViewer(contents)
    }

    private fun startViewer(contents: DisplayData) {
        Preconditions.checkNotNull(contents)

        feed(contents)
        postEnter()
    }

    /** Feed this Viewer with contents to be displayed. */
    private fun feed(contents: DisplayData?): PdfViewerFragment {
        if (contents != null) {
            postContentsAvailable(contents, null)
        }
        return this
    }

    /** Makes the views of this Viewer visible to TalkBack (in the swipe gesture circus) or not. */
    private fun participateInAccessibility(participate: Boolean) {
        view?.importantForAccessibility =
            if (participate) View.IMPORTANT_FOR_ACCESSIBILITY_YES
            else View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    companion object {
        private const val KEY_LAYOUT_REACH: String = "plr"
        private const val KEY_DATA: String = "data"
    }
}
