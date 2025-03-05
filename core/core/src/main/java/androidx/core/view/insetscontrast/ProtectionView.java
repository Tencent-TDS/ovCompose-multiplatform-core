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

package androidx.core.view.insetscontrast;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * A view which draws {@link ContrastProtection}s contained in the specified list.
 */
public class ProtectionView extends FrameLayout {

    private final ProtectionGroup mGroup;
    private boolean mDisposed;

    /**
     * Constructs a view which draws protections contained in the specified list.
     *
     * @param context the Context the view is running in, through which it can access the current
     *                theme, resources, etc.
     * @param monitor the SystemBarStateMonitor which monitors and provides necessary information
     *                about system bars that need to be protected.
     * @param protections a list of protections associated with a local area.
     */
    public ProtectionView(@NonNull Context context, @NonNull SystemBarStateMonitor monitor,
            @NonNull List<ContrastProtection> protections) {
        super(context);
        mGroup = new ProtectionGroup(monitor, protections);
        for (int i = 0, size = mGroup.size(); i < size; i++) {
            final ContrastProtection protection = mGroup.getProtection(i);
            addView(context, i, protection);
        }
        setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    private void addView(Context context, int index, ContrastProtection protection) {
        final int width;
        final int height;
        final int gravity;
        final ContrastProtection.Attributes attrs = protection.getAttributes();
        switch (protection.getSide()) {
            case WindowInsetsCompat.Side.LEFT:
                width = attrs.getWidth();
                height = MATCH_PARENT;
                gravity = Gravity.LEFT;
                break;
            case WindowInsetsCompat.Side.TOP:
                width = MATCH_PARENT;
                height = attrs.getHeight();
                gravity = Gravity.TOP;
                break;
            case WindowInsetsCompat.Side.RIGHT:
                width = attrs.getWidth();
                height = MATCH_PARENT;
                gravity = Gravity.RIGHT;
                break;
            case WindowInsetsCompat.Side.BOTTOM:
                width = MATCH_PARENT;
                height = attrs.getHeight();
                gravity = Gravity.BOTTOM;
                break;
            default:
                throw new IllegalArgumentException("Unexpected side: " + protection.getSide());
        }

        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                width, height, gravity);
        final Insets margin = attrs.getMargin();
        params.leftMargin = margin.left;
        params.topMargin = margin.top;
        params.rightMargin = margin.right;
        params.bottomMargin = margin.bottom;
        final View view = new View(context);
        view.setTranslationX(attrs.getTranslationX());
        view.setTranslationY(attrs.getTranslationY());
        view.setAlpha(attrs.getAlpha());
        view.setVisibility(attrs.isVisible() ? View.VISIBLE : View.INVISIBLE);
        view.setBackground(attrs.getDrawable());
        final ContrastProtection.Attributes.Callback callback =
                new ContrastProtection.Attributes.Callback() {

                    @Override
                    public void onWidthChanged(int width) {
                        params.width = width;
                        view.setLayoutParams(params);
                    }

                    @Override
                    public void onHeightChanged(int height) {
                        params.height = height;
                        view.setLayoutParams(params);
                    }

                    @Override
                    public void onMarginChanged(@NonNull Insets margin) {
                        params.leftMargin = margin.left;
                        params.topMargin = margin.top;
                        params.rightMargin = margin.right;
                        params.bottomMargin = margin.bottom;
                        view.setLayoutParams(params);
                    }

                    @Override
                    public void onVisibilityChanged(boolean visible) {
                        view.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
                    }

                    @Override
                    public void onDrawableChanged(@NonNull Drawable drawable) {
                        view.setBackground(drawable);
                    }

                    @Override
                    public void onTranslationXChanged(float translationX) {
                        view.setTranslationX(translationX);
                    }

                    @Override
                    public void onTranslationYChanged(float translationY) {
                        view.setTranslationY(translationY);
                    }

                    @Override
                    public void onAlphaChanged(float alpha) {
                        view.setAlpha(alpha);
                    }
                };
        attrs.setCallback(callback);
        addView(view, index, params);
    }

    /**
     * Disconnects from the given {@link SystemBarStateMonitor} and {@link ContrastProtection}s.
     */
    public void dispose() {
        if (mDisposed) {
            return;
        }
        mDisposed = true;
        removeAllViews();
        for (int i = 0, size = mGroup.size(); i < size; i++) {
            mGroup.getProtection(i).getAttributes().setCallback(null);
        }
        mGroup.dispose();
    }
}
