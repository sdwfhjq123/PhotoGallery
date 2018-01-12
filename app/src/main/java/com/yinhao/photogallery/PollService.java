package com.yinhao.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by hp on 2018/1/2.
 */

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    public static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

    public static final String API_KEY = "4b096054791f66ac2b6889b348019229";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SERACH_METHOD = "flickr.photos.search";
    public static final String ACTION_SHOW_NOTIFICATION = "com.yinhao.photogallery,SHOW_NOTIFICATION";
    public static final String PREM_PRIVATE = "com.yinhao.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    private List<GalleryItem> mItems = new ArrayList<>();
    private String mLastResultId;

    public static Intent newInstance(Context context) {
        return new Intent(context, PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = PollService.newInstance(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }

        QueryPreferences.setAlarmOn(context, isOn);
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollService.newInstance(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!isNetworkAvailableAndConnected()) {
            return;
        }

        mLastResultId = QueryPreferences.getLastResultId(this);
        updateItems();

    }

    private boolean isNetworkAvailableAndConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }

    public void updateItems() {
        //TODO 测试
        String query = QueryPreferences.getStoredQuery(this);
        Log.e(TAG, "要查询的关键词:" + query);
        String url = buildUrl(FETCH_RECENTS_METHOD, query);
        HttpUtil.AsyncGetRequest(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                mItems = handleJson(result);
                if (mItems.size() == 0) {
                    return;
                }
                String resultId = mItems.get(0).getId();
                if (resultId.equals(mLastResultId)) {
                    Log.i(TAG, "Got an old result:  " + resultId);
                } else {
                    Log.i(TAG, "Got an new result: " + resultId);
                    Resources resources = getResources();
                    Intent i = PhotoGalleryActivity.newInstance(PollService.this);
                    PendingIntent pi = PendingIntent.getActivity(PollService.this, 0, i, 0);

                    Notification notification = new NotificationCompat.Builder(PollService.this)
                            .setTicker(resources.getString(R.string.new_pictures_title))
                            .setSmallIcon(android.R.drawable.ic_menu_report_image)
                            .setContentTitle(resources.getString(R.string.new_pictures_title))
                            .setContentText(resources.getString(R.string.new_pictures_text))
                            .setContentIntent(pi)
                            .setAutoCancel(true)
                            .build();

//                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(PollService.this);
//                    notificationManager.notify(0, notification);
//                    sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION), PREM_PRIVATE);
                    showBackgroundNotification(0, notification);
                }
                QueryPreferences.setLastResultId(getApplicationContext(), resultId);
            }
        });
    }

    private void showBackgroundNotification(int requestCode, Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE, requestCode);
        i.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(i, PREM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
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
                items.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return items;
    }

    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);
        if (method.equals(SERACH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }
}
