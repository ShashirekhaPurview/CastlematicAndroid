package com.shashi.castlematic.core.network;

import android.content.Context;
import android.util.Log;

import com.shashi.castlematic.core.config.AppConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static Retrofit retrofit;
    private static Context context;
    private static OkHttpClient okHttpClient;

    public static void initialize(Context ctx) {
        context = ctx.getApplicationContext();

        // Create OkHttpClient with interceptor
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(ctx));

        // Add logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addInterceptor(loggingInterceptor);

        okHttpClient = builder.build();

        // Use the correct base URL
        String baseUrl = AppConfig.getCurrentBaseUrl();
        Log.d(TAG, "Initializing ApiClient with base URL: " + baseUrl);

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // ADD THIS METHOD - it was missing!
    public static <S> S createService(Class<S> serviceClass) {
        if (retrofit == null) {
            throw new IllegalStateException("ApiClient must be initialized before creating services");
        }
        return retrofit.create(serviceClass);
    }

    public static Retrofit getRetrofit() {
        return retrofit;
    }

    public static Context getContext() {
        return context;
    }
}
