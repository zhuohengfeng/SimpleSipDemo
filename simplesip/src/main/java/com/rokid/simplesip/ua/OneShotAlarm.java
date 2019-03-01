package com.rokid.simplesip.ua;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OneShotAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //if (!Sipdroid.release) Log.i("SipUA:","alarm");
        Receiver.engine(context).expire();
    }
}
