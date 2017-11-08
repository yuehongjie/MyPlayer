package com.yu.myplayer;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by Administrator on 2017-11-6.
 * 手势控制
 */

public class MediaPlayerGestureController {

    public interface GestureOperationListener{

        void onSingleTap(); //单击

        void onDoubleTap(); //双击

        void onStartDragProgress(); //开始手势进度

        void onDraggingProgress(float percent); //手势更改进度中

        void onEndDragProgress(float percent); //手势更改进度结束

    }

    //手势的类型，一旦确定了某种类型，就只改变某一个类型的值，以防多种功能同时调整
    private enum AdjustType {
        None,                   //默认无手势
        Volume,                 //播放音量
        Brightness,             //屏幕亮度
        FastBackwardOrForward,  //快进、快退
    }

    private AdjustType mAdjustType = AdjustType.None;//手势，默认无
    private FrameLayout mAdjustPanelContainer; //装载音量和亮度手势的布局
    private FrameLayout mProgressPanelContainer; //装载显示快进进度的布局
    private View mPlayerRootView; //响应手势的播放器布局的区域
    private AdjustPanel mAdjustPanel; //音量和亮度显示面板
    private ProgressAdjustPanel mProgressAdjustPanel; //进度显示面板

    private GestureDetector mGestureDetector;//触摸事件委托手势识别器处理

    private static final int INVALID_DRAG_PROGRESS = -1;
    private static final int START_DRAG_PROGRESS = 1;
    private int mStartDragProgressPosition = INVALID_DRAG_PROGRESS; //开始拖拽的位置
    private int mDragProgressPosition;//当前拖拽到的位置
    private float mDragPercent;// 从起点开始累加，向右滑动增加 ，向左滑动减少 起点为 0， 最大 1，最小 -1

    private float mCurrentBrightness = 0; //当前亮度
    private int mCurrentVolume = 0; //当前音量
    private GestureOperationListener mGestureListener;

    private Context mContext;

    public MediaPlayerGestureController(Context context, View playRootView){
        mContext = context;
        mPlayerRootView = playRootView;

        //根据系统亮度，设置当前界面的亮度
        MediaUtils.setBrightness(context, MediaUtils.getSystemBrightnessPercent(context));

        //初始化手势识别器
        initGestureDetector();

        //可以再视频准备好以后（并且在横屏的情况下触发）
        mPlayerRootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        mCurrentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        mCurrentBrightness = MediaUtils.getBrightnessPercent(mContext);
                        log("当前音量：" + mCurrentVolume + "   当前亮度：" + mCurrentBrightness);
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        //结束拖拽
                        if (mAdjustType == AdjustType.FastBackwardOrForward) {
                            if (mGestureListener != null) {
                                mGestureListener.onEndDragProgress(mDragPercent);
                            }
                        }
                        reset();//重置事件
                        break;
                }

                mGestureDetector.onTouchEvent(motionEvent);

