package com.rokid.simplesip.ua;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.rokid.simplesip.R;
import com.rokid.simplesip.SipEngine;
import com.rokid.simplesip.tools.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

public class Receiver extends BroadcastReceiver {

    public interface StateCallback {

        /**
         * 电话状态变化
         *
         * @param state
         * @return
         */
        public void changeStatus(int state);
    }

    private static StateCallback mStateCallback;

    final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
    final static String ACTION_SIGNAL_STRENGTH_CHANGED = "android.intent.action.SIG_STR";
    final static String ACTION_DATA_STATE_CHANGED = "android.intent.action.ANY_DATA_STATE";
    final static String ACTION_DOCK_EVENT = "android.intent.action.DOCK_EVENT";
    final static String EXTRA_DOCK_STATE = "android.intent.extra.DOCK_STATE";
    final static String ACTION_SCO_AUDIO_STATE_CHANGED = "android.media.SCO_AUDIO_STATE_CHANGED";
    final static String EXTRA_SCO_AUDIO_STATE = "android.media.extra.SCO_AUDIO_STATE";
    final static String PAUSE_ACTION = "com.android.music.musicservicecommand.pause";
    final static String TOGGLEPAUSE_ACTION = "com.android.music.musicservicecommand.togglepause";
    final static String ACTION_DEVICE_IDLE = "com.android.server.WifiManager.action.DEVICE_IDLE";
    final static String ACTION_VPN_CONNECTIVITY = "vpn.connectivity";
    final static String ACTION_EXTERNAL_APPLICATIONS_AVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";
    final static String ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";
    final static String METADATA_DOCK_HOME = "android.dock_home";
    final static String CATEGORY_DESK_DOCK = "android.intent.category.DESK_DOCK";
    final static String CATEGORY_CAR_DOCK = "android.intent.category.CAR_DOCK";
    final static int EXTRA_DOCK_STATE_DESK = 1;
    final static int EXTRA_DOCK_STATE_CAR = 2;

    public final static int MWI_NOTIFICATION = 1;
    public final static int CALL_NOTIFICATION = 2;
    public final static int MISSED_CALL_NOTIFICATION = 3;
    public final static int AUTO_ANSWER_NOTIFICATION = 4;
    public final static int REGISTER_NOTIFICATION = 5;

    final static int MSG_SCAN = 1;
    final static int MSG_ENABLE = 2;
    final static int MSG_HOLD = 3;
    final static int MSG_HANGUP = 4;

    final static long[] vibratePattern = {0,1000,1000};

    public static int docked = -1,headset = -1,bluetooth = -1;
    public static SipdroidEngine mSipdroidEngine;

    public static Context mContext;
//    public static SipdroidListener listener_video;
//    public static Call ccCall;
//    public static Connection ccConn;
    public static int call_state;
    public static int call_end_reason = -1;

    public static String pstn_state;
    public static long pstn_time;
    public static String MWI_account;
    private static String laststate, lastnumber;
    //handle sub-thread msg to UI thread
    private static Handler mStateHandler;

    public static synchronized SipdroidEngine engine(Context context, StateCallback stateCallback) {
        mStateCallback = stateCallback;
        return engine(context);
    }

    public static synchronized SipdroidEngine engine(Context context) {
        if (mContext == null || !context.getClass().getName().contains("ReceiverRestrictedContext"))
            mContext = context;
        if (mSipdroidEngine == null) {
//            if (android.os.Build.VERSION.SDK_INT > 9) {
////                ReceiverNew.setPolicy();
////            }
            mSipdroidEngine = new SipdroidEngine();
            mSipdroidEngine.StartEngine();
//            if (Integer.parseInt(Build.VERSION.SDK) >= 8)
//                Bluetooth.init();
        } else
            mSipdroidEngine.CheckEngine();
        //context.startService(new Intent(context,RegisterService.class));
        mStateHandler = new Handler(Looper.getMainLooper(), handlerCallback);
        return mSipdroidEngine;
    }

