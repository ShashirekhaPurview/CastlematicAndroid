package com.shashi.castlematic.core.network;

import com.shashi.castlematic.features.fuel_theft.models.TheftModels.*;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface AuthApiService {

    @FormUrlEncoded
    @POST("auth/token")
    Call<AuthResponse> login(
            @Field("grant_type") String grantType,
            @Field("username") String username,
            @Field("password") String password,
            @Field("scope") String scope,
            @Field("client_id") String clientId,
            @Field("client_secret") String clientSecret
    );
}
