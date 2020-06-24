package com.sibyl.httpfiledominator

import android.app.Application

/**
 * @author Sasuke on 2020/6/23.
 */
class MyApp : Application(){
    companion object{
        @JvmStatic
        var instance: MyApp? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}