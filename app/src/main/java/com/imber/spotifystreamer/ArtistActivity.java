package com.imber.spotifystreamer;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.imber.spotifystreamer.adapters.ArtistViewAdapter;
import com.imber.spotifystreamer.adapters.TrackViewAdapter;

import java.util.ArrayList;


public class ArtistActivity extends ActionBarActivity
        implements Util.Listeners.OnArtistSelectedListener, Util.Listeners.OnTrackSelectedListener,
                    Util.Listeners.OnPlaybackStartEndListener {

    private final String LOG_TAG = getClass().getSimpleName();
    ArtistFragment mArtistFragment;
    private String mQuery;
    private boolean mUpdateResults;
    private boolean mLargeLayout;
    private final String TRACK_FRAGMENT_TAG = "track_fragment";
    private final String PLAYER_FRAGMENT_TAG = "player_fragment";

    private SSService mSSService;
    private SSServiceConnection mConnection = new SSServiceConnection();
    private Menu mMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);
        mArtistFragment = (ArtistFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_artist);
        mLargeLayout = getResources().getBoolean(R.bool.large_layout);

        handleIntent(getIntent());
        if (savedInstanceState != null && savedInstanceState.containsKey(getString(R.string.query_text_label))) {
            mQuery = savedInstanceState.getString(getString(R.string.query_text_label));
            mUpdateResults = false;
        } else {
            mUpdateResults = true;
        }

        Intent serviceIntent = new Intent(this, SSService.class);
        startService(serviceIntent);
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_artist, menu);
        mMenu = menu;
        if (mSSService != null && mSSService.isPlaying()) {
            MenuItem item = mMenu.findItem(R.id.action_playing);
            if (item != null) {
                item.setVisible(true);
            }
        }

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mQuery = query;
                searchView.clearFocus();
                mArtistFragment.submitResults(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // this is called when the screen is rotated, so make sure to update the results
                // only when the data actually changes
                if (mUpdateResults) {
                    mQuery = newText;
                    mSSService.setQueryText(mQuery);
                    mArtistFragment.updateResults(newText);
                }
                mUpdateResults = true;
                return false;
            }
        });
        searchView.setIconifiedByDefault(false);
        if (mQuery != null) {
            searchView.setQuery(mQuery, false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_playing:
                try {
                    if (mLargeLayout) {
                        showFragments(mSSService.getArtistData(),
                                mSSService.getTrackData(), 
                                mSSService.getTrackPosition());
                    } else {
                        mSSService.createContentIntent().send();
                    }
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(getString(R.string.query_text_label), mQuery);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        mSSService.removePlaybackStartEndListener(this);
        unbindService(mConnection);
        super.onDestroy();
    }

    // listener for selection in ArtistFragment
    @Override
    public void onArtistItemSelected(ArtistViewAdapter.ArtistData data) {
        mSSService.setArtistData(data);
        if (mLargeLayout) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.track_fragment_container,
                            TrackFragment.newInstance(this, data.artistId, data.artistName),
                            TRACK_FRAGMENT_TAG)
                    .commit();
        } else {
            Intent detailIntent = new Intent(this, TrackActivity.class)
                    .putExtra(getString(R.string.artist_id_label), data.artistId)
                    .putExtra(getString(R.string.artist_name_label),
                            data.artistName);
            startActivity(detailIntent);
        }
    }

    // listener for selection in TrackFragment
    // this is only called in the large layout when TrackFragment's parent is ArtistActivity
    @Override
    public void onTrackItemSelected(ArrayList<TrackViewAdapter.TrackData> trackData, int trackPosition) {
        PlayerFragment pf = PlayerFragment.newInstance(this, trackData, trackPosition);
        pf.show(getSupportFragmentManager(), PLAYER_FRAGMENT_TAG);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onPlaybackEnded() {
        if (mMenu != null) {
            mMenu.findItem(R.id.action_playing).setVisible(false);
            if (mLargeLayout) {
                mMenu.findItem(R.id.action_share).setVisible(false);
            }
        }
    }

    @Override
    public void onPlaybackStarted() {
        if (mMenu != null) {
            mMenu.findItem(R.id.action_playing).setVisible(true);
            if (mLargeLayout) {
                ShareActionProvider shareActionProvider =
                        (ShareActionProvider) MenuItemCompat.getActionProvider(mMenu.findItem(R.id.action_share));
                shareActionProvider.setShareIntent(createShareIntent());
                mMenu.findItem(R.id.action_share).setVisible(true);
            }
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra(getString(R.string.query_text_label))) {
            mQuery = intent.getStringExtra(getString(R.string.query_text_label));
            mArtistFragment.updateResults(mQuery);
            if (mLargeLayout) {
                // get data from intent and then show the fragments
                ArtistViewAdapter.ArtistData artistData = intent
                        .getParcelableExtra(getString(R.string.artist_data_label));
                ArrayList<TrackViewAdapter.TrackData> trackData =
                        intent.getParcelableArrayListExtra(getString(R.string.track_data_label));
                int trackPosition = intent.getIntExtra(getString(R.string.track_position_label), 0);
                showFragments(artistData, trackData, trackPosition);
            }
        }
    }

    private void showFragments(ArtistViewAdapter.ArtistData artistData,
                               ArrayList<TrackViewAdapter.TrackData> trackData,
                               int trackPosition) {
        FragmentManager fm = getSupportFragmentManager();
        TrackFragment tf = (TrackFragment) fm.findFragmentByTag(TRACK_FRAGMENT_TAG);
        if (tf == null || !tf.getArtistName().equals(artistData.artistName)) {
            // show the track fragment
            fm.beginTransaction()
                    .replace(R.id.track_fragment_container,
                            TrackFragment.newInstance(this, artistData.artistId, artistData.artistName),
                            TRACK_FRAGMENT_TAG)
                    .commit();
        }
        if (fm.findFragmentByTag(PLAYER_FRAGMENT_TAG) == null) {
            // show the dialog player
            PlayerFragment pf = PlayerFragment.newInstance(this, trackData, trackPosition);
            pf.show(getSupportFragmentManager(), PLAYER_FRAGMENT_TAG);
        }
    }

    private Intent createShareIntent() {
        TrackViewAdapter.TrackData trackData = mSSService.getTrackData().get(mSSService.getTrackPosition());
        return new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, trackData.trackName + " " + trackData.trackUrl)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    }

    private class SSServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSSService = ((SSService.SSBinder) service).getService();
            mSSService.addPlaybackStartEndListener(ArtistActivity.this);
            if (mSSService.isPlaying() && mMenu != null) {
                mMenu.findItem(R.id.action_playing).setVisible(true);
                if (mLargeLayout) {
                    mMenu.findItem(R.id.action_share).setVisible(true);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSSService = null;
            Log.e(LOG_TAG, "ArtistActivity onServiceDisconnected called");
        }
    }
}
