package com.imber.spotifystreamer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.imber.spotifystreamer.adapters.TrackViewAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;

public class PlayerFragment extends DialogFragment
        implements SeekBar.OnSeekBarChangeListener, Util.Listeners.OnPlaybackCompletedListener,
                    Util.Listeners.OnNotificationPreviousClickListener {
    private final String LOG_TAG = getClass().getSimpleName();
    private final int PREVIEW_TRACK_LENGTH_MS = 30000;
    private final int TRACK_LENGTH_SKIP_LENGTH_MS = 3000;

    private Context mContext;
    private ShareActionProvider mShareActionProvider;
    private SSService mSSService;
    private SSServiceConnection mConnection = new SSServiceConnection();
    private PlayerFragReceiver mReceiver = new PlayerFragReceiver();

    private TextView mArtistName;
    private TextView mAlbumName;
    private ImageView mAlbumArt;
    private TextView mTrackName;
    private TextView mProgress;
    private TextView mEnd;

    private SeekBar mSeekBar;

    private Button mPrev;
    private Button mPlay;
    private Button mNext;

    private ArrayList<TrackViewAdapter.TrackData> mTrackData;
    private int mTrackPosition;

    private Timer mTimer;

    public static PlayerFragment newInstance(Context context, ArrayList<TrackViewAdapter.TrackData> trackData, int trackPosition) {
        PlayerFragment pf = new PlayerFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(context.getString(R.string.track_data_label), trackData);
        args.putInt(context.getString(R.string.track_position_label), trackPosition);
        pf.setArguments(args);
        return pf;
    }

    public PlayerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        if (!getResources().getBoolean(R.bool.large_layout)) setHasOptionsMenu(true);

        // if savedInstanceState is null, use arguments to get data, else, use the savedInstanceState
        if (savedInstanceState == null) {
            if (getArguments() != null && getArguments().containsKey(getString(R.string.track_data_label))) {
                mTrackData = getArguments().getParcelableArrayList(getString(R.string.track_data_label));
                mTrackPosition = getArguments().getInt(getString(R.string.track_position_label));
            }
        } else if (savedInstanceState.containsKey(getString(R.string.track_data_label))) {
            mTrackData = savedInstanceState.getParcelableArrayList(getString(R.string.track_data_label));
            mTrackPosition = savedInstanceState.getInt(getString(R.string.track_position_label));
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Util.ACTION_PLAY);
        filter.addAction(Util.ACTION_PAUSE);
        filter.addAction(Util.ACTION_NEXT);
        mContext.registerReceiver(mReceiver, filter);
        doBindService();

        mTimer = new Timer();
        scheduleTimerTask();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_player_layout, container, false);
        mArtistName = (TextView) rootView.findViewById(R.id.player_artist_textview);
        mAlbumName = (TextView) rootView.findViewById(R.id.player_album_textview);
        mAlbumArt = (ImageView) rootView.findViewById(R.id.player_album_imageview);
        mTrackName = (TextView) rootView.findViewById(R.id.player_track_textview);
        mProgress = (TextView) rootView.findViewById(R.id.player_progress_textview);
        mEnd = (TextView) rootView.findViewById(R.id.player_end_textview);

        mSeekBar = (SeekBar) rootView.findViewById(R.id.player_seek_bar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mPrev = (Button) rootView.findViewById(R.id.player_prev_button);
        mPlay = (Button) rootView.findViewById(R.id.player_play_button);
        mNext = (Button) rootView.findViewById(R.id.player_next_button);

        mPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSSService.isPlaying()) {
                    mSSService.pause();
                    mPlay.setBackgroundResource(android.R.drawable.ic_media_play);
                } else if (mSSService.isSetup()) {
                    mSSService.play();
                    mPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
                }
            }
        });
        mPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrackPosition > 0
                        && mSSService.getProgress() < TRACK_LENGTH_SKIP_LENGTH_MS) {
                    updateView(mTrackData.get(mTrackPosition - 1).trackId);
                    mTrackPosition--;
                }
                //mTrackPosition has position updated if track was switched
                mSSService.skipBackward(mTrackPosition);
            }
        });

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTrackPosition < mTrackData.size() - 1) {
                    mSSService.skipForward(mTrackPosition + 1);
                    updateView(mTrackData.get(mTrackPosition + 1).trackId);
                    mTrackPosition++;
                }
            }
        });

        updateView(mTrackData.get(mTrackPosition).trackId);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // this will only get called in phone layout
        inflater.inflate(R.menu.menu_player, menu);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.action_share));
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer = new Timer();
        scheduleTimerTask();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mTimer != null) mTimer.cancel();
    }

    @Override
    public void onDestroy() {
        doUnBindService();
        mContext.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(getString(R.string.track_data_label), mTrackData);
        outState.putInt(getString(R.string.track_position_label), mTrackPosition);
    }

    // OnSeekBarChangeListener methods
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mProgress.setText(Util.formatTrackLength(progress));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (mSSService != null && mSSService.isSetup()) mSSService.pause();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mSSService != null && mSSService.isSetup()) {
            mSSService.seekToAndPlay(seekBar.getProgress());
            mPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
        }
        if (mSSService != null && mSeekBar.getProgress() >= PREVIEW_TRACK_LENGTH_MS) {
            mSSService.skipForward(mTrackPosition + 1);
            updateView(mTrackData.get(mTrackPosition + 1).trackId);
            mTrackPosition++;
        }
    }

    @Override
    public void onPlaybackCompleted() {
        if (mTrackPosition < mTrackData.size() - 1) {
            updateView(mTrackData.get(mTrackPosition + 1).trackId);
            mTrackPosition++;
        }
    }

    @Override
    public void onNotificationPreviousClick() {
        // Only handle view changes. SSService handles music changes
        if (mTrackPosition > 0
                && mSSService.getProgress() < TRACK_LENGTH_SKIP_LENGTH_MS) {
            updateView(mTrackData.get(mTrackPosition - 1).trackId);
            mTrackPosition--;
        }
    }

    private Intent createShareIntent() {
        return new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, mTrackData.get(mTrackPosition).trackName + " " +
                        mTrackData.get(mTrackPosition).trackUrl)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    }

    private void scheduleTimerTask() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mSSService != null && mSSService.isPlaying()) {
                                int progress = mSSService.getProgress();
                                mSeekBar.setProgress(progress);
                                mProgress.setText(Util.formatTrackLength(progress));
                            }
                        }
                    });
                }
            }
        }, 0, 16);
    }



    public void updateView(String trackId) {
        mSeekBar.setProgress(0);
        new SetupViewTask().execute(trackId);
    }

    private void doBindService() {
        mContext.bindService(
                new Intent(mContext, SSService.class),
                mConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    private void doUnBindService() {
        mContext.unbindService(mConnection);
    }

    class SSServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSSService = ((SSService.SSBinder) service).getService();
            mSSService.setCompletionListenerClient(PlayerFragment.this);
            mSSService.setNotificationPreviousClickListener(PlayerFragment.this);
            if (mSSService.isPlaying()) {
                mPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
            } else {
                mPlay.setBackgroundResource(android.R.drawable.ic_media_play);
            }
            if (!mSSService.isSetup()
                    || !mSSService.getTrackId().equals(mTrackData.get(mTrackPosition).trackId)) {
                mSSService.setupAndStartPlayer(mTrackData, mTrackPosition);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSSService = null;
            Log.e(LOG_TAG, "PlayerFragment.onServiceDisconnected called");
        }
    }
    // the only reason another network operation is needed here is to get the optimum picture resolution
    // and its url from server
    class SetupViewTask extends AsyncTask<String, Void, Track> {
        @Override
        protected Track doInBackground(String... params) {
            SpotifyService service = new SpotifyApi().getService();
            return service.getTrack(params[0]);
        }

        @Override
        protected void onPostExecute(Track t) {
            mArtistName.setText(mTrackData.get(mTrackPosition).artistName);
            mAlbumName.setText(mTrackData.get(mTrackPosition).albumName);
            Picasso.with(mContext).load(Util.getPlayerAlbumArtUrl(mAlbumArt, t)).into(mAlbumArt);
            mTrackName.setText(mTrackData.get(mTrackPosition).trackName);
            mProgress.setText("0:00");
            mEnd.setText(Util.formatTrackLength(PREVIEW_TRACK_LENGTH_MS));
            mSeekBar.setMax(PREVIEW_TRACK_LENGTH_MS);
            mPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareIntent());
            }

        }
    }

    public class PlayerFragReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Util.ACTION_PLAY:
                    mPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
                    break;
                case Util.ACTION_PAUSE:
                    mPlay.setBackgroundResource(android.R.drawable.ic_media_play);
                    break;
                case Util.ACTION_NEXT:
                    if (mTrackPosition < mTrackData.size() - 1) {
                        updateView(mTrackData.get(mTrackPosition + 1).trackId);
                        mTrackPosition++;
                    }
                    break;
                // don't handle calls from previous button clicked because it needs to be synchronized
                // with SSService
            }
        }
    }
}
