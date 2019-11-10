package com.wildma.idcardcamera.camera;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.wildma.idcardcamera.utils.ScreenUtils;

import java.util.List;

/**
 * Author       wildma
 * Github       https://github.com/wildma
 * Date         2018/6/24
 * Desc	        ${相机预览}
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static String TAG = CameraPreview.class.getName();

    private Camera           camera;
    private AutoFocusManager mAutoFocusManager;
    private SensorControler  mSensorControler;
    private Context          mContext;
    private SurfaceHolder    mSurfaceHolder;

    //绘制参考线
    private Paint mPaint=null;

    //输入的字符个数
    int number_characters;

    int number_points;
    //参考线坐标数组,元素个数必须是4的位数，每四个为一组，绘制一条线
    float [] ptsAbove;
    float [] ptsBelow;

    public CameraPreview(Context context) {
        super(context);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSensorControler = SensorControler.getInstance(context.getApplicationContext());

        //获得输入的字符个数
        getNumberCharacters();
        Log.d("number_characters", number_characters + "");
        //参考线坐标点数目
        number_points = (number_characters - 1) * 2;
        ptsBelow = new float[(number_characters-1) * 2];
        ptsAbove = new float[(number_characters-1) * 2];
        //绘制参考线
        mPaint=new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);//设置画笔的类型是填充，还是描边，还是描边且填充
        mPaint.setStrokeWidth(6);//设置笔刷的粗细
        setWillNotDraw(false);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        camera = CameraUtils.openCamera();
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);

                Camera.Parameters parameters = camera.getParameters();
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    //竖屏拍照时，需要设置旋转90度，否者看到的相机预览方向和界面方向不相同
                    camera.setDisplayOrientation(90);
                    parameters.setRotation(90);
                } else {
                    camera.setDisplayOrientation(0);
                    parameters.setRotation(0);
                }
                List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();//获取所有支持的预览大小
                Camera.Size bestSize = getOptimalPreviewSize(sizeList, ScreenUtils.getScreenWidth(mContext), ScreenUtils.getScreenHeight(mContext));
                parameters.setPreviewSize(bestSize.width, bestSize.height);//设置预览大小
                camera.setParameters(parameters);
                camera.startPreview();
                focus();//首次对焦
                //mAutoFocusManager = new AutoFocusManager(camera);//定时对焦
            } catch (Exception e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
                try {
                    Camera.Parameters parameters = camera.getParameters();
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        //竖屏拍照时，需要设置旋转90度，否者看到的相机预览方向和界面方向不相同
                        camera.setDisplayOrientation(90);
                        parameters.setRotation(90);
                    } else {
                        camera.setDisplayOrientation(0);
                        parameters.setRotation(0);
                    }
                    camera.setParameters(parameters);
                    camera.startPreview();
                    focus();//首次对焦
                    //mAutoFocusManager = new AutoFocusManager(camera);//定时对焦
                } catch (Exception e1) {
                    e.printStackTrace();
                    camera = null;
                }
            }
        }
    }

    /**
     * 获取最佳预览大小
     *
     * @param sizes 所有支持的预览大小
     * @param w     SurfaceView宽
     * @param h     SurfaceView高
     * @return
     */
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        //因为设置了固定屏幕方向，所以在实际使用中不会触发这个方法
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        holder.removeCallback(this);
        //回收释放资源
        release();
    }

    /**
     * 释放资源
     */
    private void release() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;

            if (mAutoFocusManager != null) {
                mAutoFocusManager.stop();
                mAutoFocusManager = null;
            }
        }
    }

    /**
     * 对焦，在CameraActivity中触摸对焦或者自动对焦
     */
    public void focus() {
        if (camera != null) {
            try {
                camera.autoFocus(null);
            } catch (Exception e) {
                Log.d(TAG, "takePhoto " + e);
            }
        }
    }

    /**
     * 开关闪光灯
     *
     * @return 闪光灯是否开启
     */
    public boolean switchFlashLight() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(parameters);
                return true;
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(parameters);
                return false;
            }
        }
        return false;
    }

    /**
     * 拍摄照片
     *
     * @param pictureCallback 在pictureCallback处理拍照回调
     */
    public void takePhoto(Camera.PictureCallback pictureCallback) {
        if (camera != null) {
            try {
                camera.takePicture(null, null, pictureCallback);
            } catch (Exception e) {
                Log.d(TAG, "takePhoto " + e);
            }
        }
    }

    public void startPreview() {
        if (camera != null) {
            camera.startPreview();
        }
    }

    public void onStart() {
        addCallback();
        if (mSensorControler != null) {
            mSensorControler.onStart();
            mSensorControler.setCameraFocusListener(new SensorControler.CameraFocusListener() {
                @Override
                public void onFocus() {
                    focus();
                }
            });
        }
    }

    public void onStop() {
        if (mSensorControler != null) {
            mSensorControler.onStop();
        }
    }

    public void addCallback() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.addCallback(this);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        width= MeasureSpec.getSize(widthMeasureSpec);