    public static Handler.Callback handlerCallback = new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if (mStateCallback != null) {
                mStateCallback.changeStatus(msg.what);
            }
            return false;
        }
    };

    public static Ringtone oRingtone;
    static PowerManager.WakeLock wl;

    public static void stopRingtone() {
        if (v != null)
            v.cancel();
        if (Receiver.oRingtone != null) {
            Ringtone ringtone = Receiver.oRingtone;
            oRingtone = null;
            ringtone.stop();
        }
    }

    static android.os.Vibrator v;//振动器

    public static void onState(int state, String caller) {
        Logger.d("Receiver: onState state="+state);
        if (mStateHandler != null) {
            mStateHandler.sendEmptyMessage(state);
        }
//        if (ccCall == null) {
//            ccCall = new Call();
//            ccConn = new Connection();
//            ccCall.setConn(ccConn);
//            ccConn.setCall(ccCall);
//        }

        if (call_state != state) {
            if (state != UserAgent.UA_STATE_IDLE) {
                call_end_reason = -1;
            }
            call_state = state;

            switch (call_state) {
                case UserAgent.UA_STATE_INCOMING_CALL:
                    	/*
                        enable_wifi(true);
                        RtpStreamReceiver.good = RtpStreamReceiver.lost = RtpStreamReceiver.loss = RtpStreamReceiver.late = 0;
                        RtpStreamReceiver.speakermode = speakermode();
                        bluetooth = -1;
                        String text = caller.toString();
                        if (text.indexOf("<sip:") >= 0 && text.indexOf("@") >= 0)
                            text = text.substring(text.indexOf("<sip:") + 5, text.indexOf("@"));
                        String text2 = caller.toString();
                        if (text2.indexOf("\"") >= 0)
                            text2 = text2.substring(text2.indexOf("\"") + 1, text2.lastIndexOf("\""));
                        broadcastCallStateChanged("RINGING", caller);
                        mContext.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                        ccCall.setState(Call.State.INCOMING);
                        ccConn.setUserData(null);
                        ccConn.setAddress(text, text2);
                        ccConn.setIncoming(true);
                        ccConn.date = System.currentTimeMillis();
                        ccCall.base = 0;
                        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        int rm = am.getRingerMode();
                        int vs = am.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
                        KeyguardManager mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
                        if (v == null)
                            v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                        if ((pstn_state == null || pstn_state.equals("IDLE")) &&
                                PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_AUTO_ON, Settings.DEFAULT_AUTO_ON) &&
                                !mKeyguardManager.inKeyguardRestrictedInputMode())
                            v.vibrate(vibratePattern, 1);
                        else {
                            if ((pstn_state == null || pstn_state.equals("IDLE")) &&
                                    (rm == AudioManager.RINGER_MODE_VIBRATE ||
                                            (rm == AudioManager.RINGER_MODE_NORMAL && vs == AudioManager.VIBRATE_SETTING_ON)))
                                v.vibrate(vibratePattern, 1);
                            if (am.getStreamVolume(AudioManager.STREAM_RING) > 0) {
                                String sUriSipRingtone = PreferenceManager.getDefaultSharedPreferences(mContext).getString(Settings.PREF_SIPRINGTONE,
                                        Settings.System.DEFAULT_RINGTONE_URI.toString());
                                if (!TextUtils.isEmpty(sUriSipRingtone)) {
                                    oRingtone = RingtoneManager.getRingtone(mContext, Uri.parse(sUriSipRingtone));
                                    if (oRingtone != null) oRingtone.play();
                                }
                            }
                        }
//                        moveTop();
                        if (wl == null) {
                            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                            wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                                    PowerManager.ACQUIRE_CAUSES_WAKEUP, "Sipdroid.onState");
                        }
                        wl.acquire();
                        Checkin.checkin(true);
                        */
                    break;
                case UserAgent.UA_STATE_OUTGOING_CALL:
                    	/*
                        RtpStreamReceiver.good = RtpStreamReceiver.lost = RtpStreamReceiver.loss = RtpStreamReceiver.late = 0;
                        RtpStreamReceiver.speakermode = speakermode();
                        bluetooth = -1;
                        onText(MISSED_CALL_NOTIFICATION, null, 0, 0);
                        engine(mContext).register();
                        broadcastCallStateChanged("OFFHOOK", caller);
                        ccCall.setState(Call.State.DIALING);
                        ccConn.setUserData(null);
                        ccConn.setAddress(caller, caller);
                        ccConn.setIncoming(false);
                        ccConn.date = System.currentTimeMillis();
                        ccCall.base = 0;
//                        moveTop();
                        Checkin.checkin(true);
                        */
                    break;
                case UserAgent.UA_STATE_IDLE:
                    Logger.d("UA_STATE_IDLE");
                    //broadcastCallStateChanged("IDLE", null);
                    //onText(CALL_NOTIFICATION, null, 0, 0);
                    //ccCall.setState(Call.State.DISCONNECTED);
//                        if (listener_video != null)
//                            listener_video.onHangup();
                    //stopRingtone();
                    if (wl != null && wl.isHeld())
                        wl.release();
//                        //TODO 是否需要
////                        mContext.startActivity(createIntent(InCallScreen.class));
//                        ccConn.log(ccCall.base);
//                        ccConn.date = 0;
                    engine(mContext).listen();
                    break;
                case UserAgent.UA_STATE_INCALL:
                    	/*
                        broadcastCallStateChanged("OFFHOOK", null);
                        if (ccCall.base == 0) {
                            ccCall.base = SystemClock.elapsedRealtime();
                        }
                        progress();
                        ccCall.setState(Call.State.ACTIVE);
                        stopRingtone();
                        if (wl != null && wl.isHeld())
                            wl.release();
                        //TODO
//                        mContext.startActivity(createIntent(InCallScreen.class));
						*/
                    break;
                case UserAgent.UA_STATE_HOLD:
                    	/*
                        onText(CALL_NOTIFICATION, mContext.getString(R.string.card_title_on_hold), android.R.drawable.stat_sys_phone_call_on_hold, ccCall.base);
                        ccCall.setState(Call.State.HOLDING);
                        if (InCallScreen.started && (pstn_state == null || !pstn_state.equals("RINGING")))
//                            mContext.startActivity(createIntent(InCallScreen.class));
						*/
                    break;
            }


            //pos(true);
//            RtpStreamReceiver.ringback(false);
        }
    }

    static String cache_text;
    static int cache_res;

    /*
    public static void onText(int type,String text,int mInCallResId,long base) {
        if (mSipdroidEngine != null && type == REGISTER_NOTIFICATION+mSipdroidEngine.pref) {
            cache_text = text;
            cache_res = mInCallResId;
        }
        if (type >= REGISTER_NOTIFICATION && mInCallResId == R.drawable.sym_presence_available &&
                !PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(Settings.PREF_REGISTRATION, Settings.DEFAULT_REGISTRATION))
            text = null;
        NotificationManager mNotificationMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (text != null) {
            Notification notification = new Notification();
            notification.icon = mInCallResId;
            if (type == MISSED_CALL_NOTIFICATION) {
                notification = new Notification.Builder(mContext)
                        .setSmallIcon(mInCallResId)
                        .setContentIntent(PendingIntent.getActivity(mContext, 0, createCallLogIntent(), 0))
                        .setContentTitle(mContext.getString(R.string.app_name))
                        .setContentText(text)
                        .build();
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                if (PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).getBoolean(Settings.PREF_NOTIFY, Settings.DEFAULT_NOTIFY)) {
                    notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                    notification.ledARGB = 0xff0000ff; // blue
                    notification.ledOnMS = 125;
                    notification.ledOffMS = 2875;
                }
            } else {
                switch (type) {
                    case MWI_NOTIFICATION:
                        notification.flags |= Notification.FLAG_AUTO_CANCEL;
                        notification.contentIntent = PendingIntent.getActivity(mContext, 0,
                                createMWIIntent(), 0);
                        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                        notification.ledARGB = 0xff00ff00; //green
                        notification.ledOnMS = 125;
                        notification.ledOffMS = 2875;
                        break;
                    case AUTO_ANSWER_NOTIFICATION:
                        notification.contentIntent = PendingIntent.getActivity(mContext, 0,
                                createIntent(AutoAnswer.class), 0);
                        break;
                    default:
                        if (type >= REGISTER_NOTIFICATION && mSipdroidEngine != null && type != REGISTER_NOTIFICATION+mSipdroidEngine.pref &&
                                mInCallResId == R.drawable.sym_presence_available)
                            notification.contentIntent = PendingIntent.getActivity(mContext, 0,
                                    createIntent(ChangeAccount.class), 0);
                        else
                            notification.contentIntent = PendingIntent.getActivity(mContext, 0,
                                    createIntent(Sipdroid.class), 0);
                        if (mInCallResId == R.drawable.sym_presence_away) {
                            notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                            notification.ledARGB = 0xffff0000; //red
                            notification.ledOnMS = 125;
                            notification.ledOffMS = 2875;
                        }
                        break;
                }
                notification.flags |= Notification.FLAG_ONGOING_EVENT;
                RemoteViews contentView = new RemoteViews(mContext.getPackageName(),
                        R.layout.ongoing_call_notification);
                contentView.setImageViewResource(R.id.icon, notification.icon);
                if (base != 0) {
                    contentView.setChronometer(R.id.text1, base, text+" (%s)", true);
                } else if (type >= REGISTER_NOTIFICATION) {
                    if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_POS, Settings.DEFAULT_POS))
                        contentView.setTextViewText(R.id.text2, text+"/"+mContext.getString(R.string.settings_pos3));
                    else
                        contentView.setTextViewText(R.id.text2, text);
                    if (mSipdroidEngine != null)
                        contentView.setTextViewText(R.id.text1,
                                mSipdroidEngine.user_profiles[type-REGISTER_NOTIFICATION].username+"@"+
                                        mSipdroidEngine.user_profiles[type-REGISTER_NOTIFICATION].realm_orig);
                } else
                    contentView.setTextViewText(R.id.text1, text);
                notification.contentView = contentView;
            }
            notification.contentIntent = null; // remove all the pending intent for glasses
            mNotificationMgr.notify(type,notification);
        } else {
            mNotificationMgr.cancel(type);
        }
        if (type != AUTO_ANSWER_NOTIFICATION)
            updateAutoAnswer();
        if (mSipdroidEngine != null && type >= REGISTER_NOTIFICATION && type != REGISTER_NOTIFICATION+mSipdroidEngine.pref)
            onText(REGISTER_NOTIFICATION+mSipdroidEngine.pref,cache_text,cache_res,0);
    }
    */

