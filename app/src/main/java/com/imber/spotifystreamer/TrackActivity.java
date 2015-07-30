package com.imber.spotifystreamer;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.imber.spotifystreamer.adapters.TrackViewAdapter;

import java.util.ArrayList;


public class TrackActivity extends ActionBarActivity
        implements Util.Listeners.OnTrackSelectedListener, Util.Listeners.OnPlaybackStartEndListener{
    private final String LOG_TAG = getClass().getSimpleName();
    private Menu mMenu;
    private SSService mSSService;
    private SSServiceConnection mConnection = new SSServiceConnection();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            TrackFragment tf = TrackFragment.newInstance(
                    this,
                    intent.getStringExtra(getString(R.string.artist_id_label)),
                    intent.getStringExtra(getString(R.string.artist_name_label))
            );
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.track_fragment_container, tf).commit();
        }
        Intent serviceIntent = new Intent(this, SSService.class);
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_track, menu);
        mMenu = menu;
        if (mSSService != null && mSSService.isPlaying()) {
            MenuItem item = mMenu.findItem(R.id.action_playing);
            if (item != null) {
                item.setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_playing:
                try {
                    mSSService.createContentIntent().send();
                } catch (PendingIntent.CanceledException e) {
                    Log.e(LOG_TAG, e.getMessage());
                    e.printStackTrace();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        mSSService.removePlaybackStartEndListener(this);
        unbindService(mConnection);
        super.onDestroy();
    }

    @Override
    public void onTrackItemSelected(ArrayList<TrackViewAdapter.TrackData> trackData, int trackPosition) {
        Intent intent = new Intent(this, Player.class)
                .putParcelableArrayListExtra(getString(R.string.track_data_label), trackData)
                .putExtra(getString(R.string.track_position_label), trackPosition);
        startActivity(intent);
    }

    @Override
    public void onPlaybackEnded() {
        if (mMenu != null) {
            mMenu.findItem(R.id.action_playing).setVisible(false);
        }
    }

    @Override
    public void onPlaybackStarted() {
        if (mMenu != null) {
            mMenu.findItem(R.id.action_playing).setVisible(true);
        }
    }

    private class SSServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSSService = ((SSService.SSBinder) service).getService();
            mSSService.addPlaybackStartEndListener(TrackActivity.this);
            if (mSSService.isPlaying() && mMenu != null) {
                mMenu.findItem(R.id.action_playing).setVisible(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSSService = null;
            Log.e(LOG_TAG, "TrackActivity onServiceDisconnected called");
        }
    }
}
