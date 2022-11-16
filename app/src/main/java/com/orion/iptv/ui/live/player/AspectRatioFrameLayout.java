/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.orion.iptv.ui.live.player;

import static java.lang.annotation.ElementType.TYPE_USE;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.orion.iptv.R;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

/**
 * A {@link FrameLayout} that resizes itself to match a specified aspect ratio.
 */
public final class AspectRatioFrameLayout extends FrameLayout {
    /**
     * Either the width or height is decreased to obtain the desired aspect ratio.
     */
    public static final int RESIZE_MODE_FIT = 0;
    /**
     * The width is fixed and the height is increased or decreased to obtain the desired aspect ratio.
     */
    public static final int RESIZE_MODE_FIXED_WIDTH = 1;
    /**
     * The height is fixed and the width is increased or decreased to obtain the desired aspect ratio.
     */
    public static final int RESIZE_MODE_FIXED_HEIGHT = 2;
    /**
     * The specified aspect ratio is ignored.
     */
    public static final int RESIZE_MODE_FILL = 3;
    /**
     * Either the width or height is increased to obtain the desired aspect ratio.
     */
    public static final int RESIZE_MODE_ZOOM = 4;
    private static final String TAG = "AspectRatioFrameLayout";
    /**
     * The {@link FrameLayout} will not resize itself if the fractional difference between its natural
     * aspect ratio and the requested aspect ratio falls below this threshold.
     *
     * <p>This tolerance allows the view to occupy the whole of the screen when the requested aspect
     * ratio is very close, but not exactly equal to, the aspect ratio of the screen. This may reduce
     * the number of view layers that need to be composited by the underlying system, which can help
     * to reduce power consumption.
     */
    private final AspectRatioUpdateDispatcher aspectRatioUpdateDispatcher;
    private final int displayWidth;
    private final int displayHeight;
    private final float displayAspectRatio;
    @Nullable
    private AspectRatioListener aspectRatioListener;
    @ResizeMode
    private int resizeMode;
    private float videoAspectRatio;
    private int videoWidth;
    private int videoHeight;
    public AspectRatioFrameLayout(Context context) {
        this(context, /* attrs= */ null);
    }
    public AspectRatioFrameLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        resizeMode = RESIZE_MODE_FIT;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout, 0, 0);
            try {
                resizeMode = a.getInt(R.styleable.AspectRatioFrameLayout_resize_mode, RESIZE_MODE_FIT);
            } finally {
                a.recycle();
            }
        }
        aspectRatioUpdateDispatcher = new AspectRatioUpdateDispatcher();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        displayWidth = metrics.widthPixels;
        displayHeight = metrics.heightPixels;
        displayAspectRatio = (float) displayWidth / displayHeight;
        Log.i(TAG, String.format(Locale.getDefault(), "display size: %dx%d %.2f", displayWidth, displayHeight, displayAspectRatio));
    }

    /**
     * Sets the aspect ratio that this view should satisfy.
     *
     * @param widthHeightRatio The width to height ratio.
     */
    public void setAspectRatio(int width, int height, float widthHeightRatio) {
        if (this.videoAspectRatio != widthHeightRatio || width != this.videoWidth || height != this.videoHeight) {
            this.videoAspectRatio = widthHeightRatio;
            this.videoWidth = width;
            this.videoHeight = height;
            requestLayout();
        }
    }

    /**
     * Sets the {@link AspectRatioListener}.
     *
     * @param listener The listener to be notified about aspect ratios changes, or null to clear a
     *                 listener that was previously set.
     */
    public void setAspectRatioListener(@Nullable AspectRatioListener listener) {
        this.aspectRatioListener = listener;
    }

    /**
     * Returns the {@link ResizeMode}.
     */
    public @ResizeMode int getResizeMode() {
        return resizeMode;
    }

    /**
     * Sets the {@link ResizeMode}
     *
     * @param resizeMode The {@link ResizeMode}.
     */
    public void setResizeMode(@ResizeMode int resizeMode) {
        if (this.resizeMode != resizeMode) {
            this.resizeMode = resizeMode;
            requestLayout();
        }
    }

    private float fixSize(float calculated, int displaySize, int videoSize) {
        return Math.abs(videoSize - calculated) <= 5.0f && videoSize <= displaySize ? videoSize : calculated;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i(TAG, String.format(Locale.ENGLISH, "measureSpec: %dx%d", widthMeasureSpec, heightMeasureSpec));
        if (videoWidth == 0 || videoHeight == 0 || videoAspectRatio <= 0) {
            int width = videoWidth > 0 ? videoWidth : displayWidth;
            int height = videoHeight > 0 ? videoHeight : displayHeight;
            // Aspect ratio not set. use original video size;
            Log.i(TAG, String.format(Locale.ENGLISH, "finalSize: %dx%d", width, height));
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            return;
        }

        Log.i(TAG, String.format(Locale.ENGLISH, "videoSize: %dx%d %.2f", videoWidth, videoHeight, videoAspectRatio));
        float width = displayWidth;
        float height = displayHeight;
        switch (resizeMode) {
            case RESIZE_MODE_FIXED_WIDTH:
                height = width / videoAspectRatio;
                break;
            case RESIZE_MODE_FIXED_HEIGHT:
                width = height * videoAspectRatio;
                break;
            case RESIZE_MODE_ZOOM:
            case RESIZE_MODE_FIT:
                // fit height first
                width = displayHeight * videoAspectRatio;
                if (width > displayWidth) {
                    // if width is overflow, then fit width
                    width = displayWidth;
                    height = displayWidth / videoAspectRatio;
                    // prevent float calculation error
                    height = (width == videoWidth) ? fixSize(height, displayHeight, videoHeight) : height;
                } else {
                    // prevent float calculation error
                    width = (height == videoHeight) ? fixSize(width, displayWidth, videoWidth) : width;
                }
                break;
            case RESIZE_MODE_FILL:
            default:
                // Ignore target aspect ratio
                break;
        }
        Log.i(TAG, String.format(Locale.ENGLISH, "finalSize: %dx%d", (int) width, (int) height));
        aspectRatioUpdateDispatcher.scheduleUpdate(videoAspectRatio, displayAspectRatio, true);
        super.onMeasure(MeasureSpec.makeMeasureSpec((int) width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) height, MeasureSpec.EXACTLY));
    }

    /**
     * Listener to be notified about changes of the aspect ratios of this view.
     */
    public interface AspectRatioListener {

        /**
         * Called when either the target aspect ratio or the view aspect ratio is updated.
         *
         * @param targetAspectRatio   The aspect ratio that has been set in {@link #setAspectRatio(int, int, float)}
         * @param naturalAspectRatio  The natural aspect ratio of this view (before its width and height
         *                            are modified to satisfy the target aspect ratio).
         * @param aspectRatioMismatch Whether the target and natural aspect ratios differ enough for
         *                            changing the resize mode to have an effect.
         */
        void onAspectRatioUpdated(float targetAspectRatio, float naturalAspectRatio, boolean aspectRatioMismatch);
    }

    /**
     * Resize modes for {@link AspectRatioFrameLayout}. One of {@link #RESIZE_MODE_FIT}, {@link
     * #RESIZE_MODE_FIXED_WIDTH}, {@link #RESIZE_MODE_FIXED_HEIGHT}, {@link #RESIZE_MODE_FILL} or
     * {@link #RESIZE_MODE_ZOOM}.
     */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({
            RESIZE_MODE_FIT,
            RESIZE_MODE_FIXED_WIDTH,
            RESIZE_MODE_FIXED_HEIGHT,
            RESIZE_MODE_FILL,
            RESIZE_MODE_ZOOM
    })
    public @interface ResizeMode {
    }

    /**
     * Dispatches updates to {@link AspectRatioListener}.
     */
    private final class AspectRatioUpdateDispatcher implements Runnable {

        private float targetAspectRatio;
        private float naturalAspectRatio;
        private boolean aspectRatioMismatch;
        private boolean isScheduled;

        public void scheduleUpdate(
                float targetAspectRatio, float naturalAspectRatio, boolean aspectRatioMismatch) {
            this.targetAspectRatio = targetAspectRatio;
            this.naturalAspectRatio = naturalAspectRatio;
            this.aspectRatioMismatch = aspectRatioMismatch;

            if (!isScheduled) {
                isScheduled = true;
                post(this);
            }
        }

        @Override
        public void run() {
            isScheduled = false;
            if (aspectRatioListener == null) {
                return;
            }
            aspectRatioListener.onAspectRatioUpdated(
                    targetAspectRatio, naturalAspectRatio, aspectRatioMismatch);
        }
    }
}