//    static void updateAutoAnswer() {
//        if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_AUTO_ONDEMAND, Settings.DEFAULT_AUTO_ONDEMAND) &&
//                Sipdroid.on(mContext)) {
//            if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_AUTO_DEMAND, Settings.DEFAULT_AUTO_DEMAND))
//                updateAutoAnswer(1);
//            else
//                updateAutoAnswer(0);
//        } else
//            updateAutoAnswer(-1);
//    }

    private static int autoAnswerState = -1;

//    static void updateAutoAnswer(int status) {
//        if (status != autoAnswerState) {
//            switch (autoAnswerState = status) {
//                case 0:
//                    //Receiver.onText(Receiver.AUTO_ANSWER_NOTIFICATION,mContext.getString(R.string.auto_disabled),R.drawable.auto_answer_disabled,0);
//                    break;
//                case 1:
//                    //Receiver.onText(Receiver.AUTO_ANSWER_NOTIFICATION,mContext.getString(R.string.auto_enabled),R.drawable.auto_answer,0);
//                    break;
//                case -1:
//                    //Receiver.onText(Receiver.AUTO_ANSWER_NOTIFICATION, null, 0, 0);
//                    break;
//            }
//        }
//    }

    public static void registered() {
        pos(true);
    }

    static LocationManager lm;
    static AlarmManager am;
    static PendingIntent gps_sender,net_sender;
    static boolean net_enabled;
    static long loctrydate;

    static final int GPS_UPDATES = 4000*1000;
    static final int NET_UPDATES = 600*1000;
