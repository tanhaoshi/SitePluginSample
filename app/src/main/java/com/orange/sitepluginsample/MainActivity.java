package com.orange.sitepluginsample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.orange.sitepluginsample.plugin.HookActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.start_plugin_activity)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HookActivity.hookPackageManager(MainActivity.this, StubAppCompatActivity.class);

                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName("com.orange.person_plugin", "com.orange.person_plugin.PersonInfoActivity"));
                        startActivity(intent);
                    }
                });
    }
}
