package com.orange.sitepluginsample.plugin;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义了插件的ClassLoader,并且绕开了Framework的检测;利用ActivityThread对于LoadedApk的缓存机制,
 * 我们把携带这个自定义的ClassLoader的插件信息添加进mPackages中,进而完成了类的加载过程
 * <p>
 * http://weishu.me/2016/04/05/understand-plugin-framework-classloader/
 */
@SuppressLint("PrivateApi")
public class LoadedApkClassLoaderHookHelper {

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final Map<String, Object> sLoadedApk = new HashMap<>();

    //1.ActivityThread$H #handleMessage-->case LAUNCH_ACTIVITY 中public getPackageInfoNoCheck()
    //2.private getPackageInfo()

    /**
     * 步骤
     * 1.在ActivityThread接收到IApplication的 scheduleLaunchActivity远程调用之后,将消息转发给H
     * <p>
     * 2.H类在handleMessage的时候,调用了getPackageInfoNoCheck方法来获取待启动的组件信息.
     * 在这个方法中会优先查找mPackages中的缓存信息,而我们已经手动把插件信息添加进去;因此能够成功命中缓存,获取到独立存在的插件信息.
     * <p>
     * 3.H类然后调用handleLaunchActivity最终转发到performLaunchActivity方法;
     * 这个方法使用从getPackageInfoNoCheck中拿到LoadedApk中的mClassLoader来加载Activity类,
     * 进而使用反射创建Activity实例;接着创建Application,Context等完成Activity组件的启动.
     */
    @SuppressLint("DiscouragedPrivateApi")
    @SuppressWarnings({"unchecked", "JavaReflectionInvocation"})
    public static void hookLoadedApkInActivityThread(File apkFile) throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException, InstantiationException, NullPointerException, IOException {

        //一:获取mPackages对象 final ArrayMap<String,WeakReference<LoadedApk>> mPackages = new ArrayMap<String,WeakReference<LoadedApk>>();
        // 1.先获取到当前的ActivityThread对象
        Class<?> activityThreadClazz = Class.forName("android.app.ActivityThread");

        //2.静态方法 public static ActivityThread currentActivityThread(){}
        Method currentActivityThreadMethod = activityThreadClazz.getDeclaredMethod("currentActivityThread");
        currentActivityThreadMethod.setAccessible(true);

        //3.获取ActivityThread的实例对象
        Object currentActivityThread = currentActivityThreadMethod.invoke(null);

        //4.获取到 mPackages 这个静态成员变量,这里缓存了dex包的信息
        //final ArrayMap<String,WeakReference<LoadedApk>> mPackages = new ArrayMap<String,WeakReference<LoadedApk>>();
        Field mPackagesField = activityThreadClazz.getDeclaredField("mPackages");
        mPackagesField.setAccessible(true);

        //5.获取mPackages的实例对象
        //final ArrayMap<String,WeakReference<LoadedApk>> mPackages = new ArrayMap<String,WeakReference<LoadedApk>>();
        Map<String, WeakReference<?>> mPackages = (Map<String, WeakReference<?>>) mPackagesField.get(currentActivityThread);
        if (mPackages == null) throw new NullPointerException("mPackages == null");

        //二:获取LoadAPK
        //通过调用方法 public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai,CompatibilityInfo compatInfo) {...}
        //需要构造两个参数, ApplicationInfo ai和CompatibilityInfo compatInfo

        //6. 获取CompatibilityInfo的Class对象
        //android.content.res.CompatibilityInfo
        Class<?> compatibilityInfoClazz = Class.forName("android.content.res.CompatibilityInfo");

        //通过getPackageInfoNoCheck函数创建出我们需要的LoadedApk对象,以供接下来使用.

        //7.ActivityThread#getPackageInfoNoCheck()方法
        // public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai,CompatibilityInfo compatInfo){}
        Method getPackageInfoNoCheckMethod =
                activityThreadClazz.getDeclaredMethod("getPackageInfoNoCheck", ApplicationInfo.class, compatibilityInfoClazz);

        //8.获取CompatibilityInfo中的静态属性DEFAULT_COMPATIBILITY_INFO
        // public class CompatibilityInfo implements Parcelable {
        //    /** default compatibility info object for compatible applications */
        //    public static final CompatibilityInfo DEFAULT_COMPATIBILITY_INFO = new CompatibilityInfo() {};
        //}
        Field defaultCompatibilityInfoField = compatibilityInfoClazz.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
        defaultCompatibilityInfoField.setAccessible(true);

        //9.获取CompatibilityInfo实例对象
        //CompatibilityInfo代表设备兼容性信息,直接使用默认的值即可
        Object defaultCompatibilityInfo = defaultCompatibilityInfoField.get(null);

        //10.获取插件的ApplicationInfo实例对象

        ApplicationInfo applicationInfo = generateApplicationInfo(apkFile);

        //刚才为了获取ApplicationInfo我们费了好大一番精力;
        //回顾一下我们的初衷:
        //我们最终的目的是调用getPackageInfoNoCheck得到LoadedApk的信息,并替换其中的mClassLoader,
        //然后把LoadedApk的信息添加到ActivityThread的mPackages缓存中;
        //从而达到我们使用自己的ClassLoader加载插件中的类的目的.

        //11.反射调用getPackageInfoNoCheck()方法获取loadedApk实例对象
        // public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai,CompatibilityInfo compatInfo)
        Object loadedApk = getPackageInfoNoCheckMethod.invoke(currentActivityThread, applicationInfo, defaultCompatibilityInfo);
        if (loadedApk == null) throw new NullPointerException("loadedApk == null");


        //我们成功地构造出了LoadedAPK,接下来我们需要替换其中的ClassLoader,然后把它添加进ActivityThread的mPackages中:

        //12.获取odex,libDir路径
        String odexPath = PluginUtils.getPluginOptDexDir(applicationInfo.packageName).getPath();
        String libDir = PluginUtils.getPluginLibDir(applicationInfo.packageName).getPath();

        //13.接下来我们需要替换其中的ClassLoader
        ClassLoader customClassLoader = new CustomClassLoader(apkFile.getPath(), odexPath, libDir, ClassLoader.getSystemClassLoader());

        //14.获取LoadedApk中的ClassLoader mClassLoader属性
        // package android.app.LoadedApk
        //private ClassLoader mClassLoader;
        Field mClassLoaderField = loadedApk.getClass().getDeclaredField("mClassLoader");
        mClassLoaderField.setAccessible(true);

        //15.替换LoadedApk中的ClassLoader为我们自己创建的ClassLoader
        mClassLoaderField.set(loadedApk, customClassLoader);

        //16.由于是弱引用,因此我们必须在某个地方存一份,不然容易被GC; 那么就前功尽弃了.
        sLoadedApk.put(applicationInfo.packageName, loadedApk);

        //17.将创建的WeakReference(loadedApk)存进mPackages中
        WeakReference<?> weakReferenceLoadApk = new WeakReference<>(loadedApk);

        //ActivityThread类中mPackages成员
        //final ArrayMap<String,WeakReference<LoadedApk>> mPackages = new ArrayMap<String,WeakReference<LoadedApk>>();
        mPackages.put(applicationInfo.packageName, weakReferenceLoadApk);

        //到这里,我们已经成功地把插件的信息放入ActivityThread中的mPackages成员中,这样我们插件中的类能够成功地被加载;因此插件中的Activity实例能被成功第创建
        //由于整个流程较为复杂,我们简单梳理一下:
        //1. 在ActivityThread接收到IApplication的scheduleLaunchActivity()远程调用之后,将消息转发给H

        //2. H类在handleMessage的时候,调用了getPackageInfoNoCheck方法来获取待启动的组件信息.
        //在这个方法中会优先查找mPackages中的缓存信息,而我们已经手动把插件信息添加进去;
        //因此能够成功命中缓存,获取到独立存在的插件信息.

        //3. H类然后调用handleLaunchActivity最终转发到performLaunchActivity方法;
        // 这个方法使用从getPackageInfoNoCheck中拿到LoadedApk中的mClassLoader来加载Activity类,进而使用反射创建Activity实例;
        // 接着创建Application,Context等完成Activity组件的启动.

        //上面还不能正确实现我们的目的:
        // 我们的插件并没有安装在系统上,因此系统肯定认为插件没有安装.
        // 所以,我们还要欺骗一下PMS,让系统觉得插件已经安装在系统上了;

        // 到这里,我们已经能够成功地加载简单的独立的存在于外部文件系统中的apk了.
        // 这是一种极其复杂的Activity管理方案,我们仅仅写一个用来理解的demo就Hook了相当多的东西,在Framework层来回牵扯;
        // 这其中的来龙去脉要完全把握清楚还请读者亲自翻阅源码.
        // 上文给出的方案中,我们全盘接管了插件中类的加载过程,这是一种相对暴力的解决方案
    }

    private static ApplicationInfo generateApplicationInfo(File apkFile) throws NoSuchMethodException, IllegalAccessException, InstantiationException, IOException, InvocationTargetException, ClassNotFoundException {
        return ApplicationParserUtil.generateApplicationInfo(apkFile);
    }
}
