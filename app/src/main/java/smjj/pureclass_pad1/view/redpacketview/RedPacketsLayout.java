package smjj.pureclass_pad1.view.redpacketview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import smjj.pureclass_pad1.R;

import static smjj.pureclass_pad1.R.drawable.a;

/**
 * 自定义布局通过 属性动画(贝塞尔曲线)实现红包
 * create by yao.cui at 2016/11/28
 */
public class RedPacketsLayout extends RelativeLayout {

    private int dHeight;//红包图片高度
    private int dWidth;//红包图片宽度

    private int mHeight;//view 高度
    private int mWidth;//view宽度

    private LayoutParams mLp;//红包布局参数

    private Drawable[] mDrawables;//红包图片

    private Random mRandom = new Random();

    private Thread mRainThread;
    private LinkedBlockingQueue<View> mCacheQueue = new LinkedBlockingQueue();

    private boolean isStop;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what==1){
                addPacket();
            }
        }
    };


    public RedPacketsLayout(Context context) {
        super(context);
        init();
    }

    public RedPacketsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RedPacketsLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        mDrawables = new Drawable[4];
        mDrawables[0] = getResources().getDrawable(a);
        mDrawables[1] = getResources().getDrawable(R.drawable.b);
        mDrawables[2] = getResources().getDrawable(R.drawable.c);
        mDrawables[3] = getResources().getDrawable(R.drawable.d);
//
//        dHeight = mDrawables[1].getIntrinsicHeight();
//        dWidth = mDrawables[1].getIntrinsicWidth();
        dHeight = mHeight/5;
        dWidth = mWidth/8;

        mLp = new LayoutParams(dWidth,dHeight);
        mLp.addRule(ALIGN_PARENT_TOP,TRUE);

//        ImageView imageView = new ImageView(getContext());
//        imageView.setImageResource(R.drawable.a);
//        LayoutParams params = new LayoutParams(200,200);
////        params.addRule(CENTER_IN_PARENT,TRUE);
//        params.setMargins(200,200,0, 0);
//        imageView.setLayoutParams(params);
//        addView(imageView);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        init();
    }

    private void addPacket(){
        ImageView imageView= null;
        if (mCacheQueue.isEmpty()){
            imageView = new ImageView(getContext());
            imageView.setLayoutParams(mLp);
            imageView.setImageDrawable(mDrawables[mRandom.nextInt(mDrawables.length)]);
            imageView.setRotation(mRandom.nextInt(180));

            imageView.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    removeView(view);
                }
            });
        } else {
            imageView = (ImageView) mCacheQueue.poll();
        }

        addView(imageView);
        ValueAnimator set = genBezierAnimator(imageView);
        set.start();

    }

    public void startRain(){
        isStop = false;
        mRainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isStop){
                    try {
                        Thread.sleep(150);
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    mHandler.sendEmptyMessage(1);
                }
            }
        });
        mRainThread.start();
    }

    public void stopRain(){
        isStop = true;
    }

    private ValueAnimator genBezierAnimator(View target){
        BezierEvaluator evaluator = new BezierEvaluator(getPoint(1),getPoint(2));//传入中间两个点
        ValueAnimator valueAnimator = ValueAnimator.ofObject(evaluator,new PointF(mRandom.nextInt(mWidth-dWidth),-dHeight),
                new PointF(mRandom.nextInt(mWidth),mHeight));//传入开始位置结束位置
        valueAnimator.addUpdateListener(new BezierListener(target));
        valueAnimator.addListener(new AnimatorEndListener(target));
        valueAnimator.setTarget(target);
        valueAnimator.setDuration(3000);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        return valueAnimator;
    }

    private PointF getPoint(int scale){
        PointF pointF = new PointF();
        pointF.x = mRandom.nextInt(mWidth-100);

        pointF.y = mRandom.nextInt((mHeight-100)*scale/2);
        return pointF;
    }

    private class BezierListener implements ValueAnimator.AnimatorUpdateListener{
        private View mTarget;

        public BezierListener(View target) {
            this.mTarget = target;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            PointF pointF = (PointF) valueAnimator.getAnimatedValue();
            mTarget.setX(pointF.x);
            mTarget.setY(pointF.y);
        }
    }

    private class AnimatorEndListener extends AnimatorListenerAdapter {
        private View mTarget;

        public AnimatorEndListener(View view) {
            mTarget = view;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            removeView(mTarget);

            mCacheQueue.add(mTarget);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isStop = true;
    }
}
