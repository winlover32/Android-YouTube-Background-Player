/*
 * Copyright (C) 2016 SMedic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smedic.tubtub;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.smedic.tubtub.utils.Config;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Service class for background youtube playback
 * Created by Stevan Medic on 9.3.16..
 */
public class BackgroundAudioService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {

    private static final String TAG = "SMEDIC service";

    private static final int YOUTUBE_ITAG = 140; //mp4a - stereo, 44.1 KHz 128 Kbps

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    private MediaPlayer mMediaPlayer;
    private MediaSessionCompat mSession;
    private MediaControllerCompat mController;

    private int mediaType = Config.YOUTUBE_NO_NEW_REQUEST;

    private YouTubeVideo videoItem;

    private boolean isStarting = false;

    private ArrayList<YouTubeVideo> youTubeVideos;
    private ListIterator<YouTubeVideo> iterator;

    private NotificationCompat.Builder builder = null;

    private boolean nextWasCalled = false;
    private boolean previousWasCalled = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        videoItem = new YouTubeVideo();
        initMediaSessions();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Handles intent (player options play/pause/stop...)
     * @param intent
     */
    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;
        String action = intent.getAction();
        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            handleMedia(intent);
            mController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            mController.getTransportControls().pause();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mController.getTransportControls().skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mController.getTransportControls().skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mController.getTransportControls().stop();
        }
    }

    /**
     * Handles media - playlists and videos sent from fragments
     * @param intent
     */
    private void handleMedia(Intent intent) {

        int intentMediaType = intent.getIntExtra(Config.YOUTUBE_MEDIA_TYPE, Config.YOUTUBE_NO_NEW_REQUEST);
        switch (intentMediaType) {
            case Config.YOUTUBE_NO_NEW_REQUEST: //video is paused,so no new playback requests should be processed
                mMediaPlayer.start();
                break;
            case Config.YOUTUBE_VIDEO:
                mediaType = Config.YOUTUBE_VIDEO;
                videoItem = (YouTubeVideo) intent.getSerializableExtra(Config.YOUTUBE_TYPE_VIDEO);
                if (videoItem.getId() != null) {
                    playVideo();
                }
                break;
            case Config.YOUTUBE_PLAYLIST: //new playlist playback request
                mediaType = Config.YOUTUBE_PLAYLIST;
                youTubeVideos = (ArrayList<YouTubeVideo>) intent.getSerializableExtra(Config.YOUTUBE_TYPE_PLAYLIST);
                if (!youTubeVideos.isEmpty()) {
                    iterator = youTubeVideos.listIterator();
                    playNext();
                } else {
                    Toast.makeText(getApplicationContext(), "Playlist is empty!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Log.d(TAG, "Unknown command");
                break;
        }
    }

    /**
     * Initializes media sessions and receives media events
     */
    private void initMediaSessions() {
        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        //
        // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
        // permission in AndroidManifest.xml.
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mSession = new MediaSessionCompat(getApplicationContext(), "simple player session");

        try {
            mController = new MediaControllerCompat(getApplicationContext(), mSession.getSessionToken());

            mSession.setCallback(
                    new MediaSessionCompat.Callback() {
                        @Override
                        public void onPlay() {
                            super.onPlay();
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onPause() {
                            super.onPause();
                            pauseVideo();
                            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
                        }

                        @Override
                        public void onSkipToNext() {
                            super.onSkipToNext();
                            if (!isStarting) {
                                playNext();
                            }
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onSkipToPrevious() {
                            super.onSkipToPrevious();
                            if (!isStarting) {
                                playPrevious();
                            }
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onStop() {
                            super.onStop();
                            stopPlayer();
                            //remove notification and stop service
                            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.cancel(1);
                            Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
                            stopService(intent);
                        }

                        @Override
                        public void onSetRating(RatingCompat rating) {
                            super.onSetRating(rating);
                        }
                    }
            );
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    /**
     * Builds notification panel with buttons and info on it
     * @param action Action to be applied
     */

    private void buildNotification(NotificationCompat.Action action) {

        final NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();

        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(ACTION_STOP);

        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

        builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(videoItem.getTitle());
        builder.setContentInfo(videoItem.getDuration());
        builder.setShowWhen(false);
        builder.setDeleteIntent(pendingIntent);
        builder.setStyle(style);

        //load bitmap for largeScreen
        if (videoItem.getThumbnailURL() != null && !videoItem.getThumbnailURL().isEmpty()) {
            Picasso.with(this)
                    .load(videoItem.getThumbnailURL())
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                            updateNotificationLargeIcon(bitmap);
                        }

                        @Override
                        public void onBitmapFailed(Drawable drawable) {
                        }

                        @Override
                        public void onPrepareLoad(Drawable drawable) {
                        }
                    });
        }

        if (mediaType == Config.YOUTUBE_PLAYLIST) {
            builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
        } else {
            builder.addAction(0, null, null);
        }

        builder.addAction(action);

        if (mediaType == Config.YOUTUBE_PLAYLIST) {
            builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
        } else {
            builder.addAction(0, null, null);
        }

        style.setShowActionsInCompactView(0, 1, 2);

        //Notification notification = builder.build();
        //startForeground(R.string.app_name, notification);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());

    }

    /**
     * Updates only large icon in notification panel when bitmap is decoded
     * @param bitmap
     */
    private void updateNotificationLargeIcon(Bitmap bitmap) {
        builder.setLargeIcon(bitmap);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Generates specific action with parameters below
     * @param icon
     * @param title
     * @param intentAction
     * @return
     */
    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    /**
     * Plays next video
     */
    private void playNext() {

        if (previousWasCalled) {
            previousWasCalled = false;
            iterator.next ();
        }

        if (!iterator.hasNext()) {
            Log.d(TAG, "playNext NO NEXT. iterator: " + iterator.nextIndex());
            iterator = youTubeVideos.listIterator();
        }else {
            Log.d(TAG, "playNext YES NEXT iterator: " + iterator.nextIndex());
        }

        videoItem = iterator.next();
        nextWasCalled = true;
        playVideo();
    }

    /**
     * Plays previous video
     */
    private void playPrevious() {

        if (nextWasCalled) {
            iterator.previous();
            nextWasCalled = false;
        }

        if (!iterator.hasPrevious()) {
            Log.d(TAG, "playPrevious NO PREVIOUS. iterator: " + iterator.previousIndex());
            iterator = youTubeVideos.listIterator(youTubeVideos.size());
        } else {
            Log.d(TAG, "playPrevious YES PREVIOUS iterator: " + iterator.previousIndex());
        }

        videoItem = iterator.previous();
        previousWasCalled = true;
        playVideo();
    }

    /**
     * Plays video
     */
    private void playVideo() {
        isStarting = true;
        extractUrlAndPlay();
    }

    /**
     * Pauses video
     */
    private void pauseVideo() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    /**
     * Restarts video
     */
    private void restartVideo() {
        mMediaPlayer.start();
    }

    /**
     * Stops video
     */
    private void stopPlayer() {
        mMediaPlayer.stop();
        mMediaPlayer.release();
    }

    /**
     * Extracts link from youtube video ID, so mediaPlayer can play it
     */
    private void extractUrlAndPlay() {
        String youtubeLink = "http://youtube.com/watch?v=" + videoItem.getId();
        YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this) {
            @Override
            public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
                if (ytFiles != null) {
                    String downloadUrl = ytFiles.get(YOUTUBE_ITAG).getUrl();
                    try {
                        Log.d(TAG, "Start playback");
                        if(mMediaPlayer != null) {
                            mMediaPlayer.reset();
                            mMediaPlayer.setDataSource(downloadUrl);
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer.prepare();
                            mMediaPlayer.start();
                        }
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            }
        };
        ytEx.execute(youtubeLink);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
    }

    @Override
    public void onCompletion(MediaPlayer _mediaPlayer) {
        if (mediaType == Config.YOUTUBE_PLAYLIST) {
            playNext();
            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
        } else {
            restartVideo();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isStarting = false;
    }

}