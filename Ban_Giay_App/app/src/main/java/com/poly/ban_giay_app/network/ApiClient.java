package com.poly.ban_giay_app.network;

import com.poly.ban_giay_app.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static final long TIMEOUT_SECONDS = 30L;
    private static ApiService apiService;

    private ApiClient() {
        // no-op
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            synchronized (ApiClient.class) {
                if (apiService == null) {
                    apiService = buildRetrofit().create(ApiService.class);
                }
            }
        }
        return apiService;
    }

    private static Retrofit buildRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
