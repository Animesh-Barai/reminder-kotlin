package com.elementary.tasks

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.crashlytics.android.Crashlytics
import com.elementary.tasks.core.services.EventJobService
import com.elementary.tasks.core.utils.utilModule
import com.evernote.android.job.JobManager
import io.fabric.sdk.android.Fabric
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.logger.Logger
import org.koin.core.logger.MESSAGE
import timber.log.Timber

@Suppress("unused")
class ReminderApp : MultiDexApplication() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Fabric.with(this, Crashlytics())
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        val logger = object : Logger(level = Level.DEBUG) {
            override fun log(level: Level, msg: MESSAGE) {
            }
        }
        startKoin{
            logger(logger)
            androidContext(this@ReminderApp)
            modules(listOf(utilModule()))
        }
        JobManager.create(this).addJobCreator { EventJobService() }
    }
}
