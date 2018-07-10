package com.elementary.tasks.core.view_models.conversation

import android.app.Application
import android.content.Intent
import android.provider.ContactsContract
import android.text.TextUtils
import android.widget.Toast

import com.backdoor.engine.Action
import com.backdoor.engine.ActionType
import com.backdoor.engine.ContactOutput
import com.backdoor.engine.ContactsInterface
import com.backdoor.engine.Model
import com.backdoor.engine.Recognizer
import com.elementary.tasks.R
import com.elementary.tasks.birthdays.createEdit.AddBirthdayActivity
import com.elementary.tasks.core.SplashScreen
import com.elementary.tasks.core.app_widgets.UpdatesHelper
import com.elementary.tasks.core.data.models.Birthday
import com.elementary.tasks.core.data.models.Group
import com.elementary.tasks.core.data.models.Note
import com.elementary.tasks.core.data.models.Reminder
import com.elementary.tasks.core.dialogs.VoiceHelpDialog
import com.elementary.tasks.core.dialogs.VoiceResultDialog
import com.elementary.tasks.core.dialogs.VolumeDialog
import com.elementary.tasks.core.utils.CalendarUtils
import com.elementary.tasks.core.utils.Constants
import com.elementary.tasks.core.utils.Language
import com.elementary.tasks.core.utils.LogUtil
import com.elementary.tasks.core.utils.Permissions
import com.elementary.tasks.core.utils.Prefs
import com.elementary.tasks.core.utils.TimeCount
import com.elementary.tasks.core.utils.TimeUtil
import com.elementary.tasks.core.view_models.Commands
import com.elementary.tasks.core.view_models.reminders.BaseRemindersViewModel
import com.elementary.tasks.navigation.MainActivity
import com.elementary.tasks.reminder.create_edit.AddReminderActivity

import java.util.ArrayList
import java.util.LinkedList
import java.util.Random
import androidx.lifecycle.MutableLiveData

