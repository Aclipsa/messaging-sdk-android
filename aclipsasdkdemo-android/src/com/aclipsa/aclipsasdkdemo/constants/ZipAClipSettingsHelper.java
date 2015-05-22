package com.aclipsa.aclipsasdkdemo.constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Created by tonyfarag on 11/20/13.
 */
public class ZipAClipSettingsHelper {

    private static volatile String APP_PREFERENCES = "com.aclipsa.zipaclip.PREFERENCES";

    public static void setPreference(String pref_identifier){
        APP_PREFERENCES = pref_identifier;
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    private static Editor getEditor(Context context) {
        return getPreferences(context).edit();
    }

    public static String getString(Context context, String key, String defaultValue) {
        return getPreferences(context).getString(key, defaultValue);
    }

    public static void setString(Context context, String key, String value) {
        getEditor(context).putString(key, value).commit();
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return getPreferences(context).getBoolean(key, defaultValue);
    }

    public static void setBoolean(Context context, String key, boolean value) {
        getEditor(context).putBoolean(key, value).commit();
    }

    public static long getLong(Context context, String key, long defaultValue) {
        return getPreferences(context).getLong(key, defaultValue);
    }

    public static void setLong(Context context, String key, long value) {
        getEditor(context).putLong(key, value).commit();
    }

    public static int getInt(Context context, String key, int defaultValue) {
        return getPreferences(context).getInt(key, defaultValue);
    }

    public static void setInt(Context context, String key, int value) {
        getEditor(context).putInt(key, value).commit();
    }

    public static void clearAll(Context context) {
        getEditor(context).clear().commit();
    }

    public static void clearString(Context context, String key) {
        getEditor(context).remove(key).commit();
    }


}
