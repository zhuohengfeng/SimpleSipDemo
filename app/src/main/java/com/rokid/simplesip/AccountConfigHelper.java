package com.rokid.simplesip;

import android.support.annotation.NonNull;

import com.rokid.simplesip.tools.JSONHelper;
import com.rokid.simplesip.tools.Logger;
import com.rokid.simplesip.ua.SipParam;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Author: zhuohf
 * Version: V0.1 2018/2/19
 */
public class AccountConfigHelper {

    private static final String FORMAT_CONFIG_ACCOUNT = "account/config_%s.json";
    private final static String CONFIG_DEFAULT = "default";
    private final static String CONFIG_HIK = "hik";
    private final static String CONFIG_OPEN = "open";

    private static final String CHARSET_NAME = "UTF-8";

    private static volatile AccountConfigHelper instance;

    public enum TAG {
        DEFAULT, OPEN, HIK
    }

    public static AccountConfigHelper getInstance() {
        if (null == instance) {
            synchronized (AccountConfigHelper.class) {
                if (null == instance) {
                    instance = new AccountConfigHelper();
                }
            }
        }
        return instance;
    }

    private AccountConfigHelper() {}

    public SipParam getAccountConfig(TAG tag) {
        String tagName = CONFIG_DEFAULT;
        switch (tag) {
            case OPEN:
                tagName = CONFIG_OPEN;
                break;
            case HIK:
                tagName = CONFIG_HIK;
                break;
            default:
                break;
        }

        String configFileName = String.format(FORMAT_CONFIG_ACCOUNT, tagName);
        String internalAppJson = getDefaultJson(configFileName);
        Logger.d("getAccountConfig internalAppJson="+internalAppJson);
        SipParam sipParam = JSONHelper.fromJson(internalAppJson, SipParam.class);
        Logger.d("getAccountConfig sipParam="+sipParam);
        return sipParam;
    }


    private String getDefaultJson(@NonNull String fileName) {
        Logger.d("Start to read default: " + fileName);
        try {
            return readInputStream(BaseLibrary.getInstance().getContext().getAssets().open(fileName));
        } catch (IOException e) {
            e.printStackTrace();
            Logger.w("Not have save the default config file.");
            return "";
        }
    }

    private String readInputStream(InputStream inputStream) {
        Logger.d("Start to read inputStream.");

        String jsonStr = "";

        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteOut.write(buffer, 0, len);
            }

            jsonStr = byteOut.toString(CHARSET_NAME);
            byteOut.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return jsonStr;
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return jsonStr;
    }


}
