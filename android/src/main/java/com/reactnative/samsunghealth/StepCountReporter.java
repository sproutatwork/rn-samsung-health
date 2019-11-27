package com.reactnative.samsunghealth;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataObserver;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadRequest;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadResult;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthResultHolder;
import com.samsung.android.sdk.healthdata.HealthDataResolver.Filter;

import android.util.Log;

import java.util.Calendar;
import java.util.TimeZone;

public class StepCountReporter {
    private final HealthDataStore mStore;
    private StepCountObserver mStepCountObserver;
    private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;

    private static final String REACT_MODULE = "RNSamsungHealth";

    public StepCountReporter(HealthDataStore store) {
        mStore = store;
    }

    public void start(StepCountObserver listener) {
        mStepCountObserver = listener;
        // Register an observer to listen changes of step count and get today step count
        HealthDataObserver.addObserver(mStore, SamsungHealthModule.STEP_DAILY_TREND_TYPE, mObserver);
        readTodayStepCount();
    }

    public void cancel(StepCountObserver listener) {
        HealthDataObserver.removeObserver(mStore, mObserver);
    }

    // Read the today's step count on demand
    private void readTodayStepCount() {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range from start time of today to the current time
        long startTime = getStartTimeOfToday();
        long endTime = startTime + ONE_DAY_IN_MILLIS;

        Filter filter = Filter.and(
            Filter.greaterThanEquals(SamsungHealthModule.DAY_TIME, startTime),
            Filter.lessThanEquals(SamsungHealthModule.DAY_TIME, endTime)
        );

        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                    .setDataType(SamsungHealthModule.STEP_DAILY_TREND_TYPE)
                    .setProperties(new String[] {
                        HealthConstants.StepCount.COUNT,
                        SamsungHealthModule.DAY_TIME
                        })
                    .setFilter(filter)
                    .build();

        try {
            resolver.read(request).setResultListener(mListener);
        } catch (Exception e) {
            Log.e(REACT_MODULE, "Samsung Health: Getting step count failed in StepCountReporter.", e);
        }
    }

    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }

    private final HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> mListener =
        new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
            int count = 0;

        @Override
        public void onResult(HealthDataResolver.ReadResult result) {
            try {
                for (HealthData data : result) {
                    count += data.getInt(HealthConstants.StepCount.COUNT);
                }
            } finally {
                result.close();
            }

            if (mStepCountObserver != null) {
                mStepCountObserver.onChanged(count);
            }
        }
    };

    private final HealthDataObserver mObserver = new HealthDataObserver(null) {

        // Update the step count when a change event is received
        @Override
        public void onChange(String dataTypeName) {
            Log.d(REACT_MODULE, "Samsung Health: StepCountObserver received a data changed event");
            readTodayStepCount();
        }
    };

    public interface StepCountObserver {
        void onChanged(int count);
    }
}

