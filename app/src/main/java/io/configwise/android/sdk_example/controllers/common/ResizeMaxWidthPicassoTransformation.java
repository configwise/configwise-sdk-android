package io.configwise.android.sdk_example.controllers.common;

import android.graphics.Bitmap;

import com.squareup.picasso.Transformation;

public class ResizeMaxWidthPicassoTransformation implements Transformation {

    private int maxWidth;

    public ResizeMaxWidthPicassoTransformation(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        final float factor = (float) source.getWidth() / (float) this.maxWidth;
        if (factor <= 1) {
            return source;
        }

        Bitmap result = Bitmap.createScaledBitmap(
                source,
                (int) (source.getWidth() / factor),
                (int) (source.getHeight() / factor),
                true
        );

        if (result != source) {
            source.recycle();
        }

        return result;
    }

    @Override
    public String key() {
        return "square(" + maxWidth + ")";
    }
}
