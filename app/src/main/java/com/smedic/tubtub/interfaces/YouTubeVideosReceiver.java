package com.smedic.tubtub.interfaces;

import com.smedic.tubtub.YouTubeVideo;

import java.util.ArrayList;

/**
 * Created by Stevan Medic on 10.3.16..
 */
public interface YouTubeVideosReceiver {

    void receive(ArrayList<YouTubeVideo> youTubeVideos);

}