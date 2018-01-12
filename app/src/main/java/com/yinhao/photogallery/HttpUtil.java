package com.yinhao.photogallery;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by hp on 2017/12/27.
 */

public class HttpUtil {

    public static void AsyncGetRequest(String address, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .get()
                .url(address)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
