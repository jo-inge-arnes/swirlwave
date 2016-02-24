package com.swirlwave.android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.swirlwave.android.service.Actions;
import com.swirlwave.android.service.SwirlwaveService;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Switch serviceSwitch = (Switch) findViewById(R.id.serviceSwitch);
        serviceSwitch.setChecked(SwirlwaveService.isRunning());
        serviceSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Intent intent = new Intent(this, SwirlwaveService.class);
        String actionName = isChecked ? Actions.ACTION_INIT_SERVICE : Actions.ACTION_SHUT_DOWN_SERVICE;
        intent.setAction(actionName);
        startService(intent);
    }
}
