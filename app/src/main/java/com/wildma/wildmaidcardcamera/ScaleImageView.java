package com.wildma.wildmaidcardcamera;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class ScaleImageView extends android.support.v7.widget.AppCompatImageView {
    private Matrix matrix=new Matrix();
    private Matrix savedMatrix=new Matrix();

    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    public ScaleImageView(Context context) {
        super(context);
    }

    public ScaleImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScaleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getActionMasked()==MotionEvent.ACTION_POINTER_UP)
            Log.d("Infor", "多点操作");
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                matrix.set(getImageMatrix());
                savedMatrix.set(matrix);
                start.set(event.getX(),event.getY());
                Log.d("Infor", "触摸了...");
                mode=DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN: //多点触控
                oldDist=this.spacing(event);
                if(oldDist>10f){
                    Log.d("Infor", "oldDist"+oldDist);
                    savedMatrix.set(matrix);
                    midPoint(mid,event);
                    mode=ZOOM;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode=NONE;
                break;
            case MotionEvent.ACTION_MOVE:
                if(mode==DRAG){     //此实现图片的拖动功能...
                    matrix.set(savedMatrix);
                    matrix.postTranslate(event.getX()-start.x, event.getY()-start.y);
                }
                else if(mode==ZOOM){// 此实现图片的缩放功能...
                    float newDist=spacing(event);
                    if(newDist>10){
                        matrix.set(savedMatrix);
                        float scale=newDist/oldDist;
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }
        setImageMatrix(matrix);
        return true;
    }
}
