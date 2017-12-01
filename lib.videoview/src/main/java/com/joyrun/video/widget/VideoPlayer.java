package com.joyrun.video.widget;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.joyrun.video.R;
import com.joyrun.video.utils.ImageLoader;

import java.io.IOException;

import static android.media.MediaPlayer.MEDIA_ERROR_TIMED_OUT;

public class VideoPlayer extends FrameLayout implements View.OnClickListener {

    private final static String TAG = VideoPlayer.class.getSimpleName();
    private static final int UPDATE_PROGRESS = 0x1;
    private static final int HIDE_CONTROLLER = 0x2;

    public static final int STATE_ERROR = -1;/**/
    public static final int STATE_IDLE = 0;
    public static final int STATE_LOADING = 1;
    public static final int STATE_PAUSE = 2;
    public static final int STATE_PLAYING = 3;
    public static final int STATE_PREPARE = 4;

    private Handler mHandler;
    private boolean mPortrait = true;
    private View mRoot, mCover;
    private ImageView mCoverImg;
    private SeekBar mSeekBar_progress;
    private ImageView mImageView_volume, mImageView_expend, mImageView_mute, mImageView_resize, mImageView_play, mImageView_pause, mImageview_back;
    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;
    private LinearLayout mLayout_video;
    private TextView mTextView_currentTime, mTextView_totalTime;
    private FrameLayout mFrameLayout_controller;
    private Activity mActivity;
    private int mHeight, mWidth;
    private String mPath;
    private ProgressBar mProgressBar_loading;
    private SurfaceHolder mSurfaceHolder;
    private AudioManager audioManager = null; // 音频
    private int mCurrentState = STATE_IDLE;
    private String mCoverPath;
    private String mUrl;
    private boolean isPlaying;
    private OnStartVideoListener mStartVideoListener;
    private int mProgress;

    public VideoPlayer(Context context) {
        this(context, null, 0);
    }

