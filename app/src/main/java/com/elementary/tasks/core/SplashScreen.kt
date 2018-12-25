package com.elementary.tasks.core

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.elementary.tasks.core.async.EnableThread
import com.elementary.tasks.core.data.AppDb
import com.elementary.tasks.core.services.PermanentReminderReceiver
import com.elementary.tasks.groups.GroupsUtil
import com.elementary.tasks.intro.IntroActivity
import com.elementary.tasks.navigation.MainActivity
import com.elementary.tasks.navigation.settings.security.PinLoginActivity

class SplashScreen : ThemedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPrefs()
        if (prefs.isSbNotificationEnabled) {
            notifier.updateReminderPermanent(PermanentReminderReceiver.ACTION_SHOW)
        }
    }

    private fun checkIfAppUpdated() {
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            if (!prefs.getVersion(info.versionName)) {
                prefs.saveVersionBoolean(info.versionName)
                EnableThread(this).start()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        gotoApp()
    }

    private fun gotoApp() {
        checkIfAppUpdated()
        if (!prefs.isUserLogged) {
            openIntroScreen()
        } else {
            initGroups()
            if (prefs.hasPinCode) {
                openPinLogin()
            } else {
                runApplication()
            }
        }
    }

    private fun openPinLogin() {
        startActivity(Intent(this, PinLoginActivity::class.java))
        finish()
    }

    private fun openIntroScreen() {
        startActivity(Intent(this, IntroActivity::class.java))
        finish()
    }

    private fun initGroups() {
        if (AppDb.getAppDatabase(this).reminderGroupDao().all().isEmpty()) {
            GroupsUtil.initDefault(this)
        }
    }

    private fun initPrefs() {
        prefs.initPrefs(this)
        prefs.checkPrefs()
    }

    private fun runApplication() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}