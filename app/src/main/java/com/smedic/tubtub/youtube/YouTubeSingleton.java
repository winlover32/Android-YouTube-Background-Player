package com.smedic.tubtub.youtube;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.smedic.tubtub.R;
import com.smedic.tubtub.YTApplication;

import java.io.IOException;

/**
 * Created by smedic on 5.3.17..
 */
public class YouTubeSingleton {

    private static YouTube youTube;

    private static YouTubeSingleton ourInstance = new YouTubeSingleton();

    public static YouTubeSingleton getInstance() {
        return ourInstance;
    }

    private YouTubeSingleton() {
        youTube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {

            }
        }).setApplicationName(YTApplication.getAppContext().getString(R.string.app_name)).build();
    }

    public YouTube getYouTube() {
        return youTube;
    }
}
