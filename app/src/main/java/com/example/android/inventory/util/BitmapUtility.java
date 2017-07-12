package com.example.android.inventory.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static android.R.attr.path;

/***
 * Bitmap util class
 */
public class BitmapUtility {

    public static Bitmap getBitmapFromUri(Context context, Uri uri, int viewWidth, int viewHeight) {
        InputStream in;
        ExifInterface exif;

        if (uri==null) { return null; }
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                in = context.getContentResolver().openInputStream(uri);
                exif = new ExifInterface(in);
            } else
            {
                exif = new ExifInterface(uri.getPath());
            }

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            int angle = 0;

            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                angle = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                angle = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                angle = 270;
            }

            Matrix mat = new Matrix();
            mat.postRotate(angle);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(
                    context.getContentResolver().openInputStream(uri), null, options);

            int height = options.outHeight;
            int width = options.outWidth;
            int scaleFactor = 1;

            if (height > viewHeight || width > viewWidth) {
                if (width > height) {
                    scaleFactor = Math.round((float)height / (float)viewHeight);
                } else {
                    scaleFactor = Math.round((float)width / (float)viewWidth);
                }
            }

            options.inSampleSize = scaleFactor;
            options.inJustDecodeBounds = false;

            Bitmap bmp = BitmapFactory.decodeStream(
                    context.getContentResolver().openInputStream(uri), null, options);
            return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
                    bmp.getHeight(), mat, true);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        return output;
    }
}
