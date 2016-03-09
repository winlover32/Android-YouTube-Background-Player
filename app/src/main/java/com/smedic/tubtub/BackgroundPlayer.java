package com.smedic.tubtub;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import com.smedic.tubtub.utils.Config;

import java.io.IOException;

import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Created by Stevan Medic on 9.3.16..
 */
public class BackgroundPlayer extends Service implements MediaPlayer.OnCompletionListener {
    private static final String TAG = "SMEDIC service";
    MediaPlayer mediaPlayer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mediaPlayer = new MediaPlayer(); //MediaPlayer.create(this);// raw/s.mp3
        mediaPlayer.setOnCompletionListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int mediaType = intent.getIntExtra("YT_MEDIA_TYPE", 0);

        switch(mediaType) {
            case Config.YOUTUBE_VIDEO:
                String url = intent.getStringExtra("YT_VIDEO");
                extractUrl(url);
                break;
            case Config.YOUTUBE_PLAYLIST:
                //TODO
                break;
            default:
                Log.d(TAG, "Unknown media type");
                break;
        }

        return START_STICKY;
    }

    public void onDestroy() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
    }

    public void onCompletion(MediaPlayer _mediaPlayer) {
        stopSelf();
    }

    private void extractUrl(String videoId) {
        String youtubeLink = "http://youtube.com/watch?v=" + videoId;
        YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this) {
            @Override
            public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
                if (ytFiles != null) {
                    final int itag = 18;
                    String downloadUrl = ytFiles.get(itag).getUrl();
                    Log.d(TAG, "URI AVAILABLE: " + downloadUrl);
                    //videoView.setVideoPath(downloadUrl);
                    //videoView.start();

                    try {
                        if (!mediaPlayer.isPlaying()) {
                            Log.d(TAG, "Playback start");
                            mediaPlayer.setDataSource(downloadUrl);
                            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            Log.d(TAG, "Playback started");
                        }
                    }catch(IOException io) {
                        io.printStackTrace();
                    }
                }
            }
        };
        ytEx.execute(youtubeLink);
    }

}