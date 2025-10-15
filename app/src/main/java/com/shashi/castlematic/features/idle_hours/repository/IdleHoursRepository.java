package com.shashi.castlematic.features.idle_hours.repository;

import android.util.Log;

import com.shashi.castlematic.core.network.ApiClient;
import com.shashi.castlematic.features.idle_hours.api.IdleHoursApiService;
import com.shashi.castlematic.features.idle_hours.models.IdleModels.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IdleHoursRepository {
    private static final String TAG = "IdleHoursRepository";
    private final IdleHoursApiService apiService;

    public IdleHoursRepository() {
        this.apiService = ApiClient.createService(IdleHoursApiService.class);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    // Fetch vehicle idling hours
    public void getIdlingHours(String fromDate, String toDate, String ownedOrHired,
                               DataCallback<VehicleIdleResponse> callback) {
        IdleHoursRequest request = new IdleHoursRequest(fromDate, toDate, ownedOrHired);

        Log.d(TAG, "Fetching idling hours: " + fromDate + " to " + toDate + ", filter: " + ownedOrHired);

        Call<VehicleIdleResponse> call = apiService.getIdlingHours(request);

        call.enqueue(new Callback<VehicleIdleResponse>() {
            @Override
            public void onResponse(Call<VehicleIdleResponse> call, Response<VehicleIdleResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Successfully fetched " + response.body().vehicles.size() + " vehicles");
                    callback.onSuccess(response.body());
                } else {
                    String error = "Failed to fetch idling hours: " + response.code();
                    Log.e(TAG, error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<VehicleIdleResponse> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, error, t);
                callback.onError(error);
            }
        });
    }

    // Assign idle reason to vehicle
    public void assignIdleReason(AssignReasonRequest request, DataCallback<Void> callback) {
        Log.d(TAG, "Assigning reason: " + request.assignedReason + " for " +
                request.assignedHours + " hours to vehicle: " + request.vehicleName);

        Call<Void> call = apiService.assignIdleReason(request);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Successfully assigned idle reason");
                    callback.onSuccess(null);
                } else {
                    String error = "Failed to assign reason: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            error = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    Log.e(TAG, error);
                    callback.onError(error);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String error = "Network error: " + t.getMessage();
                Log.e(TAG, error, t);
                callback.onError(error);
            }
        });
    }
}
