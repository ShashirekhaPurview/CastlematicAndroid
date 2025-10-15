package com.shashi.castlematic.features.idle_hours.api;

import com.shashi.castlematic.features.idle_hours.models.IdleModels.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface IdleHoursApiService {

    @POST("vehicle-idling-hours")
    Call<VehicleIdleResponse> getIdlingHours(@Body IdleHoursRequest request);

    @POST("vehicle-idle-reason")
    Call<Void> assignIdleReason(@Body AssignReasonRequest request);
}
