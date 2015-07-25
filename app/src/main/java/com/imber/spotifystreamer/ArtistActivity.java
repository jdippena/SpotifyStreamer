package com.imber.spotifystreamer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SearchView;

import com.imber.spotifystreamer.adapters.ArtistViewAdapter;
import com.imber.spotifystreamer.adapters.TrackViewAdapter;

import java.util.ArrayList;


public class ArtistActivity extends ActionBarActivity
        implements Util.Listeners.OnArtistSelectedListener, Util.Listeners.OnTrackSelectedListener {

    private final String LOG_TAG = getClass().getSimpleName();
    ArtistFragment mArtistFragment;
    private String mQuery;
    private boolean mUpdateResults;
    private boolean mLargeLayout;
    private final String TRACK_FRAGMENT_TAG = "track_fragment";
    private final String PLAYER_FRAGMENT_TAG = "player_fragment";

    private SSService mSSService;
    private SSServiceConnection mConnection = new SSServiceConnection();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);
        mArtistFragment = (ArtistFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_artist);
        mLargeLayout = getResources().getBoolean(R.bool.large_layout);

        handleIntent();
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(getString(R.string.query_text_label), mQuery);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

    // listener for selection in ArtistFragment
    @Override
    public void onArtistItemSelected(ArtistViewAdapter.ArtistData data, ImageView artistImage) {
        mSSService.setArtistData(data);
        if (mLargeLayout) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.track_fragment_container,
                            TrackFragment.newInstance(this, data.artistId, data.artistName),
                            TRACK_FRAGMENT_TAG)
                    .commit();
        } else {
            int paletteColorMain;
            int paletteColorBar;
            // get palette colors from thumbnails to color the action and status bar
            Bitmap b = ((BitmapDrawable) artistImage.getDrawable()).getBitmap();
            paletteColorMain = Palette.from(b).generate().getMutedColor(Color.GRAY);
            // make paletteColorBar darker than paletteColorMain
            paletteColorBar = (paletteColorMain & 0xfefefefe) >> 1;

            Intent detailIntent = new Intent(this, TrackActivity.class)
                    .putExtra(getString(R.string.artist_id_label), data.artistId)
                    .putExtra(getString(R.string.artist_name_label),
                            data.artistName)
                    .putExtra(getString(R.string.palette_color_main_label),
                            paletteColorMain)
                    .putExtra(getString(R.string.palette_color_bar_label),
                            paletteColorBar);
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

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(getString(R.string.query_text_label))) {
            mQuery = intent.getStringExtra(getString(R.string.query_text_label));
            mArtistFragment.updateResults(mQuery);
            if (mLargeLayout) {
                // show the track fragment
                ArtistViewAdapter.ArtistData data = intent
                        .getParcelableExtra(getString(R.string.track_data_label));
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.track_fragment_container,
                                TrackFragment.newInstance(this, data.artistId, data.artistName),
                                TRACK_FRAGMENT_TAG)
                        .commit();
                ArrayList<TrackViewAdapter.TrackData> trackData = intent.getParcelableArrayListExtra(
                        getString(R.string.track_data_label)
                );
                // show the dialog player
                int trackPosition = intent.getIntExtra(getString(R.string.track_position_label), 0);
                PlayerFragment pf = PlayerFragment.newInstance(this, trackData, trackPosition);
                pf.show(getSupportFragmentManager(), PLAYER_FRAGMENT_TAG);
            }
        }
    }

    class SSServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSSService = ((SSService.SSBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSSService = null;
            Log.e(LOG_TAG, "PlayerFragment.onServiceDisconnected called");
        }
    }
}
