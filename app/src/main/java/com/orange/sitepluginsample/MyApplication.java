package com.orange.sitepluginsample;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.orange.sitepluginsample.plugin.HookInstrumentation;
import com.orange.sitepluginsample.plugin.LoadedApkClassLoaderHookHelper;
import com.orange.sitepluginsample.plugin.PluginApkNameVersion;
import com.orange.sitepluginsample.plugin.PluginUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * create by ths on 2021/10/11
 */
public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    @SuppressLint("StaticFieldLeak")
    private static MyApplication mApplication;

    private final ExecutorService mSingleThreadExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        initApplication();
        handleActivity(base);
        installActivity();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void initApplication() {
        mApplication = this;
    }

    public static MyApplication getInstance() {
        return mApplication;
    }

    private void handleActivity(Context context) {
        HookInstrumentation.hookInstrumentation(context);
    }

    //一种是通过Hook IActivityManager来实现，另一种是Hook Instrumentation实现 目前是采用后者
    private void installActivity() {
        Runnable patchClassLoaderRunnable = new Runnable() {

            @Override
            public void run() {
                PluginUtils.extractAssets(MyApplication.getInstance(), PluginApkNameVersion.PLUGIN_ACTIVITY_APK);
                File dexFile = getFileStreamPath(PluginApkNameVersion.PLUGIN_ACTIVITY_APK);

                try {
                    //插件使用自己的ClassLoader加载
                    LoadedApkClassLoaderHookHelper.hookLoadedApkInActivityThread(dexFile);
                    Log.i(TAG,"loaded apk finish ... ");
                } catch (Throwable e) {
                    Log.i(TAG,"error = " + e.getMessage().toString());
                    e.printStackTrace();
                }
            }
        };
        mSingleThreadExecutor.execute(patchClassLoaderRunnable);
    }
}
