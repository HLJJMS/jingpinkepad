package smjj.pureclass_pad1.view.redpacketview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;

import smjj.pureclass_pad1.R;

/**
 * 结合公司大神的方法 自定义surfaceview 然后通过贝塞尔曲线动画来计算 坐标，不断重绘界面实现
 *
 * create by yao.cui at 2016/11/29
 */
public class RedPacketsSurfaceVew extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {

    private static final int INTERVAL = 300;//默认绘制间隔 单位 ms
    private static final int DURATION = 10*1000; // 默认执行时长 单位 ms
    private static final int GRAVITY = 80;

    private int mInterval = INTERVAL;
    private int mDuration = DURATION;
    private int mGravity = GRAVITY;

    private int mHeight;
    private int mWidth;

    private Thread mDrawThread;
    private Thread mMsgThread;
    private boolean isStop;

    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private Bitmap mBitmap;
    private Bitmap mBitmap1;
    private Bitmap mBitmap2;
    private Bitmap mBitmap3;
    private ArrayList<Sprites> mSprits = new ArrayList();
    private SpriteManager spriteManager;
    private Context context;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what ==1){
                addSprites();
            }
        }
    };

    public RedPacketsSurfaceVew(Context context) {
        super(context);
        init(context);
    }

    public RedPacketsSurfaceVew(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RedPacketsSurfaceVew(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        this.setZOrderOnTop(true);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        mSurfaceHolder.addCallback(this);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.b);
        mBitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.a);
        mBitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.c);
        mBitmap3 = BitmapFactory.decodeResource(getResources(), R.drawable.d);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int height = dm.heightPixels;
        int width = dm.widthPixels;

        //设置红包占屏幕的大小
        mBitmap1 = setImgSize(mBitmap1, width/12 ,height/6);
        mBitmap = setImgSize(mBitmap, width/12 ,height/6);
        mBitmap2 = setImgSize(mBitmap2, width/12 ,height/6);
        mBitmap3 = setImgSize(mBitmap3, width/12 ,height/6);

        setOnTouchListener(this);
        this.context = context;


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();
        spriteManager = SpriteManager.getInstance();
        spriteManager.init(context,mWidth,mHeight);

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mDrawThread = new Thread(new DrawThread());
        mMsgThread = new Thread(new MsgThread());
        init(context);


    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.i("surface red", "destory view");
        isStop = true;
        stop();

    }

    private void addSprites(){
        Sprites sprites = new Sprites(mBitmap,mWidth,mHeight);
        Sprites sprites1 = new Sprites(mBitmap1,mWidth,mHeight);
        Sprites sprites2 = new Sprites(mBitmap2,mWidth,mHeight);
        Sprites sprites3 = new Sprites(mBitmap3,mWidth,mHeight);
        mSprits.add(sprites);
        mSprits.add(sprites1);
        mSprits.add(sprites2);
        mSprits.add(sprites3);


    }

    public void startRain(){
        isStop = false;
        if (mDrawThread!= null){
            mDrawThread.start();

        }

        if (mMsgThread != null){
            mMsgThread.start();

        }

    }

    public void stopRain(){

        isStop = true;

    }


    /**
     * 移除已经执行完的红包，防止arraylist里面对象越来越多
     */
    private void recycle(){

        ArrayList<Sprites> recycleSp = new ArrayList<>();

        for (int i = 0, siz = mSprits.size();i<siz;i++){
            if (mSprits.get(i).isOver){
                recycleSp.add(mSprits.get(i));
            }
        }

        for (int i =0; i <recycleSp.size();i++){
            mSprits.remove(recycleSp.get(i));
        }
    }

    /**
     * 停止红包雨 清空红包对象
     */
    private void stop(){
        for (int i = 0, size = mSprits.size(); i < size;i++){
            Sprites sprites = mSprits.get(i);
            sprites.stop();
            sprites = null;
        }
        mSprits.clear();
        mBitmap.recycle();
        mBitmap1.recycle();
        mBitmap2.recycle();
        mBitmap3.recycle();


    }

    private Sprites getTouchSprites(int x,int y){
        for (int i =0,size=mSprits.size();i<size;i++){
            if (mSprits.get(i).isInner(x,y)){
                return mSprits.get(i);
            }
        }
        return null;
    }
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                Sprites sprites = getTouchSprites((int)motionEvent.getX(),(int)motionEvent.getY());
                if (sprites!= null){
                    sprites.isOver = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                break;
        }
        return true;
    }

    private class DrawThread implements Runnable {
        @Override
        public void run() {
            while(!isStop){

                recycle();

                Canvas canvas = null;
                synchronized (mSurfaceHolder){
                    canvas = mSurfaceHolder.lockCanvas();

                    if (canvas== null){
                        isStop = true;
                        return;
                    }

                    /**清空画布*/
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                    for (int i = 0, size = mSprits.size();i < size;i++){
                        mSprits.get(i).draw(canvas,mPaint);
                    }

                    mSurfaceHolder.unlockCanvasAndPost(canvas);

                }
                try {
                    Thread.sleep(10);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }


    class MsgThread implements Runnable {
        @Override
        public void run() {
            while (!isStop){
                try {
                    Thread.sleep(500);
                } catch (Exception e){
                    e.printStackTrace();
                }

                mHandler.sendEmptyMessage(1);
            }
        }
    }



    public Bitmap setImgSize(Bitmap bm, int newWidth , int newHeight){
        // 获得图片的宽高.
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例.
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数.
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        // 得到新的图片.
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }



}
