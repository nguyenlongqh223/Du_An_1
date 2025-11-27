package com.poly.ban_giay_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.poly.ban_giay_app.network.model.UserResponse;

/**
 * Quản lý token và thông tin người dùng sau khi đăng nhập.
 */
public class SessionManager {
    private static final String PREFS_NAME = "ban_giay_session";
    private static final String KEY_IS_LOGGED_IN = "key_is_logged_in";
    private static final String KEY_TOKEN = "key_token";
    private static final String KEY_USER_ID = "key_user_id";
    private static final String KEY_USER_NAME = "key_user_name";
    private static final String KEY_FULL_NAME = "key_full_name";
    private static final String KEY_EMAIL = "key_email";

    private final SharedPreferences sharedPreferences;
    private final Context appContext;

    public SessionManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.sharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveAuthSession(String token, UserResponse user) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_TOKEN, token);
        if (user != null) {
            editor.putString(KEY_USER_ID, user.getId());
            editor.putString(KEY_USER_NAME, safeValue(user.getUsername()));
            editor.putString(KEY_FULL_NAME, safeValue(user.getFullName()));
            editor.putString(KEY_EMAIL, safeValue(user.getEmail()));
        }
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) && !TextUtils.isEmpty(getToken());
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, "");
    }

    public String getUserName() {
        String name = sharedPreferences.getString(KEY_FULL_NAME, null);
        if (TextUtils.isEmpty(name)) {
            name = sharedPreferences.getString(KEY_USER_NAME, null);
        }
        if (TextUtils.isEmpty(name)) {
            return appContext.getString(R.string.default_user_name);
        }
        return name;
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, "");
    }

    public void logout() {
        sharedPreferences.edit()
                .clear()
                .apply();
    }

    private String safeValue(String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return value;
    }
}
