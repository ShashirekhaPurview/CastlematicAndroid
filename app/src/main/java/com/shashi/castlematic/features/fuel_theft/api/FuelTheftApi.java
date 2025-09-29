package com.shashi.castlematic.features.fuel_theft.api;

import com.shashi.castlematic.core.config.AppConfig;
import com.shashi.castlematic.features.fuel_theft.models.TheftModels.*;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import java.util.List;

public interface FuelTheftApi {

    @POST(AppConfig.FUEL_THEFT)
    Call<List<TheftAlert>> getTheftAlerts(@Body DateRequest request);

    @POST(AppConfig.FUEL_THEFT_LOGS)
    Call<List<TheftLog>> getTheftLogs(@Body DateRequest request);

    @POST(AppConfig.FUEL_THEFT_LOG)
    Call<TheftLog> createTheftLog(@Body CreateLogRequest request);

    @POST(AppConfig.FUEL_THEFT_RESOLVE)
    Call<TheftLog> resolveTheft(@Body ResolveRequest request);

    @POST(AppConfig.VEHICLE_COUNTS)
    Call<VehicleCountResponse> getVehicleCounts(@Body DateRequest request);
}
