/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.viewfinder;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.viewfinder.core.ImplementationMode;
import androidx.camera.viewfinder.core.ScaleType;
import androidx.camera.viewfinder.core.TransformationInfo;
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest;
import androidx.camera.viewfinder.core.ViewfinderSurfaceSession;
import androidx.camera.viewfinder.core.impl.RefCounted;
import androidx.camera.viewfinder.core.impl.ViewfinderSurfaceSessionImpl;
import androidx.camera.viewfinder.internal.futures.Futures;
import androidx.camera.viewfinder.internal.quirk.DeviceQuirks;
import androidx.camera.viewfinder.internal.quirk.SurfaceViewNotCroppedByParentQuirk;
import androidx.camera.viewfinder.internal.quirk.SurfaceViewStretchedQuirk;
import androidx.camera.viewfinder.internal.utils.Logger;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.common.util.concurrent.ListenableFuture;

import kotlin.Unit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CancellationException;

/**
 * Base viewfinder widget that can display the camera feed for Camera2.
 *
 * <p> It internally uses either a {@link TextureView} or {@link SurfaceView} to display the
 * camera feed, and applies required transformations on them to correctly display the viewfinder,
 * this involves correcting their aspect ratio, scale and rotation.
 */
public final class CameraViewfinder extends FrameLayout {

    private static final String TAG = "CameraViewFinder";

    @ColorRes private static final int DEFAULT_BACKGROUND_COLOR = android.R.color.black;
    private static final ImplementationMode DEFAULT_IMPL_MODE = ImplementationMode.EXTERNAL;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    final @NonNull ViewfinderTransformation mViewfinderTransformation =
            new ViewfinderTransformation();

    @SuppressWarnings("WeakerAccess")
    private final @NonNull DisplayRotationListener mDisplayRotationListener =
            new DisplayRotationListener();

    private final @NonNull Looper mRequiredLooper = Objects.requireNonNull(Looper.myLooper());

    /**
     * Holds the implementation mode set through the {@code app:implementationMode} attr or the
     * {@link #DEFAULT_IMPL_MODE} if that is not set.
     *
     * <p>The applied implementation mode, which is stored in {@code mCurrentImplementationMode},
     * will use the implementation mode set through the {@link ViewfinderSurfaceRequest}, or will
     * default to {@code mDefaultImplementationMode} if the surface request does not specify an
     * implementation mode.
     */
    @NonNull ImplementationMode mDefaultImplementationMode;
    @NonNull ImplementationMode mCurrentImplementationMode;

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    @Nullable ViewfinderImplementation mImplementation;


    private final OnLayoutChangeListener mOnLayoutChangeListener =
            (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                boolean isSizeChanged =
                        right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop;
                if (isSizeChanged) {
                    redrawViewfinder();
                }
            };

    @UiThread
    public CameraViewfinder(@NonNull Context context) {
        this(context, null);
    }