//        height= MeasureSpec.getSize(heightMeasureSpec);
//        if(0==mRatioWidth||0==mRatioHeight){//初次绘制的情况
//            setMeasuredDimension(width,height);
//            specifiedWeight=width;//将当下绘制的SurfaceView的长宽比用于赋值，以便计算格线的位置
//            specifiedHeight=height;
//        }else{
//            if(width<height*mRatioWidth/mRatioHeight)//哪边占比小就用它为绘制参考便，实际上是在选择同比例最大绘制范围
//            {
//                setMeasuredDimension(width,width*mRatioHeight/mRatioWidth);//设置SurfaceView的大小适应于预览流的大小
//                specifiedWeight=width;//将当下绘制的SurfaceView的长宽比用于赋值，以便计算格线的位置
//                specifiedHeight=width*mRatioHeight/mRatioWidth;
//            }else{
//                setMeasuredDimension(height*mRatioWidth/mRatioHeight,height);
//                specifiedWeight=height*mRatioWidth/mRatioHeight;
//                specifiedHeight=height;
//            }
//        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //float [] pts = {394,405,394,675};
        //canvas.drawLines(pts, mPaint);
        getXYofLines();
        for (int i=0; i<number_points; i+=2) {
            canvas.drawLine(ptsAbove[i], ptsAbove[i + 1], ptsBelow[i], ptsBelow[i + 1], mPaint);
        }
    }

    /**
     * 获取屏幕尺寸，确定参考线的坐标
     */
    public void getXYofLines() {
        float screenMinSize = Math.min(ScreenUtils.getScreenWidth(getContext()), ScreenUtils.getScreenHeight(getContext()));
        float screenMaxSize = Math.max(ScreenUtils.getScreenWidth(getContext()), ScreenUtils.getScreenHeight(getContext()));
        float height = (int) (screenMinSize * 0.15);
        float width = (int) (height * 300.0f / 47.0f);

        //计算第一条线的起始坐标
        float x = (screenMaxSize - width) / 2;
        float y = (screenMinSize - height) / 2;
        Log.d("xy坐标", x + "," + y);
        //计算矩形上部分各条参考线的坐标
        for (int i=0; i<number_points; i+=2) {
            ptsAbove[i] = x + (i/2 + 1) * (width / number_characters);
            ptsBelow[i] = ptsAbove[i];
        }
        for (int i=1; i<number_points; i+=2) {
            ptsAbove[i] = y;
            ptsBelow[i] = y + height;
        }
    }

    private void getNumberCharacters() {
        SharedPreferences pref = getContext().getSharedPreferences("number_characters", Context.MODE_PRIVATE);
        number_characters = pref.getInt("number_character", 0);
        //Log.d("number:", number_characters + "");
    }
}
