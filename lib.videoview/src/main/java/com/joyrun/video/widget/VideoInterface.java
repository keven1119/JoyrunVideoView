package com.joyrun.video.widget;

import android.content.res.Configuration;
import android.text.TextUtils;

/**
 * Created by keven-liang on 2017/12/4.
 */

public interface VideoInterface {

    /**
     * 播放器初始化
     */
    public void init();

    /**
     * 设置播放时本地地址
     * @param path
     */
    public void setVideoPath(String path);

    /**
     * 获取播放器本地地址
     * @return
     */
    public String getVideoPath();

    /**
     * 设置播放器 url
     * @param videoUrl
     */
    public void setVideoUrl(String videoUrl);

    /**
     * 获取播放器 url
     * @return
     */
    public String getVideoUrl();

    /**
     * 设置封面图片
     * @param url
     */
    public void setCover(String url);

    /**
     * 获取封面地址
     * @return
     */
    public String getCoverPath();

    /**
     * 开始播放
     */
    public void start();

    /**
     * 暂停播放
     */
    public void pause();

    /**
     * 关闭视频
     */
    public void close();

    /**
     * 横竖屏改变
     * @param newConfig
     */
    public void onChanged( Configuration newConfig);

    /**
     * 是否竖屏
     * @return
     */
    public boolean isPortrait();


    public void setStartVideoListener(VideoPlayer.OnStartVideoListener listener);

    public int getCurrentState();

    /**
     * 准备播放器
     */
    public void prepare();
}
