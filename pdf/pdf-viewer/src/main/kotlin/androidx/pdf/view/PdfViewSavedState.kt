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

import android.net.Uri
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.ClassLoaderCreator
import androidx.customview.view.AbsSavedState

/** [AbsSavedState] implementation for [PdfView] */
internal class PdfViewSavedState : AbsSavedState {
    var contentCenterX: Float = 0F
    var contentCenterY: Float = 0F
    var zoom: Float = 1F
    var documentUri: Uri? = null
    var paginationModel: PaginationModel? = null
    /**
     * The width of the PdfView before the last layout change (e.g., before rotation). Used to
     * preserve the zoom level when the device is rotated.
     */
    var viewWidth: Int = 0

    /**
     * If we don't know what document this state belongs to, we cannot restore it. If we do not have
     * dimensions from the previous [PdfView] instance, we cannot restore position reliably. These
     * values are expected to be present in a correctly-saved instance.
     */
    val hasEnoughStateToRestore: Boolean
        get() {
            return documentUri != null && paginationModel != null
        }

    /**
     * Constructor used in [PdfView.onSaveInstanceState] to create an instance from superclass state
     */
    constructor(superState: Parcelable?) : super(requireNotNull(superState))

    /** Constructor used when reading from a [Parcel], i.e. in [PdfView.onRestoreInstanceState] */
    @Suppress("DEPRECATION")
    constructor(parcel: Parcel, loader: ClassLoader?) : super(parcel, loader) {
        contentCenterX = parcel.readFloat()
        contentCenterY = parcel.readFloat()
        zoom = parcel.readFloat()
        documentUri =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                parcel.readParcelable(loader, Uri::class.java)
            } else {
                parcel.readParcelable(loader)
            }
        paginationModel =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                parcel.readParcelable(loader, PaginationModel::class.java)
            } else {
                parcel.readParcelable(loader)
            }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeFloat(contentCenterX)
        dest.writeFloat(contentCenterY)
        dest.writeFloat(zoom)
        dest.writeParcelable(documentUri, flags)
        dest.writeParcelable(paginationModel, flags)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PdfViewSavedState> =
            object : ClassLoaderCreator<PdfViewSavedState> {
                override fun createFromParcel(
                    source: Parcel,
                    loader: ClassLoader?
                ): PdfViewSavedState {
                    return PdfViewSavedState(source, loader)
                }

                override fun createFromParcel(source: Parcel): PdfViewSavedState {
                    return PdfViewSavedState(source, null)
                }

                override fun newArray(size: Int): Array<PdfViewSavedState?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
