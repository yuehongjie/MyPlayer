package com.yu.myplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String VIDEO_URL = "http://gslb.miaopai.com/stream/JcgCcPco3LYy3z7eQKDei2izkff3R9DO-OPWNw__.mp4";

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
