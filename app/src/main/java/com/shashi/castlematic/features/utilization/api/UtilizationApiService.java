package com.shashi.castlematic.features.utilization.api;

import com.shashi.castlematic.features.utilization.models.UtilizationModels.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface UtilizationApiService {

    @POST("vehicle-stats")
    Call<VehicleStatsResponse> getVehicleStats(@Body StatsRequest request);

    // TODO: Add shift assignment endpoint when available
    // @POST("assign-shift")
    // Call<Void> assignShift(@Body ShiftAssignmentRequest request);
}
