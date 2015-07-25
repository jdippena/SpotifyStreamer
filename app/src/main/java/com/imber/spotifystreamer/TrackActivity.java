package com.imber.spotifystreamer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.imber.spotifystreamer.adapters.TrackViewAdapter;

import java.util.ArrayList;


public class TrackActivity extends ActionBarActivity implements Util.Listeners.OnTrackSelectedListener{
    private final String LOG_TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);
        if (savedInstanceState == null) {
            Intent intent = getIntent();
            TrackFragment tf = TrackFragment.newInstance(
                    this,
                    intent.getStringExtra(getString(R.string.artist_id_label)),
                    intent.getStringExtra(getString(R.string.artist_name_label)),
                    intent.getIntExtra(getString(R.string.palette_color_main_label), Color.GRAY),
                    intent.getIntExtra(getString(R.string.palette_color_bar_label), (Color.GRAY & 0xfefefefe) >> 1)
            );
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.track_fragment_container, tf).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_track, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTrackItemSelected(ArrayList<TrackViewAdapter.TrackData> trackData, int trackPosition) {
        Intent intent = new Intent(this, Player.class)
                .putParcelableArrayListExtra(getString(R.string.track_data_label), trackData)
                .putExtra(getString(R.string.track_position_label), trackPosition);
        startActivity(intent);
    }
}
