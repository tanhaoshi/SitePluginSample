package com.orange.sitepluginsample;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * create by ths on 2021/10/11
 */
public class StubAppCompatActivity extends AppCompatActivity {

    private static final String TAG = "StubAppCompatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        textView.setText("StubAppCompatActivity");
        setContentView(textView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, TAG + ":onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, TAG + ":onResume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, TAG + ":onRestart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, TAG + ":onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, TAG + ":onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, TAG + ":onDestroy");
    }
}
