package com.joyrun.video.joyrunvideoview;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.joyrun.video.widget.VideoPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by keven-liang on 2017/12/4.
 */

public class VideoFragment extends Fragment {

//
    String shipin = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";

    String fengmian = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1512133829765&di=aa6cfb825d8c8ed1e401222190a811a2&imgtype=jpg&src=http%3A%2F%2Fimg4.imgtn.bdimg.com%2Fit%2Fu%3D2760457749%2C4161462131%26fm%3D214%26gp%3D0.jpg";


    private List<VideoBean> beanList = new ArrayList<>();

    private VideoPlayer currentVideoPlayer;
    VideoAdapter videoAdapter;
    RecyclerView recyclerView;
    LinearLayoutManager layoutManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View inflate = inflater.inflate(R.layout.video_fragment_layout, null, false);

        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));
        beanList.add(new VideoBean(shipin, fengmian));

        return inflate;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recycler_vide_fragment);
        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        videoAdapter = new VideoAdapter();

        recyclerView.setAdapter(videoAdapter);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);



        if(currentVideoPlayer != null) {
            currentVideoPlayer.onChanged(newConfig);
        }
    }


    class VideoAdapter  extends RecyclerView.Adapter{

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item_layout, parent, false);
            return new VideoHolder(inflate);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            VideoBean videoBean = beanList.get(position);
            ((VideoHolder)holder).setData(videoBean);
            currentVideoPlayer = ((VideoHolder)holder).videoPlayer;
        }

        @Override
        public int getItemCount() {
            return beanList.size();
        }
    }

    class VideoHolder extends RecyclerView.ViewHolder {


        VideoPlayer videoPlayer;

        public VideoHolder(View itemView) {
            super(itemView);
            videoPlayer = itemView.findViewById(R.id.videoplayer_video_item
            );

        }

        public void setData(VideoBean videoBean){
            videoPlayer.setVideoUrl(videoBean.getVideoUrl());
            videoPlayer.init();
            videoPlayer.setCover(videoBean.getFengMianUrl());
        }



    }

    class VideoBean {
        String videoUrl;
        String fengMianUrl;

        public VideoBean(String videoUrl, String fengMianUrl) {
            this.videoUrl = videoUrl;
            this.fengMianUrl = fengMianUrl;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public String getFengMianUrl() {
            return fengMianUrl;
        }

        public void setFengMianUrl(String fengMianUrl) {
            this.fengMianUrl = fengMianUrl;
        }
    }


}