//
    public static void pos(boolean enable) {

//        if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_POS, Settings.DEFAULT_POS) ||
//                PreferenceManager.getDefaultSharedPreferences(mContext).getString(Settings.PREF_POSURL, Settings.DEFAULT_POSURL).length() < 1) {
//            if (lm != null && am != null) {
//                //pos_gps(false);
//                //pos_net(false);
//                lm = null;
//                am = null;
//            }
//            return;
//        }
//
//        if (lm == null) lm = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
//        if (am == null) am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
//        //pos_gps(false);
//        if (enable) {
//            if (call_state == UserAgent.UA_STATE_IDLE && SipEngine.on(mContext) &&
//                    PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_POS, Settings.DEFAULT_POS) &&
//                    PreferenceManager.getDefaultSharedPreferences(mContext).getString(Settings.PREF_POSURL, Settings.DEFAULT_POSURL).length()>0) {
//                Location last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//                if (System.currentTimeMillis() - loctrydate > GPS_UPDATES && (last == null || System.currentTimeMillis() - last.getTime() > GPS_UPDATES)) {
//                    loctrydate = System.currentTimeMillis();
//                    //pos_gps(true);
//                    //pos_net(false);
//                } else if (last != null)
//                    Receiver.url("lat="+last.getLatitude()+"&lon="+last.getLongitude()+"&rad="+last.getAccuracy());
//                //pos_net(true);
//            } else {
//                //pos_net(false);
//            }
//        }
    }
