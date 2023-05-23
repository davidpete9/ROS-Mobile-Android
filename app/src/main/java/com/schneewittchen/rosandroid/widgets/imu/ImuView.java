package com.schneewittchen.rosandroid.widgets.imu;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.schneewittchen.rosandroid.R;
import com.schneewittchen.rosandroid.ui.views.widgets.PublisherWidgetView;


/**
 * TODO: Description
 *
 * @author Dragos Circa
 * @version 1.0.0
 * @created on 02.11.2020
 * @updated on 18.11.2020
 * @modified by Nils Rottmann
 * @updated on 10.03.2021
 * @modified by Nico Studt
 */

public class ImuView extends PublisherWidgetView {

    public static final String TAG = ImuView.class.getSimpleName();


    public ImuView(Context context) {
        super(context);
        init();
    }

    public ImuView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
    }


    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
