package com.orange.sitepluginsample.plugin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.orange.sitepluginsample.MyApplication;
import com.orange.sitepluginsample.StubAppCompatActivity;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import androidx.annotation.Keep;

/**
 * create by ths on 2021/10/11
 */
public class HookInstrumentation {

    private static final String TARGET_INTENT_CLASS = "target_intent_class";

    public static void hookInstrumentation(Context context) {
        try {
            //1.ContextImpl-->mMainThread
            //package android.app
            //class ContextImpl
            Class<?> contextImplClazz = Class.forName("android.app.ContextImpl");

            //final @NonNull ActivityThread mMainThread;
            Field mMainThreadField = contextImplClazz.getDeclaredField("mMainThread");
            mMainThreadField.setAccessible(true);

            //2.ActivityThread Object
            Object activityThreadObj = mMainThreadField.get(context);

            //3.mInstrumentation Object
            Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");

            //Instrumentation mInstrumentation;
            Field mInstrumentationField = activityThreadClazz.getDeclaredField("mInstrumentation");
            mInstrumentationField.setAccessible(true);
            Instrumentation mInstrumentationObj = (Instrumentation) mInstrumentationField.get(activityThreadObj);

            //4.reset set value
            mInstrumentationField.set(activityThreadObj, new InstrumentationProxy(mInstrumentationObj, context.getPackageManager(), StubAppCompatActivity.class));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    private static class InstrumentationProxy extends Instrumentation {

        private final Instrumentation mInstrumentation;
        private final PackageManager mPackageManager;
        private final Class<?> mStubActivityClass;

        InstrumentationProxy(Instrumentation instrumentation, PackageManager packageManager, Class<?> stubActivityClassName) {
            mInstrumentation = instrumentation;
            mPackageManager = packageManager;
            mStubActivityClass = stubActivityClassName;
        }

        /**
         * android16-android29
         * Instrumentation???execStartActivity????????????Activity????????????
         * ???????????????Activity?????????AMS?????????.
         */
        @Keep
        @SuppressWarnings("unused")
        public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode, Bundle options) {
            List<ResolveInfo> resolveInfoList = null;
            try {
                int flags = 0;
                if (Build.VERSION.SDK_INT >= 23) {
                    flags = PackageManager.MATCH_ALL;
                }
                resolveInfoList = mPackageManager.queryIntentActivities(intent, flags);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            Intent finalIntent = intent;
            if (resolveInfoList == null || resolveInfoList.size() == 0) {
                //??????Activity?????????AndroidManifest.xml???????????????,?????????Activity???ClassName????????????Intent???.
                finalIntent = new Intent(who, mStubActivityClass);
                //public class Intent implements Parcelable;
                //Intent??????????????????Parcelable??????
                finalIntent.putExtra(TARGET_INTENT_CLASS, intent);
            }
            try {
                //??????????????????execStartActivity??????,?????????????????????Activity??????AMS?????????.
                Method execStartActivityMethod = Instrumentation.class.getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class, Bundle.class);
                execStartActivityMethod.setAccessible(true);
                return (ActivityResult) execStartActivityMethod.invoke(mInstrumentation, who, contextThread, token, target, finalIntent, requestCode, options);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * just for android-15
         * Instrumentation#execStartActivity()?????????????????????????????????, ??????????????????
         * Instrumentation???execStartActivity????????????Activity????????????
         * ???????????????Activity?????????AMS?????????.
         * http://androidxref.com/4.0.3_r1/xref/frameworks/base/core/java/android/app/Instrumentation.java
         */
        @Keep
        @SuppressLint("WrongConstant")
        @SuppressWarnings("unused")
        public ActivityResult execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode) {
            if (Build.VERSION.SDK_INT != 15) return null;
            List<ResolveInfo> resolveInfoList = null;
            try {
                int flags = 0;
                resolveInfoList = mPackageManager.queryIntentActivities(intent, flags);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }

            Intent finalIntent = intent;
            if (resolveInfoList == null || resolveInfoList.size() == 0) {
                //??????Activity?????????AndroidManifest.xml???????????????,?????????Activity???ClassName????????????Intent???.
                finalIntent = new Intent(who, mStubActivityClass);
                //public class Intent implements Parcelable;
                //Intent??????????????????Parcelable??????
                finalIntent.putExtra(TARGET_INTENT_CLASS, intent);
            }
            try {
                //just for android-15
                //??????????????????execStartActivity??????,?????????????????????Activity??????AMS?????????.
                Method execStartActivityMethod = Instrumentation.class.getDeclaredMethod("execStartActivity", Context.class, IBinder.class, IBinder.class, Activity.class, Intent.class, int.class);
                execStartActivityMethod.setAccessible(true);
                return (ActivityResult) execStartActivityMethod.invoke(mInstrumentation, who, contextThread, token, target, finalIntent, requestCode);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * Instrumentation???newActivity??????,????????????????????????Activity??????
         * ????????????Activity.
         */
        @Keep
        @Override
        public Activity newActivity(ClassLoader classLoader, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            Intent pluginIntent = intent.getParcelableExtra(TARGET_INTENT_CLASS);
            boolean pluginIntentClassNameExist = pluginIntent != null && !TextUtils.isEmpty(pluginIntent.getComponent().getClassName());

            //1.className
            String finalClassName = pluginIntentClassNameExist ? pluginIntent.getComponent().getClassName() : className;

            //2.intent
            Intent finalIntent = pluginIntentClassNameExist ? pluginIntent : intent;

            //3.classLoader
            File pluginDexFile = MyApplication.getInstance().getFileStreamPath(PluginApkNameVersion.PLUGIN_ACTIVITY_APK);
            ClassLoader finalClassLoader = pluginIntentClassNameExist ? CustomClassLoader.getPluginClassLoader(pluginDexFile, "com.orange.person_plugin") : classLoader;

            Log.i("HookInstrumentation","className = " + finalClassName);

            if (Build.VERSION.SDK_INT >= 28) {
                return mInstrumentation.newActivity(finalClassLoader, finalClassName, finalIntent);
            }
            return super.newActivity(finalClassLoader, finalClassName, finalIntent);
        }
    }
}
