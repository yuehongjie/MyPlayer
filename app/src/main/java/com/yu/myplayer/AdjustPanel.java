package com.yu.myplayer;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * Created by Administrator on 2017-11-7.
 */

public class AdjustPanel extends FrameLayout {

    private static final String TAG = "AdjustPanel";
    private ImageView mIvPanel_icon;
    private ProgressBar mPbProgress;

    public AdjustPanel(@NonNull Context context) {
        super(context);
        initLayout(context);
    }

    public AdjustPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public AdjustPanel(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    private void initLayout(Context context) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View panelRootView = inflater.inflate(R.layout.layout_adjust_panel, this);//用 null 不显示
        mIvPanel_icon = (ImageView) panelRootView.findViewById(R.id.iv_panel_icon);
        mPbProgress = (ProgressBar) panelRootView.findViewById(R.id.pb_progress);
        hidePanel();
    }

    //调节音量 （0 ~ 1）
    public void adjustVolume(float volumePercent){
        adjust(R.drawable.ic_volume, volumePercent);
    }

    //调节亮度 （0 ~ 1）
    public void adjustBrightness(float brightnessPercent){
        adjust(R.drawable.ic_brightness, brightnessPercent);
    }

    private void adjust(int resId, float percent) {
        //Log.d(TAG, "adjust panel: " + percent);
        mIvPanel_icon.setImageResource(resId);
        mPbProgress.setProgress((int) (percent * 100));
        setVisibility(VISIBLE);
    }

    public void hidePanel(){
        //Log.d(TAG, "hide adjust panel");
        setVisibility(GONE);
    }

}
