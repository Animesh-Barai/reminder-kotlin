package com.elementary.tasks.birthdays.preview

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.elementary.tasks.BuildConfig
import com.elementary.tasks.R
import com.elementary.tasks.core.BaseNotificationActivity
import com.elementary.tasks.core.data.models.Birthday
import com.elementary.tasks.core.utils.*
import com.elementary.tasks.core.viewModels.Commands
import com.elementary.tasks.core.viewModels.birthdays.BirthdayViewModel
import kotlinx.android.synthetic.main.activity_show_birthday.*
import java.util.*

/**
 * Copyright 2016 Nazar Suhovich
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
class ShowBirthdayActivity : BaseNotificationActivity() {

    private lateinit var viewModel: BirthdayViewModel
    private var mBirthday: Birthday? = null
    private var isEventShowed = false
    override var isScreenResumed: Boolean = false
        private set
    override var summary: String = ""
        private set

    private val isBirthdaySilentEnabled: Boolean
        get() {
            var isEnabled = prefs.isSoundInSilentModeEnabled
            if (Module.isPro && !isGlobal) {
                isEnabled = prefs.isBirthdaySilentEnabled
            }
            return isEnabled
        }

    private val isTtsEnabled: Boolean
        get() {
            var isEnabled = prefs.isTtsEnabled
            if (Module.isPro && !isGlobal) {
                isEnabled = prefs.isBirthdayTtsEnabled
            }
            return isEnabled
        }

    override val ttsLocale: Locale?
        get() {
            var locale = language.getLocale(false)
            if (Module.isPro && !isGlobal) {
                locale = language.getLocale(true)
            }
            return locale
        }

    override val melody: String
        get() = if (Module.isPro && !isGlobal) {
            prefs.birthdayMelody
        } else {
            prefs.melodyFile
        }

    override val isBirthdayInfiniteVibration: Boolean
        get() {
            var vibrate = prefs.isInfiniteVibrateEnabled
            if (Module.isPro && !isGlobal) {
                vibrate = prefs.isBirthdayInfiniteVibrationEnabled
            }
            return vibrate
        }

    override val isBirthdayInfiniteSound: Boolean
        get() {
            var isLooping = prefs.isInfiniteSoundEnabled
            if (Module.isPro && !isGlobal) {
                isLooping = prefs.isBirthdayInfiniteSoundEnabled
            }
            return isLooping
        }

    override val isVibrate: Boolean
        get() {
            var vibrate = prefs.isVibrateEnabled
            if (Module.isPro && !isGlobal) {
                vibrate = prefs.isBirthdayVibrationEnabled
            }
            return vibrate
        }

    override val uuId: String
        get() = if (mBirthday != null) {
            mBirthday?.uuId ?: ""
        } else
            ""

    override val id: Int
        get() = if (mBirthday != null) {
            mBirthday?.uniqueId ?: 112
        } else
            0

    override val ledColor: Int
        get() {
            var ledColor = LED.getLED(prefs.ledColor)
            if (Module.isPro && !isGlobal) {
                ledColor = LED.getLED(prefs.birthdayLedColor)
            }
            return ledColor
        }

    override val isAwakeDevice: Boolean
        get() {
            var isWake = prefs.isDeviceAwakeEnabled
            if (Module.isPro && !isGlobal) {
                isWake = prefs.isBirthdayWakeEnabled
            }
            return isWake
        }

    override val maxVolume: Int
        get() = prefs.loudness

    override val isGlobal: Boolean
        get() = prefs.isBirthdayGlobalEnabled

    override val isUnlockDevice: Boolean
        get() = prefs.isDeviceUnlockEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isScreenResumed = intent.getBooleanExtra(Constants.INTENT_NOTIFICATION, false)
        val key = intent.getStringExtra(Constants.INTENT_ID) ?: ""
        setContentView(R.layout.activity_show_birthday)

        buttonOk.setOnClickListener { ok() }
        buttonCall.setOnClickListener { makeCall() }
        buttonSms.setOnClickListener { sendSMS() }

        contactPhoto.borderColor = themeUtil.getColor(themeUtil.colorPrimary())
        contactPhoto.visibility = View.GONE

        initViewModel(key)
    }

    private fun initViewModel(id: String) {
        viewModel = ViewModelProviders.of(this, BirthdayViewModel.Factory(application, id)).get(BirthdayViewModel::class.java)
        viewModel.birthday.observe(this, Observer<Birthday>{ birthday ->
            if (birthday != null) {
                showBirthday(birthday)
            }
        })
        viewModel.result.observe(this, Observer<Commands>{ commands ->
            if (commands != null) {
                when (commands) {
                    Commands.SAVED -> close()
                    else -> {
                    }
                }
            }
        })
        if (id == "" && BuildConfig.DEBUG) {
            loadTest()
        }
    }

    private fun loadTest() {
        val isMocked = intent.getBooleanExtra(ARG_TEST, false)
        if (isMocked) {
            val birthday = intent.getSerializableExtra(ARG_TEST_ITEM) as Birthday?
            if (birthday != null) showBirthday(birthday)
        }
    }

    private fun showBirthday(birthday: Birthday) {
        if (isEventShowed) return

        this.mBirthday = birthday

        if (!TextUtils.isEmpty(birthday.number) && checkContactPermission()) {
            birthday.number = Contacts.getNumber(birthday.name, this)
        }
        if (birthday.contactId == 0L && !TextUtils.isEmpty(birthday.number) && checkContactPermission()) {
            birthday.contactId = Contacts.getIdFromNumber(birthday.number, this)
        }
        val photo = Contacts.getPhoto(birthday.contactId)
        if (photo != null) {
            contactPhoto.setImageURI(photo)
            contactPhoto.visibility = View.VISIBLE
        } else {
            contactPhoto.visibility = View.GONE
        }
        val years = TimeUtil.getAgeFormatted(this, birthday.date)
        userName.text = birthday.name
        userName.contentDescription = birthday.name
        userYears.text = years
        userYears.contentDescription = years
        summary = birthday.name + "\n" + years
        if (TextUtils.isEmpty(birthday.number)) {
            buttonCall.visibility = View.INVISIBLE
            buttonSms.visibility = View.INVISIBLE
            userNumber.visibility = View.GONE
        } else {
            userNumber.text = birthday.number
            userNumber.contentDescription = birthday.number
            buttonCall.visibility = View.VISIBLE
            buttonSms.visibility = View.VISIBLE
            userNumber.visibility = View.VISIBLE
        }
        showNotification(TimeUtil.getAge(birthday.date), birthday.name)
        if (isTtsEnabled) {
            startTts()
        }
    }

    private fun checkContactPermission(): Boolean {
        return Permissions.checkPermission(this, Permissions.READ_CONTACTS, Permissions.READ_CALLS)
    }

    private fun showNotification(years: Int, name: String) {
        val builder = NotificationCompat.Builder(this, Notifier.CHANNEL_REMINDER)
        builder.setContentTitle(name)
        builder.setContentText(TimeUtil.getAgeFormatted(this, years))
        if (Module.isLollipop) {
            builder.setSmallIcon(R.drawable.ic_cake_white_24dp)
            builder.color = ViewUtils.getColor(this, R.color.bluePrimary)
        } else {
            builder.setSmallIcon(R.drawable.ic_cake_nv_white)
        }
        if (!isScreenResumed && (!SuperUtil.isDoNotDisturbEnabled(this) || SuperUtil.checkNotificationPermission(this) && isBirthdaySilentEnabled)) {
            val sound = sound
            sound?.playAlarm(soundUri, isBirthdayInfiniteSound)
        }
        if (isVibrate) {
            var pattern = longArrayOf(150, 400, 100, 450, 200, 500, 300, 500)
            if (isBirthdayInfiniteVibration) {
                pattern = longArrayOf(150, 86400000)
            }
            builder.setVibrate(pattern)
        }
        if (Module.isPro) {
            builder.setLights(ledColor, 500, 1000)
        }
        val isWear = prefs.isWearEnabled
        if (isWear && Module.isJellyMR2) {
            builder.setOnlyAlertOnce(true)
            builder.setGroup("GROUP")
            builder.setGroupSummary(true)
        }
        Notifier.getManager(this)?.notify(id, builder.build())
        if (isWear) {
            showWearNotification(name)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFlags()
    }

    override fun onBackPressed() {
        discardMedia()
        if (prefs.isFoldingEnabled) {
            removeFlags()
            finish()
        } else {
            Toast.makeText(this@ShowBirthdayActivity, getString(R.string.select_one_of_item), Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeCall() {
        if (Permissions.checkPermission(this, Permissions.CALL_PHONE) && mBirthday != null) {
            TelephonyUtil.makeCall(mBirthday!!.number, this)
            updateBirthday(mBirthday)
        } else {
            Permissions.requestPermission(this, CALL_PERM, Permissions.CALL_PHONE)
        }
    }

    private fun sendSMS() {
        if (Permissions.checkPermission(this@ShowBirthdayActivity, Permissions.SEND_SMS) && mBirthday != null) {
            TelephonyUtil.sendSms(mBirthday!!.number, this@ShowBirthdayActivity)
            updateBirthday(mBirthday)
        } else {
            Permissions.requestPermission(this@ShowBirthdayActivity, SMS_PERM, Permissions.SEND_SMS)
        }
    }

    private fun ok() {
        updateBirthday(mBirthday)
    }

    private fun updateBirthday(birthday: Birthday?) {
        isEventShowed = true
        if (birthday != null) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            val year = calendar.get(Calendar.YEAR)
            birthday.showedYear = year
            viewModel.saveBirthday(birthday)
        }
    }

    private fun close() {
        removeFlags()
        discardNotification(id)
        finish()
    }

    override fun showSendingError() {
        Toast.makeText(this, R.string.error_sending, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty()) return
        when (requestCode) {
            CALL_PERM -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeCall()
            }
            SMS_PERM -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSMS()
            }
        }
    }

    companion object {

        private const val CALL_PERM = 612
        private const val SMS_PERM = 613
        private const val ARG_TEST = "arg_test"
        private const val ARG_TEST_ITEM = "arg_test_item"

        fun mockTest(context: Context, birthday: Birthday) {
            val intent = Intent(context, ShowBirthdayActivity::class.java)
            intent.putExtra(ARG_TEST, true)
            intent.putExtra(ARG_TEST_ITEM, birthday)
            context.startActivity(intent)
        }

        fun getLaunchIntent(context: Context, id: String): Intent {
            val resultIntent = Intent(context, ShowBirthdayActivity::class.java)
            resultIntent.putExtra(Constants.INTENT_ID, id)
            resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            return resultIntent
        }
    }
}
