package com.imber.spotifystreamer;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;


public class MainActivity extends ActionBarActivity {
    private final String LOG_TAG = getClass().getSimpleName();
    MainFragment mMainFragment;
    private String mQuery;
    private final String QUERY_TAG = "bundle_query";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null && savedInstanceState.containsKey(QUERY_TAG)) {
            mQuery = savedInstanceState.getString(QUERY_TAG);
        }

        mMainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_artist);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mQuery = query;
                searchView.clearFocus();
                mMainFragment.submitResults(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mQuery = newText;
                mMainFragment.updateResults(newText);
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
