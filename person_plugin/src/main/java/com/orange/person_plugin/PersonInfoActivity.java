package com.orange.person_plugin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import com.kyleduo.switchbutton.SwitchButton;

public class PersonInfoActivity extends BaseActivity {

    private static final String TAG = "PersonInfoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, TAG + ":onCreate 232323232323");
        View view = LayoutInflater.from(mContext).inflate(R.layout.activity_person, null);
        setContentView(view);
//        setContentView(R.layout.activity_person);

        LinearLayout linearLayout = view.findViewById(R.id.teamView);

//        RadioButton educationRadio = view.findViewById(R.id.educationRadio);
//        educationRadio.setBackground(PluginResourceUtil.getResource(mContext).getDrawable(R.drawable.color_radiobutton));
//
//        RadioButton educationNoRadio = view.findViewById(R.id.educationNoRadio);
//        educationNoRadio.setBackground(PluginResourceUtil.getResource(mContext).getDrawable(R.drawable.color_radiobutton));

        SwitchButton customSwitch = new SwitchButton(this);
        customSwitch.setTintColor(PluginResourceUtil.getResource(mContext).getColor(R.color.colorAccent));
        linearLayout.addView(customSwitch);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, TAG + ":onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, TAG + ":onResume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, TAG + ":onRestart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, TAG + ":onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, TAG + ":onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, TAG + ":onDestroy");
    }
}
