package com.imber.spotifystreamer;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

public class ArtistFragment extends Fragment implements Util.Listeners.OnSearchViewTextListener {
    private Context mContext;
    private Util.Listeners.OnArtistSelectedListener mArtistSelectedListener;
    private final String LOG_TAG = getClass().getSimpleName();
    private ArtistViewAdapter mArtistAdapter;
    private ArrayList<ArtistData> mArtistData;
    private final String ARTIST_DATA_TAG = "artist_data";
    private int mPositionSelected;
    private final String POS_SELECTED_TAG = "pos";

    public ArtistFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        mContext = getActivity();
        try {
            mArtistSelectedListener = (Util.Listeners.OnArtistSelectedListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() +
                    " must implement OnArtistSelectedListener");
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist_layout, container, false);

        final ListView listView = (ListView) rootView.findViewById(R.id.artist_list_view);

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
                mPositionSelected = position;
                mArtistSelectedListener.onArtistItemSelected(
                        mArtistAdapter.getItem(position),
                        (ImageView) view.findViewById(R.id.list_item_artist_image));
            }
        });
        if (savedInstanceState != null && savedInstanceState.containsKey(POS_SELECTED_TAG)) {
            mPositionSelected = savedInstanceState.getInt(POS_SELECTED_TAG);
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.smoothScrollToPosition(mPositionSelected);
                }
            });
        }
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
        outState.putInt(POS_SELECTED_TAG, mPositionSelected);
    }

    // callback methods from search view
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
                mArtistData = Util.createArtistDataArrayList(mContext, result);
                mArtistAdapter.addAll(mArtistData);
            }
        }
    }
}
