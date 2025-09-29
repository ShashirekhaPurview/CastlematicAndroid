package com.shashi.castlematic.features.fuel_theft.repository;

import com.shashi.castlematic.core.network.ApiClient;
import com.shashi.castlematic.features.fuel_theft.api.FuelTheftApi;
import com.shashi.castlematic.features.fuel_theft.models.TheftModels.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.List;

public class FuelTheftRepository {

    private FuelTheftApi api;

    public FuelTheftRepository() {
        api = ApiClient.getClient().create(FuelTheftApi.class);
    }

    // Get theft alerts (pending review)
    public void getTheftAlerts(String fromDate, String toDate, String ownedOrHired,
                               DataCallback<List<TheftAlert>> callback) {

        DateRequest request = new DateRequest(fromDate, toDate, ownedOrHired);
        Call<List<TheftAlert>> call = api.getTheftAlerts(request);

        call.enqueue(new Callback<List<TheftAlert>>() {
            @Override
            public void onResponse(Call<List<TheftAlert>> call, Response<List<TheftAlert>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to get alerts: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<TheftAlert>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Get theft logs (confirmed, resolved, false)
    public void getTheftLogs(String fromDate, String toDate, String ownedOrHired,
                             DataCallback<List<TheftLog>> callback) {

        DateRequest request = new DateRequest(fromDate, toDate, ownedOrHired);
        Call<List<TheftLog>> call = api.getTheftLogs(request);

        call.enqueue(new Callback<List<TheftLog>>() {
            @Override
            public void onResponse(Call<List<TheftLog>> call, Response<List<TheftLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to get logs: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<TheftLog>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Mark alert as theft or false
    public void markAlert(CreateLogRequest request, DataCallback<TheftLog> callback) {
        Call<TheftLog> call = api.createTheftLog(request);

        call.enqueue(new Callback<TheftLog>() {
            @Override
            public void onResponse(Call<TheftLog> call, Response<TheftLog> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to mark alert: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<TheftLog> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Resolve theft
    public void resolveTheft(String vehicleName, String dateOfData, String comments,
                             DataCallback<TheftLog> callback) {

        ResolveRequest request = new ResolveRequest(vehicleName, dateOfData, comments);
        Call<TheftLog> call = api.resolveTheft(request);

        call.enqueue(new Callback<TheftLog>() {
            @Override
            public void onResponse(Call<TheftLog> call, Response<TheftLog> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to resolve theft: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<TheftLog> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Get vehicle counts for cards
    public void getVehicleCounts(String fromDate, String toDate, String ownedOrHired,
                                 DataCallback<VehicleCountResponse> callback) {

        DateRequest request = new DateRequest(fromDate, toDate, ownedOrHired);
        Call<VehicleCountResponse> call = api.getVehicleCounts(request);

        call.enqueue(new Callback<VehicleCountResponse>() {
            @Override
            public void onResponse(Call<VehicleCountResponse> call, Response<VehicleCountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to get vehicle counts: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<VehicleCountResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Simple callback interface
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }
}
