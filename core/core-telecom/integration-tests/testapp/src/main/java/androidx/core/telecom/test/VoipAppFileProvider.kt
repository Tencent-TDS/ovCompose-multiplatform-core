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

package androidx.core.telecom.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.telecom.util.ExperimentalAppActions
import java.io.File
import java.io.FileOutputStream

/**
 * `VoipAppFileProvider` is a custom implementation of `FileProvider` designed to handle file
 * operations specifically for storing and retrieving bitmaps associated with `VoipCall` objects. It
 * ensures that bitmaps are stored in a private directory accessible only to the app.
 */
@OptIn(ExperimentalAppActions::class)
class VoipAppFileProvider() : FileProvider() {
    /** The directory where files (bitmaps) will be stored. */
    var mVoipAppDirectory: File? = null

    /** The application context. */
    var mContext: Context? = null

    companion object {
        val TAG: String = VoipAppFileProvider::class.java.simpleName

        /**
         * The authority of this `FileProvider`, used in defining the content URI. This should match
         * the authority defined in the app's manifest file.
         */
        const val FILE_PROVIDER_AUTHORITIES = "androidx.core.telecom.test.fileprovider"

        /**
         * The name of the subdirectory within the app's files directory where bitmaps will be
         * stored.
         */
        const val FILE_DIR_NAME = "files"
    }

    /**
     * Initializes the `VoipAppFileProvider` with the given application context.
     *
     * This method sets up the file directory where bitmaps will be stored. It creates the directory
     * if it doesn't exist.
     *
     * @param context The application context.
     * @return The initialized `VoipAppFileProvider` instance.
     */
    fun init(context: Context): VoipAppFileProvider {
        mContext = context
        val internalStorageDir = context.filesDir
        mVoipAppDirectory = File(internalStorageDir, FILE_DIR_NAME)
        if (!mVoipAppDirectory!!.exists()) {
            mVoipAppDirectory!!.mkdirs()
            if (mVoipAppDirectory!!.exists()) {
                Log.i(TAG, "Files directory created successfully")
            } else {
                Log.e(TAG, "Failed to create files directory")
            }
        }
        return this
    }

    /**
     * Writes a bitmap associated with a `VoipCall` to a file and updates the call's icon URI.
     *
     * @param call The `VoipCall` object containing the bitmap to be written.
     * @return The URI of the newly created file, or null if an error occurred.
     */
    fun writeCallIconBitMapToFile(call: VoipCall): Uri? {
        return try {
            val fileName = call.getIconFileName()
            val bitmap = call.getIconBitmap()
            val imageFile = File(mVoipAppDirectory, "$fileName.png")
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            val uri = getUriForFile(mContext!!, FILE_PROVIDER_AUTHORITIES, imageFile)
            call.setIconUri(uri)
            Log.d(TAG, "wrote imageFile=[$imageFile]. uri=[$uri], dir=[$mVoipAppDirectory]")
            return uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads a bitmap from a file specified by its URI.
     *
     * @param uri The URI of the file to read.
     * @return The bitmap read from the file, or null if an error occurred.
     */
    fun readCallIconUriFromFile(uri: Uri): Bitmap? {
        return try {
            val inputStream = mContext!!.contentResolver.openInputStream(uri)
            val b = BitmapFactory.decodeStream(inputStream)
            return b
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
