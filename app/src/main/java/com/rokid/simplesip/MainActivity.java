package com.rokid.simplesip;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.rokid.simplesip.tools.Logger;
import com.rokid.simplesip.ua.Receiver;
import com.rokid.simplesip.ua.SipParam;
import com.rokid.simplesip.ua.SipdroidEngine;
import com.rokid.simplesip.ua.UserAgent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //"33.192.64.100",  test  Hik12345  ---- 账号密码
        SipParam param = AccountConfigHelper.getInstance().getAccountConfig(AccountConfigHelper.TAG.OPEN);

        Receiver.initSip(this, param);
        Receiver.engine(this, new Receiver.StateCallback() {
            @Override
            public void changeStatus(int state) {
                Logger.d("MainActivity  changeStatus state=" + state);
            }
        });
        Receiver.engine(this).setSipdroidEngineCallback(new SipdroidEngine.SipdroidEngineCallback() {
            @Override
            public void onCallAccepted(UserAgent agent, UserAgent.onRingParam param) {
                Logger.d("MainActivity onCallAccepted isstart=");
            }

            @Override
            public void onCallClosing() {
                Logger.d("MainActivity onCallClosing()");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Receiver.finishSip();
    }


}