/**
 * Copyright 2018 Nazar Suhovich
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
class ConversationViewModel(application: Application) : BaseRemindersViewModel(application) {

    var shoppingLists = MutableLiveData<List<Reminder>>()
    var enabledReminders = MutableLiveData<List<Reminder>>()
    var activeReminders = MutableLiveData<List<Reminder>>()
    var notes = MutableLiveData<List<Note>>()
    var birthdays = MutableLiveData<List<Birthday>>()

    private val recognizer: Recognizer

    init {

        val prefs = Prefs.getInstance(application)
        val language = Language.getLanguage(prefs.voiceLocale)
        val morning = prefs.morningTime
        val day = prefs.noonTime
        val evening = prefs.eveningTime
        val night = prefs.nightTime
        val times = arrayOf(morning, day, evening, night)
        recognizer = Recognizer.Builder()
                .setLocale(language)
                .setTimes(times)
                .setContactsInterface(ContactHelper())
                .build()
    }

    fun getNotes() {
        isInProgress.postValue(true)
        run {
            val list = LinkedList(appDb!!.notesDao().all)
            end {
                isInProgress.postValue(false)
                notes.postValue(list)
            }
        }
    }

    fun getShoppingReminders() {
        isInProgress.postValue(true)
        run {
            val list = LinkedList(appDb!!.reminderDao().getAllTypes(true, false, intArrayOf(Reminder.BY_DATE_SHOP)))
            end {
                isInProgress.postValue(false)
                shoppingLists.postValue(list)
            }
        }
    }

    fun getEnabledReminders(dateTime: Long) {
        isInProgress.postValue(true)
        run {
            val list = LinkedList(appDb!!.reminderDao().getAllTypesInRange(
                    true,
                    false,
                    TimeUtil.getGmtFromDateTime(System.currentTimeMillis()),
                    TimeUtil.getGmtFromDateTime(dateTime)))
            end {
                isInProgress.postValue(false)
                enabledReminders.postValue(list)
            }
        }
    }

    fun getReminders(dateTime: Long) {
        isInProgress.postValue(true)
        run {
            val list = LinkedList(appDb!!.reminderDao().getActiveInRange(
                    false,
                    TimeUtil.getGmtFromDateTime(System.currentTimeMillis()),
                    TimeUtil.getGmtFromDateTime(dateTime)))
            end {
                isInProgress.postValue(false)
                activeReminders.postValue(list)
            }
        }
    }

    fun getBirthdays(dateTime: Long, time: Long) {
        isInProgress.postValue(true)
        run {
            val list = LinkedList(appDb!!.birthdaysDao().all)
            for (i in list.indices.reversed()) {
                val itemTime = list[i].getDateTime(time)
                if (itemTime < System.currentTimeMillis() || itemTime > dateTime) {
                    list.removeAt(i)
                }
            }
            end {
                isInProgress.postValue(false)
                birthdays.postValue(list)
            }
        }
    }

    fun findSuggestion(suggestion: String): Model? {
        return recognizer.parse(suggestion)
    }

    fun findResults(matches: ArrayList<*>): Reminder? {
        for (i in matches.indices) {
            val key = matches[i]
            val keyStr = key.toString()
            val model = recognizer.parse(keyStr)
            if (model != null) {
                LogUtil.d(TAG, "parseResults: $model")
                return createReminder(model)
            }
        }
        return null
    }

    fun parseResults(matches: ArrayList<*>, isWidget: Boolean) {
        for (i in matches.indices) {
            val key = matches[i]
            val keyStr = key.toString()
            val model = findSuggestion(keyStr)
            if (model != null) {
                LogUtil.d(TAG, "parseResults: $model")
                val types = model.type
                if (types == ActionType.ACTION && isWidget) {
                    val action = model.action
                    if (action == Action.APP) {
                        getApplication<Application>().startActivity(Intent(getApplication(), SplashScreen::class.java))
                    } else if (action == Action.HELP) {
                        getApplication<Application>().startActivity(Intent(getApplication(), VoiceHelpDialog::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT))
                    } else if (action == Action.BIRTHDAY) {
                        getApplication<Application>().startActivity(Intent(getApplication(), AddBirthdayActivity::class.java))
                    } else if (action == Action.REMINDER) {
                        getApplication<Application>().startActivity(Intent(getApplication(), AddReminderActivity::class.java))
                    } else if (action == Action.VOLUME) {
                        getApplication<Application>().startActivity(Intent(getApplication(), VolumeDialog::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT))
                    } else if (action == Action.TRASH) {
                        emptyTrash(true)
                    } else if (action == Action.DISABLE) {
                        disableAllReminders(true)
                    } else if (action == Action.SETTINGS) {
                        val startActivityIntent = Intent(getApplication(), MainActivity::class.java)
                        startActivityIntent.putExtra(Constants.INTENT_POSITION, R.id.nav_settings)
                        getApplication<Application>().startActivity(startActivityIntent)
                    } else if (action == Action.REPORT) {
                        val startActivityIntent = Intent(getApplication(), MainActivity::class.java)
                        startActivityIntent.putExtra(Constants.INTENT_POSITION, R.id.nav_feedback)
                        getApplication<Application>().startActivity(startActivityIntent)
                    }
                } else if (types == ActionType.NOTE) {
                    saveNote(createNote(model.summary), true, true)
                } else if (types == ActionType.REMINDER) {
                    saveReminder(model, isWidget)
                } else if (types == ActionType.GROUP) {
                    saveGroup(createGroup(model), true)
                }
                break
            }
        }
    }

    private fun saveReminder(model: Model, widget: Boolean) {
        val reminder = createReminder(model)
        saveAndStartReminder(reminder)
        if (widget) {
            getApplication<Application>().startActivity(Intent(getApplication(), VoiceResultDialog::class.java)
                    .putExtra(Constants.INTENT_ID, reminder.uuId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        } else {
            Toast.makeText(getApplication(), R.string.saved, Toast.LENGTH_SHORT).show()
        }
    }

    fun disableAllReminders(showToast: Boolean) {
        isInProgress.postValue(true)
        run {
            for (reminder in appDb!!.reminderDao().getAll(true, false)) {
                stopReminder(reminder)
            }
            end {
                isInProgress.postValue(false)
                result.postValue(Commands.DELETED)
                if (showToast) {
                    Toast.makeText(getApplication(), R.string.all_reminders_were_disabled, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun emptyTrash(showToast: Boolean) {
        isInProgress.postValue(true)
        run {
            val archived = appDb!!.reminderDao().getAll(false, true)
            for (reminder in archived) {
                deleteReminder(reminder, false)
                CalendarUtils.deleteEvents(getApplication(), reminder.uniqueId)
            }
            end {
                isInProgress.postValue(false)
                result.postValue(Commands.TRASH_CLEARED)
                if (showToast) {
                    Toast.makeText(getApplication(), R.string.trash_cleared, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun createReminder(model: Model): Reminder {
        val action = model.action
        val number = model.target
        val summary = model.summary
        val repeat = model.repeatInterval
        val weekdays = model.weekdays
        val isCalendar = model.isHasCalendar
        val startTime = model.dateTime
        var eventTime = TimeUtil.getDateTimeFromGmt(startTime)
        var typeT = Reminder.BY_DATE
        if (action == Action.WEEK || action == Action.WEEK_CALL || action == Action.WEEK_SMS) {
            typeT = Reminder.BY_WEEK
            eventTime = TimeCount.getInstance(getApplication()).getNextWeekdayTime(TimeUtil.getDateTimeFromGmt(startTime), weekdays, 0)
            if (!TextUtils.isEmpty(number)) {
                if (action == Action.WEEK_CALL)
                    typeT = Reminder.BY_WEEK_CALL
                else
                    typeT = Reminder.BY_WEEK_SMS
            }
        } else if (action == Action.CALL) {
            typeT = Reminder.BY_DATE_CALL
        } else if (action == Action.MESSAGE) {
            typeT = Reminder.BY_DATE_SMS
        } else if (action == Action.MAIL) {
            typeT = Reminder.BY_DATE_EMAIL
        }
        val item = defaultGroup.value
        var categoryId: String? = ""
        if (item != null) {
            categoryId = item.uuId
        }
        val prefs = Prefs.getInstance(getApplication())
        val isCal = prefs.getBoolean(Prefs.EXPORT_TO_CALENDAR)
        val isStock = prefs.getBoolean(Prefs.EXPORT_TO_STOCK)
        val reminder = Reminder()
        reminder.type = typeT
        reminder.summary = summary
        reminder.groupUuId = categoryId
        reminder.weekdays = weekdays
        reminder.repeatInterval = repeat
        reminder.target = number
        reminder.eventTime = TimeUtil.getGmtFromDateTime(eventTime)
        reminder.startTime = TimeUtil.getGmtFromDateTime(eventTime)
        reminder.isExportToCalendar = isCalendar && (isCal || isStock)
        return reminder
    }

    fun createNote(note: String?): Note {
        val color = Random().nextInt(15)
        val item = Note()
        item.color = color
        item.summary = note
        item.date = TimeUtil.gmtDateTime
        return item
    }

    fun saveNote(note: Note, showToast: Boolean, addQuickNote: Boolean) {
        val prefs = Prefs.getInstance(getApplication())
        if (addQuickNote && prefs.getBoolean(Prefs.QUICK_NOTE_REMINDER)) {
            saveQuickReminder(note.key, note.summary)
        }
        appDb!!.notesDao().insert(note)
        UpdatesHelper.getInstance(getApplication()).updateNotesWidget()
        if (showToast) {
            Toast.makeText(getApplication(), R.string.saved, Toast.LENGTH_SHORT).show()
        }
    }

    fun saveQuickReminder(key: String?, summary: String?): Reminder {
        val after = (Prefs.getInstance(getApplication()).getInt(Prefs.QUICK_NOTE_REMINDER_TIME) * 1000 * 60).toLong()
        val due = System.currentTimeMillis() + after
        val mReminder = Reminder()
        mReminder.type = Reminder.BY_DATE
        mReminder.delay = 0
        mReminder.eventCount = 0
        mReminder.isUseGlobal = true
        mReminder.noteId = key
        mReminder.summary = summary
        val def = defaultGroup.value
        if (def != null) {
            mReminder.groupUuId = def.uuId
        }
        mReminder.startTime = TimeUtil.getGmtFromDateTime(due)
        mReminder.eventTime = TimeUtil.getGmtFromDateTime(due)
        saveAndStartReminder(mReminder)
        return mReminder
    }

    fun createGroup(model: Model): Group {
        return Group(model.summary, Random().nextInt(16))
    }

    fun saveGroup(model: Group, showToast: Boolean) {
        appDb!!.groupDao().insert(model)
        if (showToast) {
            Toast.makeText(getApplication(), R.string.saved, Toast.LENGTH_SHORT).show()
        }
    }

    private inner class ContactHelper : ContactsInterface {

        override fun findEmail(input: String): ContactOutput? {
            var input = input
            if (!Permissions.checkPermission(getApplication(), Permissions.READ_CONTACTS)) {
                return null
            }
            var number: String? = null
            val parts = input.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (part in parts) {
                while (part.length > 1) {
                    val selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like '%" + part + "%'"
                    val projection = arrayOf(ContactsContract.CommonDataKinds.Email.DATA)
                    val c = getApplication<Application>().contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            projection, selection, null, null)
                    if (c != null && c.moveToFirst()) {
                        number = c.getString(0)
                        c.close()
                    }
                    if (number != null)
                        break
                    part = part.substring(0, part.length - 2)
                }
                if (number != null) {
                    input = input.replace(part, "")
                    break
                }
            }
            return ContactOutput(input, number)
        }

        override fun findNumber(input: String): ContactOutput? {
            var input = input
            if (!Permissions.checkPermission(getApplication(), Permissions.READ_CONTACTS)) {
                return null
            }
            var number: String? = null
            val parts = input.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (part in parts) {
                while (part.length > 1) {
                    val selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " like '%" + part + "%'"
                    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val c = getApplication<Application>().contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            projection, selection, null, null)
                    if (c != null && c.moveToFirst()) {
                        number = c.getString(0)
                        c.close()
                    }
                    if (number != null) {
                        break
                    }
                    part = part.substring(0, part.length - 1)
                }
                if (number != null) {
                    input = input.replace(part, "")
                    break
                }
            }
            return ContactOutput(input.trim { it <= ' ' }, number)
        }
    }

    companion object {

        private val TAG = "ConversationViewModel"
    }
}
