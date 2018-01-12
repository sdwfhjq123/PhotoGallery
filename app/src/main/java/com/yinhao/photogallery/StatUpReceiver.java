package com.yinhao.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by hp on 2018/1/10.
 */

public class StatUpReceiver extends BroadcastReceiver {

    private static final String TAG = "StatUpReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Receiverd broadcast intent: " + intent.getAction());
        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);
    }
}
