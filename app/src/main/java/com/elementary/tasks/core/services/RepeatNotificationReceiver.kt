package com.elementary.tasks.core.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.legacy.content.WakefulBroadcastReceiver
import com.elementary.tasks.R
import com.elementary.tasks.core.data.AppDb
import com.elementary.tasks.core.data.models.Reminder
import com.elementary.tasks.core.utils.*
import com.elementary.tasks.reminder.preview.ReminderDialogActivity
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

class RepeatNotificationReceiver : WakefulBroadcastReceiver() {

    private var alarmMgr: AlarmManager? = null
    private var alarmIntent: PendingIntent? = null

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(Constants.INTENT_ID, 0)
        val item = AppDb.getAppDatabase(context).reminderDao().getById(id)
        if (item != null) {
            showNotification(context, item)
        }
    }

    fun setAlarm(context: Context, id: Int) {
        val repeat = Prefs.getInstance(context).notificationRepeatTime
        val minutes = repeat * 1000 * 60
        val intent = Intent(context, RepeatNotificationReceiver::class.java)
        intent.putExtra(Constants.INTENT_ID, id)
        alarmIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        if (Module.isMarshmallow) {
            alarmMgr!!.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis + minutes, minutes.toLong(), alarmIntent)
        } else {
            alarmMgr!!.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis + minutes, minutes.toLong(), alarmIntent)
        }
    }

    fun cancelAlarm(context: Context, id: Int) {
        val intent = Intent(context, RepeatNotificationReceiver::class.java)
        alarmIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmMgr != null) {
            alarmMgr!!.cancel(alarmIntent)
        }
    }

    private fun getSoundUri(melody: String?, context: Context): Uri {
        return if (!TextUtils.isEmpty(melody)) {
            UriUtil.getUri(context, melody!!)
        } else {
            val defMelody = Prefs.getInstance(context).melodyFile
            if (!TextUtils.isEmpty(defMelody) && !Sound.isDefaultMelody(defMelody)) {
                UriUtil.getUri(context, defMelody)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }
        }
    }

    private fun showNotification(context: Context, reminder: Reminder) {
        val builder = NotificationCompat.Builder(context, Notifier.CHANNEL_REMINDER)
        builder.setContentTitle(reminder.summary)
        builder.setAutoCancel(false)
        builder.priority = NotificationCompat.PRIORITY_MAX
        if (Prefs.getInstance(context).isFoldingEnabled && !Reminder.isBase(reminder.type, Reminder.BY_WEEK)) {
            val intent = PendingIntent.getActivity(context, reminder.uniqueId,
                    ReminderDialogActivity.getLaunchIntent(context, reminder.uniqueId), PendingIntent.FLAG_CANCEL_CURRENT)
            builder.setContentIntent(intent)
        }
        if (Module.isPro) {
            builder.setContentText(context.getString(R.string.app_name_pro))
        } else {
            builder.setContentText(context.getString(R.string.app_name))
        }
        if (Module.isLollipop) {
            builder.setSmallIcon(R.drawable.ic_notifications_white_24dp)
        } else {
            builder.setSmallIcon(R.drawable.ic_notification_nv_white)
        }
        if (!SuperUtil.isDoNotDisturbEnabled(context) || SuperUtil.checkNotificationPermission(context) && Prefs.getInstance(context).isSoundInSilentModeEnabled) {
            val uri = getSoundUri(reminder.melodyPath, context)
            context.grantUriPermission("com.android.systemui", uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            builder.setSound(uri)
        }
        if (Prefs.getInstance(context).isVibrateEnabled) {
            val pattern: LongArray = if (Prefs.getInstance(context).isInfiniteVibrateEnabled) {
                longArrayOf(150, 86400000)
            } else {
                longArrayOf(150, 400, 100, 450, 200, 500, 300, 500)
            }
            builder.setVibrate(pattern)
        }
        if (Module.isPro && Prefs.getInstance(context).isLedEnabled) {
            if (reminder.color != 0) {
                builder.setLights(reminder.color, 500, 1000)
            } else {
                builder.setLights(LED.getLED(Prefs.getInstance(context).ledColor), 500, 1000)
            }
        }
        val mNotifyMgr = NotificationManagerCompat.from(context)
        mNotifyMgr.notify(reminder.uniqueId, builder.build())
    }
}