package com.schneewittchen.rosandroid.widgets.imu;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.schneewittchen.rosandroid.R;
import com.schneewittchen.rosandroid.model.entities.widgets.BaseEntity;
import com.schneewittchen.rosandroid.ui.views.details.PublisherWidgetViewHolder;
import com.schneewittchen.rosandroid.utility.Utils;

import java.util.Collections;
import java.util.List;

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
 * @updated on 20.03.2021
 * @modified by Nico Studt
 */
public class ImuDetailVH extends PublisherWidgetViewHolder {


    @Override
    public void initView(View view) {
    }

    @Override
    protected void bindEntity(BaseEntity entity) {
    }

    @Override
    protected void updateEntity(BaseEntity entity) {
    }

    @Override
    public List<String> getTopicTypes() {
        return Collections.singletonList(Imu._TYPE);
    }
}
