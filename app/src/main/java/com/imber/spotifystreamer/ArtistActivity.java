package com.imber.spotifystreamer;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;


public class ArtistActivity extends ActionBarActivity {
    private final String LOG_TAG = getClass().getSimpleName();
    ArtistFragment mArtistFragment;
    private String mQuery;
    private final String QUERY_TAG = "bundle_query";
    private boolean mUpdateResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);
        if (savedInstanceState != null && savedInstanceState.containsKey(QUERY_TAG)) {
            mQuery = savedInstanceState.getString(QUERY_TAG);
            mUpdateResults = false;
        } else {
            mUpdateResults = true;
        }
        mArtistFragment = (ArtistFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_artist);
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
        outState.putString(QUERY_TAG, mQuery);
        super.onSaveInstanceState(outState);
    }

    public interface Callback {
        void updateResults(String newText);
        void submitResults(String finalQuery);
    }
}