//
//    static void pos_gps(boolean enable) {
//        if (gps_sender == null) {
//            Intent intent = new Intent(mContext, OneShotLocation.class);
//            gps_sender = PendingIntent.getBroadcast(mContext,
//                    0, intent, 0);
//        }
//        if (enable) {
//            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_UPDATES, 3000, gps_sender);
//            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+10*1000, gps_sender);
//        } else {
//            am.cancel(gps_sender);
//            lm.removeUpdates(gps_sender);
//        }
//    }
//
//    static void pos_net(boolean enable) {
//        if (net_sender == null) {
//            Intent loopintent = new Intent(mContext, LoopLocation.class);
//            net_sender = PendingIntent.getBroadcast(mContext,
//                    0, loopintent, 0);
//        }
//        if (net_enabled != enable) {
//            if (enable) {
//                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, NET_UPDATES, 3000, net_sender);
//            } else {
//                lm.removeUpdates(net_sender);
//            }
//            net_enabled = enable;
//        }
//    }
//
//    static void enable_wifi(boolean enable) {
//        if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_OWNWIFI, Settings.DEFAULT_OWNWIFI))
//            return;
//        if (enable && !PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_WIFI_DISABLED, Settings.DEFAULT_WIFI_DISABLED))
//            return;
//        WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
//        ContentResolver cr = Receiver.mContext.getContentResolver();
//        if (!enable && Settings.Secure.getInt(cr, Settings.Secure.WIFI_ON,0) == 0)
//            return;
//        Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
//
//        edit.putBoolean(Settings.PREF_WIFI_DISABLED,!enable);
//        edit.commit();
//    		/*
//    		if (enable) {
//                Intent intent = new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION);
//                intent.putExtra(WifiManager.EXTRA_NEW_STATE, wm.getWifiState());
//                mContext.sendBroadcast(intent);
//    		}
//    		*/
//        wm.setWifiEnabled(enable);
//    }

    public static void url(final String opt) {
        (new Thread() {
            public void run() {
                try {
                    URL url = new URL(PreferenceManager.getDefaultSharedPreferences(mContext).getString(Settings.PREF_POSURL, Settings.DEFAULT_POSURL)+
                            "?"+opt);
                    BufferedReader in;
                    in = new BufferedReader(new InputStreamReader(url.openStream()));
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    static boolean was_playing;

    static void broadcastCallStateChanged(String state,String number) {
        if (state == null) {
            state = laststate;
            number = lastnumber;
        }
        if (android.os.Build.VERSION.SDK_INT < 19) {
            Intent intent = new Intent(ACTION_PHONE_STATE_CHANGED);
            intent.putExtra("state",state);
            if (number != null)
                intent.putExtra("incoming_number", number);
            intent.putExtra(mContext.getString(R.string.app_name), true);
            mContext.sendBroadcast(intent, android.Manifest.permission.READ_PHONE_STATE);
        }
        if (state.equals("IDLE")) {
            if (was_playing) {
                if (pstn_state == null || pstn_state.equals("IDLE"))
                    mContext.sendBroadcast(new Intent(TOGGLEPAUSE_ACTION));
                was_playing = false;
            }
        } else {
            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            if ((laststate == null || laststate.equals("IDLE")) && (was_playing = am.isMusicActive()))
                mContext.sendBroadcast(new Intent(PAUSE_ACTION));
        }
        laststate = state;
        lastnumber = number;
    }

    public static void alarm(int renew_time,Class <?>cls) {
       // if (!Sipdroid.release) Log.i("SipUA:","alarm "+renew_time);
        Intent intent = new Intent(mContext, cls);
        PendingIntent sender = PendingIntent.getBroadcast(mContext,
                0, intent, 0);
        AlarmManager am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
        if (renew_time > 0)
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+renew_time*1000, sender);
    }

    public static long expire_time;

    public static synchronized void reRegister(int renew_time) {
        if (renew_time == 0)
            expire_time = 0;
        else {
            if (expire_time != 0 && renew_time*1000 + SystemClock.elapsedRealtime() > expire_time) return;
            expire_time = renew_time*1000 + SystemClock.elapsedRealtime();
        }
        alarm(renew_time-15, OneShotAlarm.class);
    }

    static Intent createIntent(Class<?>cls) {
        Intent startActivity = new Intent();
        startActivity.setClass(mContext,cls);
        startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return startActivity;
    }

    public static Intent createCallLogIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW, null);
        intent.setType("vnd.android.cursor.dir/calls");
        return intent;
    }

    static Intent createHomeDockIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        if (docked == EXTRA_DOCK_STATE_CAR) {
            intent.addCategory(CATEGORY_CAR_DOCK);
        } else if (docked == EXTRA_DOCK_STATE_DESK) {
            intent.addCategory(CATEGORY_DESK_DOCK);
        } else {
            return null;
        }

        ActivityInfo ai = intent.resolveActivityInfo(
                mContext.getPackageManager(), PackageManager.GET_META_DATA);
        if (ai == null) {
            return null;
        }

        if (ai.metaData != null && ai.metaData.getBoolean(METADATA_DOCK_HOME)) {
            intent.setClassName(ai.packageName, ai.name);
            return intent;
        }

        return null;
    }

    public static Intent createHomeIntent() {
        Intent intent = createHomeDockIntent();
        if (intent != null) {
            try {
                return intent;
            } catch (ActivityNotFoundException e) {
            }
        }
        intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_HOME);
        return intent;
    }

    static Intent createMWIIntent() {
        Intent intent;

        if (MWI_account != null)
            intent = new Intent(Intent.ACTION_CALL, Uri.parse(MWI_account.replaceFirst("sip:","sipdroid:")));
        else
            intent = new Intent(Intent.ACTION_DIAL);
        return intent;
    }

//    public static void moveTop() {
//        //move this logic
//        progress();
//        mContext.startActivity(createIntent(Activity2.class));
//    }

