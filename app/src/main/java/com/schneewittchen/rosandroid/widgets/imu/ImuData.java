package com.schneewittchen.rosandroid.widgets.imu;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.util.Log;

import com.schneewittchen.rosandroid.model.entities.widgets.BaseEntity;
import com.schneewittchen.rosandroid.model.repositories.rosRepo.node.BaseData;
import com.schneewittchen.rosandroid.model.repositories.rosRepo.node.ImuPublisherNode;

import org.ros.internal.message.Message;
import org.ros.message.Time;
import org.ros.node.topic.Publisher;

import java.time.Instant;

import sensor_msgs.Imu;
import std_msgs.Bool;

/**
 * TODO: Description
 *
 * @author Dragos Circa
 * @version 1.0.0
 * @created on 02.11.2020
 * @updated on 18.11.2020
 * @modified by Nils Rottmann
 */

public class ImuData extends BaseData {

    private static int SEQUENCE_NUMBER = 1;
    private boolean isAccelerometerMessagePending;
    private boolean isGyroscopeMessagePending;
    private boolean isOrientationMessagePending;

    private SensorEventListener accelerometerListener;
    private SensorEventListener gyroscopeListener;
    private SensorEventListener orientationListener;

    private float ax, ay, az;
    private float aRoll, aPitch, aYaw;
    private float roll, pitch, yaw;
    private String imuFrameId;
    private float prevRoll, prevPitch, prevYaw;
    private OnFrameIdChangeListener imuFrameIdChangeListener;

    public ImuData() {

        isAccelerometerMessagePending = false;
        isGyroscopeMessagePending = false;
        isOrientationMessagePending = false;

        accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (!(
                        ax == sensorEvent.values[0] &&
                                ay == sensorEvent.values[1] &&
                                az == sensorEvent.values[2]
                )) {
                    ax = sensorEvent.values[0];
                    ay = sensorEvent.values[1];
                    az = sensorEvent.values[2];
                    isAccelerometerMessagePending = true;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        gyroscopeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (!(
                        aRoll == -sensorEvent.values[1] &&
                                aPitch == -sensorEvent.values[2] &&
                                aYaw == sensorEvent.values[0]
                )) {
                    aRoll = -sensorEvent.values[1];
                    aPitch = -sensorEvent.values[2];
                    aYaw = sensorEvent.values[0];
                    isGyroscopeMessagePending = true;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        orientationListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (!(
                        roll == -sensorEvent.values[1] &&
                                pitch == -sensorEvent.values[2] &&
                                yaw == 360 - sensorEvent.values[0]
                )) {
                    roll = -sensorEvent.values[1];
                    pitch = -sensorEvent.values[2];
                    yaw = 360 - sensorEvent.values[0];

                    isOrientationMessagePending = true;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };

        imuFrameIdChangeListener = new OnFrameIdChangeListener() {
            @Override
            public void onFrameIdChanged(String newFrameId) {
                imuFrameId = newFrameId;
            }
        };

    }

    @Override
    public Message toRosMessage(Publisher<Message> publisher, BaseEntity widget) {
        Imu imuMessage = (Imu) publisher.newMessage();
        Log.d("IMUTEST", "SEND IMU DATA");

        long currentTimeMillis = System.currentTimeMillis();
        if (isAccelerometerMessagePending && isGyroscopeMessagePending && isOrientationMessagePending) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Time stamp = new Time((int) System.currentTimeMillis(), (int) (System.nanoTime() % 1000000000));
                imuMessage.getHeader().setStamp(new Time(stamp));
            }
            imuMessage.getHeader().setFrameId("1");
            imuMessage.getHeader().setSeq(SEQUENCE_NUMBER);

            imuMessage.getLinearAcceleration().setX(ax);
            imuMessage.getLinearAcceleration().setY(ay);
            imuMessage.getLinearAcceleration().setZ(az);

            float dt = (currentTimeMillis - ImuPublisherNode.getInstance().getLastPublishTimeStamp()) / 1000.f;
            float dRoll = (roll - prevRoll);
            if (dRoll > 180)
                dRoll = 360 - dRoll;
            float dPitch = (pitch - prevPitch);
            if (dPitch > 180)
                dPitch = 360 - dPitch;
            float dYaw = (yaw - prevYaw);
            if (dYaw > 180)
                dYaw = 360 - dYaw;

            imuMessage.getAngularVelocity().setX(dRoll / dt);
            imuMessage.getAngularVelocity().setY(dPitch / dt);
            imuMessage.getAngularVelocity().setZ(dYaw / dt);

            prevRoll = roll;
            prevPitch = pitch;
            prevYaw = yaw;

            imuMessage.getOrientation().setW(roll);
            imuMessage.getOrientation().setX(roll);
            imuMessage.getOrientation().setY(pitch);
            imuMessage.getOrientation().setZ(yaw);

            ++SEQUENCE_NUMBER;

            isAccelerometerMessagePending = false;
            isGyroscopeMessagePending = false;
            isOrientationMessagePending = false;
        }


        return imuMessage;
    }

    public SensorEventListener getAccelerometerListener() {
        return accelerometerListener;
    }

    public SensorEventListener getGyroscopeListener() {
        return gyroscopeListener;
    }

    public SensorEventListener getOrientationListener() {
        return orientationListener;
    }


}
