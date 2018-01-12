package com.yinhao.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import javax.security.auth.login.LoginException;

/**
 * Created by hp on 2018/1/11.
 */

public class VisibleFragment extends Fragment {

    private static final String TAG = "VisibleFragment";

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        getActivity().registerReceiver(mOnShowNotification, filter, PollService.PREM_PRIVATE, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mOnShowNotification);
    }

    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Toast.makeText(getActivity(), "Got a broadcast: " + intent.getAction(), Toast.LENGTH_LONG).show();
            //if we receive this.we're visible,so cancel 意思就是说
            //广播是在onStart()方法中收到的，说明应用在前台，不应该发送通知notification
            //the notification
            Log.i(TAG, "canceling notification");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };
}
