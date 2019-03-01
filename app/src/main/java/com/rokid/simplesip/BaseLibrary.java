package com.rokid.simplesip;

import android.app.Application;

import com.rokid.simplesip.tools.Logger;

import java.lang.ref.WeakReference;

/**
 * Author: zhuohf
 * Version: V0.1 2018/2/19
 */
public class BaseLibrary {

    private static volatile BaseLibrary mInstance;

    private WeakReference<Application> applicationWeak;

    public static BaseLibrary getInstance() {
        if (null == mInstance) {
            synchronized (BaseLibrary.class) {
                if (null == mInstance) {
                    mInstance = new BaseLibrary();
                }
            }
        }
        return mInstance;
    }

    public static void initialize(Application application) {
        Logger.d("Start to init the XBase lib.");

        // Init Config
        getInstance().applicationWeak = new WeakReference<>(application);
    }

    public Application getContext() {
        if (null == applicationWeak || null == applicationWeak.get()) {
            applicationWeak = new WeakReference<>(getApplicationContext());
        }

        return applicationWeak.get();
    }

    public static Application getApplicationContext() {
        try {
            Application application = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null, (Object[]) null);
            if (application != null) {
                return application;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Application application = (Application) Class.forName("android.app.AppGlobals")
                    .getMethod("getInitialApplication").invoke(null, (Object[]) null);
            if (application != null) {
                return application;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
