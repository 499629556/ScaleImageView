package com.qxc.widgets;


import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

/**
 * 描述：图片缩放拖住
 * 作者：qxc on 2019-5-23 17:59
 * 邮箱：499629556@qq.com
 */
public class ScalebleImageView extends View {

    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    static final float WIDTH = Utils.dpToPixel(300);

    static final float bigOver = 1.5f;//放大系数
    //最大最小放大倍数
    float bigScale = 1.0f,samallScale = 1.0f;
    //当前放大倍数
    float curScale = 1.0f;
    //初始偏移
    float orientationOffsetX,orientationOffsetY;
    //当前偏移
    float offsetX,offsetY;
    //可偏移边界
    float offsetMinX,offsetMinY,offsetMaxX,offsetMaxY;
    //滑到边界的反弹效果的大小
    static final int offsetOver = 100;

    public float getCurScale() {
        return curScale;
    }
    public void setCurScale(float curScale) {
        this.curScale = curScale;

        //修正偏移量
        float curDD = (curScale - samallScale)/(bigScale - samallScale);
        offsetX = offsetX * curDD;
        offsetY = offsetY * curDD;

        setOffsets();
        invalidate();
    }


    Bitmap bitmap;

    //手势侦测器
    GestureDetectorCompat gestureDetectorCompat;
    //手势侦测器监听
    MyOnGestureListener myOnGestureListener = new MyOnGestureListener();
    //缩放侦测器
    ScaleGestureDetector scaleGestureDetector;
    //缩放侦测器监听
    MyOnScaleGestureListener myOnScaleGestureListener = new MyOnScaleGestureListener();
    //用来实时计算偏移量
    OverScroller overScroller;
    //偏移的任务
    MyRunnable myRunnable = new MyRunnable();

    public ScalebleImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    {
        bitmap = Utils.getAvatar(getResources(),(int) WIDTH);
        gestureDetectorCompat = new GestureDetectorCompat(getContext(), myOnGestureListener);
        scaleGestureDetector = new ScaleGestureDetector(getContext(),myOnScaleGestureListener);
        overScroller = new OverScroller(getContext());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        orientationOffsetX = ((float)getWidth()-WIDTH)/2;
        orientationOffsetY = ((float)getHeight()-WIDTH)/2;
        float biliw = (float) getWidth()/WIDTH;
        float bilih = (float) getHeight()/WIDTH;
        if(bilih<biliw){
            bigScale = biliw*bigOver;
            samallScale = bilih;
        }else {
            bigScale = bilih*bigOver;
            samallScale = biliw;
        }
        curScale = samallScale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(offsetX,offsetY);
        canvas.scale(curScale,curScale,getWidth()/2,getHeight()/2);
        canvas.drawBitmap(bitmap,orientationOffsetX,orientationOffsetY,paint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //两个侦测器的选择
        boolean result = scaleGestureDetector.onTouchEvent(event);
        if(!scaleGestureDetector.isInProgress()){//发生双指缩放了
            result = gestureDetectorCompat.onTouchEvent(event);
        }
        return result;
    }

    ObjectAnimator objectAnimator;
    private ObjectAnimator getObjectAnimator(){
        if(objectAnimator == null){
            objectAnimator = ObjectAnimator.ofFloat(this,"curScale",0f);
        }
        objectAnimator.setFloatValues(samallScale,bigScale);
        return objectAnimator;
    }

    //手势侦测器监听
    class MyOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            //要使用就返回true
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            //按下超过100ms触发
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            //单机触发
            return false;
        }

        /**
         * @param e1 Down
         * @param e2 当前
         * @param distanceX 上一个点到当前点位置
         * @param distanceY 上一个点到当前点位置
         * @return
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //滑动
            if(curScale > samallScale){
                offsetX = offsetX - distanceX;
                offsetY = offsetY - distanceY;
                //修正边界
                fixOffsets();
                invalidate();
            }

            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            //长按触发
        }

        /**
         * @param e1 down
         * @param e2 当前
         * @param velocityX 速度
         * @param velocityY 速度
         * @return
         */
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //快速滑动，手指离开触发   惯性滑动
            if(curScale > samallScale){
                overScroller.fling((int) offsetX,(int)offsetY,(int)velocityX,(int)velocityY,(int)offsetMinX,(int)offsetMaxX,(int)offsetMinY,(int)offsetMaxY,offsetOver,offsetOver);
                postOnAnimation(myRunnable);
            }

            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //用户双击 第二次触摸到就调用
            if(curScale<bigScale){
                offsetX = (e.getX() - getWidth()/2) - (e.getX() - getWidth()/2)*bigScale/samallScale;
                offsetY = (e.getY() - getHeight()/2) - (e.getY() - getHeight()/2)*bigScale/samallScale;
                //修正边界
                fixOffsets();
                getObjectAnimator().start();
            }else {
                getObjectAnimator().reverse();
            }
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            //用户双击 用户第二次触摸到 触摸后移动 触摸后台起都调用
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            //用户单机 在用户单机300ms内没出现双击时触发
            return false;
        }
    }

    /**
     * 根据当前缩放值 获滑动的取边界值
     */
    private void setOffsets(){
        offsetMinX = ((float)getWidth() - curScale*WIDTH)/2;
        if(offsetMinX>0){
            offsetMinX = - offsetMinX;
        }

        offsetMinY = ((float)getHeight() - curScale*WIDTH)/2;
        if(offsetMinY>0){
            offsetMinY = - offsetMinY;
        }

        offsetMaxX =  - offsetMinX;
        offsetMaxY = - offsetMinY;


    }

    /**
     * 根据边界值修正边界
     */
    private void fixOffsets(){
        offsetX = Math.max(offsetMinX,Math.min(offsetX,offsetMaxX));
        offsetY = Math.max(offsetMinY,Math.min(offsetY,offsetMaxY));
    }

    //缩放侦测器监听
    class MyOnScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener{
        float initFactor = 1.0f;
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            float temp_scale = initFactor * detector.getScaleFactor();
            if(temp_scale>bigScale || temp_scale<samallScale){
                return false;
            }
            offsetX = (detector.getFocusX() - (float)getWidth()/2) - (detector.getFocusX() - (float) getWidth()/2)*curScale/temp_scale;
            offsetY = (detector.getFocusY() - (float)getHeight()/2) - (detector.getFocusY() - (float)getHeight()/2)*curScale/temp_scale;

            curScale = temp_scale;

            setOffsets();
            //修正边界
            fixOffsets();

            invalidate();
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            //确定要消费这个缩放，必须返回true
            initFactor = getCurScale();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    }

    class MyRunnable implements Runnable{
        @Override
        public void run() {
            //判断滚动是否完成 true滚动未完成 需继续
            if(overScroller.computeScrollOffset()){
                offsetX = overScroller.getCurrX();
                offsetY = overScroller.getCurrY();
                invalidate();
                postOnAnimation(this);
            }
        }
    }
}
