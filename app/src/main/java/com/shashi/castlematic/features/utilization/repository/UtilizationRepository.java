package com.shashi.castlematic.features.utilization.repository;

import android.util.Log;

import com.shashi.castlematic.core.network.ApiClient;
import com.shashi.castlematic.features.utilization.api.UtilizationApiService;
import com.shashi.castlematic.features.utilization.models.UtilizationModels.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UtilizationRepository {
    private static final String TAG = "UtilizationRepository";
    private final UtilizationApiService apiService;

    public UtilizationRepository() {
        this.apiService = ApiClient.createService(UtilizationApiService.class);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    // Fetch vehicle statistics
    public void getVehicleStats(String fromDate, String toDate, String ownedOrHired,
                                DataCallback<VehicleStatsResponse> callback) {
        StatsRequest request = new StatsRequest(fromDate, toDate, ownedOrHired);

        Log.d(TAG, "Fetching vehicle stats: " + fromDate + " to " + toDate + ", filter: " + ownedOrHired);

        Call<VehicleStatsResponse> call = apiService.getVehicleStats(request);

        call.enqueue(new Callback<VehicleStatsResponse>() {
            @Override
            public void onResponse(Call<VehicleStatsResponse> call, Response<VehicleStatsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully fetched vehicle stats");
                    callback.onSuccess(response.body());
                } else {
                    String error = "Failed to fetch stats: " + response.code();
                    Log.e(TAG, error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<VehicleStatsResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, error, t);
                callback.onError(error);
            }
        });
    }

    // TODO: Implement shift assignment when endpoint is available
    public void assignShift(ShiftAssignmentRequest request, DataCallback<Void> callback) {
        // Placeholder for future implementation
        Log.d(TAG, "Shift assignment endpoint not yet available");
        callback.onError("Shift assignment endpoint not yet implemented");
    }
}
