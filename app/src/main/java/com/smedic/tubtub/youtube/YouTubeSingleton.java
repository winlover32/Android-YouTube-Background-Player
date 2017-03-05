package com.smedic.tubtub.youtube;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.smedic.tubtub.R;
import com.smedic.tubtub.YTApplication;

import java.util.Arrays;

import static com.smedic.tubtub.utils.Auth.SCOPES;

/**
 * Created by smedic on 5.3.17..
 */
public class YouTubeSingleton {

    private static YouTube youTube;
    private static GoogleAccountCredential credential;

    private static YouTubeSingleton ourInstance = new YouTubeSingleton();

    public static YouTubeSingleton getInstance() {
        return ourInstance;
    }

    private YouTubeSingleton() {

        credential = GoogleAccountCredential.usingOAuth2(
                YTApplication.getAppContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        youTube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                .setApplicationName(YTApplication.getAppContext().getString(R.string.app_name))
                .build();
    }

    public static YouTube getYouTube() {
        return youTube;
    }

    public static GoogleAccountCredential getCredential() {
        return credential;
    }
}
