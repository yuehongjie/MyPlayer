package com.yu.myplayer;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * MediaPlayer 生命周期  http://www.cnblogs.com/gansc23/archive/2011/04/08/2009868.html
 *
 * new MediaPlayer  |
 * .reset           |-> [IDLE] -> .setDataSource
 *                         |             |
 *                         |             -> [Initialized] -> .prepareAsync -> [Preparing]
 *                         |                             |                        |
 *                         |                             |-> .prepare      -> [Prepared]
 *                         |                                                      |
 *                         |                                                      |
 *                         |                        |<- .seekTo <-|               |
 *                         |                        |             |               |
 *                         |                        |             |               |
 * [END] <- .release <- [Stopped] <---  .stop  <--- [___Started___] <--- .start <-|
 *                                                    |          |
 *                                                    |          |
 *                                                 .pause      .start
 *                                                    |          |
 *                                                    |          |
 *                                                  [____Paused___]
 *
 */
public class SimplePlayerActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    private static final String TAG = "SimplePlayerActivity";

    // view
    private FrameLayout flContainer; // view root
    private SurfaceView mSurfaceView; // 视频显示控件
    private LinearLayout mBottomControlerLayout; // 底部控制控件
    private ImageView mIvPlaySwitch; // 暂停和开始切换控件
    private SeekBar mProgressBar; // 进度条
    private TextView mTvCurrentTime; // 当前播放时间点
    private TextView mTvTotalTime; // 总时长
    private ImageView mIvScreenSwitch; // 全屏切换控件
    private Context mContext;

    private SurfaceHolder mSurfaceHolder;
    private boolean mIsSurfaceCreated; //surface 是否创建成功

    //url
    private static final String EXTRA_VIDEO_URL = "extra_video_url"; // 参数
    private String mUrl; // 视频 Url
    private final Object mLock = new Object();

    //play
    private MediaPlayer mMediaPlayer;
    private boolean mIsMediaPrepared; //播放器是否准备完成
    private int mPausePosition; //从上次暂停的位置恢复播放
    private int mBufferPercent; //缓冲进度 1~100
    private boolean mDragingProgress; //正在拖拽进度条
    private boolean mIsPaused;
    private int mStartDragPosition; //需要跳转的位置

    private Handler mHandler = new Handler();
    private MediaPlayerGestureController mMediaPlayerGestureController;//手势控制器


    public static void start(Context context, String url) {
        Intent intent = new Intent(context, SimplePlayerActivity.class);
        intent.putExtra(EXTRA_VIDEO_URL, url);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("SurfaceView 播放视频");
        setContentView(R.layout.activity_simple_player);

        mContext = getApplicationContext();

        initView();

        mUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        if (!TextUtils.isEmpty(mUrl)) {
            play();
        }else {
            Toast.makeText(mContext, "video url is null", Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        flContainer = findView(R.id.fl_container);
        mSurfaceView = findView(R.id.surfaceView);
        mBottomControlerLayout = findView(R.id.ll_player_controller);
        mIvPlaySwitch = findView(R.id.iv_play);
        mProgressBar = findView(R.id.seek_bar_progress);
        mTvCurrentTime = findView(R.id.play_duration);
        mTvTotalTime = findView(R.id.full_duration);
        mIvScreenSwitch = findView(R.id.iv_fullscreen_video);
        //设置视频区域默认高度
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int height = (int) (metrics.widthPixels / 1.77f); // 默认比例 16:9
        ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
        layoutParams.height = height;
        mSurfaceView.setLayoutParams(layoutParams);

        mIvPlaySwitch.setOnClickListener(this);
        mIvScreenSwitch.setOnClickListener(this);
        mProgressBar.setOnSeekBarChangeListener(this);;

        //手势识别
        mMediaPlayerGestureController = new MediaPlayerGestureController(this, flContainer);
        //设置音量、亮度调节面板
        mMediaPlayerGestureController.setAdjustPanelContainer(flContainer);
        //设置进度调节面板
        mMediaPlayerGestureController.setProgressAdjustPanelContainer(flContainer);

    }

    private <T extends View> T findView(int id) {
        return (T)findViewById(id);
    }

    private void initMedia(){

        Log.d(TAG, "create media player");
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnErrorListener(mOnErrorListener);
        mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
        mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
        mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompletionListener);
        mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mMediaPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
    }

    private void initSurface(){
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            // 当 SurfaceView 中的 Surface 被创建的时候调用，
            // 这时可以指定 MediaPlayer 在当前的 Surface 中进行播放
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) { //当界面恢复的时候，也会回调这个方法
                Log.d(TAG, "surfaceCreated success");
                mIsSurfaceCreated = true;
                playWithSurface();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
                Log.d(TAG, "surfaceHolder changed");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) { //当页面不可见 stop 的时候 会调用该方法，减少资源占用
                Log.d(TAG, "surfaceDestroyed...");
                mIsSurfaceCreated = false;
            }
        });
    }

    private void play(){
        if (mMediaPlayer != null) {
            releaseMedia();
        }

        initMedia();
        initSurface();

        // idle
        Log.d(TAG, "reset media player to idle");
        mMediaPlayer.reset();

        // initialized
        Log.d(TAG, "init media player");
        try {
            mMediaPlayer.setDataSource(mContext, Uri.parse(mUrl)); //如果是网络资源 需要有访问网络的权限 如果是 SD 卡文件需要读写 SD 卡的权限
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "The video source may be parsed error");
            return;
        }
        // prepare for play，成功回调 OnPreparedListener
        Log.d(TAG, "prepare media player");
        try{
            mMediaPlayer.prepareAsync();
        }catch (IllegalStateException e) {
            e.printStackTrace();
            Log.e(TAG, "Video PrepareAsync FAILED! Video might be damaged!!");
        }

    }



    // 点击事件
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_play: // 暂停 开始 切换
                if (isPlaying()) { // 播放 --> 暂停
                    pause();
                }else { //暂停到播放
                    resume();
                }
                break;
            case R.id.iv_fullscreen_video: // 横竖屏切换
                break;
        }
    }

    //是否正在播放
    private boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    private boolean isPaused() {
        return mIsPaused;
    }

    // SeekBar 进度条进度变化监听
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        //Log.d(TAG, "用户拖拽：" + fromUser);
        if (!fromUser) {
            return;
        }

        updateSeek(progress, false);

    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mDragingProgress = true;
        mHandler.removeCallbacks(mUpdateProgressRunnable); //取消自动更新进度条
    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //Log.d(TAG, "停止拖拽");
        mDragingProgress = false;
        updateSeek(seekBar.getProgress(), true);
    }

    private void updateSeek(int progress, boolean doSeek) {
        float percent = (float) progress / (float) mProgressBar.getMax();
        int duration = mMediaPlayer.getDuration();
        int seekToPosition = (int) (duration * percent);
        mTvCurrentTime.setText(MediaUtils.formatTime(seekToPosition));

        if (doSeek) {
            mMediaPlayer.seekTo(seekToPosition);
        }
    }


    private Runnable mUpdateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private void updateProgress(){
        mHandler.postDelayed(mUpdateProgressRunnable, 1000); // 1s 更新一次进度

        if (mMediaPlayer != null && mDragingProgress || !mIsMediaPrepared || !mMediaPlayer.isPlaying()) { //没有在播放就不更新了
            return;
        }

        int currentPosition = mMediaPlayer.getCurrentPosition(); //当前进度
        int duration = mMediaPlayer.getDuration(); //总时长

        updateProgressInternal(currentPosition, duration);
    }

    private void updateProgressInternal(int position, int duration){
        if (position < 0) {
            position = 0;
        }

        if (position >= duration) {
            position = duration;
        }

        if (duration == 0) {
            position = 0;
            mProgressBar.setSecondaryProgress(0);
            mProgressBar.setProgress(0);
        } else {
            mProgressBar.setSecondaryProgress((int) (mBufferPercent / 100.0f * duration));
            mProgressBar.setProgress(position);
            mProgressBar.setMax(duration);
        }

        mTvCurrentTime.setText(MediaUtils.formatTime(position));
        mTvTotalTime.setText(MediaUtils.formatTime(duration));
    }

    @Override
    protected void onPause() {
        super.onPause();
        pause();
    }

    private void pause(){
        if (mMediaPlayer == null || !mIsMediaPrepared) {
            return;
        }

        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mIsPaused = true;
            mPausePosition = mMediaPlayer.getCurrentPosition();

            mIvPlaySwitch.setImageResource(R.drawable.btn_play);
        }
    }

    private void resume() {

        Log.d(TAG, "isPlaying: " + isPlaying());

        if (!isPlaying()) {
            synchronized(mLock){ //同步一下，因为 mIsSurfaceCreated 和 mIsMediaPrepared 是在两个线程后进行初始化的
                if (mMediaPlayer != null && mIsSurfaceCreated && mIsMediaPrepared) { // surface 已准备好 && MediaPlayer 已准备好 可以播放
                    mMediaPlayer.start();
                    mIsPaused = false;
                    mIvPlaySwitch.setImageResource(R.drawable.btn_pause);
                }
            }
        }
    }

    //destroy
    @Override
    protected void onDestroy() {
        super.onDestroy();

        mHandler.removeCallbacks(mUpdateProgressRunnable); //取消自动更新进度条

        releaseMedia();
    }

    private void releaseMedia(){

        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.setDisplay(null);
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            Log.d(TAG, "release media player ok");
        }
    }

    private void playWithSurface(){
        Log.d(TAG, "playWithSurface && isSurfaceCreated: " + mIsSurfaceCreated + ";   isMediaPrepared: " + mIsMediaPrepared);
        synchronized(mLock){ //同步一下，因为 mIsSurfaceCreated 和 mIsMediaPrepared 是在两个线程后进行初始化的
            if (mIsSurfaceCreated && mIsMediaPrepared) { // surface 已准备好 && MediaPlayer 已准备好 可以播放
                mMediaPlayer.setDisplay(mSurfaceHolder);
                mMediaPlayer.setScreenOnWhilePlaying(true);
                mMediaPlayer.start();
                mMediaPlayer.seekTo(mPausePosition);

                addGestureListener();// 开始播放添加手势监听
            }
        }
    }

    // ------------------------------- 添加手势控制监听 --------------------------------------

    private void addGestureListener() {
        if (mMediaPlayerGestureController != null) {
            mMediaPlayerGestureController.setGestureOperationListener(mGestureListener);
        }
    }

    private MediaPlayerGestureController.GestureOperationListener mGestureListener = new MediaPlayerGestureController.GestureOperationListener() {
        @Override
        public void onSingleTap() { //单击
            if (mBottomControlerLayout.getVisibility() == View.VISIBLE) {
                mBottomControlerLayout.setVisibility(View.GONE);
            }else {
                mBottomControlerLayout.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onDoubleTap() { //双击
            if (isPlaying()) {
                pause();
            }else {
                resume();
            }
        }

        @Override
        public void onStartDragProgress() {//开始拖拽
            if (mMediaPlayer != null) {
                mStartDragPosition = mMediaPlayer.getCurrentPosition();

                mHandler.removeCallbacks(mUpdateProgressRunnable);
            }

        }

        @Override
        public void onDraggingProgress(float percent) {//拖拽中
            if (mMediaPlayer != null) {
                int duration = mMediaPlayer.getDuration();
                int seekOffset = (int) (percent * duration);
                int seekToPosition = mStartDragPosition + seekOffset;

                if (seekToPosition < 0) {
                    seekToPosition = 0;
                }else if (seekToPosition > duration) {
                    seekToPosition = duration;
                }

                mMediaPlayerGestureController.showAdjustProgress(percent > 0, seekToPosition, duration);

                updateSeek(seekToPosition, false);
                mProgressBar.setProgress(seekToPosition);
            }

        }

        @Override
        public void onEndDragProgress(float percent) { //结束拖拽
            int duration = mMediaPlayer.getDuration();
            int seekOffset = (int) (percent * duration);
            int seekToPosition = mStartDragPosition + seekOffset;

            updateSeek(seekToPosition, true);
        }
    };


    //--------------------------------- MediaPlayer Listener -------------------------------------

    private MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            Log.d(TAG, "Prepared media player success");
            mIsMediaPrepared = true;
            playWithSurface();
        }
    };

    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            Log.d(TAG, "media player onCompletion");
            mHandler.removeCallbacks(mUpdateProgressRunnable); //取消自动更新进度条
            mIvPlaySwitch.setImageResource(R.drawable.btn_play);
            mMediaPlayer.seekTo(0);
            mProgressBar.setProgress(0);
            mTvCurrentTime.setText(MediaUtils.formatTime(0));
        }
    };

    private MediaPlayer.OnSeekCompleteListener mOnSeekCompletionListener = new MediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(MediaPlayer mediaPlayer) {
            Log.d(TAG, "media player onSeekComplete");
            mHandler.post(mUpdateProgressRunnable); //更新进度
        }
    };

    private MediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
            mBufferPercent = percent;
            Log.d(TAG, "media player onBufferingUpdate percent = " + mBufferPercent);
        }
    };

    private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
            Log.d(TAG, "media player onVideoSizeChanged  ( width = " + width + " , height = " + height + " )" );
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
            String errorWhat;
            switch (what) {
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    errorWhat = "MEDIA_ERROR_UNKNOWN";
                    break;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    errorWhat = "MEDIA_ERROR_SERVER_DIED";
                    break;
                default:
                    errorWhat = "!";
            }

            String errorExtra;
            switch (extra) {
                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                    errorExtra = "MEDIA_ERROR_UNSUPPORTED";
                    break;
                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                    errorExtra = "MEDIA_ERROR_MALFORMED";
                    break;
                case MediaPlayer.MEDIA_ERROR_IO:
                    errorExtra = "MEDIA_ERROR_IO";
                    break;
                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                    errorExtra = "MEDIA_ERROR_TIMED_OUT";
                    break;
                default:
                    errorExtra = "!";
            }

            String msg = String.format("what = %d (%s), extra = %d (%s)",
                    what, errorWhat, extra, errorExtra);

            Log.e(TAG, msg);
            return true;
        }
    };
}