    @UiThread
    public CameraViewfinder(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @UiThread
    public CameraViewfinder(@NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @UiThread
    public CameraViewfinder(@NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.Viewfinder, defStyleAttr, defStyleRes);
        ViewCompat.saveAttributeDataForStyleable(this, context, R.styleable.Viewfinder, attrs,
                attributes, defStyleAttr, defStyleRes);

        try {
            final int scaleTypeId = attributes.getInteger(
                    R.styleable.Viewfinder_scaleType,
                    mViewfinderTransformation.getScaleType().getId());
            setScaleType(ScaleType.fromId(scaleTypeId));

            int implementationModeId =
                    attributes.getInteger(R.styleable.Viewfinder_implementationMode,
                            DEFAULT_IMPL_MODE.getId());
            mDefaultImplementationMode = ImplementationMode.fromId(
                    implementationModeId);
            mCurrentImplementationMode = mDefaultImplementationMode;
        } finally {
            attributes.recycle();
        }

        // Set background only if it wasn't already set. A default background prevents the content
        // behind the viewfinder from being visible before the viewfinder starts streaming.
        if (getBackground() == null) {
            setBackgroundColor(ContextCompat.getColor(getContext(), DEFAULT_BACKGROUND_COLOR));
        }
    }

    /**
     * Returns the {@link ImplementationMode}.
     *
     * <p> For each {@link ViewfinderSurfaceRequest} sent to
     * {@link CameraViewfinder}, the
     * {@link ImplementationMode} set in the
     * {@link ViewfinderSurfaceRequest} will be used first.
     * If it's not set, the {@code app:implementationMode} in the layout xml will be used. If
     * it's not set in the layout xml, the default value
     * {@link ImplementationMode#EXTERNAL}
     * will be used. Each {@link ViewfinderSurfaceRequest} sent
     * to {@link CameraViewfinder} can override the
     * {@link ImplementationMode} once it has set the
     * {@link ImplementationMode}.
     *
     * @return The {@link ImplementationMode} for
     * {@link CameraViewfinder}.
     */
    @UiThread
    public @NonNull ImplementationMode getSurfaceImplementationMode() {
        checkUiThread();
        return mCurrentImplementationMode;
    }

    /**
     * Applies a {@link ScaleType} to the viewfinder.
     *
     * <p> This value can also be set in the layout XML file via the {@code app:scaleType}
     * attribute.
     *
     * <p> The default value is {@link ScaleType#FILL_CENTER}.
     *
     * <p> This method should be called after {@link CameraViewfinder} is inflated and can be
     * called before or after
     * {@link CameraViewfinder#requestSurfaceSessionAsync(ViewfinderSurfaceRequest, TransformationInfo)}.
     * The {@link ScaleType} to set will be effective immediately after the method is called.
     *
     * @param scaleType The {@link ScaleType} to apply to the viewfinder.
     * @attr name app:scaleType
     */
    @UiThread
    public void setScaleType(final @NonNull ScaleType scaleType) {
        checkUiThread();
        mViewfinderTransformation.setScaleType(scaleType);
        redrawViewfinder();
    }

    /**
     * Returns the {@link ScaleType} currently applied to the viewfinder.
     *
     * <p> The default value is {@link ScaleType#FILL_CENTER}.
     *
     * @return The {@link ScaleType} currently applied to the viewfinder.
     */
    @UiThread
    public @NonNull ScaleType getScaleType() {
        checkUiThread();
        return mViewfinderTransformation.getScaleType();
    }

    /**
     * Requests surface by sending a {@link ViewfinderSurfaceRequest}.
     *
     * <p> Only one request can be handled at the same time. If requesting a surface with
     * a new {@link ViewfinderSurfaceRequest}, or the previous request, the previous
     * returned {@link ListenableFuture} will be cancelled if it has not yet completed.
     *
     * <p> The result is a {@link ListenableFuture} of {@link ViewfinderSurfaceSession}, which
     * provides the functionality to attach listeners and propagate exceptions.
     *
     * <pre>{@code
     * ViewfinderSurfaceRequest request = new ViewfinderSurfaceRequest(width, height);
     *
     * ListenableFuture<ViewfinderSurfaceSession> sessionFuture =
     *     mCameraViewFinder.requestSurfaceSessionAsync(request);
     *
     * Futures.addCallback(sessionFuture, new FutureCallback<ViewfinderSurfaceSession>() {
     *     {@literal @}Override
     *     public void onSuccess({@literal @}Nullable ViewfinderSurfaceSession session) {
     *         if (session != null) {
     *             createCaptureSession(session);
     *         }
     *     }
     *
     *     {@literal @}Override
     *     public void onFailure(Throwable t) {}
     * }, ContextCompat.getMainExecutor(getContext()));
     * }</pre>
     *
     * <p> Calling this method will replace any {@link TransformationInfo} previously set by
     * {@link #requestSurfaceSessionAsync(ViewfinderSurfaceRequest, TransformationInfo)} or
     * {@link #setTransformationInfo(TransformationInfo)} with default transformation info that
     * uses the default values which are part of the {@link TransformationInfo#DEFAULT} instance.
     * This assumes no rotation, mirroring, or crop region relative to the display.
     *
     * <p> If the source will produce frames that are rotated, mirrored, or require a crop, relative
     * to the display orientation, use
     * {@link #requestSurfaceSessionAsync(ViewfinderSurfaceRequest, TransformationInfo)}.
     *
     * @param surfaceRequest The {@link ViewfinderSurfaceRequest} to get a surface session.
     * @return A {@link ListenableFuture} to retrieve the eventual surface session.
     * @see ViewfinderSurfaceRequest
     * @see ViewfinderSurfaceSession
     * @see TransformationInfo#DEFAULT
     */
    @UiThread
    public @NonNull ListenableFuture<ViewfinderSurfaceSession> requestSurfaceSessionAsync(
            @NonNull ViewfinderSurfaceRequest surfaceRequest) {
        return requestSurfaceSessionAsync(surfaceRequest, TransformationInfo.DEFAULT);
    }

    /**
     * Requests surface by sending a {@link ViewfinderSurfaceRequest} for a source that produces
     * frames with characteristics described by {@link TransformationInfo}.
     *
     * <p> This is equivalent to calling
     * {@link #requestSurfaceSessionAsync(ViewfinderSurfaceRequest)}, but allows specifying a
     * {@link TransformationInfo} that will immediately be applied. This is useful when the
     * source produces frames that are rotated or mirrored from the {@link Display}'s current
     * orientation, such as if the source is a camera.
     *
     * <p> The {@link TransformationInfo} passed in will replace any transformation info that was
     * previously set by other calls to this method or
     * {@link #setTransformationInfo(TransformationInfo)}.
     *
     * @param surfaceRequest     The {@link ViewfinderSurfaceRequest} to get a surface session.
     * @param transformationInfo The {@link TransformationInfo} that specifies characteristics of
     *                          the frames produced by the source, such as rotation, mirroring,
     *                           and the desired crop rectangle.
     * @return A {@link ListenableFuture} to retrieve the eventual surface session.
     * @see ViewfinderSurfaceRequest
     * @see ViewfinderSurfaceSession
     * @see TransformationInfo
     */
    @UiThread
    public @NonNull ListenableFuture<ViewfinderSurfaceSession> requestSurfaceSessionAsync(
            @NonNull ViewfinderSurfaceRequest surfaceRequest,
            @NonNull TransformationInfo transformationInfo) {
        checkUiThread();

        if (surfaceRequest.getImplementationMode() != null) {
            mCurrentImplementationMode = surfaceRequest.getImplementationMode();
        } else {
            mCurrentImplementationMode = mDefaultImplementationMode;
        }

        ViewfinderImplementation viewfinderImplementation =
                shouldUseTextureView(mCurrentImplementationMode)
                        ? new TextureViewImplementation(
                        CameraViewfinder.this, mViewfinderTransformation)
                        : new SurfaceViewImplementation(
                                CameraViewfinder.this, mViewfinderTransformation);

        if (mImplementation != null && mImplementation != viewfinderImplementation) {
            mImplementation.onImplementationReplaced();
        }
        mImplementation = viewfinderImplementation;
        ListenableFuture<RefCounted<Surface>> surfaceFuture = CallbackToFutureAdapter.getFuture(
                (surfaceResponse) -> {
                    viewfinderImplementation.onSurfaceRequested(surfaceRequest,
                            surfaceResponse);
                    return "requestSurfaceSessionAsync(" + surfaceRequest + ")";
                }
        );

        Logger.d(TAG, "Surface requested by Viewfinder.");


        Display display = getDisplay();
        if (display != null) {
            Size resolution = new Size(surfaceRequest.getWidth(), surfaceRequest.getHeight());
            mViewfinderTransformation.setTransformationInfo(transformationInfo, resolution);
            redrawViewfinder();
        }

        return Futures.transform(surfaceFuture, (refCountedSurface) -> {
            Surface surface = Objects.requireNonNull(refCountedSurface).acquire();
            if (surface != null) {
                return new ViewfinderSurfaceSessionImpl(
                        surface, surfaceRequest, () -> {
                    Objects.requireNonNull(refCountedSurface).release();
                    return Unit.INSTANCE;
                });
            } else {
                throw new CancellationException();
            }
        }, Runnable::run);
    }

    /**
     * Updates the {@link TransformationInfo} used by the current surface session.
     *
     * <p>This is commonly used to update the crop rect of the displayed frames.
     *
     * <p>Setting this value will replace any {@link TransformationInfo} previously set by
     * {@link #requestSurfaceSessionAsync(ViewfinderSurfaceRequest, TransformationInfo)} or
     * previous invocations of this method.
     *
     * <p>This should only be called after
     * {@link #requestSurfaceSessionAsync(ViewfinderSurfaceRequest)} or any of its overloads has
     * been called, as calling those methods will overwrite the transformation info set here.
     *
     * @param transformationInfo the updated transformation info.
     */
    @UiThread
    public void setTransformationInfo(@NonNull TransformationInfo transformationInfo) {
        mViewfinderTransformation.updateTransformInfo(transformationInfo);
        redrawViewfinder();
    }

    /**
     * Returns the {@link TransformationInfo} currently applied to the viewfinder.
     *
     * @return the previously set transformation info, or {@code null} if none has been set by
     * {@link #requestSurfaceSessionAsync(ViewfinderSurfaceRequest)} or its overloads.
     */
    @UiThread
    @Nullable
    public TransformationInfo getTransformationInfo() {
        return mViewfinderTransformation.getTransformationInfo();
    }

    /**
     * Returns a {@link Bitmap} representation of the content displayed on the
     * {@link CameraViewfinder}, or {@code null} if the camera viewfinder hasn't started yet.
     * <p>
     * The returned {@link Bitmap} uses the {@link Bitmap.Config#ARGB_8888} pixel format and its
     * dimensions are the same as this view's.
     * <p>
     * <strong>Do not</strong> invoke this method from a drawing method
     * ({@link View#onDraw(Canvas)} for instance).
     * <p>
     * If an error occurs during the copy, an empty {@link Bitmap} will be returned.
     *
     * @return A {@link Bitmap.Config#ARGB_8888} {@link Bitmap} representing the content
     * displayed on the {@link CameraViewfinder}, or null if the camera viewfinder hasn't started
     * yet.
     */
    @UiThread
    public @Nullable Bitmap getBitmap() {
        checkUiThread();
        return mImplementation == null ? null : mImplementation.getBitmap();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        addOnLayoutChangeListener(mOnLayoutChangeListener);
        startListeningToDisplayChange();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnLayoutChangeListener(mOnLayoutChangeListener);
        stopListeningToDisplayChange();
    }

    @VisibleForTesting
    static boolean shouldUseTextureView(
            final @NonNull ImplementationMode implementationMode
    ) {
        boolean hasSurfaceViewQuirk = DeviceQuirks.get(SurfaceViewStretchedQuirk.class) != null
                ||  DeviceQuirks.get(SurfaceViewNotCroppedByParentQuirk.class) != null;
        if (Build.VERSION.SDK_INT <= 24 || hasSurfaceViewQuirk) {
            // Force to use TextureView when the device is running android 7.0 and below, legacy
            // level or SurfaceView has quirks.
            Logger.d(TAG, "Implementation mode to set is not supported, forcing to use "
                    + "TextureView, because transform APIs are not supported on these devices.");
            return true;
        }
        switch (implementationMode) {
            case EMBEDDED:
                return true;
            case EXTERNAL:
                return false;
            default:
                throw new IllegalArgumentException(
                        "Invalid implementation mode: " + implementationMode);
        }
    }

    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    void redrawViewfinder() {
        if (mImplementation != null) {
            mImplementation.redrawViewfinder();
        }
    }

    /**
     * Checks if the current thread is the same UI thread on which the class was constructed.
     *
     * @see <a href="http://go/android-api-guidelines/concurrency#uithread">API Guidelines</a>
     */
    private void checkUiThread() {
        // Ignore mRequiredLooper == null because this can be called from the super
        // class constructor before the class's own constructor has run.
        if (Looper.myLooper() != mRequiredLooper) {
            Throwable throwable = new Throwable(
                    "A method was called on thread '" + Thread.currentThread().getName()
                            + "'. All methods must be called on the same thread. (Expected Looper "
                            + mRequiredLooper + ", but called on " + Looper.myLooper() + ".");
            throw new RuntimeException(throwable);
        }
    }

    private void startListeningToDisplayChange() {
        DisplayManager displayManager = getDisplayManager();
        if (displayManager == null) {
            return;
        }
        displayManager.registerDisplayListener(mDisplayRotationListener,
                new Handler(Looper.getMainLooper()));
    }

    private void stopListeningToDisplayChange() {
        DisplayManager displayManager = getDisplayManager();
        if (displayManager == null) {
            return;
        }
        displayManager.unregisterDisplayListener(mDisplayRotationListener);
    }

    private @Nullable DisplayManager getDisplayManager() {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        return (DisplayManager) context.getApplicationContext()
                .getSystemService(Context.DISPLAY_SERVICE);
    }
    /**
     * Listener for display rotation changes.
     *
     * <p> When the device is rotated 180° from side to side, the activity is not
     * destroyed and recreated. This class is necessary to make sure preview's target rotation
     * gets updated when that happens.
     */
    // Synthetic access
    @SuppressWarnings("WeakerAccess")
    class DisplayRotationListener implements DisplayManager.DisplayListener {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            Display display = getDisplay();
            if (display != null && display.getDisplayId() == displayId) {
                redrawViewfinder();
            }
        }
    }
}