    public VideoPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mActivity = (Activity) context;
        audioManager = (AudioManager) mActivity.getSystemService(Service.AUDIO_SERVICE);
        mRoot = inflate(context, R.layout.layout_video, this);
        mSeekBar_progress = (SeekBar) mRoot.findViewById(R.id.video_progress);
        mImageView_expend = (ImageView) mRoot.findViewById(R.id.imageview_expend);
        mImageView_volume = (ImageView) mRoot.findViewById(R.id.imageview_volume);
        mImageView_play = (ImageView) mRoot.findViewById(R.id.imageview_play);
        mImageView_pause = (ImageView) mRoot.findViewById(R.id.imageview_pause);
        mCoverImg = (ImageView) mRoot.findViewById(R.id.imageview_cover);
        mImageView_mute = (ImageView) mRoot.findViewById(R.id.imageview_mute);
        mImageView_resize = (ImageView) mRoot.findViewById(R.id.imageview_resize);
        mImageview_back = (ImageView) mRoot.findViewById(R.id.imageview_back);
        mSurfaceView = (SurfaceView) mRoot.findViewById(R.id.surface);
        mProgressBar_loading = (ProgressBar) mRoot.findViewById(R.id.progress_loading);
        mSurfaceView.getHolder().setKeepScreenOn(true);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mSurfaceHolder = holder;
                if (mMediaPlayer != null) {
                    mMediaPlayer.setDisplay(holder);
                    if (isInPlaybackState()) {
                        if (isPlaying) {
                            mMediaPlayer.start();
                        } else {
                            mMediaPlayer.seekTo(mProgress);
                        }
                    }
                } else {
                    initMediaPlayer(holder);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mMediaPlayer != null && isInPlaybackState()) {
                    isPlaying = mCurrentState == STATE_PLAYING;
                    mMediaPlayer.pause();
                    mProgress = mSeekBar_progress.getProgress();
                }
                mSurfaceHolder = null;
            }
        });
        mLayout_video = (LinearLayout) mRoot.findViewById(R.id.layout_video);
        mCover = mRoot.findViewById(R.id.view_video);
        mTextView_currentTime = (TextView) mRoot.findViewById(R.id.tv_video_currenttime);
        mTextView_totalTime = (TextView) mRoot.findViewById(R.id.tv_video_totaltime);
        mFrameLayout_controller = (FrameLayout) mRoot.findViewById(R.id.layout_controller);
        mSeekBar_progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

                mHandler.removeMessages(UPDATE_PROGRESS);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null) {
                    mMediaPlayer.seekTo(mSeekBar_progress.getProgress());
                }
                mHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 1000);
            }
        });

        mLayout_video.setOnClickListener(this);
        mCover.setOnClickListener(this);
        mImageView_volume.setOnClickListener(this);
        mImageView_expend.setOnClickListener(this);
        mImageView_pause.setOnClickListener(this);
        mImageView_play.setOnClickListener(this);
        mImageView_mute.setOnClickListener(this);
        mImageview_back.setOnClickListener(this);
        mImageView_volume.setOnClickListener(this);
        mImageView_resize.setOnClickListener(this);
        init();
    }

    public void init() {
        if (mCoverPath != null) {
            setCover(mCoverPath);
        }
        mSeekBar_progress.setMax(0);
        mSeekBar_progress.setProgress(0);
        mSeekBar_progress.setSecondaryProgress(0);
        mCurrentState = STATE_IDLE;
        updateView();
        mTextView_currentTime.setText(dataFormat(0));
        mTextView_totalTime.setText(dataFormat(0));
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == UPDATE_PROGRESS) {
                    mSeekBar_progress.setProgress(mMediaPlayer.getCurrentPosition());
                    mHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 1000);
                    mTextView_currentTime.setText(dataFormat((int) (mMediaPlayer.getCurrentPosition() + 500)));
                } else if (msg.what == HIDE_CONTROLLER) {
                    mLayout_video.setVisibility(GONE);
                    mCover.setVisibility(VISIBLE);
                    mImageView_play.setVisibility(GONE);
                    mImageView_pause.setVisibility(GONE);
                }
            }
        };
        if (mSurfaceHolder != null)
            initMediaPlayer(mSurfaceHolder);

    }

    private float mLastY, mLastX;
    private int mTouchSlop = 10;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mCurrentState == STATE_LOADING)
            return true;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getY();
                mLastX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                float absX, absY;
                absX = Math.abs(ev.getX() - mLastX);
                absY = Math.abs(ev.getY() - mLastY);
                if (absY > absX && absY > mTouchSlop) {
                    requestDisallowInterceptTouchEvent(true);
                    return true;
                }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isInPlaybackState() || mPortrait)
            return false;
        if (event.getAction() == MotionEvent.ACTION_MOVE && isInPlaybackState()) {
            float diffY = event.getY() - mLastY;
            if (Math.abs(diffY) > mTouchSlop) {
                if (diffY > 0) {
                    //降低音量
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER, AudioManager.FLAG_PLAY_SOUND
                                    | AudioManager.FLAG_SHOW_UI);
                } else {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND
                                    | AudioManager.FLAG_SHOW_UI);
                }
                mLastY = event.getY();
            }
        }
        return true;
    }

    private void initMediaPlayer(SurfaceHolder holder) {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer iMediaPlayer) {
                mCurrentState = STATE_PREPARE;
                initText();
                start();
            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer iMediaPlayer) {
                if(!isInPlaybackState())
                    return;
                mCurrentState = STATE_PREPARE;
                updateView();
                if (mHandler != null) {
                    mHandler.removeMessages(UPDATE_PROGRESS);
                    mHandler.removeMessages(HIDE_CONTROLLER);
                }
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer iMediaPlayer, int i, int i1) {
                mCurrentState = STATE_ERROR;
                if(i == MEDIA_ERROR_TIMED_OUT){
                    Toast.makeText(getContext(),"网络超时..",Toast.LENGTH_LONG).show();
                }
                reset();
                return false;
            }
        });
        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer iMediaPlayer) {
                if (mCurrentState == STATE_LOADING) {
                    start();
                }
            }
        });
        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer iMediaPlayer, int i) {
                int p = (int) (mSeekBar_progress.getMax() * (i / 100f));
                if (p > mSeekBar_progress.getMax()) {
                    p = mSeekBar_progress.getMax();
                }
                mSeekBar_progress.setSecondaryProgress(p);
            }
        });
        mMediaPlayer.setDisplay(holder);
    }

    private void initText() {
        if (mMediaPlayer == null)
            return;
        mSeekBar_progress.setMax(mMediaPlayer.getDuration());
        mSeekBar_progress.setProgress(0);
        if (mPath != null) {
            mSeekBar_progress.setSecondaryProgress(mMediaPlayer.getDuration());
        }
        mMediaPlayer.seekTo(0);
        mTextView_currentTime.setText(dataFormat(0));
        mTextView_totalTime.setText(dataFormat((int) (mMediaPlayer.getDuration() + 500)));
    }

    //是否在可操作状态
    public boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_LOADING);
    }

    //输出16：9
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = (int) ((widthSize * 9 / 16) + 0.5);
        if (mWidth == 0 && mHeight == 0) {
            mWidth = widthSize;
            mHeight = heightSize;
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
    }

    public String dataFormat(int time) {
        int totalSeconds = (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes,
                seconds) : String.format("%02d:%02d", minutes, seconds);
    }

    public void setVideoPath(String path) {
        if (!TextUtils.isEmpty(path)) {
            mPath = path;
        }
    }

    public String getVideoPath(){
        return mPath;
    }


    public void setVideoUrl(String videoUrl) {
        if (!TextUtils.isEmpty(videoUrl)) {
            mUrl = videoUrl;
        }
    }

    public String getVideoUrl(){
        return mUrl;
    }

    private void start() {
        if (!isInPlaybackState())
            return;
        mMediaPlayer.start();
        if (mMediaPlayer != null)
            mHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS, 1000);
        mCurrentState = STATE_PLAYING;
        updateView();
    }

    private MaterialDialog createDialog() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(mActivity);
        MaterialDialog dialog = builder.content(mActivity.getString(R.string.video_download_no_wifi))
                .positiveText(mActivity.getString(R.string.video_download_no_wifi_continue))
                .negativeText(mActivity.getString(R.string.video_download_no_wifi_cancel))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        prepare();
                        if (mStartVideoListener != null) {
                            mStartVideoListener.onStart();
                        }
                    }
                }).build();

        return dialog;
    }

    public void prepare() {
        try {
            String uri = mPath == null ? mUrl : mPath;
            mMediaPlayer.setDataSource(uri);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_LOADING;
            updateView();
        } catch (IOException e) {

        }
    }

    public void setCover(String url) {
        mCoverPath = url;
        if(!TextUtils.isEmpty(mCoverPath)) {
            try {
                int  width =View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
                measure(width,width);
                int measuredWidth = getMeasuredWidth();
                int heightSize = (int) ((measuredWidth * 9 / 16) + 0.5);

                Log.d(TAG,"mWidth ==>"+ measuredWidth + "    mHeight==>"+ heightSize);
                ImageLoader.build(mActivity).bindBitmap(mCoverPath, mCoverImg, measuredWidth, heightSize);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public String getCoverPath() {
        return mCoverPath;
    }

    public void reset(){
        if(mMediaPlayer==null)
            return;
        mCurrentState = STATE_IDLE;
        updateView();
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        if (mHandler != null) {
            mHandler.removeMessages(UPDATE_PROGRESS);
            mHandler.removeMessages(HIDE_CONTROLLER);
        }
        mMediaPlayer.reset();

    }

    public void pause() {
        if (!isInPlaybackState()) {
            return;
        }
        if (mHandler != null) {
            mHandler.removeMessages(UPDATE_PROGRESS);
            mCurrentState = STATE_PAUSE;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            updateView();
        }
    }

    public void close() {
        if (mHandler != null) {
            mHandler.removeMessages(UPDATE_PROGRESS);
            mHandler.removeMessages(HIDE_CONTROLLER);
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private boolean isWifiConnected() {
        ConnectivityManager connectivity = (ConnectivityManager) mActivity
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.layout_video) {//隐藏控制按钮
            mLayout_video.setVisibility(GONE);
            mCover.setVisibility(VISIBLE);
            mImageView_play.setVisibility(GONE);
            mImageView_pause.setVisibility(GONE);

        } else if (i == R.id.view_video) {//显示控制按钮
            mLayout_video.setVisibility(VISIBLE);
            mCover.setVisibility(GONE);
            if (mCurrentState == STATE_PLAYING) {
                mImageView_pause.setVisibility(VISIBLE);
            } else {
                mImageView_play.setVisibility(VISIBLE);
            }

        } else if (i == R.id.imageview_pause) {
            pause();

        } else if (i == R.id.imageview_play) {
            if (mCurrentState == STATE_IDLE) {
                if (!isWifiConnected() && mPath == null) {
                    createDialog().show();
                    return;
                }
                prepare();
                if (mStartVideoListener != null) {
                    mStartVideoListener.onStart();
                }
            } else {
                start();
                mImageView_pause.setVisibility(VISIBLE);
            }
        } else if (i == R.id.imageview_mute) {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
            mImageView_volume.setVisibility(VISIBLE);
            mImageView_mute.setVisibility(GONE);

        } else if (i == R.id.imageview_volume) {
            audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            mImageView_mute.setVisibility(VISIBLE);
            mImageView_volume.setVisibility(GONE);

        } else if (i == R.id.imageview_expend) {
            if (mActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

        } else if (i == R.id.imageview_resize) {
            if (mActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else if (i == R.id.imageview_back) {
            if (mActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }
        mHandler.removeMessages(HIDE_CONTROLLER);
        mHandler.sendEmptyMessageDelayed(HIDE_CONTROLLER, 3000);
    }

    public void onChanged(final Configuration newConfig) {
        mPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        doOnConfigurationChanged(mPortrait);
    }

    public boolean isPortrait() {
        return mPortrait;
    }

    /**
     * 当竖横屏切换时处理视频窗口
     *
     * @param portrait
     */
    private void doOnConfigurationChanged(final boolean portrait) {
        if (mMediaPlayer != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setFullScreen(!portrait);
                    if (portrait) {
                        ViewGroup.LayoutParams layoutParams = getLayoutParams();
                        mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                        layoutParams.width = mWidth;
                        layoutParams.height = mHeight;
                        setLayoutParams(layoutParams);
                        requestLayout();
                    } else {
                        int heightPixels = mActivity.getResources().getDisplayMetrics().heightPixels;
                        int widthPixels = mActivity.getResources().getDisplayMetrics().widthPixels;
                        ViewGroup.LayoutParams layoutParams = getLayoutParams();
                        layoutParams.height = heightPixels;
                        layoutParams.width = widthPixels;
                        setLayoutParams(layoutParams);
                    }
                    updateScreenButton();
                }
            });
        }
    }

    private void updateScreenButton() {
        if (mPortrait) {
            mImageView_resize.setVisibility(GONE);
            mImageView_expend.setVisibility(VISIBLE);
            mImageView_play.setImageResource(R.drawable.video_play);
            mImageView_pause.setImageResource(R.drawable.video_pause);
            mImageView_volume.setImageResource(R.drawable.ico_volume);
            mImageView_mute.setImageResource(R.drawable.ico_mute);
            mImageview_back.setVisibility(GONE);
        } else {
            mImageView_resize.setVisibility(VISIBLE);
            mImageView_expend.setVisibility(GONE);
            mImageView_play.setImageResource(R.drawable.play_fullscreen);
            mImageView_pause.setImageResource(R.drawable.pause_fullscreen);
            mImageView_volume.setImageResource(R.drawable.ico_volume_fullscreen);
            mImageView_mute.setImageResource(R.drawable.ico_mute_fullscreen);
            mImageview_back.setVisibility(VISIBLE);
        }
    }

    private void setFullScreen(boolean fullScreen) {
        if (mActivity != null) {
            WindowManager.LayoutParams attrs = mActivity.getWindow()
                    .getAttributes();
            if (fullScreen) {
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                mActivity.getWindow().setAttributes(attrs);
                mActivity.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            } else {
                attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                mActivity.getWindow().setAttributes(attrs);
                mActivity.getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
        }

    }

    private void initControllerView() {
        mImageView_play.setVisibility(GONE);
        mImageView_pause.setVisibility(GONE);
        mProgressBar_loading.setVisibility(GONE);
        mFrameLayout_controller.setVisibility(VISIBLE);
        mCoverImg.setVisibility(VISIBLE);
    }

    private void updateView() {
        switch (mCurrentState) {
            case STATE_IDLE:
                //停止状态
                initControllerView();
                mFrameLayout_controller.setVisibility(INVISIBLE);
                mImageView_play.setVisibility(VISIBLE);
                mImageView_mute.setVisibility(GONE);
                mImageView_volume.setVisibility(VISIBLE);
                break;
            case STATE_LOADING:
                //加载中
                initControllerView();
                mProgressBar_loading.setVisibility(VISIBLE);
                break;
            case STATE_PREPARE:
                //准备中
                initControllerView();
                initText();
                mImageView_play.setVisibility(VISIBLE);
                break;
            case STATE_PAUSE:
                //暂停
                initControllerView();
                mImageView_play.setVisibility(VISIBLE);
                mCoverImg.setVisibility(GONE);
                break;
            case STATE_PLAYING:
                //播放
                initControllerView();
                mImageView_pause.setVisibility(VISIBLE);
                mCoverImg.setVisibility(GONE);
        }
    }

    public void setStartVideoListener(OnStartVideoListener listener) {
        mStartVideoListener = listener;
    }

    public int getCurrentState() {
        return mCurrentState;
    }


    public interface OnStartVideoListener {
        void onStart();
    }
}