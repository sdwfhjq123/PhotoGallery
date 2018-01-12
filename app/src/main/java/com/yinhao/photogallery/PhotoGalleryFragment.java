package com.yinhao.photogallery;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by hp on 2017/12/27.
 */

public class PhotoGalleryFragment extends VisibleFragment {
    private static final String TAG = "PhotoGalleryFragment";
    public static final String API_KEY = "4b096054791f66ac2b6889b348019229";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SERACH_METHOD = "flickr.photos.search";

    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);
        final MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.i(TAG, "QueryTextSubmit: " + query);
                QueryPreferences.setStoredQuery(getActivity(), query);
                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.i(TAG, "QueryTextChange: " + newText);
                return false;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                searchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    private List<GalleryItem> handleJson(String json) {
        List<GalleryItem> items = new ArrayList<>();
        try {
            JSONObject jsonBody = new JSONObject(json);
            JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
            JSONArray photoJsonJSONArray = photosJsonObject.getJSONArray("photo");
            for (int i = 0; i < photoJsonJSONArray.length(); i++) {
                JSONObject photoJsonObject = photoJsonJSONArray.getJSONObject(i);
                GalleryItem item = new GalleryItem();
                item.setId(photoJsonObject.getString("id"));
                item.setCaption(photoJsonObject.getString("title"));
                if (!photoJsonObject.has("url_s")) {
                    continue;
                }
                item.setUrl(photoJsonObject.getString("url_s"));
                item.setOwner(photoJsonObject.getString("owner"));
                items.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return items;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setAdapter();
        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);
        if (method.equals(SERACH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, parent, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            String url = galleryItem.getUrl();
            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            holder.bindGalleryItem(galleryItem);
            holder.bindDrawable(url);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mItemImageView;
        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);
            itemView.setOnClickListener(this);
        }

        public void bindDrawable(String picUrl) {
            Glide.with(getActivity()).load(picUrl).into(mItemImageView);
        }

        public void bindGalleryItem(GalleryItem galleryItem) {
            mGalleryItem = galleryItem;
        }

        @Override
        public void onClick(View v) {
            Intent i = PhotoPageActivity.newIntent(getActivity(), mGalleryItem.getPhotoPageUri());
            startActivity(i);
        }
    }

    public void updateItems() {
        //TODO 测试
        String query = QueryPreferences.getStoredQuery(getActivity());
        Log.e(TAG, "要查询的关键词:" + query);
        String url = buildUrl(FETCH_RECENTS_METHOD, query);
        HttpUtil.AsyncGetRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                //Log.i(TAG, "onResponse: " + result);
                mItems = handleJson(result);
                if (getActivity() instanceof PhotoGalleryActivity) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setAdapter();
                        }
                    });
                }
            }
        });
    }
}
