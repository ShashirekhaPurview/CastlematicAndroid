package com.shashi.castlematic.core.network;

import android.content.Context;
import com.shashi.castlematic.core.config.AppConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkInterceptor implements Interceptor {

    private Context context;

    public NetworkInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        Request.Builder requestBuilder = originalRequest.newBuilder()
                .addHeader("Content-Type", "application/json")
                .addHeader("accept", "application/json")
                .addHeader("client-id", AppConfig.CLIENT_ID);

        // Add authorization header if available
        if (context != null) {
            AuthManager authManager = AuthManager.getInstance(context);
            String bearerToken = authManager.getBearerToken();
            if (bearerToken != null) {
                requestBuilder.addHeader("Authorization", bearerToken);
            }
        }

        Request newRequest = requestBuilder.build();
        return chain.proceed(newRequest);
    }
}
