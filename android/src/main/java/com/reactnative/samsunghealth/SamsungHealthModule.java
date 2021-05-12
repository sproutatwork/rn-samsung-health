package com.reactnative.samsunghealth;

import android.util.Log;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.LifecycleEventListener;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.Filter;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadRequest;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthUserProfile;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by firodj on 5/2/17.
 */

@ReactModule(name = "RNSamsungHealth")
public class SamsungHealthModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private static final String REACT_MODULE = "RNSamsungHealth";
    public static final String STEP_DAILY_TREND_TYPE = "com.samsung.shealth.step_daily_trend";
    
    public static final String DAY_TIME = "day_time";

    private HealthDataStore mStore;

    public SamsungHealthModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACT_MODULE;
    }

    @Override
    public void initialize() {
        super.initialize();

        getReactApplicationContext().addLifecycleEventListener(this);
        initSamsungHealth();
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("WEIGHT", HealthConstants.Weight.HEALTH_DATA_TYPE);
        constants.put("HEIGHT", HealthConstants.Height.HEALTH_DATA_TYPE);
        constants.put("HEART_RATE", HealthConstants.HeartRate.HEALTH_DATA_TYPE);
        constants.put("SLEEP", HealthConstants.Sleep.HEALTH_DATA_TYPE);
        constants.put("EXERCISE", HealthConstants.Exercise.HEALTH_DATA_TYPE);
        constants.put("BODY_FAT", HealthConstants.BodyFat.HEALTH_DATA_TYPE);
        constants.put("USER_PROFILE", HealthConstants.USER_PROFILE_DATA_TYPE);
        constants.put("STEP_COUNT", HealthConstants.StepCount.HEALTH_DATA_TYPE);

        return constants;
    }

    @Override
    public void onHostResume() {
    }

    @Override
    public void onHostPause() {
    }

    @Override
    public void onHostDestroy() {
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName,
                params);
    }

    public void initSamsungHealth() {
        Log.d(REACT_MODULE, "initialize Samsung Health...");
        HealthDataService healthDataService = new HealthDataService();
        try {
            healthDataService.initialize(getReactApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HealthDataStore getStore() {
        return mStore;
    }

    public ReactContext getContext() {
        return getReactApplicationContext();
    }

    @ReactMethod
    public void connect(Boolean requestPermission, ReadableArray permissions, Promise mPromise) {
        ConnectionListener listener = new ConnectionListener(this, mPromise, requestPermission);
        for (int i = 0; i < permissions.size(); i++) {
            listener.addReadPermission(permissions.getString(i));
        }

        mStore = new HealthDataStore(getReactApplicationContext(), listener);
        mStore.connectService();
    }

    @ReactMethod
    public void disconnect() {
        if (mStore != null) {
            Log.d(REACT_MODULE, "disconnectService");
            mStore.disconnectService();
            mStore = null;
        }
    }

    /*
     * private final HealthDataObserver mObserver = new HealthDataObserver(null) {
     * // Update the step count when a change event is received
     * 
     * @Override public void onChange(String dataTypeName) { Log.d(REACT_MODULE,
     * "Observer receives a data changed event"); readStepCount(); } }; private void
     * start() { // Register an observer to listen changes of step count and get
     * today step count // HealthDataObserver.addObserver(mStore,
     * HealthConstants.StepCount.HEALTH_DATA_TYPE, mObserver); readStepCount(); }
     */

    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance();

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }

    // Read the today's step count on demand
    @ReactMethod
    public void readStepCountDailies(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        Filter filter = Filter.and(Filter.greaterThanEquals(SamsungHealthModule.DAY_TIME, (long) startDate),
                Filter.lessThanEquals(SamsungHealthModule.DAY_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(SamsungHealthModule.STEP_DAILY_TREND_TYPE)
                .setProperties(new String[] { HealthConstants.StepCount.COUNT, HealthConstants.StepCount.DISTANCE,
                        SamsungHealthModule.DAY_TIME, HealthConstants.StepCount.CALORIE,
                        HealthConstants.StepCount.SPEED, HealthConstants.StepCount.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting step trend fails.");
            error.invoke("Samsung Health: Getting step count failed.");
        }

    }

    @ReactMethod
    public void readStepCountSamples(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.StepCount.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.StepCount.END_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.StepCount.COUNT, HealthConstants.StepCount.DISTANCE,
                        HealthConstants.StepCount.START_TIME, HealthConstants.StepCount.TIME_OFFSET,
                        HealthConstants.StepCount.END_TIME, HealthConstants.StepCount.SPEED,
                        HealthConstants.StepCount.CALORIE, HealthConstants.StepCount.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting step count fails.");
            error.invoke("Samsung Health: Getting step count failed.");
        }

    }

    @ReactMethod
    public void readHeight(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.Height.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.Height.START_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.Height.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.Height.HEIGHT, HealthConstants.Height.START_TIME,
                        HealthConstants.Height.TIME_OFFSET, HealthConstants.Height.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting Height fails.");
            error.invoke("Samsung Health: Getting Height failed.");
        }
    }

    @ReactMethod
    public void readBloodPressure(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.BloodPressure.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.BloodPressure.START_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.BloodPressure.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.BloodPressure.DIASTOLIC,
                        HealthConstants.BloodPressure.MEAN, HealthConstants.BloodPressure.PULSE,
                        HealthConstants.BloodPressure.START_TIME, HealthConstants.BloodPressure.SYSTOLIC,
                        HealthConstants.BloodPressure.TIME_OFFSET, HealthConstants.BloodPressure.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting BloodPressure fails.");
            error.invoke("Samsung Health: Getting BloodPressure failed.");
        }
    }

    @ReactMethod
    public void readBodyTemprature(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(
                Filter.greaterThanEquals(HealthConstants.BodyTemperature.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.BodyTemperature.START_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.BodyTemperature.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.BodyTemperature.START_TIME,
                        // HealthConstants.BodyTemperature.END_TIME,
                        HealthConstants.BodyTemperature.TEMPERATURE, HealthConstants.BodyTemperature.TIME_OFFSET,
                        HealthConstants.BodyTemperature.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting BodyTemperature fails.");
            error.invoke("Samsung Health: Getting BodyTemperature failed.");
        }
    }

    @ReactMethod
    public void readHeartRate(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.HeartRate.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.HeartRate.END_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.HeartRate.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.HeartRate.START_TIME, HealthConstants.HeartRate.END_TIME,
                        HealthConstants.HeartRate.HEART_RATE, HealthConstants.HeartRate.TIME_OFFSET,
                        HealthConstants.HeartRate.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting HeartRate fails.");
            error.invoke("Samsung Health: Getting HeartRate failed.");
        }
    }

    @ReactMethod
    public void readSleep(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.Sleep.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.Sleep.END_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.Sleep.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.Sleep.START_TIME, HealthConstants.Sleep.END_TIME,
                        HealthConstants.Sleep.TIME_OFFSET, HealthConstants.Sleep.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting Sleep fails.");
            error.invoke("Samsung Health: Getting Sleep failed.");
        }
    }

    @ReactMethod
    public void readWeight(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.Weight.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.Weight.START_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.Weight.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.Weight.WEIGHT,
                        // HealthConstants.Weight.END_TIME,
                        HealthConstants.Weight.START_TIME, HealthConstants.Weight.TIME_OFFSET,
                        HealthConstants.Weight.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting weight fails.");
            error.invoke("Samsung Health: Getting weight failed.");
        }
    }

    @ReactMethod
    public void readCholesterol(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(
                Filter.greaterThanEquals(HealthConstants.TotalCholesterol.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.TotalCholesterol.START_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.TotalCholesterol.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.TotalCholesterol.START_TIME,
                        HealthConstants.TotalCholesterol.TIME_OFFSET, HealthConstants.TotalCholesterol.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting TotalCholesterol fails.");
            error.invoke("Samsung Health: Getting TotalCholesterol failed.");
        }
    }

    @ReactMethod
    public void readWaterIntake(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.WaterIntake.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.WaterIntake.START_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.WaterIntake.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.WaterIntake.AMOUNT,
                        // HealthConstants.Weight.END_TIME,
                        HealthConstants.WaterIntake.START_TIME, HealthConstants.WaterIntake.TIME_OFFSET,
                        HealthConstants.WaterIntake.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting WATER INTAKE fails.");
            error.invoke("Samsung Health: Getting WATER INTAKE failed.");
        }
    }

    @ReactMethod
    public void readNutrition(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.Nutrition.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.Nutrition.START_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.Nutrition.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.Nutrition.CALORIE, HealthConstants.Nutrition.MEAL_TYPE,
                        HealthConstants.Nutrition.SATURATED_FAT, HealthConstants.Nutrition.CARBOHYDRATE,
                        HealthConstants.Nutrition.PROTEIN, HealthConstants.Nutrition.CHOLESTEROL,
                        HealthConstants.Nutrition.START_TIME, HealthConstants.Nutrition.TIME_OFFSET,
                        HealthConstants.Nutrition.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting Nutrition fails.");
            error.invoke("Samsung Health: Getting Nutrition failed.");
        }
    }

    @ReactMethod
    public void readExercise(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.Exercise.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.Exercise.START_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.Exercise.HEALTH_DATA_TYPE)
                .setProperties(new String[] { HealthConstants.Exercise.CALORIE, HealthConstants.Exercise.EXERCISE_TYPE,
                        HealthConstants.Exercise.DURATION, HealthConstants.Exercise.CALORIE,
                        HealthConstants.Exercise.START_TIME, HealthConstants.Exercise.END_TIME,
                        HealthConstants.Exercise.DISTANCE, HealthConstants.Exercise.TIME_OFFSET,
                        HealthConstants.Exercise.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting Exercise fails.");
            error.invoke("Samsung Health: Getting Exercise failed.");
        }
    }

    @ReactMethod
    public void readFloorsClimbed(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(Filter.greaterThanEquals(HealthConstants.FloorsClimbed.START_TIME, (long) startDate),
                Filter.lessThanEquals(HealthConstants.FloorsClimbed.END_TIME, (long) endDate));
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.FloorsClimbed.HEALTH_DATA_TYPE)
                .setProperties(
                        new String[] { HealthConstants.FloorsClimbed.FLOOR, HealthConstants.FloorsClimbed.START_TIME,
                                HealthConstants.FloorsClimbed.TIME_OFFSET, HealthConstants.FloorsClimbed.DEVICE_UUID })
                .setFilter(filter).build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting Floors Climbed fails.");
            error.invoke("Samsung Health: Getting Floors Climbed failed.");
        }
    }

    @ReactMethod
    public void readDateOfBirth(Callback error, Callback success) {
        try {
            HealthUserProfile usrProfile = HealthUserProfile.getProfile(mStore);

            // Date of birth - yyyymmdd
            String birthDate = usrProfile.getBirthDate();
            if (birthDate.isEmpty()) {
                Log.d(REACT_MODULE, "Date of birth is not set by the user yet.");
                error.invoke("Date of birth is not set by the user yet.");
            } else {
                Log.d(REACT_MODULE, "Date of Birth read.");
                success.invoke(birthDate);
            }
        } catch (Exception e) {
            Log.d(REACT_MODULE, "Do not have permission to get user profile");
            error.invoke("Samsung Health: Getting data of birth failed");
        }
    }

    @ReactMethod
    public void readGender(Callback error, Callback success) {
        try {
            HealthUserProfile usrProfile = HealthUserProfile.getProfile(mStore);

            // Gender
            int gender = usrProfile.getGender();
            if (gender == HealthUserProfile.GENDER_UNKNOWN) {
                Log.d(REACT_MODULE, "Gender is not set by the user yet.");
                error.invoke("Gender is not set by the user yet.");
            } else {
                Log.d(REACT_MODULE, "Gender read.");
                success.invoke(gender);
            }
        } catch (Exception e) {
            Log.d(REACT_MODULE, "Do not have permission to get user profile");
            error.invoke("Samsung Health: Getting gender failed");
        }
    }

    @ReactMethod
    public void readBodyFatPercentage(double startDate, double endDate, Callback error, Callback success) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(
            Filter.greaterThanEquals(HealthConstants.Weight.START_TIME, (long)startDate),
            Filter.lessThanEquals(HealthConstants.Weight.START_TIME, (long)endDate)
        );
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.Weight.HEALTH_DATA_TYPE) 
                .setProperties(new String[]{
                        HealthConstants.Weight.START_TIME,
                        HealthConstants.Weight.BODY_FAT,
                        HealthConstants.Weight.TIME_OFFSET, 
                        HealthConstants.Weight.DEVICE_UUID  
                })
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting weight body fat fails.");
            error.invoke("Samsung Health: Getting weight body fat failed.");
        }
    }

    // Step Count Observer
    private StepCountReporter mStepReporter;
    private long mCurrentStartTime;
    private Callback stepErrorCallback;
    private Callback stepSuccessCallback;


    @ReactMethod
    public void startStepCountObserver(Callback error, Callback success) {
        Log.d(REACT_MODULE, "startStepCountObserver");

        stepErrorCallback = error;
        stepSuccessCallback = success;

        mStepReporter = new StepCountReporter(mStore);
        mStepReporter.start(mStepCountObserver);
    }

    private final StepCountReporter.StepCountObserver mStepCountObserver = new StepCountReporter.StepCountObserver() {
        @Override
        public void onChanged(int count) {
            if (count > 0) {
                Log.d(REACT_MODULE, "StepCountObserver onChanged");
                stepSuccessCallback.invoke("steps changed");
            }
            stepErrorCallback.invoke("getting step change failed");
        }
    };

    @ReactMethod
    public void readWorkoutSamples(double startDate, double endDate, Callback error, Callback success) {
       HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        Filter filter = Filter.and(
            Filter.greaterThanEquals(HealthConstants.Exercise.START_TIME, (long)startDate),
            Filter.lessThanEquals(HealthConstants.Exercise.START_TIME, (long)endDate)
        );
        HealthDataResolver.ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.Exercise.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.Exercise.START_TIME,
                        HealthConstants.Exercise.END_TIME,
                        HealthConstants.Exercise.TIME_OFFSET, 
                        HealthConstants.Exercise.DEVICE_UUID,
                        HealthConstants.Exercise.EXERCISE_TYPE,
                        HealthConstants.Exercise.CALORIE,
                        HealthConstants.Exercise.DURATION,
                        HealthConstants.Exercise.DISTANCE
                })
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthDataResultListener(this, error, success));
        } catch (Exception e) {
            Log.e(REACT_MODULE, e.getClass().getName() + " - " + e.getMessage());
            Log.e(REACT_MODULE, "Getting TotalCholesterol fails.");
            error.invoke("Getting TotalCholesterol fails.");
        }
    }
}
