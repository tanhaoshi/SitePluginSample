package com.orange.person_plugin;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import java.lang.reflect.Method;

/**
 * create by ths on 2021/10/18
 */
public class PluginResourceUtil {

    private static Resources mResources;

    public static Resources getResource(Context context) {
        if (mResources == null) {
            mResources = loadResource(context);
            if (mResources == null) {
                throw new NullPointerException("plugin Resources==null");
            }
        }
        return mResources;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static Resources loadResource(Context context) {
        try {
            // 执行此addAssetPath(String path) 方法，能把插件的路径添加进去
            AssetManager assetManager = AssetManager.class.newInstance();
            Method addAssetPathMethod = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);
            addAssetPathMethod.invoke(assetManager, "/data/user/0/com.orange.sitepluginsample/files/person_plugin-debug.apk");

            Resources resources = context.getResources();
            return new Resources(assetManager, resources.getDisplayMetrics(), resources.getConfiguration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
