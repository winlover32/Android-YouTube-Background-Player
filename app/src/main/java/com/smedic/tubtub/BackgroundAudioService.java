package com.smedic.tubtub;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.util.SparseArray;

import com.smedic.tubtub.utils.Config;

import java.io.IOException;

import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Created by Stevan Medic on 9.3.16..
 */
public class BackgroundAudioService extends Service implements MediaPlayer.OnCompletionListener {

    private static final String TAG = "SMEDIC service";

    private static final int YOUTUBE_ITAG = 140; //mp4a - stereo, 44.1 KHz 128 Kbps

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_REWIND = "action_rewind";
    public static final String ACTION_FAST_FORWARD = "action_fast_foward";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";

    private MediaPlayer mMediaPlayer;
    private MediaSession mSession;
    private MediaController mController;

    private boolean isPaused = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mMediaPlayer = new MediaPlayer();
        initMediaSessions();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;

        String action = intent.getAction();

        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            Log.d(TAG, "ACTION PLAY!");
            handleMedia(intent);

            mController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            Log.d(TAG, "Action pause");

            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                isPaused = true;
            }
            mController.getTransportControls().pause();
        } else if (action.equalsIgnoreCase(ACTION_FAST_FORWARD)) {
            mController.getTransportControls().fastForward();
        } else if (action.equalsIgnoreCase(ACTION_REWIND)) {
            mController.getTransportControls().rewind();
        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            mController.getTransportControls().skipToPrevious();
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            mController.getTransportControls().skipToNext();
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mController.getTransportControls().stop();
        }
    }

    private void handleMedia(Intent intent) {

        int mediaType = intent.getIntExtra("YT_MEDIA_TYPE", Config.YOUTUBE_NO_NEW_REQUEST);

        switch (mediaType) {
            case Config.YOUTUBE_NO_NEW_REQUEST: //video is paused,so no new playback requests should be processed
                mMediaPlayer.start();
                isPaused = false;
                break;
            case Config.YOUTUBE_VIDEO:
                String url = intent.getStringExtra("YT_VIDEO"); //new video playback request
                extractUrl(url);
                break;
            case Config.YOUTUBE_PLAYLIST: //new playlist playback request
                //TODO
                break;
            default:
                Log.d(TAG, "Unknown media type");
                break;
        }
    }

    private void initMediaSessions() {
        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        //
        // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
        // permission in AndroidManifest.xml.
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mSession = new MediaSession(getApplicationContext(), "simple player session");
        mController = new MediaController(getApplicationContext(), mSession.getSessionToken());

        mSession.setCallback(
                new MediaSession.Callback() {
                    @Override
                    public void onPlay() {
                        super.onPlay();
                        Log.e(TAG, "onPlay");
                        buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                    }

                    @Override
                    public void onPause() {
                        super.onPause();
                        Log.e(TAG, "onPause");
                        buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
                    }

                    @Override
                    public void onSkipToNext() {
                        super.onSkipToNext();
                        Log.e(TAG, "onSkipToNext");
                        //Change media here
                        buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                    }

                    @Override
                    public void onSkipToPrevious() {
                        super.onSkipToPrevious();
                        Log.e(TAG, "onSkipToPrevious");
                        //Change media here
                        buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                    }

                    @Override
                    public void onFastForward() {
                        super.onFastForward();
                        Log.e(TAG, "onFastForward");
                        //Manipulate current media here
                    }

                    @Override
                    public void onRewind() {
                        super.onRewind();
                        Log.e(TAG, "onRewind");
                        //Manipulate current media here
                    }

                    @Override
                    public void onStop() {
                        super.onStop();
                        Log.e(TAG, "onStop");
                        //Stop media player here
                        mMediaPlayer.stop();
                        mMediaPlayer.release();
                        //remove notification and stop service
                        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.cancel(1);
                        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
                        stopService(intent);
                    }

                    @Override
                    public void onSeekTo(long pos) {
                        super.onSeekTo(pos);
                    }

                    @Override
                    public void onSetRating(Rating rating) {
                        super.onSetRating(rating);
                    }
                }
        );
    }

    private void buildNotification(Notification.Action action) {
        Notification.MediaStyle style = new Notification.MediaStyle();

        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Media Title")
                .setContentText("Media Artist")
                .setDeleteIntent(pendingIntent)
                .setStyle(style);

        builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
        //builder.addAction(generateAction(android.R.drawable.ic_media_rew, "Rewind", ACTION_REWIND));
        builder.addAction(action);
        //builder.addAction(generateAction(android.R.drawable.ic_media_ff, "Fast Forward", ACTION_FAST_FORWARD));
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
        style.setShowActionsInCompactView(0, 1, 2, 3, 4);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private Notification.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
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

                    String downloadUrl = ytFiles.get(YOUTUBE_ITAG).getUrl();
                    Log.d(TAG, "URI AVAILABLE: " + downloadUrl);
                    try {
                        if(mMediaPlayer.isPlaying()) {
                            Log.d(TAG, "Stop playback");
                            mMediaPlayer.reset();
                        }
                        if (!mMediaPlayer.isPlaying()) {
                            Log.d(TAG, "Start playback");
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
    public boolean onUnbind(Intent intent) {
        mSession.release();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.release();
    }

}