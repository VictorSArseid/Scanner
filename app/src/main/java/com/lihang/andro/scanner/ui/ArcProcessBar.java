package com.lihang.andro.scanner.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.lihang.andro.scanner.R;
import com.lihang.andro.scanner.utils.Utils;

/**
 * Created by andro on 2017/2/8.
 */

/**
 * customized arc process bar to show the process of ongoing scan
 */
public class ArcProcessBar extends View{

    private Paint paint;           // the reference of the paint
    private int textColor;         // the color of the percentage string in the middle
    private float textSize;        // the size of the percentage string in the middle
    private int max;               // maximum percentage;
    private int progress;           // current percentage;
    private boolean isDisplayText; // whether display the percentage string in the middle or not
    private String title;
    private Bitmap bmpTemp = null;
    private int degrees;

    public ArcProcessBar(Context context) {
        this(context, null);
    }

    public ArcProcessBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArcProcessBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        degrees = 0;
        paint = new Paint();
        // get customized attributes and default from attrs.xml
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ArcProcessBar);
        textColor = typedArray.getColor(R.styleable.ArcProcessBar_textColor, Color.BLUE);
        textSize = typedArray.getDimension(R.styleable.ArcProcessBar_textSize, 15);
        max = typedArray.getInteger(R.styleable.ArcProcessBar_max, 100);
        isDisplayText = typedArray.getBoolean(R.styleable.ArcProcessBar_textIsDisplayable, true);
        typedArray.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int centerX = getWidth() / 2;          // get the x coordinate of center
        int centerY = getHeight() / 2;         // get the y coordinate of center

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas can = new Canvas(bitmap);
        // draw bottom background image
        bmpTemp = Utils.decodeCustomRes(getContext(), R.drawable.arc_bg);
        float dstWidth = (float) width;
        float dstHeight = (float) height;
        int srcWidth = bmpTemp.getWidth();
        int srcHeight = bmpTemp.getHeight();
        can.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG
                | Paint.FILTER_BITMAP_FLAG));
        Bitmap bmpBg = Bitmap.createScaledBitmap(bmpTemp, width, height, true);
        can.drawBitmap(bmpBg, 0, 0, null);

        // draw progress foreground
        Matrix matrixProgress = new Matrix();
        matrixProgress.postScale(dstWidth / srcWidth, dstHeight / srcHeight);
        bmpTemp = Utils.decodeCustomRes(getContext(), R.drawable.arc_progress);
        Bitmap bmpProgress = Bitmap.createBitmap(bmpTemp, 0, 0, srcWidth, srcHeight,
                matrixProgress, true);
        if (progress == 0) {
            degrees = progress * 270 / max - 270 - 4;
        } else {
            degrees = progress * 270 / max - 270;
        }


        // mask the foreground and background image
        can.save();
        can.rotate(degrees, centerX, centerY);
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        can.drawBitmap(bmpProgress, 0, 0, paint);
        can.restore();

        if ((-degrees) >= 85) {
            int posX = 0;
            int posY = 0;
            if ((-degrees) >= 270) {
                posX = 0;
                posY = 0;
            } else if ((-degrees) >= 225) {
                posX = centerX / 2;
                posY = 0;
            } else if ((-degrees >= 180)) {
                posX = centerX;
                posY = 0;
            } else if ((-degrees) >= 135) {
                posX = centerX;
                posY = 0;
            } else if ((-degrees) >= 85) {
                posX = centerX;
                posY = centerY;
            }

            if ((-degrees) >= 225) {
                can.save();
                // Bitmap dst = Bitmap.createBitmap(bitmap, 0, 0, centerX, centerX);
                paint.setAntiAlias(true);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
                Bitmap src = Bitmap.createBitmap(bmpBg, 0, 0, centerX, centerX);
                can.drawBitmap(src, 0, 0, paint);
                can.restore();

                can.save();
                // dst = Bitmap.createBitmap(bitmap, centerX, 0, centerX, height);
                paint.setAntiAlias(true);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
                src = Bitmap.createBitmap(bmpBg, centerX, 0, centerX, height);
                can.drawBitmap(src, centerX, 0, paint);
                can.restore();
            } else {
                can.save();
                // Bitmap dst = Bitmap.createBitmap(bitmap, posX, posY, width - posX, height - posY);
                paint.setAntiAlias(true);
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
                Bitmap src = Bitmap.createBitmap(bmpBg, posX, posY, width - posX, height - posY);
                can.drawBitmap(src, posX, posY, paint);
                can.restore();
            }
        }

        // draw mask layer bitmap
        canvas.drawBitmap(bitmap, 0, 0, null);

        // draw percentage string in the middle
        paint.reset();
        paint.setStrokeWidth(0);
        paint.setColor(textColor);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        int percent = (int) (((float) progress / (float) max) * 100); // calculate percentage
        float textWidth = paint.measureText(percent + "%"); // measure text width
        if (isDisplayText && percent != 0) {
            canvas.drawText(percent + "%", centerX - textWidth / 2,
                    centerX + textSize / 2 - 25, paint);
        }
        // draw title in the bottom
        paint.setTextSize(textSize / 2);
        textWidth = paint.measureText(title);
        canvas.drawText(title, centerX - textWidth / 2, height - textSize / 2, paint);
    }

    public Paint getPaint() {
        return paint;
    }
    public void setPaint(Paint paint) {
        this.paint = paint;
    }
    public int getTextColor() {
        return textColor;
    }
    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }
    public float getTextSize() {
        return textSize;
    }
    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }
    public synchronized int getMax() {
        return max;
    }
    public synchronized void setMax(int max) {
        if(max < 0){
            throw new IllegalArgumentException("max must more than 0");
        }
        this.max = max;
    }
    public synchronized int getProgress() {
        return progress;
    }

    /**
     * set progress, use synchronized to make sure thread security
     * use postInvalidate to let you can update in the non-ui thread
     * @param progress
     */
    public synchronized void setProgress(int progress) {
        if(progress < 0){
            throw new IllegalArgumentException("progress must more than 0");
        }
        if(progress > max){
            this.progress = progress;
        }
        if(progress <= max){
            this.progress = progress;
            postInvalidate();
        }
    }
    public boolean isDisplayText() {
        return isDisplayText;
    }
    public void setDisplayText(boolean isDisplayText) {
        this.isDisplayText = isDisplayText;
    }
    public String getTitle(){
        return title;
    }
    public void setTitle(String title){
        this.title = title;
    }
}
