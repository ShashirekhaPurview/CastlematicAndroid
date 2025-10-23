package com.shashi.castlematic.features.user_management.api;

import com.shashi.castlematic.features.user_management.models.UserManagementModels.*;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UserManagementApiService {

    // Onboard User
    @POST("api/v1/user/onboard")
    Call<ResponseBody> onboardUser(
            @Header("Authorization") String authorization,
            @Body OnboardUserRequest request
    );

    // Onboard Driver
    @Multipart
    @POST("api/drivers-records")
    Call<ResponseBody> onboardDriver(
            @Header("Authorization") String authorization,
            @Header("client-id") String clientId,
            @Part("driver_name") RequestBody driverName,
            @Part("phone_number") RequestBody phoneNumber,
            @Part("licence_number") RequestBody licenceNumber,
            @Part("expire_date") RequestBody expireDate,
            @Part("joining_date") RequestBody joiningDate,
            @Part("aadhar_card") RequestBody aadharCard,
            @Part MultipartBody.Part licencePhoto
    );

    // Request models
    class OnboardUserRequest {
        public String user_id;
        public String user_name;
        public String user_role;
        public String client_id;
        public String user_password;

        public OnboardUserRequest(String userId, String userName, String userRole,
                                  String clientId, String userPassword) {
            this.user_id = userId;
            this.user_name = userName;
            this.user_role = userRole;
            this.client_id = clientId;
            this.user_password = userPassword;
        }
    }
}
