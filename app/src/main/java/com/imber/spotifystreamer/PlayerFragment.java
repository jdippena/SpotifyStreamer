package com.imber.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {
    private final String LOG_TAG = getClass().getSimpleName();

    Context mContext;
    TextView mArtistName;
    TextView mAlbumName;
    ImageView mAlbumArt;
    TextView mTrackName;
    TextView mProgress;
    TextView mEnd;

    SeekBar mSeekBar;

    Button mPrev;
    Button mPlay;
    Button mNext;

    MediaPlayer mPlayer;
    Timer mTimer;

    public PlayerFragment() {
    }

    //TODO: crashes on rotation

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            new SetupViewTask().execute(intent.getStringExtra(Intent.EXTRA_TEXT));
        }
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

        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        return rootView;
    }

    @Override
    public void onStop() {
        super.onStop();
        mTimer.cancel();
        mPlayer.release();
        mPlayer = null;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mPlayer != null) mPlayer.seekTo(seekBar.getProgress());
    }

    class SetupViewTask extends AsyncTask<String, Void, Track> {
        @Override
        protected Track doInBackground(String... params) {
            SpotifyService service = new SpotifyApi().getService();
            return service.getTrack(params[0]);
        }

        @Override
        protected void onPostExecute(Track t) {
            //TODO: get artist as well here instead of from track id
            mArtistName.setText(t.artists.get(0).name);
            mAlbumName.setText(t.album.name);
            //TODO: get better picture resolution
            Picasso.with(mContext).load(t.album.images.get(0).url).into(mAlbumArt);
            mTrackName.setText(t.name);
            mProgress.setText("0:00");
            mEnd.setText(Utility.formatTrackLength(30000));
            mSeekBar.setMax(30000);
            mPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mPlayer != null) {
                        if (mPlayer.isPlaying()) {
                            mPlayer.pause();
                            //TODO: use better images
                            mPlay.setBackgroundResource(android.R.drawable.ic_media_play);
                        } else {
                            mPlayer.start();
                            mPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
                        }
                    }
                }
            });
            try {
                mPlayer.setDataSource(t.preview_url);
                mPlayer.prepareAsync();
                mPlayer.start();
                mPlay.setBackgroundResource(android.R.drawable.ic_media_pause);
                mTimer = new Timer();
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int progress = mPlayer.getCurrentPosition();
                                mSeekBar.setProgress(progress);
                                mProgress.setText(Utility.formatTrackLength(progress));
                            }
                        });
                    }
                }, 0, 16);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }

        }
    }

}
