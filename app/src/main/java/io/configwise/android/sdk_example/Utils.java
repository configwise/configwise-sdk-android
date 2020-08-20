package io.configwise.android.sdk_example;


import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;


public class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    public static void checkOnMainThread() throws IllegalStateException {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            throw new IllegalStateException("This method must be executed from main UI thread");
        }
    }

    public static void runOnUiThread(@NonNull Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public static void fadeIn(View v) {
        if (v != null) {
            Animation animFadeIn = AnimationUtils.loadAnimation(v.getContext(), R.anim.fade_in);
            v.startAnimation(animFadeIn);
        }
    }

    public static void hideSoftwareKeyboard(Activity activity) {
        hideSoftwareKeyboard(activity, null);
    }

    public static void hideSoftwareKeyboard(Context context, View view) {
        if (context != null) {
            if (view == null) {
                if (context instanceof Activity) {
                    view = ((Activity) context).getCurrentFocus();
                }
            }

            if (view != null) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static void showSoftwareKeyboard(Context context) {
        if (context != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    public static int dpToPx(final Context context, final int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int pxToDp(final Context context, final int px) {
        return (int) (px / context.getResources().getDisplayMetrics().density);
    }

    public static int pxFromRes(Context context, @DimenRes int resId) {
        return context.getResources().getDimensionPixelOffset(resId);
    }

    @Nullable
    public static Integer colorResIdToColor(@NonNull Context context, @Nullable Integer colorResId) {
        if (colorResId == null) {
            return null;
        }

        return ContextCompat.getColor(context, colorResId);
    }

    public static void applyColorResIdFilterToView(Context context, View view, Integer solidColorResId) {
        applyColorResIdFilterToView(context, view, solidColorResId, null);
    }

    public static void applyColorResIdFilterToView(@NonNull Context context, @NonNull View view, @Nullable Integer solidColorResId, @Nullable Integer strokeColorResId) {
        applyColorFilterToView(
                context,
                view,
                colorResIdToColor(context, solidColorResId),
                colorResIdToColor(context, strokeColorResId)
        );
    }

    public static void applyColorFilterToView(@NonNull Context context, @NonNull View view, @Nullable Integer solidColor) {
        applyColorFilterToView(context, view, solidColor, null);
    }

    public static void applyColorFilterToView(@NonNull Context context, @NonNull View view, @Nullable Integer solidColor, @Nullable Integer strokeColor) {
        Drawable bd = view.getBackground();
        if (bd instanceof GradientDrawable) {
            GradientDrawable gd = (GradientDrawable) bd;

            if (solidColor != null) {
                gd.setColor(solidColor);
            }

            if (strokeColor != null) {
                gd.setStroke(dpToPx(context, 2), strokeColor);
            }
        }
    }

    public interface SuccessDelegate {
        void onSuccess();
    }

    public static void expandViewVertical(final View v, int duration, int targetHeight, @Nullable SuccessDelegate successDelegate) {
        int prevHeight = v.getHeight();
        v.setVisibility(View.VISIBLE);
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.addUpdateListener(animation -> {
            v.getLayoutParams().height = (int) animation.getAnimatedValue();
            v.requestLayout();
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (successDelegate != null) {
                    successDelegate.onSuccess();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (successDelegate != null) {
                    successDelegate.onSuccess();
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static void collapseViewVertical(final View v, int duration, int targetHeight, @Nullable SuccessDelegate successDelegate) {
        int prevHeight = v.getHeight();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            v.getLayoutParams().height = (int) animation.getAnimatedValue();
            v.requestLayout();
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (successDelegate != null) {
                    successDelegate.onSuccess();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (successDelegate != null) {
                    successDelegate.onSuccess();
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static void expandViewHorizontal(final View v, int duration, int targetWidth, @Nullable SuccessDelegate successDelegate) {
        int prevWidth = v.getWidth();
        v.setVisibility(View.VISIBLE);
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevWidth, targetWidth);
        valueAnimator.addUpdateListener(animation -> {
            v.getLayoutParams().width = (int) animation.getAnimatedValue();
            v.requestLayout();
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (successDelegate != null) {
                    successDelegate.onSuccess();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (successDelegate != null) {
                    successDelegate.onSuccess();
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static void collapseViewHorizontal(final View v, int duration, int targetWidth, @Nullable SuccessDelegate successDelegate) {
        int prevWidth = v.getWidth();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevWidth, targetWidth);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            v.getLayoutParams().width = (int) animation.getAnimatedValue();
            v.requestLayout();
        });
        valueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (successDelegate != null) {
                    successDelegate.onSuccess();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                if (successDelegate != null) {
                    successDelegate.onSuccess();
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }


    public static boolean isDebug() {
        return "debug".equalsIgnoreCase(BuildConfig.BUILD_TYPE);
    }

    public static boolean isRelease() {
        return "release".equalsIgnoreCase(BuildConfig.BUILD_TYPE);
    }

    public static Bitmap addWaterMark(@NonNull Context context, @NonNull Bitmap bitmap, @NonNull Bitmap watermark) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        Bitmap result = Bitmap.createBitmap(bitmapWidth, bitmapHeight, bitmap.getConfig());

        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bitmap, 0, 0, null);

        int watermarkWidth = watermark.getWidth();
        int watermarkHeight = watermark.getHeight();
        Matrix matrix = new Matrix();
        matrix.setScale(
                (bitmapWidth * 0.3f) / watermarkWidth,
                (bitmapWidth * 0.3f) / watermarkHeight
        );
        matrix.setTranslate(10, bitmapHeight - watermarkHeight - 10);
        canvas.drawBitmap(watermark, matrix, null);
//        canvas.drawBitmap(
//                watermark,
//                10,
//                bitmapHeight - watermarkHeight - 10,
//                null
//        );

        return result;
    }

    @Nullable
    public static Uri stringToUri(@NonNull String url) {
        if (url.trim().isEmpty()) {
            return null;
        }

        try {
            return Uri.parse(url.trim());
        } catch (Exception e) {
            Log.e(TAG, "Unable to convert string to uri due error", e);
            return null;
        }
    }
}
