package com.joyrun.video.joyrunvideoview;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by keven-liang on 2017/12/4.
 */

public class VideoActivity extends Activity{

  @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_video);

      FragmentManager fragmentManager = getFragmentManager();
      FragmentTransaction fragmentTransaction =
              fragmentManager.beginTransaction();

      FragmentTransaction add = fragmentTransaction.replace(R.id.fragment_container, new VideoFragment());
      add.commit();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

  }
}
