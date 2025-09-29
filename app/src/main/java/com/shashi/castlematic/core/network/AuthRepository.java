package com.shashi.castlematic.core.network;

import com.shashi.castlematic.core.config.AppConfig;
import com.shashi.castlematic.features.fuel_theft.models.TheftModels.*;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class AuthRepository {

    private AuthApiService apiService;

    public AuthRepository() {
        // Create separate client for auth (without auth interceptor)
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .addHeader("accept", "application/json")
                        .build()))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppConfig.AUTH_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(AuthApiService.class);
    }

    public void login(String username, String password, AuthCallback callback) {
        Call<AuthResponse> call = apiService.login(
                "password",
                username,
                password,
                "",
                AppConfig.CLIENT_ID,
                AppConfig.CLIENT_SECRET
        );

        call.enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Login failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public interface AuthCallback {
        void onSuccess(AuthResponse response);
        void onError(String error);
    }
}
