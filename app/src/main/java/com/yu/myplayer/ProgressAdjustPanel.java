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
import android.widget.TextView;

/**
 * Created by Administrator on 2017-11-7.
 * 调节进度显示
 */

public class ProgressAdjustPanel extends FrameLayout {

    private static final String TAG = "AdjustPanel";
    private ImageView mIvPanel_icon;
    private TextView mTvProgress;
    private TextView mTvTotal;

    public ProgressAdjustPanel(@NonNull Context context) {
        super(context);
        initLayout(context);
    }

    public ProgressAdjustPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout(context);
    }

    public ProgressAdjustPanel(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout(context);
    }

    private void initLayout(Context context) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View panelRootView = inflater.inflate(R.layout.layout_progress_adjust_panel, this); //用 null 不显示
        mIvPanel_icon = (ImageView) panelRootView.findViewById(R.id.adjust_icon);
        mTvProgress = (TextView) panelRootView.findViewById(R.id.curr_progress);
        mTvTotal = (TextView) panelRootView.findViewById(R.id.total_duration);
        hidePanel();
    }

    //快进
    public void adjustForward(int current, int total){
        adjustProgress(R.drawable.quick_force,current,total);
    }

    //快退
    public void adjustBackward(int current, int total){
        adjustProgress(R.drawable.quick_back,current,total);
    }

    private void adjustProgress(int resId, int current, int total) {
        mIvPanel_icon.setImageResource(resId);
        mTvProgress.setText(MediaUtils.formatTime(current));
        mTvTotal.setText(MediaUtils.formatTime(total));
        setVisibility(VISIBLE);
    }

    public void hidePanel(){
        setVisibility(GONE);
    }

}
