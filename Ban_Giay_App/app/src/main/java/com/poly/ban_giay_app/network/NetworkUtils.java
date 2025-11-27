package com.poly.ban_giay_app.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Response;

public final class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    private NetworkUtils() {
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network network = cm.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    public static String extractErrorMessage(Response<?> response) {
        if (response == null || response.errorBody() == null) {
            return "Có lỗi xảy ra. Vui lòng thử lại.";
        }
        ResponseBody errorBody = response.errorBody();
        try {
            String raw = errorBody.string();
            if (TextUtils.isEmpty(raw)) {
                return "Có lỗi xảy ra. Vui lòng thử lại.";
            }
            JSONObject json = new JSONObject(raw);
            if (json.has("message")) {
                return json.getString("message");
            }
            if (json.has("error")) {
                return json.getString("error");
            }
            return raw;
        } catch (Exception e) {
            Log.e(TAG, "extractErrorMessage: ", e);
            return "Không thể kết nối máy chủ.";
        } finally {
            try {
                errorBody.close();
            } catch (Exception ignored) {
            }
        }
    }
}