//    public static void progress() {
//        if (call_state == UserAgent.UA_STATE_IDLE) return;
//        int mode = RtpStreamReceiver.speakermode;
//        if (mode == -1)
//            mode = speakermode();
//        if (mode == AudioManager.MODE_NORMAL)
//            Receiver.onText(Receiver.CALL_NOTIFICATION, mContext.getString(R.string.menu_speaker), android.R.drawable.stat_sys_speakerphone,Receiver.ccCall.base);
//        else if (bluetooth > 0)
//            Receiver.onText(Receiver.CALL_NOTIFICATION, mContext.getString(R.string.menu_bluetooth), R.drawable.stat_sys_phone_call_bluetooth,Receiver.ccCall.base);
//        else switch (call_state) {
//                case UserAgent.UA_STATE_INCALL:
//                    Receiver.onText(Receiver.CALL_NOTIFICATION, mContext.getString(R.string.card_title_in_progress), R.drawable.stat_sys_phone_call,Receiver.ccCall.base);
//                    break;
//                case UserAgent.UA_STATE_OUTGOING_CALL:
//                    Receiver.onText(Receiver.CALL_NOTIFICATION, mContext.getString(R.string.card_title_dialing), R.drawable.stat_sys_phone_call,Receiver.ccCall.base);
//                    break;
//                case UserAgent.UA_STATE_INCOMING_CALL:
//                    Receiver.onText(Receiver.CALL_NOTIFICATION, mContext.getString(R.string.card_title_incoming_call), R.drawable.stat_sys_phone_call,Receiver.ccCall.base);
//                    break;
//            }
//    }
//
    public static boolean on_wlan = true;
//
//    static boolean on_vpn() {
//        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_ON_VPN, Settings.DEFAULT_ON_VPN);
//    }
//
//    static void on_vpn(boolean enable) {
//        Editor edit = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext).edit();
//
//        edit.putBoolean(Settings.PREF_ON_VPN,enable);
//        edit.commit();
//    }

    public static boolean isFast(int i) {
        return true;
//        WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wi = wm.getConnectionInfo();
//
//        if (PreferenceManager.getDefaultSharedPreferences(mContext).getString(Settings.PREF_USERNAME+(i!=0?i:""),"").equals("") ||
//                PreferenceManager.getDefaultSharedPreferences(mContext).getString(Settings.PREF_SERVER+(i!=0?i:""),"").equals(""))
//            return false;
//        if (wi != null) {
////            if (!Sipdroid.release) Log.i("SipUA:","isFastWifi() "+WifiInfo.getDetailedStateOf(wi.getSupplicantState())
////                    +" "+wi.getIpAddress());
//            if (wi.getIpAddress() != 0 && (WifiInfo.getDetailedStateOf(wi.getSupplicantState()) == DetailedState.OBTAINING_IPADDR
//                    || WifiInfo.getDetailedStateOf(wi.getSupplicantState()) == DetailedState.CONNECTED)
//                    || WifiInfo.getDetailedStateOf(wi.getSupplicantState()) == DetailedState.CONNECTING) {
//                on_wlan = true;
//                if (!on_vpn())
//                    return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_WLAN+(i!=0?i:""), Settings.DEFAULT_WLAN);
//                else
//                    return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_VPN+(i!=0?i:""), Settings.DEFAULT_VPN);
//            }
//        }
//        on_wlan = false;
//        return isFastGSM(i);
    }

