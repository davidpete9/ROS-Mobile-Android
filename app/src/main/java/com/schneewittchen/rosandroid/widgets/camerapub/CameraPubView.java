package com.schneewittchen.rosandroid.widgets.camerapub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.schneewittchen.rosandroid.R;
import com.schneewittchen.rosandroid.ui.views.widgets.PublisherWidgetView;


/**
 * TODO: Description
 *
 * @author Nils Rottmann
 * @version 1.0.1
 * @created on 27.04.19
 * @updated on 20.10.2020
 * @modified by Nico Studt
 * @updated on 17.09.20
 * @modified by Nils Rottmann
 */
public class CameraPubView extends PublisherWidgetView {

    public static final String TAG = CameraPubView.class.getSimpleName();

    private Paint borderPaint;
    private Paint paintBackground;
    private float cornerWidth;
    private CameraPubData data;
    private RectF imageRect = new RectF();


    public CameraPubView(Context context) {
        super(context);
        init();
    }

    public CameraPubView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        this.cornerWidth = 0; //Utils.dpToPx(getContext(), 8);

        borderPaint = new Paint();
        borderPaint.setColor(getResources().getColor(R.color.borderColor));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(8);

        // Background color
        paintBackground = new Paint();
        paintBackground.setColor(Color.argb(100, 100, 0, 0));
        paintBackground.setStyle(Paint.Style.FILL);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float widthViz = getWidth();
        float heightViz = getHeight();
        canvas.drawRoundRect(0, 0, widthViz, heightViz, cornerWidth, cornerWidth, borderPaint);
        canvas.drawPaint(paintBackground);
    }

}