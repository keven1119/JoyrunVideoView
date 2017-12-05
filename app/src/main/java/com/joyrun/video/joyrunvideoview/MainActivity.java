package com.joyrun.video.joyrunvideoview;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.joyrun.video.widget.VideoPlayer;

public class MainActivity extends Activity {

    VideoPlayer videoPlayer;

    String shipin = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

    String fengmian = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1512133829765&di=aa6cfb825d8c8ed1e401222190a811a2&imgtype=jpg&src=http%3A%2F%2Fimg4.imgtn.bdimg.com%2Fit%2Fu%3D2760457749%2C4161462131%26fm%3D214%26gp%3D0.jpg";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoPlayer = (VideoPlayer) findViewById(R.id.videoplayer);
        videoPlayer.init();
        videoPlayer.setVideoUrl(shipin);
        videoPlayer.setCover(fengmian);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        videoPlayer.onChanged(newConfig);
    }
}
