/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.xr.extensions;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.asset.EnvironmentToken;
import androidx.xr.extensions.asset.GltfModelToken;
import androidx.xr.extensions.asset.SceneToken;
import androidx.xr.extensions.media.XrSpatialAudioExtensions;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTransaction;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.ReformOptions;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.extensions.space.ActivityPanel;
import androidx.xr.extensions.space.ActivityPanelLaunchParameters;
import androidx.xr.extensions.space.HitTestResult;
import androidx.xr.extensions.space.SpatialCapabilities;
import androidx.xr.extensions.space.SpatialState;
import androidx.xr.extensions.space.SpatialStateEvent;
import androidx.xr.extensions.splitengine.SplitEngineBridge;
import androidx.xr.extensions.subspace.Subspace;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * The main extensions class that creates or provides instances of various XR Extensions components.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface XrExtensions {
    String IMAGE_TOO_OLD =
            "This device's system image doesn't include the necessary "
                    + "implementation for this API. Please update to the latest system image. "
                    + "This API requires a corresponding implementation on the device to function "
                    + "correctly.";

    /** Get the current version of the {@link XrExtensions} API. */
    int getApiVersion();

    /**
     * Synchronously creates a node that can host a 2D panel or 3D subspace.
     *
     * @return A {@link Node}.
     */
    @NonNull
    Node createNode();

    /**
     * Synchronously creates a new transaction that can be used to update multiple {@link Node}'s
     * data and transformation in the 3D space.
     *
     * @return A {@link NodeTransaction} that can be used to queue the updates and submit to backend
     *     at once.
     */
    @NonNull
    NodeTransaction createNodeTransaction();

    /**
     * Synchronously creates a subspace.
     *
     * @param splitEngineBridge The splitEngineBridge.
     * @param subspaceId The unique identifier of the subspace.
     * @return A {@link Subspace} that can be used to render 3D content in.
     */
    @NonNull
    Subspace createSubspace(@NonNull SplitEngineBridge splitEngineBridge, int subspaceId);

    /**
     * Loads and caches the glTF model in the SpaceFlinger.
     *
     * @param asset The input stream data of the glTF model.
     * @param regionSizeBytes The size of the memory region where the model is stored (in bytes).
     * @param regionOffsetBytes The offset from the beginning of the memory region (in bytes).
     * @param url The URL of the asset to be loaded. This string is only used for caching purposes.
     * @return A {@link CompletableFuture} that either contains the {@link GltfModelToken}
     *     representing the loaded model or 'null' if the asset could not be loaded successfully.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    @NonNull
    CompletableFuture</* @Nullable */ GltfModelToken> loadGltfModel(
            InputStream asset, int regionSizeBytes, int regionOffsetBytes, String url);

    /**
     * Loads and caches the environment in the SpaceFlinger.
     *
     * @param asset The input stream data of the EXR or JPEG environment.
     * @param regionSizeBytes The size of the memory region where the environment is stored (in
     *     bytes).
     * @param regionOffsetBytes The offset from the beginning of the memory region (in bytes).
     * @param url The URL of the asset to be loaded. This string is only used for caching purposes.
     * @return A {@link CompletableFuture} that either contains the {@link EnvironmentToken}
     *     representing the loaded environment or 'null' if the asset could not be loaded
     *     successfully.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    @NonNull
    CompletableFuture</* @Nullable */ EnvironmentToken> loadEnvironment(
            InputStream asset, int regionSizeBytes, int regionOffsetBytes, String url);

    /**
     * Loads and caches the environment in the SpaceFlinger.
     *
     * @param asset The input stream data of the EXR or JPEG environment.
     * @param regionSizeBytes The size of the memory region where the environment is stored (in
     *     bytes).
     * @param regionOffsetBytes The offset from the beginning of the memory region (in bytes).
     * @param url The URL of the asset to be loaded.
     * @param textureWidth The target width of the final texture which will be downsampled/upsampled
     *     from the original image.
     * @param textureHeight The target height of the final texture which will be
     *     downsampled/upsampled from the original image.
     * @return A {@link CompletableFuture} that either contains the {@link EnvironmentToken}
     *     representing the loaded environment or 'null' if the asset could not be loaded
     *     successfully.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    @NonNull
    CompletableFuture</* @Nullable */ EnvironmentToken> loadEnvironment(
            InputStream asset,
            int regionSizeBytes,
            int regionOffsetBytes,
            String url,
            int textureWidth,
            int textureHeight);

    /**
     * Loads and caches the Impress scene in the SpaceFlinger.
     *
     * @param asset The input stream data of the textproto Impress scene.
     * @param regionSizeBytes The size of the memory region where the Impress scene is stored (in
     *     bytes).
     * @param regionOffsetBytes The offset from the beginning of the memory region (in bytes).
     * @return A {@link CompletableFuture} that either contains the {@link SceneToken} representing
     *     the loaded Impress scene or 'null' if the asset could not be loaded successfully.
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    @NonNull
    CompletableFuture</* @Nullable */ SceneToken> loadImpressScene(
            InputStream asset, int regionSizeBytes, int regionOffsetBytes);

    /**
     * Synchronously returns a {@link SplitEngineBridge}.
     *
     * @return A {@link SplitEngineBridge}.
     */
    @NonNull
    SplitEngineBridge createSplitEngineBridge();

    /**
     * Synchronously returns the implementation of the {@link XrSpatialAudioExtensions} component.
     *
     * @return The {@link XrSpatialAudioExtensions}.
     */
    @NonNull
    XrSpatialAudioExtensions getXrSpatialAudioExtensions();

    /**
     * Attaches the given {@code sceneNode} as the presentation for the given {@code activity} in
     * the space, and asks the system to attach the 2D content of the {@code activity} into the
     * given {@code windowNode}.
     *
     * <p>The {@code sceneNode} will only be visible if the {@code activity} is visible as in a
     * lifecycle state between {@link Activity#onStart()} and {@link Activity#onStop()} and is
     * SPATIAL_UI_CAPABLE too.
     *
     * <p>One activity can only attach one scene node. When a new scene node is attached for the
     * same {@code activity}, the previous one will be detached.
     *
     * @param activity the owner activity of the {@code sceneNode}.
     * @param sceneNode the node to show as the presentation of the {@code activity}.
     * @param windowNode a leash node to allow the app to control the position and size of the
     *     activity's main window.
     * @param callback the callback that will be called with the result. XrExtensionResult will
     *     indicate either of the following: XrExtensionResult.XR_RESULT_SUCCESS: The request has
     *     been accepted, and the client can expect that a spatial state callback with an updated
     *     SpatialState will run shortly. XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE: The
     *     request has been accepted, but will not immediately change the spatial state. A spatial
     *     state callback with an updated SpatialState won't run until the activity becomes
     *     SPATIAL_UI_CAPABLE. XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED: The request has
     *     been ignored because the activity is already in the requested state.
     *     XrExtensionResult.XR_RESULT_ERROR_NOT_ALLOWED: The request has been rejected because the
     *     activity does not have the required capability (e.g. called by an embedded guest
     *     activity.) XrExtensionResult.XR_RESULT_ERROR_SYSTEM: A unrecoverable service side error
     *     has happened.
     * @param executor the executor the callback will be called on.
     */
    void attachSpatialScene(
            @NonNull Activity activity,
            @NonNull Node sceneNode,
            @NonNull Node windowNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor);

    /**
     * Detaches the {@code sceneNode} that was previously attached for the {@code activity} via
     * {@link #attachSpatialScene}.
     *
     * <p>When an {@link Activity} is destroyed, it must call this method to detach the scene node
     * that was attached for itself.
     *
     * @param activity the owner activity of the {@code sceneNode}.
     * @param callback the callback that will be called with the result. XrExtensionResult will
     *     indicate either of the following: XrExtensionResult.XR_RESULT_SUCCESS: The request has
     *     been accepted, and the client can expect that a spatial state callback with an updated
     *     SpatialState will run shortly. XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE: The
     *     request has been accepted, but will not immediately change the spatial state. A spatial
     *     state callback with an updated SpatialState won't run until the activity becomes
     *     SPATIAL_UI_CAPABLE. XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED: The request has
     *     been ignored because the activity is already in the requested state.
     *     XrExtensionResult.XR_RESULT_ERROR_SYSTEM: A unrecoverable service side error has
     *     happened.
     * @param executor the executor the callback will be called on.
     */
    void detachSpatialScene(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor);

    /**
     * Resizes the main window of the given activity to the requested size.
     *
     * @param activity the activity whose main window should be resized.
     * @param width the new main window width in pixels.
     * @param height the new main window height in pixels.
     * @param callback the callback that will be called with the result. XrExtensionResult will
     *     indicate either of the following: XrExtensionResult.XR_RESULT_SUCCESS: The request has
     *     been accepted, and the client can expect that a spatial state callback with an updated
     *     SpatialState will run shortly. XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE: The
     *     request has been accepted, but will not immediately change the spatial state. A spatial
     *     state callback with an updated SpatialState won't run until the activity becomes
     *     SPATIAL_UI_CAPABLE. XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED: The request has
     *     been ignored because the activity is already in the requested state.
     *     XrExtensionResult.XR_RESULT_ERROR_NOT_ALLOWED: The request has been rejected because the
     *     activity does not have the required capability (e.g. called by an embedded guest
     *     activity.) XrExtensionResult.XR_RESULT_ERROR_SYSTEM: A unrecoverable service side error
     *     has happened.
     * @param executor the executor the callback will be called on.
     */
    void setMainWindowSize(
            @NonNull Activity activity,
            int width,
            int height,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor);

    /**
     * Sets the main window of the given activity to the curvature radius. Note that it's allowed
     * only for the activity in full space mode.
     *
     * @param activity the activity of the main window to which the curvature should be applied.
     * @param curvatureRadius the panel curvature radius. It is measured in "radius * 1 /
     *     curvature". A value of 0.0f means that the panel will be flat.
     * @deprecated Use Split Engine to create a curved panel.
     */
    @Deprecated
    void setMainWindowCurvatureRadius(@NonNull Activity activity, float curvatureRadius);

    /**
     * Attaches an environment node for a given activity to make it visible.
     *
     * <p>SysUI will attach the environment node to the task node when the activity gains the
     * APP_ENVIRONMENTS_CAPABLE capability.
     *
     * <p>This method can be called multiple times, SysUI will attach the new environment node and
     * detach the old environment node if it exists.
     *
     * <p>Note that once an environmentNode is attached and the caller gains
     * APP_ENVIRONMENTS_CAPABLE capability, spatial callback's environment visibility status changes
     * to APP_VISIBLE even if your application hasn't set a skybox or geometry to the environment
     * node yet. For that reason, call this API only when your application wants to show a skybox or
     * geometry. Otherwise, the APP_VISIBLE spatial state may lead to an unexpected behavior. For
     * example, home environment's ambient audio (if any) may stop even if the user can still see
     * the home environment.
     *
     * @param activity the activity that provides the environment node to attach.
     * @param environmentNode the environment node provided by the activity to be attached.
     * @param callback the callback that will be called with the result. XrExtensionResult will
     *     indicate either of the following: XrExtensionResult.XR_RESULT_SUCCESS: The request has
     *     been accepted, and the client can expect that a spatial state callback with an updated
     *     SpatialState will run shortly. XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE: The
     *     request has been accepted, but will not immediately change the spatial state. A spatial
     *     state callback with an updated SpatialState won't run until the activity becomes
     *     APP_ENVIRONMENTS_CAPABLE. XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED: The
     *     request has been ignored because the activity is already in the requested state.
     *     XrExtensionResult.XR_RESULT_ERROR_NOT_ALLOWED: The request has been rejected because the
     *     activity does not have the required capability (e.g. called by an embedded guest
     *     activity.) XrExtensionResult.XR_RESULT_ERROR_SYSTEM: A unrecoverable service side error
     *     has happened.
     * @param executor the executor the callback will be called on.
     */
    void attachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Node environmentNode,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor);

    /**
     * Detaches the environment node and its sub tree for a given activity to make it invisible.
     *
     * <p>This method will detach and cleanup the environment node and its subtree passed from the
     * activity.
     *
     * @param activity the activity with which SysUI will detach and clean up the environment node
     *     tree.
     * @param callback the callback that will be called with the result. XrExtensionResult will
     *     indicate either of the following: XrExtensionResult.XR_RESULT_SUCCESS: The request has
     *     been accepted, and the client can expect that a spatial state callback with an updated
     *     SpatialState will run shortly. XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE: The
     *     request has been accepted, but will not immediately change the spatial state. A spatial
     *     state callback with an updated SpatialState won't run until the activity becomes
     *     APP_ENVIRONMENTS_CAPABLE. XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED: The
     *     request has been ignored because the activity is already in the requested state.
     *     XrExtensionResult.XR_RESULT_ERROR_SYSTEM: A unrecoverable service side error has
     *     happened.
     * @param executor the executor the callback will be called on.
     */
    void detachSpatialEnvironment(
            @NonNull Activity activity,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor);

    /**
     * Synchronously registers a callback to receive {@link SpatialState} for the {@code activity}.
     *
     * <p>One activity can only set one callback. When a new callback is set for the same {@code
     * activity}, the previous one will be cleared.
     *
     * <p>The {@code executor}'s execute() method will soon be called to run the callback with the
     * current state when it is available, but it never happens directly from within this call.
     *
     * <p>This API throws IllegalArgumentException if it is called by an embedded (guest) activity.
     *
     * @param activity the activity for the {@code callback} to listen to.
     * @param callback the callback to set.
     * @param executor the executor that the callback will be called on.
     * @see #clearSpatialStateCallback
     */
    void registerSpatialStateCallback(
            @NonNull Activity activity,
            @NonNull Consumer<SpatialState> callback,
            @NonNull Executor executor);

    /**
     * Synchronously clears the {@link SpatialStateEvent} callback that was previously set to the
     * {@code activity} via {@link #setSpatialStateCallback}.
     *
     * <p>When an {@link Activity} is destroyed, it must call this method to clear the callback that
     * was set for itself.
     *
     * @param activity the activity for the {@code callback} to listen to.
     */
    void clearSpatialStateCallback(@NonNull Activity activity);

    /**
     * Synchronously creates an {@link ActivityPanel} to be embedded inside the given {@code host}
     * activity.
     *
     * <p>Caller must make sure the {@code host} can embed {@link ActivityPanel}. See {@link
     * getSpatialState()}. When embedding is possible, SpatialState's {@link SpatialCapabilities}
     * has {@code SPATIAL_ACTIVITY_EMBEDDING_CAPABLE}.
     *
     * <p>For the {@link ActivityPanel} to be shown in the scene, caller needs to attach the {@link
     * ActivityPanel#getNode()} to the scene node attached through {@link #attachSpatialScene}.
     *
     * <p>This API throws IllegalArgumentException if it is called by an embedded (guest) activity.
     *
     * @param host the host activity to embed the {@link ActivityPanel}.
     * @param launchParameters the parameters to define the initial state of the {@link
     *     ActivityPanel}.
     * @return the {@link ActivityPanel} created.
     * @throws IllegalStateException if the {@code host} is not allowed to embed {@link
     *     ActivityPanel}.
     */
    @NonNull
    ActivityPanel createActivityPanel(
            @NonNull Activity host, @NonNull ActivityPanelLaunchParameters launchParameters);

    /**
     * Requests to put an activity in a different mode when it has focus.
     *
     * @param activity the activity that requires to enter full space mode.
     * @param requestEnter when true, activity is put in full space mode. Home space mode otherwise.
     * @param callback the callback that will be called with the result. XrExtensionResult will
     *     indicate either of the following: XrExtensionResult.XR_RESULT_SUCCESS: The request has
     *     been accepted, and the client can expect that a spatial state callback with an updated
     *     SpatialState will run shortly. XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED: The
     *     request has been ignored because the activity is already in the requested mode.
     *     XrExtensionResult.XR_RESULT_ERROR_NOT_ALLOWED: The request has been rejected because the
     *     activity does not have the required capability (e.g. not the top activity in a top task
     *     in the desktop, called by an embedded guest activity.)
     *     XrExtensionResult.XR_RESULT_ERROR_SYSTEM: A unrecoverable service side error has
     *     happened.
     * @param executor the executor the callback will be called on.
     */
    void requestFullSpaceMode(
            @NonNull Activity activity,
            boolean requestEnter,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor);

    /**
     * Synchronously sets the full space mode flag to the given {@link Bundle}.
     *
     * <p>The {@link Bundle} then could be used to launch an {@link Activity} with requesting to
     * enter full space mode through {@link Activity#startActivity}. If there's a bundle used for
     * customizing how the {@link Activity} should be started by {@link ActivityOptions.toBundle} or
     * {@link androidx.core.app.ActivityOptionsCompat.toBundle}, it's suggested to use the bundle to
     * call this method.
     *
     * <p>The flag will be ignored when no {@link Intent.FLAG_ACTIVITY_NEW_TASK} is set in the
     * bundle, or it is not started from a focused Activity context.
     *
     * <p>This flag is also ignored when the {@link android.window.PROPERTY_XR_ACTIVITY_START_MODE}
     * property is set to a value other than XR_ACTIVITY_START_MODE_UNDEFINED in the
     * AndroidManifest.xml file for the activity being launched.
     *
     * @param bundle the input bundle to set with the full space mode flag.
     * @return the input {@code bundle} with the full space mode flag set.
     */
    @NonNull
    Bundle setFullSpaceMode(@NonNull Bundle bundle);

    /**
     * Synchronously sets the inherit full space mode environvment flag to the given {@link Bundle}.
     *
     * <p>The {@link Bundle} then could be used to launch an {@link Activity} with requesting to
     * enter full space mode while inherit the existing environment through {@link
     * Activity#startActivity}. If there's a bundle used for customizing how the {@link Activity}
     * should be started by {@link ActivityOptions.toBundle} or {@link
     * androidx.core.app.ActivityOptionsCompat.toBundle}, it's suggested to use the bundle to call
     * this method.
     *
     * <p>When launched, the activity will be in full space mode and also inherits the environment
     * from the launching activity. If the inherited environment needs to be animated, the launching
     * activity has to continue updating the environment even after the activity is put into the
     * stopped state.
     *
     * <p>The flag will be ignored when no {@link Intent.FLAG_ACTIVITY_NEW_TASK} is set in the
     * intent, or it is not started from a focused Activity context.
     *
     * <p>The flag will also be ignored when there is no environment to inherit or the activity has
     * its own environment set already.
     *
     * <p>This flag is ignored too when the {@link android.window.PROPERTY_XR_ACTIVITY_START_MODE}
     * property is set to a value other than XR_ACTIVITY_START_MODE_UNDEFINED in the
     * AndroidManifest.xml file for the activity being launched.
     *
     * <p>For security reasons, Z testing for the new activity is disabled, and the activity is
     * always drawn on top of the inherited environment. Because Z testing is disabled, the activity
     * should not spatialize itself, and should not curve its panel too much either.
     *
     * @param bundle the input bundle to set with the inherit full space mode environment flag.
     * @return the input {@code bundle} with the inherit full space mode flag set.
     */
    @NonNull
    Bundle setFullSpaceModeWithEnvironmentInherited(@NonNull Bundle bundle);

    /**
     * Synchronously returns system config information.
     *
     * @return A {@link Config} object.
     */
    @NonNull
    Config getConfig();

    /**
     * Hit-tests a ray against the virtual scene. If the ray hits an object in the scene,
     * information about the hit will be passed to the callback. If nothing is hit, the hit distance
     * will be infinite. Note that attachSpatialScene() must be called before calling this method.
     * Otherwise, an IllegalArgumentException is thrown.
     *
     * @param activity the requesting activity.
     * @param origin the origin of the ray to test, in the activity's task coordinates.
     * @param direction the direction of the ray to test, in the activity's task coordinates.
     * @param callback the callback that will be called with the hit test result.
     * @param executor the executor the callback will be called on.
     */
    void hitTest(
            @NonNull Activity activity,
            @NonNull Vec3 origin,
            @NonNull Vec3 direction,
            @NonNull Consumer<HitTestResult> callback,
            @NonNull Executor executor);

    /**
     * Synchronously returns the OpenXR reference space type.
     *
     * @return the OpenXR reference space type used as world space for the shared scene.
     */
    int getOpenXrWorldSpaceType();

    /**
     * Synchronously creates a new ReformOptions instance.
     *
     * @param callback the callback that will be called with reform events.
     * @param executor the executor the callback will be called on.
     * @return the new builder instance.
     */
    @NonNull
    ReformOptions createReformOptions(
            @NonNull Consumer<ReformEvent> callback, @NonNull Executor executor);

    /**
     * Synchronously makes a View findable via findViewById().
     *
     * <p>This is done without it being a child of the given group.
     *
     * @param view the view to add as findable.
     * @param group a group that is part of the hierarchy that findViewById() will be called on.
     */
    void addFindableView(@NonNull View view, @NonNull ViewGroup group);

    /**
     * Synchronously removes a findable view from the given group.
     *
     * @param view the view to remove as findable.
     * @param group the group to remove the findable view from.
     */
    void removeFindableView(@NonNull View view, @NonNull ViewGroup group);

    /**
     * Returns the surface tracking node for a view, if there is one.
     *
     * <p>The surface tracking node is centered on the Surface that the view is attached to, and is
     * sized to match the surface's size. Note that the view's position in the surface can be
     * retrieved via View.getLocationInSurface().
     *
     * @param view the view.
     * @return the surface tracking node, or null if no such node exists.
     */
    @Nullable
    Node getSurfaceTrackingNode(@NonNull View view);

    /**
     * Sets a preferred main panel aspect ratio for an activity that is not SPATIAL_UI_CAPABLE.
     *
     * <p>The ratio is only applied to the activity. If the activity launches another activity in
     * the same task, the ratio is not applied to the new activity. Also, while the activity is
     * SPATIAL_UI_CAPABLE, the preference is temporarily removed. While the activity is
     * SPATIAL_UI_CAPABLE, use ReformOptions API instead.
     *
     * @param activity the activity to set the preference.
     * @param preferredRatio the aspect ratio determined by taking the panel's width over its
     *     height. A value <= 0.0f means there are no preferences.
     * @param callback the callback that will be called with the result. XrExtensionResult will
     *     indicate either of the following: XrExtensionResult.XR_RESULT_SUCCESS: The request has
     *     been accepted, and the client can expect that a spatial state callback with an updated
     *     SpatialState will run shortly. XrExtensionResult.XR_RESULT_SUCCESS_NOT_VISIBLE: The
     *     request has been accepted, but will not immediately change the spatial state. A spatial
     *     state callback with an updated SpatialState won't run until the activity loses the
     *     SPATIAL_UI_CAPABLE capability. XrExtensionResult.XR_RESULT_IGNORED_ALREADY_APPLIED: The
     *     request has been ignored because the activity is already in the requested state.
     *     XrExtensionResult.XR_RESULT_ERROR_NOT_ALLOWED: The request has been rejected because the
     *     activity does not have the required capability (e.g. called by an embedded guest
     *     activity.) XrExtensionResult.XR_RESULT_ERROR_SYSTEM: A unrecoverable service side error
     *     has happened.
     * @param executor the executor the callback will be called on.
     */
    void setPreferredAspectRatio(
            @NonNull Activity activity,
            float preferredRatio,
            @NonNull Consumer<XrExtensionResult> callback,
            @NonNull Executor executor);

    /**
     * Synchronously gets the spatial state of the activity.
     *
     * <p>Do not call the API from the Binder thread. That may cause a deadlock.
     *
     * <p>This API throws IllegalArgumentException if it is called by an embedded (guest) activity,
     * and also throws RuntimeException if the calling thread is interrupted.
     *
     * @param activity the activity to get the capabilities.
     * @return the state of the activity.
     */
    @NonNull
    SpatialState getSpatialState(@NonNull Activity activity);

    /**
     * The result of a displayGltfModel request.
     *
     * @deprecated JXR Core doesn't need this anymore as it does the same with Split Engine.
     */
    @Deprecated
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    class SceneViewerResult {}
}
