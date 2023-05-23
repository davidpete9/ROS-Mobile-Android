package com.schneewittchen.rosandroid.widgets.imu;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import com.schneewittchen.rosandroid.model.entities.widgets.PublisherWidgetEntity;
import com.schneewittchen.rosandroid.model.repositories.rosRepo.message.Topic;

import sensor_msgs.Imu;


/**
 * TODO: Description
 *
 * @author Dragos Circa
 * @version 1.0.0
 * @created on 02.11.2020
 * @updated on 18.11.2020
 * @modified by Nils Rottmann
 */

public class ImuEntity extends PublisherWidgetEntity implements IImuPublisher{

    private static final String TAG = ImuEntity.class.getSimpleName();

    public ImuEntity() {
        this.topic = new Topic("imu", Imu._TYPE);
    }

}
