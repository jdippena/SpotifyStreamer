package com.imber.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.imber.spotifystreamer.adapters.ArtistViewAdapter;
import com.imber.spotifystreamer.adapters.ArtistViewAdapter.ArtistData;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import retrofit.RetrofitError;

public class ArtistFragment extends Fragment implements ArtistActivity.Callback{
    Context mContext;
    private final String LOG_TAG = getClass().getSimpleName();
    private ArtistViewAdapter mArtistAdapter;
    private String mPaletteIntentLabel;
    private String mPaletteIntentLabelBar;
    private ArrayList<ArtistData> mArtistData;
    private final String ARTIST_DATA_TAG = "artist_data";

    public ArtistFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mContext = getActivity();
        mPaletteIntentLabel = getString(R.string.palette_intent_label);
        mPaletteIntentLabelBar  = getString(R.string.palette_intent_label_status_bar);

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_layout, container, false);

        ListView listView = (ListView) rootView.findViewById(R.id.main_list_view);

        if (savedInstanceState == null) {
            mArtistData = new ArrayList<>();
        } else {
            mArtistData = savedInstanceState.getParcelableArrayList(ARTIST_DATA_TAG);
        }
        mArtistAdapter = new ArtistViewAdapter(mContext,
                R.layout.list_item_artist,
                mArtistData);
        listView.setAdapter(mArtistAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int paletteColorMain;
                int paletteColorBar;
                // get palette colors from thumbnails to color the action and status bar
                ImageView imageView = (ImageView) view.findViewById(R.id.list_item_artist_image);
                Bitmap b = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                paletteColorMain = Palette.from(b).generate().getMutedColor(Color.GRAY);
                // make paletteColorBar darker than paletteColorMain
                paletteColorBar = (paletteColorMain & 0xfefefefe) >> 1;

                Intent detailIntent = new Intent(mContext, TrackActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, mArtistAdapter.getItem(position).artistId)
                        .putExtra(mPaletteIntentLabel, paletteColorMain)
                        .putExtra(mPaletteIntentLabelBar, paletteColorBar);
                startActivity(detailIntent);
            }
        });
        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(mContext, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(ARTIST_DATA_TAG, mArtistData);
    }

    // callback methods from activity
    @Override
    public void updateResults(String newText) {
        new FetchArtistAndPicture().execute(newText);
    }

    @Override
    public void submitResults(String finalQuery) {
        //TODO: find better way to check for no results
        if (mArtistAdapter.isEmpty()) {
            Toast.makeText(mContext, "No results found", Toast.LENGTH_SHORT).show();
        }
    }

    class FetchArtistAndPicture extends AsyncTask<String, Void, ArrayList<Artist>> {
        @Override
        protected ArrayList<Artist> doInBackground(String... params) {
            ArrayList<Artist> data = new ArrayList<>();

            SpotifyService service = new SpotifyApi().getService();
            String artistName = params[0];
            try {
                data.addAll(service.searchArtists(artistName).artists.items);
            } catch (RetrofitError e) {
                Log.e(LOG_TAG, e.getMessage() + "Spotify Service didn't get all artists");
            }
            return data;
        }

        @Override
        protected void onPostExecute(ArrayList<Artist> result) {
            if (result != null) {
                mArtistAdapter.clear();
                mArtistData = Utility.createArtistDataArrayList(mContext, result);
                mArtistAdapter.addAll(mArtistData);
            }
        }
    }
}
