package com.jiang.inject_api;

import android.app.Activity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class InjectHelper {
    public static void inject(Activity activity){
        String className=activity.getClass().getName()+"$$ViewBinding";

        try {
            Class<?> proxy = Class.forName(className);
            Constructor<?> constructor = proxy.getConstructor(activity.getClass());
            constructor.newInstance(activity);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
