package com.garbage.management

import android.app.Application
import com.garbage.management.di.AppContainer

class SmartGarbageApp : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
