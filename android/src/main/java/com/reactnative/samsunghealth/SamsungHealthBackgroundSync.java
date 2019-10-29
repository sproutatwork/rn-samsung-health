package com.reactnative.samsunghealth;

import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionResult;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType;
import com.samsung.android.sdk.healthdata.HealthResultHolder;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.Filter;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadRequest;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadResult;
import com.samsung.android.sdk.healthdata.HealthDataResolver.SortOrder;
import com.samsung.android.sdk.healthdata.HealthDataUtil;

import android.content.ComponentName;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SamsungHealthBackgroundSync extends JobService {
    private static final String TAG = SamsungHealthBackgroundSync.class.getSimpleName();
    private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L;
    boolean isWorking = false;
    boolean jobCancelled = false;
    int jobId = 0;

    private boolean samsungHealthConnected;
    private HealthDataStore mStore;
    private static final String REACT_MODULE = "RNSamsungHealth";

    public void createBackgroundJob() {
        ComponentName componentName = new ComponentName(this, SamsungHealthBackgroundSync.class);
        JobInfo jobInfo = new JobInfo.Builder(12, componentName)
                .setRequiresCharging(false)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(ONE_HOUR_IN_MILLIS)
                .build();

        JobScheduler jobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(jobInfo);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled!");
        } else {
            Log.d(TAG, "Job not scheduled");
        }
        jobId = jobInfo.getId();
    }

    // Called by the Android system when it's time to run the job
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started!");
        isWorking = true;
        // We need 'jobParameters' so we can call 'jobFinished'
        startWorkOnNewThread(jobParameters); // Services do NOT run on a separate thread

        return isWorking;
    }

    private void startWorkOnNewThread(final JobParameters jobParameters) {
        new Thread(new Runnable() {
            public void run() {
                doWork(jobParameters);
            }
        }).start();
    }

    private void doWork(JobParameters jobParameters) {
        samsungHealthConnected = false;
        // 10 seconds of 'working' (1000*10ms)
        for (int i = 0; i < 1000; i++) {
            // If the job has been cancelled, stop working; the job will be rescheduled.
            if (jobCancelled)
                return;

            try { Thread.sleep(10); } catch (Exception e) { }
        }

        Log.d(TAG, "Job finished!");
        isWorking = false;
        boolean needsReschedule = false;
        jobFinished(jobParameters, needsReschedule);

        // init and connect Samsung Health
        connectSamsungHealth();
    }

    // Called if the job was cancelled before being finished
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job cancelled before being completed.");
        jobCancelled = true;
        boolean needsReschedule = isWorking;
        jobFinished(jobParameters, needsReschedule);
        return needsReschedule;
    }

    private void connectSamsungHealth() {
        // Create a HealthDataStore instance and set its listener
        mStore = new HealthDataStore(this, mConnectionListener);

        // Request the connection to the health data store
        mStore.connectService();
    }

    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }
    private static final long ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L;

    private void getSteps() {
        // Get steps
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        long startTime = getStartTimeOfToday();
        long endTime = startTime + ONE_DAY_IN_MILLIS;

        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.StepCount.COUNT,
                        HealthConstants.StepCount.DISTANCE,
                        HealthConstants.StepCount.START_TIME,
                        HealthConstants.StepCount.CALORIE,
                        HealthConstants.StepCount.DEVICE_UUID 
                })
                .setLocalTimeRange(HealthConstants.StepCount.START_TIME, HealthConstants.StepCount.TIME_OFFSET,
                    startTime, endTime)
                .build();

        try {
            resolver.read(request).setResultListener(mStepsResultsListener);
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting step count fails.");
        }
    }

