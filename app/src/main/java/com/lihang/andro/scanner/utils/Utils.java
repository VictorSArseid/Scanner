package com.lihang.andro.scanner.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.File;
import java.io.InputStream;

/**
 * Created by andro on 2017/2/8.
 */

public class Utils {

    /**
     * get image from Resources, convert it to bitmap
     * @param context
     * @param resource
     * @return
     */
    public static Bitmap decodeCustomRes(Context context, int resource) {
        InputStream is = context.getResources().openRawResource(resource);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = 1; // use original size
        Bitmap bmp = BitmapFactory.decodeStream(is, null, options);
        return bmp;
    }

    /**
     * get screen resolution - width
     * @param context
     * @return
     */
    public static int getWindowWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        int mScreenWidth = dm.widthPixels;
        return mScreenWidth;
    }

    /**
     * get screen resolution - height
     * @param context
     * @return
     */
    public static int getWindowHeight(Context context) {
        WindowManager wm = (WindowManager) (context
                .getSystemService(Context.WINDOW_SERVICE));
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        int mScreenHeigh = dm.heightPixels;
        return mScreenHeigh;
    }

    /**
     * get extension name of a file
     * @param filename
     * @return
     */
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }


}
