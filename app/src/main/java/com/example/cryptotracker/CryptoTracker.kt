package com.example.cryptotracker

import android.app.Application
import android.util.Log

class CryptoTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GlobalCrashHandler", "Uncaught exception in thread ${thread.name}", throwable)
        }
    }
}