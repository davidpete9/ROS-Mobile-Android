package com.schneewittchen.rosandroid.model.repositories.rosRepo.node;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import com.schneewittchen.rosandroid.widgets.imu.ImuData;
import com.schneewittchen.rosandroid.widgets.imu.OnFrameIdChangeListener;

public class ImuPublisherNode extends PubNode {

    private static ImuPublisherNode instance = null;

    private ImuData imuData;

    private OnFrameIdChangeListener locationFrameIdListener, imuFrameIdListener;

    private long lastPublishTimeStamp = 0;

    public void initIMU(Context context) {
        locationFrameIdListener = new OnFrameIdChangeListener() {
            @Override
            public void onFrameIdChanged(String newFrameId) {
                Log.w(TAG, "Default location OnFrameIdChangedListener called");
            }
        };
        imuFrameIdListener = new OnFrameIdChangeListener() {
            @Override
            public void onFrameIdChanged(String newFrameId) {
                Log.w(TAG, "Default IMU OnFrameIdChangedListener called");
            }
        };


        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        try {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(imuData.getAccelerometerListener(), accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
            return;
        }

        SensorManager sensorManager1 = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        try {
            Sensor gyroscope = sensorManager1.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorManager1.registerListener(imuData.getGyroscopeListener(), gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
            return;
        }

        SensorManager sensorManager2 = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        try {
            Sensor orientation = sensorManager2.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            sensorManager2.registerListener(imuData.getOrientationListener(), orientation, SensorManager.SENSOR_DELAY_FASTEST);
        } catch (NullPointerException e) {
            Log.e(TAG, e.toString());
            return;

        }
        Log.i("IMUTEST","Imu initialized.");
    }

    public ImuPublisherNode() {
        super();
        this.imuData = new ImuData();
        this.setImmediatePublish(false);
        this.setFrequency(10);
        super.setData(this.imuData);
    }

    public static ImuPublisherNode getInstance() {
        if (instance == null) {
            instance = new ImuPublisherNode();
            return instance;
        }
        return instance;
    }

    public long getLastPublishTimeStamp() {
        return this.lastPublishTimeStamp;
    }

    @Override
    protected void publish() {
        this.lastPublishTimeStamp = System.currentTimeMillis();
        super.publish();
    }

    @Override
    protected void createAndStartSchedule() {
        this.lastPublishTimeStamp = System.currentTimeMillis();
        Log.d("IMUTEST","createAndStartSchedule");
        super.createAndStartSchedule();
    }


}
