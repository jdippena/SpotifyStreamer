package com.imber.spotifystreamer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.imber.spotifystreamer.adapters.TrackViewAdapter;

import java.util.ArrayList;

public class Player extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(getString(R.string.track_data_label))
                && savedInstanceState == null) {
            ArrayList<TrackViewAdapter.TrackData> trackData  =
                    intent.getParcelableArrayListExtra(getString(R.string.track_data_label));
            PlayerFragment pf = PlayerFragment.newInstance(
                    this,
                    trackData,
                    intent.getIntExtra(getString(R.string.track_position_label), 0));

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.player_fragment_container, pf)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);
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

}
