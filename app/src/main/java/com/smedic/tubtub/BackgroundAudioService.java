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
import com.smedic.tubtub.utils.Utils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import at.huber.youtubeExtractor.YouTubeUriExtractor;
import at.huber.youtubeExtractor.YtFile;

/**
 * Created by Stevan Medic on 9.3.16..
 */
public class BackgroundAudioService extends Service {

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

    ArrayList<YouTubeVideo> youTubeVideos;
    ListIterator<YouTubeVideo> iterator;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer _mediaPlayer) {
                playNext();
            }
        });
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isStarting = false;
            }
        });

        videoItem = new YouTubeVideo();
        initMediaSessions();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null)
            return;

        String action = intent.getAction();

        if (action.equalsIgnoreCase(ACTION_PLAY)) {
            handleMedia(intent);

            mController.getTransportControls().play();
        } else if (action.equalsIgnoreCase(ACTION_PAUSE)) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
            mController.getTransportControls().pause();

        } else if (action.equalsIgnoreCase(ACTION_PREVIOUS)) {
            if (!isStarting) {
                playPrevious();
                mController.getTransportControls().skipToPrevious();
            }
        } else if (action.equalsIgnoreCase(ACTION_NEXT)) {
            if (!isStarting) {
                playNext();
                mController.getTransportControls().skipToNext();
            }
        } else if (action.equalsIgnoreCase(ACTION_STOP)) {
            mController.getTransportControls().stop();
        }
    }

    private void handleMedia(Intent intent) {

        mediaType = intent.getIntExtra(Config.YOUTUBE_MEDIA_TYPE, Config.YOUTUBE_NO_NEW_REQUEST);

        switch (mediaType) {
            case Config.YOUTUBE_NO_NEW_REQUEST: //video is paused,so no new playback requests should be processed
                mMediaPlayer.start();
                break;
            case Config.YOUTUBE_VIDEO:

                YouTubeVideo youTubeVideo = (YouTubeVideo) intent.getSerializableExtra(Config.YOUTUBE_TYPE_VIDEO);
                if (youTubeVideo.getId() != null) {
                    playVideo(youTubeVideo);
                }
                break;
            case Config.YOUTUBE_PLAYLIST: //new playlist playback request

                youTubeVideos = (ArrayList<YouTubeVideo>) intent.getSerializableExtra(Config.YOUTUBE_TYPE_PLAYLIST);
                if (!youTubeVideos.isEmpty()) {
                    Utils.prettyPrintVideos(youTubeVideos);
                    iterator = youTubeVideos.listIterator();
                    playNext();
                } else {
                    Toast.makeText(getApplicationContext(), "Playlist is empty!", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Log.d(TAG, "Unknown media type");
                break;
        }
    }

    private void playNext() {
        if (!iterator.hasNext()) {
            iterator = youTubeVideos.listIterator();
        }
        playVideo(iterator.next());
    }

    private void playPrevious() {
        if (!iterator.hasPrevious()) {
            iterator = youTubeVideos.listIterator(youTubeVideos.size());
        }
        playVideo(iterator.previous());
    }

    private void playVideo(YouTubeVideo video) {
        isStarting = true;
        Utils.prettyPrintVideoItem(video);
        videoItem.setTitle(video.getTitle());
        videoItem.setDuration(video.getDuration());
        videoItem.setThumbnailURL(video.getThumbnailURL());
        extractUrl(video.getId());
    }

    private void extractUrl(String videoId) {
        String youtubeLink = "http://youtube.com/watch?v=" + videoId;
        YouTubeUriExtractor ytEx = new YouTubeUriExtractor(this) {
            @Override
            public void onUrisAvailable(String videoId, String videoTitle, SparseArray<YtFile> ytFiles) {
                if (ytFiles != null) {
                    String downloadUrl = ytFiles.get(YOUTUBE_ITAG).getUrl();
                    try {
                        Log.d(TAG, "Start playback");
                        mMediaPlayer.reset();
                        mMediaPlayer.setDataSource(downloadUrl);
                        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mMediaPlayer.prepare();
                        mMediaPlayer.start();
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            }
        };
        ytEx.execute(youtubeLink);
    }

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
                            buildNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY));
                        }

                        @Override
                        public void onSkipToNext() {
                            super.onSkipToNext();
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onSkipToPrevious() {
                            super.onSkipToPrevious();
                            Log.d(TAG, "onSkipToPrevious");
                            buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE));
                        }

                        @Override
                        public void onStop() {
                            super.onStop();
                            Log.d(TAG, "onStop");
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
                        public void onSetRating(RatingCompat rating) {
                            super.onSetRating(rating);
                        }
                    }
            );
        } catch (RemoteException re) {
            re.printStackTrace();
        }
    }

    private void buildNotification(NotificationCompat.Action action) {

        NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();

        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(ACTION_STOP);

        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.icon);
        builder.setContentTitle(videoItem.getTitle());
        builder.setContentInfo(videoItem.getDuration());
        builder.setShowWhen(false);
        builder.setDeleteIntent(pendingIntent);
        builder.setStyle(style);

        if (videoItem.getThumbnailURL() != null && !videoItem.getThumbnailURL().isEmpty()) {
            Picasso.with(this)
                    .load(videoItem.getThumbnailURL())
                    .into(new Target() {
                        @Override
                        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                            builder.setLargeIcon(bitmap);
                        }

                        @Override
                        public void onBitmapFailed(Drawable drawable) {
                        }

                        @Override
                        public void onPrepareLoad(Drawable drawable) {
                        }
                    });
        }

        if(mediaType == Config.YOUTUBE_PLAYLIST) {
            Log.d(TAG, "PLAYLIST ?!?!?");
            builder.addAction(generateAction(android.R.drawable.ic_media_previous, "Previous", ACTION_PREVIOUS));
        } else {
            Log.d(TAG, "PLAYLIST ?!?!? NOT?");
            builder.addAction(0, null, null);
        }

        builder.addAction(action);

        if(mediaType == Config.YOUTUBE_PLAYLIST) {
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


    private NotificationCompat.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), BackgroundAudioService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new NotificationCompat.Action.Builder(icon, title, pendingIntent).build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(1);
    }
}