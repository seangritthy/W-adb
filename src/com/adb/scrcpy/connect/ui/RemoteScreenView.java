package com.adb.scrcpy.connect.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import com.adb.scrcpy.connect.scrcpy.ScrcpyController;

public class RemoteScreenView extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
    private ScrcpyController controller;

    public RemoteScreenView(Context context) {
        super(context);
        init();
    }

    public RemoteScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RemoteScreenView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        setOnTouchListener(this);
    }

    public void setController(ScrcpyController controller) {
        this.controller = controller;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {}

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (controller == null) return false;

        int action = event.getActionMasked();
        int scrcpyAction;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                scrcpyAction = 0; // DOWN
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                scrcpyAction = 1; // UP
                break;
            case MotionEvent.ACTION_MOVE:
                scrcpyAction = 2; // MOVE
                break;
            default:
                return false;
        }

        int pointerIndex = event.getActionIndex();
        long pointerId = event.getPointerId(pointerIndex);
        int x = (int) event.getX(pointerIndex);
        int y = (int) event.getY(pointerIndex);
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        float pressure = event.getPressure(pointerIndex);

        controller.sendTouchEvent(scrcpyAction, pointerId, x, y, viewWidth, viewHeight, pressure);
        return true;
    }
}
