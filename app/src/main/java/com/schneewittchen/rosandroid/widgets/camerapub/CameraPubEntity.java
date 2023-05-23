package com.schneewittchen.rosandroid.widgets.camerapub;

import android.util.Log;

import com.schneewittchen.rosandroid.model.entities.widgets.PublisherWidgetEntity;
import com.schneewittchen.rosandroid.model.repositories.rosRepo.message.Topic;

import sensor_msgs.CompressedImage;
import sensor_msgs.Image;


/**
 * TODO: Description
 *
 * @author Nico Studt
 * @version 1.1.1
 * @created on 31.01.20
 * @updated on 10.05.20
 * @modified by Nico Studt
 */
public class CameraPubEntity extends PublisherWidgetEntity implements ICameraPublisherEntity {

    public CameraPubEntity() {
        this.topic = new Topic("camera/image_raw/compressed", CompressedImage._TYPE);
    }

}
