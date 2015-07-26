package com.imber.spotifystreamer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.imber.spotifystreamer.adapters.ArtistViewAdapter;
import com.imber.spotifystreamer.adapters.TrackViewAdapter;

import java.io.IOException;
import java.util.ArrayList;

public class SSService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private final String LOG_TAG = getClass().getSimpleName();

    private SSBinder mBinder = new SSBinder();
    private MediaPlayer mPlayer = new MediaPlayer();
    private ArrayList<TrackViewAdapter.TrackData> mTrackData;
    private int mTrackPosition;
    private SSReceiver mReceiver;
    private NotificationManager mNotificationManager;
    private boolean mPlayerStarted = false;
    Util.Listeners.OnPlaybackCompletedListener mPlaybackCompletedListener;
    Util.Listeners.OnNotificationPreviousClickListener mNotificationPreviousListener;

    private String mQuery;
    private ArtistViewAdapter.ArtistData mArtistData;

    public static final int NOTIFICATION_ID = 1;
    private final int TRACK_LENGTH_SKIP_LENGTH_MS = 3000;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPlayer.release();
        mPlayer = null;
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        play();
        mPlayerStarted = true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        skipForward(mTrackPosition + 1);
        if (mPlaybackCompletedListener != null) {
            mPlaybackCompletedListener.onPlaybackCompleted();
        }
    }

    public void setupAndStartPlayer(ArrayList<TrackViewAdapter.TrackData> trackData, int trackPosition) {
        // receiver for notifications
        mReceiver = new SSReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Util.ACTION_PLAY);
        filter.addAction(Util.ACTION_PAUSE);
        filter.addAction(Util.ACTION_PREVIOUS);
        filter.addAction(Util.ACTION_NEXT);
        registerReceiver(mReceiver, filter);

        // setting up and starting mPlayer
        mTrackData = trackData;
        mTrackPosition = trackPosition;
        mPlayer.reset();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        String trackUrl = mTrackData.get(mTrackPosition).trackUrl;
        try {
            mPlayer.setDataSource(trackUrl);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to get track at " + trackUrl);
        }
        mPlayer.prepareAsync();
    }

    private void showNotification(boolean willPlay) {
        // contentIntent is the main intent to launch when notification is clicked
        PendingIntent contentIntent;
        Intent resultIntent;
        // set up intents for large layouts
        if (getResources().getBoolean(R.bool.large_layout)) {
            resultIntent = new Intent(this, ArtistActivity.class)
                    .putExtra(getString(R.string.query_text_label), mQuery)
                    .putExtra(getString(R.string.artist_data_label), mArtistData)
                    .putParcelableArrayListExtra(getString(R.string.track_data_label), mTrackData)
                    .putExtra(getString(R.string.track_position_label), mTrackPosition);
            TaskStackBuilder builder = TaskStackBuilder.create(this);
            builder.addParentStack(ArtistActivity.class);
            builder.addNextIntent(resultIntent);
            contentIntent = builder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            resultIntent = new Intent(this, Player.class)
                    .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putParcelableArrayListExtra(getString(R.string.track_data_label), mTrackData)
                    .putExtra(getString(R.string.track_position_label), mTrackPosition);
            TaskStackBuilder builder = TaskStackBuilder.create(this);
            builder.addParentStack(Player.class);
            builder.addNextIntent(resultIntent);
            builder.editIntentAt(0)
                    .putExtra(getString(R.string.query_text_label), mQuery)
                    .putExtra(getString(R.string.artist_data_label), mArtistData)
                    .putParcelableArrayListExtra(getString(R.string.track_data_label), mTrackData)
                    .putExtra(getString(R.string.track_position_label), mTrackPosition);
            builder.editIntentAt(1)
                    .putExtra(getString(R.string.artist_id_label), mArtistData.artistId)
                    .putExtra(getString(R.string.artist_name_label), mArtistData.artistName);
            contentIntent = builder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Action playPauseAction;
        if (willPlay) {
            PendingIntent pauseIntent = PendingIntent.getBroadcast(this, 0,
                    new Intent(Util.ACTION_PAUSE),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            playPauseAction = new NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    pauseIntent);
        } else {
            PendingIntent playIntent = PendingIntent.getBroadcast(this, 0,
                    new Intent(Util.ACTION_PLAY),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            playPauseAction = new NotificationCompat.Action(
                    android.R.drawable.ic_media_play,
                    "Play",
                    playIntent);
        }

        PendingIntent previousIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(Util.ACTION_PREVIOUS),
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action previousAction = new NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                "Previous",
                previousIntent);

        PendingIntent nextIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(Util.ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action nextAction = new NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                "Next",
                nextIntent);


        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(mTrackData.get(mTrackPosition).trackName)
                .setContentText(mTrackData.get(mTrackPosition).albumName)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_notification)
                .addAction(previousAction)
                .addAction(playPauseAction)
                .addAction(nextAction)
                .build();
        if (willPlay) {
            startForeground(NOTIFICATION_ID, notification);
        } else {
            mNotificationManager.notify(NOTIFICATION_ID, notification);
            stopForeground(false);
        }
    }

    // wrapper methods for the media player
    public void play() {
        mPlayer.start();
        showNotification(true);
    }

    public void pause() {
        mPlayer.pause();
        showNotification(false);
    }

    public void skipForward(int newPosition) {
        if (newPosition > mTrackData.size() - 1) return;
        String trackUrl = mTrackData.get(newPosition).trackUrl;
        mTrackPosition = newPosition;
        try {
            mPlayer.reset();
            mPlayer.setDataSource(trackUrl);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to get track at " + trackUrl);
        }
    }

    public void skipBackward(int newPosition) {
        if (getProgress() >= TRACK_LENGTH_SKIP_LENGTH_MS || newPosition < 0) {
            seekToAndPlay(0);
            return;
        }
        String trackUrl = mTrackData.get(newPosition).trackUrl;
        mTrackPosition = newPosition;
        try {
            mPlayer.reset();
            mPlayer.setDataSource(trackUrl);
            mPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Failed to get track at " + trackUrl);
        }
    }

    public void stop() {
        mPlayer.stop();
    }

    public int getProgress() {
        return mPlayer.getCurrentPosition();
    }

    public void seekToAndPlay(int progress) {
        mPlayer.seekTo(progress);
        play();
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public boolean isSetup() {return mPlayerStarted;}

    public String getTrackId() {return mTrackData.get(mTrackPosition).trackId; }

    public void setCompletionListenerClient(Util.Listeners.OnPlaybackCompletedListener listener) {
        mPlaybackCompletedListener = listener;
    }

    public void setNotificationPreviousClickListener(Util.Listeners.OnNotificationPreviousClickListener listener) {
        mNotificationPreviousListener = listener;
    }

    public void setQueryText(String query) {
        mQuery = query;
    }

    public void setArtistData(ArtistViewAdapter.ArtistData artistData) {
        mArtistData = artistData;
    }

    public class SSBinder extends Binder {
        public SSService getService() {
            return SSService.this;
        }
    }

    public class SSReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Util.ACTION_PLAY:
                    play();
                    break;
                case Util.ACTION_PAUSE:
                    pause();
                    break;
                case Util.ACTION_NEXT:
                    skipForward(mTrackPosition + 1); // includes notification changing
                    break;
                case Util.ACTION_PREVIOUS:
                    // must call onNotificationPreviousClick before skipBackward
                    if (mNotificationPreviousListener != null) {
                        mNotificationPreviousListener.onNotificationPreviousClick();
                    }
                    skipBackward(mTrackPosition - 1); // includes notification changing
                    break;
            }
        }
    }
}