private final HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> mStepsResultsListener =
        new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
            long count = 0;

        @Override
        public void onResult(HealthDataResolver.ReadResult result) {
            try {
                for (HealthData data : result) {
                    count = data.getLong(HealthConstants.StepCount.COUNT);
                }
            } finally {
                result.close();
            }

            WritableMap steps = Arguments.createMap();
            steps.putDouble("steps", count);

            long startTime = getStartTimeOfToday();
            long endTime = startTime + ONE_DAY_IN_MILLIS;
            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String startTimeString = sdf.format(new Date(startTime));
            String endTimeString = sdf.format(new Date(endTime));

            String offset = getOffset();
            Log.d(REACT_MODULE, "offset: " + offset);

            WritableMap dailyStepRecords = Arguments.createMap();
            dailyStepRecords.putString("type", "steps");
            dailyStepRecords.putString("source", "samsungHealth");
            dailyStepRecords.putBoolean("manual", false);
            dailyStepRecords.putMap("metrics", steps);
            dailyStepRecords.putString("startTime", startTimeString);
            dailyStepRecords.putString("endTime", endTimeString);
            dailyStepRecords.putString("offset", offset);

            Log.d(REACT_MODULE, "dailyStepRecords: " + dailyStepRecords);
            // submit
        }
    };

    private void getWorkouts() {
        // Get workouts
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        long startTime = getStartTimeOfToday();
        long endTime = startTime + ONE_DAY_IN_MILLIS;

        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.Exercise.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.Exercise.START_TIME,
                        HealthConstants.Exercise.END_TIME,
                        HealthConstants.Exercise.EXERCISE_TYPE,
                        HealthConstants.Exercise.CALORIE,
                        HealthConstants.Exercise.DURATION,
                        HealthConstants.Exercise.DISTANCE
                })
                .setLocalTimeRange(HealthConstants.Exercise.START_TIME, HealthConstants.Exercise.TIME_OFFSET,
                    startTime, endTime)
                .build();

        try {
            resolver.read(request).setResultListener(mWorkoutResultsListener);
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting step count fails.");
        }
    }

    private final HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> mWorkoutResultsListener =
        new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
    @Override
        public void onResult(HealthDataResolver.ReadResult result) {
            WritableArray results = Arguments.createArray();

            try {
                for (HealthData data : result) {
                    WritableMap workout = Arguments.createMap();

                    SimpleDateFormat sdf = new SimpleDateFormat();
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    long workoutStart = data.getLong(HealthConstants.Exercise.START_TIME);
                    String startTimeString = sdf.format(new Date(workoutStart));
                    long workoutEnd = data.getLong(HealthConstants.Exercise.END_TIME);
                    String endTimeString = sdf.format(new Date(workoutEnd));

                    workout.putString("startTime", startTimeString);
                    workout.putString("endTime", endTimeString);
                    String offset = getOffset();
                    workout.putString("offset", offset);
                    workout.putString("source", "samsunghealth");
                    workout.putBoolean("manual", false);

                    //metrics
                    WritableMap metrics = Arguments.createMap();
                    metrics.putDouble("distance", data.getLong(HealthConstants.Exercise.DISTANCE));
                    metrics.putDouble("duration", data.getLong(HealthConstants.Exercise.DURATION));
                    metrics.putDouble("caloriesBurned", data.getLong(HealthConstants.Exercise.CALORIE));
                    workout.putMap("metrics", metrics);

                    //type
                    String type = convertActivityToTypeString(data.getInt(HealthConstants.Exercise.EXERCISE_TYPE));
                    workout.putString("type", type);

                    Log.d(REACT_MODULE, "workout: " + workout);
                    results.pushMap(workout);
                }
            } finally {
                result.close();
            }

            Log.d(REACT_MODULE, "workout results: " + results);
        }
    };

 private static String convertActivityToTypeString(int activityID) {
        String activityString = "";
        switch (activityID) {
            case 1001:
                activityString = "walking"; 
                break;
            case 1002:
                activityString = "running"; 
                break;
            case 2001:
                activityString = "baseball"; 
                break;
            case 2002:
                activityString = "baseball"; 
                break;
            case 2003:
                activityString = "cricket"; 
                break;
            case 3001:
                activityString = "golf"; 
                break;
            case 3002:
                activityString = "individual_sport"; 
                break;
            case 3003:
                activityString = "bowling"; 
                break;
            case 4001:
                activityString = "hockey"; 
                break;
            case 4002:
                activityString = "rugby"; 
                break;
            case 4003:
                activityString = "basketball"; 
                break;
            case 4004:
                activityString = "gootball"; 
                break;
            case 4005:
                activityString = "handball"; 
                break;
            case 4006:
                activityString = "soccer"; 
                break;
            case 5001:
                activityString = "volleyball"; 
                break;
            case 5002:
                activityString = "volleyball"; 
                break;
            case 6001:
                activityString = "squash"; 
                break;
            case 6002:
                activityString = "tennis"; 
                break;
            case 6003:
                activityString = "badminton"; 
                break;
            case 6004:
                activityString = "table_tennis"; 
                break;
            case 6005:
                activityString = "racquetball"; 
                break;
            case 7001:
                activityString = "martial_arts"; 
                break;
            case 7002:
                activityString = "boxing"; 
                break;
            case 7003:
                activityString = "martial_arts"; 
                break;
            case 8001:
                activityString = "dancing"; 
                break;
            case 8002:
                activityString = "dancing"; 
                break;
            case 8003:
                activityString = "dancing"; 
                break;
            case 9001:
                activityString = "pilates"; 
                break;
            case 9002:
                activityString = "yoga"; 
                break;
            case 10001:
                activityString = "stretch"; 
                break;
            case 10002:
                activityString = "jumping_rope"; 
                break;
            case 10003:
                activityString = "individual_sport"; 
                break;
            case 10004:
                activityString = "strength_training"; 
                break;
            case 10005:
                activityString = "strength_training"; 
                break;
            case 10006:
                activityString = "strength_training"; 
                break;
            case 10007:
                activityString = "circuit_training"; 
                break;
            case 10008:
                activityString = "individual_sport"; 
                break;
            case 10009:
                activityString = "individual_sport"; 
                break;
            case 10010:
                activityString = "strength_training"; 
                break;
            case 10011:
                activityString = "strength_training"; 
                break;
            case 10012:
                activityString = "strength_training"; 
                break;
            case 10013:
                activityString = "strength_training"; 
                break;
            case 10014:
                activityString = "strength_training"; 
                break;
            case 10015:
                activityString = "strength_training"; 
                break;
            case 10016:
                activityString = "strength_training"; 
                break;
            case 10017:
                activityString = "strength_training"; 
                break;
            case 10018:
                activityString = "strength_training"; 
                break;
            case 10019:
                activityString = "strength_training"; 
                break;
            case 10020:
                activityString = "strength_training"; 
                break;
            case 10021:
                activityString = "strength_training"; 
                break;
            case 10022:
                activityString = "strength_training"; 
                break;
            case 10023:
                activityString = "strength_training"; 
                break;
            case 10024:
                activityString = "strength_training"; 
                break;
            case 10025:
                activityString = "strength_training"; 
                break;
            case 10026:
                activityString = "strength_training"; 
                break;
            case 10027:
                activityString = "strength_training"; 
                break;
            case 11001:
                activityString = "rollerblading"; 
                break;
            case 11002:
                activityString = "individual_sport"; 
                break;
            case 11003:
                activityString = "individual_sport"; 
                break;
            case 11004:
                activityString = "individual_sport"; 
                break;
            case 11005:
                activityString = "individual_sport"; 
                break;
            case 11007:
                activityString = "cycling"; 
                break;
            case 11008:
                activityString = "individual_sport"; 
                break;
            case 11009:
                activityString = "rollerblading"; 
                break;
            case 12001:
                activityString = "fitness_class"; 
                break;
            case 13001:
                activityString = "hiking"; 
                break;
            case 13002:
                activityString = "rock_climbing"; 
                break;
            case 13003:
                activityString = "individual_sport"; 
                break;
            case 13004:
                activityString = "cycling"; 
                break;
            case 13005:
                activityString = "running"; 
                break;
            case 14001:
                activityString = "Swimming"; 
                break;
            case 14002:
                activityString = "water_sports"; 
                break;
            case 14003:
                activityString = "water_sports"; 
                break;
            case 14004:
                activityString = "water_sports"; 
                break;
            case 14005:
                activityString = "water_sports"; 
                break;
            case 14006:
                activityString = "water_sports"; 
                break;
            case 14007:
                activityString = "water_sports"; 
                break;
            case 14008:
                activityString = "water_sports"; 
                break;
            case 14009:
                activityString = "water_sports"; 
                break;
            case 14010:
                activityString = "rowing"; 
                break;
            case 14011:
                activityString = "water_sports"; 
                break;
            case 14012:
                activityString = "water_sports"; 
                break;
            case 14013:
                activityString = "water_sports"; 
                break;
            case 15001:
                activityString = "stair_climbing"; 
                break;
            case 15002:
                activityString = "strength_training"; 
                break;
            case 15003:
                activityString = "cycling"; 
                break;
            case 15004:
                activityString = "rowing"; 
                break;
            case 15005:
                activityString = "running"; 
                break;
            case 15006:
                activityString = "elliptical"; 
                break;
            case 16001:
                activityString = "skiing_cross_country"; 
                break;
            case 16002:
                activityString = "skiing_downhill"; 
                break;
            case 16003:
                activityString = "skating"; 
                break;
            case 16004:
                activityString = "skating"; 
                break;
            case 16006:
                activityString = "hockey"; 
                break;
            case 16007:
                activityString = "snowboarding"; 
                break;
            case 16008:
                activityString = "skiing_downhill"; 
                break;
            case 16009:
                activityString = "winter_sports"; 
                break;
            default:
                activityString = "individual_sport";
        }

        return activityString;
    }

    private static String getOffset() {
        TimeZone tz = TimeZone.getDefault();  
        Calendar cal = Calendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());

        String offset = String.format("%02d:%02d", Math.abs(offsetInMillis / 3600000), Math.abs((offsetInMillis / 60000) % 60));
        offset = (offsetInMillis >= 0 ? "+" : "-") + offset;
        return offset;
    }


    // Health Data Store
    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            if (isPermissionAcquired()) {
                //mReporter.requestDailyStepCount(mCurrentStartTime);
                samsungHealthConnected = true;
                getSteps();
            } else {
                //requestPermission();
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "onConnectionFailed");
            //showConnectionFailureDialog(error);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected");
            //if (!isFinishing()) {
            //    mStore.connectService();
            //}
        }
    };

    private boolean isPermissionAcquired() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            // Check whether the permissions that this application needs are acquired
            Map<PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(generatePermissionKeySet());
            return !resultMap.values().contains(Boolean.FALSE);
        } catch (Exception e) {
            Log.e(TAG, "Permission request fails.", e);
        }
        return false;
    }

    private Set<PermissionKey> generatePermissionKeySet() {
        Set<PermissionKey> pmsKeySet = new HashSet<>();
        pmsKeySet.add(new PermissionKey("com.samsung.shealth.step_daily_trend", PermissionType.READ));
        //pmsKeySet.add(new PermissionKey(StepCountReader.STEP_SUMMARY_DATA_TYPE_NAME, PermissionType.READ));
        return pmsKeySet;
    }
}