                return true;
            }
        });
    }

    //设置音量和亮度要显示在哪个布局中
    public void setAdjustPanelContainer(FrameLayout containerLayout) {
        this.mAdjustPanelContainer = containerLayout;
        mAdjustPanel = new AdjustPanel(mContext);
        mAdjustPanelContainer.addView(mAdjustPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    //设置进度更新要显示在哪个布局中
    public void setProgressAdjustPanelContainer(FrameLayout containerLayout) {
        this.mProgressPanelContainer = containerLayout;
        mProgressAdjustPanel = new ProgressAdjustPanel(mContext);
        mProgressPanelContainer.addView(mProgressAdjustPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    //设置手势监听
    public void setGestureOperationListener(GestureOperationListener listener) {
        this.mGestureListener = listener;
    }

    private Runnable mSingleTapRunnable = new Runnable() {
        @Override
        public void run() {
            mGestureListener.onSingleTap();
        }
    };

    private void initGestureDetector() {
        mGestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener(){

            //单击事件
            @Override
            public boolean onSingleTapUp(MotionEvent e) { //连续点击两次，这个方法也会执行
                log("单击事件");
                if (mGestureListener != null) {
                    mPlayerRootView.postDelayed(mSingleTapRunnable, 200); //延迟执行，如果是连续点击，就认为是双击事件，不执行单击事件
                }
                return true;
            }

            //双击事件
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                log("双击事件");
                if (mGestureListener != null) {
                    mPlayerRootView.removeCallbacks(mSingleTapRunnable); //不执行单击事件
                    mGestureListener.onDoubleTap();
                }
                return true;
            }

            //触摸滑动事件
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

                if (e1 == null || e2 == null) {
                    return true;
                }

                //先初始化，确定是处理什么事件
                if (mAdjustType == AdjustType.None) {
                    //先判断开始是水平滑动还是竖直方向滑动
                    if (Math.abs(distanceX) > Math.abs(distanceY)) { //水平方向滑动大，则判断为调整进度
                        mAdjustType = AdjustType.FastBackwardOrForward;
                    }else { //竖直方向滑动大，在细分左右，左边为亮度调节，右边为音量调整
                        if (e1.getX() < mPlayerRootView.getMeasuredWidth() / 2) { //起点为播放区域的左半部分，调节亮度
                            mAdjustType = AdjustType.Brightness;
                        }else { //右半部分，调节音量
                            mAdjustType = AdjustType.Volume;
                        }
                    }
                }

                //log("(distanceX,distanceY) = (" + distanceX + "," + distanceY + ")");
                //log("(e2-e1 X,e2-e1 Y) = (" + (e2.getX() - e1.getX()) + "," + (e2.getY() - e1.getY()) + ")");

                distanceX = e2.getX() - e1.getX();
                distanceY = e2.getY() - e1.getY();
                return adjustInternal(distanceX, distanceY);
            }
        });
    }

    //处理事件
    private boolean adjustInternal(float distanceX, float distanceY) {
        if (mAdjustType == AdjustType.FastBackwardOrForward) { //处理快进和快退
            return adjustProgress(distanceX);
        }else if (mAdjustType == AdjustType.Brightness) { //调整亮度
            return adjustBrightness(distanceY);
        }else if (mAdjustType == AdjustType.Volume) { //调节音量
            return adjustVolume(distanceY);
        }
        return true;
    }

    //调节音量
    private boolean adjustVolume(float distanceY) {

        distanceY *= -1; //坐标系反方向
        float percent = distanceY / (float) mPlayerRootView.getMeasuredHeight();
        //音频管理器
        AudioManager manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volumeOffsetAccurate = maxVolume * percent * 1.2f;
        int volumeOffset = (int) volumeOffsetAccurate;

        if (volumeOffset == 0 && Math.abs(volumeOffsetAccurate) > 0.2f) {
            if (distanceY > 0) {
                volumeOffset = 1;
            } else if (distanceY < 0) {
                volumeOffset = -1;
            }
        }

        int currVolume = mCurrentVolume + volumeOffset;
        if (currVolume < 0) {
            currVolume = 0;
        } else if (currVolume >= maxVolume) {
            currVolume = maxVolume;
        }

        manager.setStreamVolume(AudioManager.STREAM_MUSIC, currVolume, 0);

        //log("调节音量：" + currVolume);

        float volumePercent = (float) currVolume / (float) maxVolume;
        if (mAdjustPanel != null) {
            mAdjustPanel.adjustVolume(volumePercent);
        }

        return true;
    }

    //调节亮度
    private boolean adjustBrightness(float distanceY) {

        distanceY *= -1; // 与坐标系方向相反，向上才是增大亮度
        float percent = distanceY / (float) mPlayerRootView.getMeasuredHeight();
        float brightnessOffset = percent * 1.2f; // 每次 1.2 倍变化
        float brightness = mCurrentBrightness + brightnessOffset; //变化后的亮度值

        //数据校验 0 - 1
        if (brightness < 0) {
            brightness = 0;
        }else if (brightness > 1) {
            brightness = 1;
        }
        //设置亮度
        MediaUtils.setBrightness(mContext, brightness);
        //log("调节亮度（0~1）：" + brightness);

        if (mAdjustPanel != null) {
            mAdjustPanel.adjustBrightness(brightness);
        }
        return true;
    }

    //处理快进、快退
    private boolean adjustProgress(float distanceX) {
        //开始拖拽
        if (mStartDragProgressPosition == INVALID_DRAG_PROGRESS) {
            mStartDragProgressPosition = START_DRAG_PROGRESS;
            if (mGestureListener != null) {
                mGestureListener.onStartDragProgress();
            }
        }

        mDragPercent = distanceX / (float) mPlayerRootView.getMeasuredWidth();
        //log("播放进度变化率：" + mDragPercent);
        if (mGestureListener != null) {
            mGestureListener.onDraggingProgress(mDragPercent);
        }

        return true;
    }

    public void showAdjustProgress(boolean isForward, int currPosition, int total) {
        mDragProgressPosition = currPosition;

        if (isForward) {
            mProgressAdjustPanel.adjustForward(currPosition, total);
        } else {
            mProgressAdjustPanel.adjustBackward(currPosition, total);
        }
    }

    private void reset(){
        //隐藏音量、亮度面板
        if (mAdjustPanel != null) {
            mAdjustPanel.hidePanel();
        }

        //隐藏进度拖拽面板
        if (mProgressAdjustPanel != null) {
            mProgressAdjustPanel.hidePanel();
        }
        mAdjustType = AdjustType.None;
        mStartDragProgressPosition = INVALID_DRAG_PROGRESS;
        mDragPercent = 0;
    }


    private static final String TAG = "MediaPlayerGesture";
    private void log(String msg){
        Log.d(TAG, msg);
    }
}