//    static boolean isFastGSM(int i) {
//        TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
//
//        if (Sipdroid.market)
//            return false;
//        if (on_vpn() && (tm.getNetworkType() >= TelephonyManager.NETWORK_TYPE_EDGE))
//            return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_VPN+(i!=0?i:""), Settings.DEFAULT_VPN);
//        if (tm.getNetworkType() >= TelephonyManager.NETWORK_TYPE_UMTS)
//            return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_3G+(i!=0?i:""), Settings.DEFAULT_3G);
//        if (tm.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE)
//            return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_EDGE+(i!=0?i:""), Settings.DEFAULT_EDGE);
//        return false;
//    }

    public static int speakermode() {
        if (docked > 0 && headset <= 0)
            return AudioManager.MODE_NORMAL;
        else
            return AudioManager.MODE_IN_CALL;
    }

    static Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SCAN:
                    WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                    wm.startScan();
                    break;
                case MSG_ENABLE:
                    //enable_wifi(true);
                    break;
                case MSG_HOLD:
                    engine(mContext).togglehold();
                    break;
                case MSG_HANGUP:
                    engine(mContext).rejectcall();
                    break;
            }
        }
    };

    int asu(ScanResult scan) {
        if (scan == null)
            return 0;
        return Math.round((scan.level + 113f) / 2f);
    }

    static long lastscan;

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (!SipEngine.on(context)) return;
        //if (!Sipdroid.release) Log.i("SipUA:",intentAction);
        if (mContext == null) mContext = context;
        if (intentAction.equals(Intent.ACTION_BOOT_COMPLETED)){
            //on_vpn(false);
            //engine(context).register();
        } else
        if (intentAction.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                intentAction.equals(ACTION_EXTERNAL_APPLICATIONS_AVAILABLE) ||
                intentAction.equals(ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE) ||
                intentAction.equals(Intent.ACTION_PACKAGE_REPLACED)) {
            engine(context).register();
        } else
        if (intentAction.equals(ACTION_VPN_CONNECTIVITY) && intent.hasExtra("connection_state")) {
//            String state = intent.getSerializableExtra("connection_state").toString();
//            if (state != null && on_vpn() != state.equals("CONNECTED")) {
//                on_vpn(state.equals("CONNECTED"));
//                for (SipProvider sip_provider : engine(context).sip_providers)
//                    if (sip_provider != null)
//                        sip_provider.haltConnections();
//                engine(context).register();
//            }
        }
        else if (intentAction.equals(ACTION_DATA_STATE_CHANGED)) {
            engine(context).registerMore();
        } else if (intentAction.equals(ACTION_PHONE_STATE_CHANGED) &&
                !intent.getBooleanExtra(context.getString(R.string.app_name),false)) {
            stopRingtone();
            pstn_state = intent.getStringExtra("state");
            pstn_time = SystemClock.elapsedRealtime();
            if (pstn_state.equals("IDLE") && call_state != UserAgent.UA_STATE_IDLE)
                broadcastCallStateChanged(null,null);
            if (!pstn_state.equals("IDLE") && call_state == UserAgent.UA_STATE_INCALL)
                mHandler.sendEmptyMessageDelayed(MSG_HOLD, 5000);
            else if (!pstn_state.equals("IDLE") && (call_state == UserAgent.UA_STATE_INCOMING_CALL || call_state == UserAgent.UA_STATE_OUTGOING_CALL))
                mHandler.sendEmptyMessageDelayed(MSG_HANGUP, 5000);
            else if (pstn_state.equals("IDLE")) {
                mHandler.removeMessages(MSG_HOLD);
                mHandler.removeMessages(MSG_HANGUP);
                if (call_state == UserAgent.UA_STATE_HOLD)
                    mHandler.sendEmptyMessageDelayed(MSG_HOLD, 1000);
            }
        } else
        if (intentAction.equals(ACTION_DOCK_EVENT)) {
//            docked = intent.getIntExtra(EXTRA_DOCK_STATE, -1) & 7;
//            if (call_state == UserAgent.UA_STATE_INCALL)
//                engine(mContext).speaker(speakermode());
        } else
        if (intentAction.equals(ACTION_SCO_AUDIO_STATE_CHANGED)) {
            bluetooth = intent.getIntExtra(EXTRA_SCO_AUDIO_STATE, -1);
            //progress();
            //RtpStreamSender.changed = true;
        } else
        if (intentAction.equals(Intent.ACTION_HEADSET_PLUG)) {
//            headset = intent.getIntExtra("state", -1);
//            if (call_state == UserAgent.UA_STATE_INCALL)
//                engine(mContext).speaker(speakermode());
        } else
        if (intentAction.equals(Intent.ACTION_SCREEN_ON)) {
            //if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_OWNWIFI, Settings.DEFAULT_OWNWIFI))
            //    alarm(0,OwnWifi.class);
        } else
        if (intentAction.equals(Intent.ACTION_USER_PRESENT)) {
            mHandler.sendEmptyMessageDelayed(MSG_ENABLE,3000);
        } else
        if (intentAction.equals(Intent.ACTION_SCREEN_OFF)) {
            if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_OWNWIFI, Settings.DEFAULT_OWNWIFI)) {
                WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wi = wm.getConnectionInfo();
                //if (wm.getWifiState() != WifiManager.WIFI_STATE_ENABLED || wi == null || wi.getSupplicantState() != SupplicantState.COMPLETED
                 //       || wi.getIpAddress() == 0)
                    //alarm(2*60,OwnWifi.class);
                //else
                    //alarm(15*60,OwnWifi.class);
            }
            if (SipdroidEngine.pwl != null)
                for (PowerManager.WakeLock pwl : SipdroidEngine.pwl)
                    if (pwl != null && pwl.isHeld()) {
                        pwl.release();
                        pwl.acquire();
                    }
        } else
        if (intentAction.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_SELECTWIFI, Settings.DEFAULT_SELECTWIFI))
                mHandler.sendEmptyMessageDelayed(MSG_SCAN, 3000);
        } else
        if (intentAction.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            if (SystemClock.uptimeMillis() > lastscan + 45000 &&
                    PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(Settings.PREF_SELECTWIFI, Settings.DEFAULT_SELECTWIFI)) {
                WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wi = wm.getConnectionInfo();
                String activeSSID = null;
                if (wi != null) activeSSID = wi.getSSID();
                if (activeSSID != null && (activeSSID.length() == 0 || activeSSID.equals("0x"))) activeSSID = null;
                List<ScanResult> mScanResults = wm.getScanResults();
                List<WifiConfiguration> configurations = wm.getConfiguredNetworks();
                if (configurations != null) {
                    WifiConfiguration bestconfig = null,maxconfig = null,activeconfig = null;
                    for(final WifiConfiguration config : configurations) {
                        if (maxconfig == null || config.priority > maxconfig.priority) {
                            maxconfig = config;
                        }
                        if (config.SSID != null && (config.SSID.equals("\""+activeSSID+"\"") ||
                                config.SSID.equals(activeSSID)))
                            activeconfig = config;
                    }
                    ScanResult bestscan = null,activescan = null;
                    if (mScanResults != null)
                        for(final ScanResult scan : mScanResults) {
                            for(final WifiConfiguration config : configurations) {
                                if (config.SSID != null && config.SSID.equals("\""+scan.SSID+"\"")) {
                                    if (bestscan == null || scan.level > bestscan.level) {
                                        bestscan = scan;
                                        bestconfig = config;
                                    }
                                    if (config == activeconfig)
                                        activescan = scan;
                                }
                            }
                        }
                    if (bestconfig != null &&
                            (activeconfig == null || bestconfig.priority != activeconfig.priority) &&
                            asu(bestscan) > asu(activescan)*1.5 &&
                            /* (activeSSID == null || activescan != null) && */ asu(bestscan) >= 16) {
//                        if (!Sipdroid.release) Log.i("SipUA:","changing to "+bestconfig.SSID);
                        if (activeSSID == null || !activeSSID.equals(bestscan.SSID))
                            wm.disconnect();
                        bestconfig.priority = maxconfig.priority + 1;
                        wm.updateNetwork(bestconfig);
                        wm.enableNetwork(bestconfig.networkId, true);
                        wm.saveConfiguration();
                        if (activeSSID == null || !activeSSID.equals(bestscan.SSID))
                            wm.reconnect();
                        lastscan = SystemClock.uptimeMillis();
                    } else if (activescan != null && asu(activescan) < 15) {
                        wm.disconnect();
                        wm.disableNetwork(activeconfig.networkId);
                        wm.saveConfiguration();
                    }
                }
            }
        }
    }


    public static void initSip(Context context, SipParam param) {
        mContext = context;
        //账号信息设置，具体的账号信息自己修改
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().
                putString(Settings.PREF_USERNAME, param.getUsername())
                .putString(Settings.PREF_PASSWORD, param.getPassword())
                .putString(Settings.PREF_SERVER, param.getServer())//"服务器地址"
                .putString(Settings.PREF_DOMAIN, param.getDomain())
                .putString(Settings.PREF_PORT, param.getPort())// "服务器端口"
                .putString(Settings.PREF_PROTOCOL, param.getProtocol())//传输协议
                .putBoolean(Settings.PREF_CALLBACK, param.callback())
                .putString(Settings.PREF_POSURL, param.getPosUrl())
                .putBoolean(Settings.PREF_ON, param.isOpen())
                .putBoolean(Settings.PREF_3G, param.is3G())
                .putBoolean(Settings.PREF_WLAN, param.isWlan())
                .putBoolean(Settings.PREF_EDGE, param.edge())
                .putString(Settings.PREF_FROMUSER, param.getFromUser())
                .commit();

        //注册账号
//			Receiver.engine(mContext).register();
        Receiver.engine(mContext).halt();
        Receiver.engine(mContext).StartEngine();
//			Sipdroid.on(mContext, true);
    }

    public static void finishSip() {
        if (mContext == null) {
            return;
        }
        SharedPreferences.Editor edit = PreferenceManager
                .getDefaultSharedPreferences(mContext)
                .edit().putBoolean(Settings.PREF_ON, false);
        edit.commit();
        SipEngine.on(mContext, false);
        Receiver.pos(true);
        Receiver.engine(mContext).halt();
        Receiver.mSipdroidEngine = null;
        Receiver.reRegister(0);
        //mContext.stopService(new Intent(mContext, RegisterService.class));

        if (mStateHandler != null) {
            mStateHandler.removeCallbacksAndMessages(null);
            mStateHandler = null;
        }
        handlerCallback = null;
    }

}
