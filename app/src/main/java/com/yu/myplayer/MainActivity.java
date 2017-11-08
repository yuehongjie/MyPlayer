package com.yu.myplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //private static final String VIDEO_URL = "http://gslb.miaopai.com/stream/JcgCcPco3LYy3z7eQKDei2izkff3R9DO-OPWNw__.mp4";
    private static final String VIDEO_URL = "https://f.us.sinaimg.cn/0015B96Vlx07fB6EZV9K010401013MW70k01.mp4?label=mp4_hd&template=28&Expires=1510143898&ssig=inpMOoK7BB&KID=unistore,video";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_play).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_play:
                SimplePlayerActivity.start(MainActivity.this, VIDEO_URL);
                break;
        }
    }
}
