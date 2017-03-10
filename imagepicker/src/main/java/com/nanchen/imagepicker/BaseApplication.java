package com.nanchen.imagepicker;

import android.app.Application;

import com.nanchen.imagepicker.util.Utils;

/**
 * Author: nanchen
 * Email: liushilin520@foxmail.com
 * Date: 2017-03-10  18:09
 */

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Utils.init(this);
    }

}
