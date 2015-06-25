package com.imber.spotifystreamer;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;


public class ArtistDetailActivity extends ActionBarActivity {
    private final String LOG_TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);
    }
}
