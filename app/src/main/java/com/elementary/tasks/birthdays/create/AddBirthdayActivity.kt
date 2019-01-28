package com.elementary.tasks.birthdays.create

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.elementary.tasks.R
import com.elementary.tasks.ReminderApp
import com.elementary.tasks.core.ThemedActivity
import com.elementary.tasks.core.data.models.Birthday
import com.elementary.tasks.core.services.PermanentBirthdayReceiver
import com.elementary.tasks.core.utils.*
import com.elementary.tasks.core.view_models.Commands
import com.elementary.tasks.core.view_models.birthdays.BirthdayViewModel
import com.elementary.tasks.navigation.settings.security.PinLoginActivity
import kotlinx.android.synthetic.main.activity_add_birthday.*
import timber.log.Timber
import java.text.ParseException
import java.util.*
import javax.inject.Inject

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
class AddBirthdayActivity : ThemedActivity() {

    private lateinit var viewModel: BirthdayViewModel
    private var mBirthday: Birthday? = null
    @Inject
    lateinit var backupTool: BackupTool

    private var mDateCallBack: DatePickerDialog.OnDateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, monthOfYear)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        viewModel.date.postValue(calendar.timeInMillis)
    }

    init {
        ReminderApp.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_birthday)
        initActionBar()

        if (prefs.isTelephonyAllowed) {
            contactCheck.visibility = View.VISIBLE
            contactCheck.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !prefs.isTelephonyAllowed) return@setOnCheckedChangeListener
                viewModel.isContactAttached.postValue(isChecked)
            }
        } else {
            contactCheck.visibility = View.GONE
        }

        ViewUtils.listenScrollableView(scrollView) {
            appBar.isSelected = it > 0
        }

        birthDate.setOnClickListener { dateDialog() }
        pickContact.setOnClickListener { pickContact() }

        loadBirthday()

        if (savedInstanceState == null) {
            viewModel.isContactAttached.postValue(false)
            viewModel.isLogged = intent.getBooleanExtra(ARG_LOGGED, false)
        }
    }

    override fun onStart() {
        super.onStart()

        if (prefs.hasPinCode && !viewModel.isLogged) {
            PinLoginActivity.verify(this)
        }
    }

    private fun initActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.navigationIcon = ViewUtils.backIcon(this, isDark)
    }

    private fun showBirthday(birthday: Birthday?) {
        this.mBirthday = birthday

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        toolbar.setTitle(R.string.add_birthday)
        if (birthday != null) {
            toolbar.setTitle(R.string.edit_birthday)
            if (!viewModel.isEdited) {
                birthName.setText(birthday.name)
                try {
                    val dt = TimeUtil.BIRTH_DATE_FORMAT.parse(birthday.date)
                    if (dt != null) calendar.time = dt
                } catch (e: ParseException) {
                    e.printStackTrace()
                }

                if (!TextUtils.isEmpty(birthday.number)) {
                    numberView.setText(birthday.number)
                    contactCheck.isChecked = true
                }
                viewModel.isEdited = true
            }
        }
    }

    private fun loadBirthday() {
        val id = intent.getStringExtra(Constants.INTENT_ID) ?: ""
        initViewModel(id)
        when {
            intent.data != null -> {
                try {
                    val name = intent.data ?: return
                    val scheme = name.scheme
                    mBirthday = if (ContentResolver.SCHEME_CONTENT != scheme) {
                        backupTool.getBirthday(name.path, null)
                    } else null
                    showBirthday(mBirthday)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            intent.hasExtra(Constants.INTENT_ITEM) -> {
                try {
                    mBirthday = intent.getSerializableExtra(Constants.INTENT_ITEM) as Birthday?
                    showBirthday(mBirthday)
                } catch (e: Exception) {
                }
            }
            intent.hasExtra(Constants.INTENT_DATE) -> {
                viewModel.date.postValue(intent.getLongExtra(Constants.INTENT_DATE, System.currentTimeMillis()))
            }
            else -> {
                if ((viewModel.date.value ?: 0L) == 0L) {
                    viewModel.date.postValue(System.currentTimeMillis())
                }
            }
        }
    }

    private fun initViewModel(id: String) {
        viewModel = ViewModelProviders.of(this, BirthdayViewModel.Factory(id)).get(BirthdayViewModel::class.java)
        viewModel.birthday.observe(this, Observer<Birthday> { this.showBirthday(it) })
        viewModel.result.observe(this, Observer<Commands> { commands ->
            commands?.let {
                when (it) {
                    Commands.SAVED, Commands.DELETED -> closeScreen()
                    else -> {
                    }
                }
            }
        })
        viewModel.date.observe(this, Observer { millis ->
            millis?.let {
                Timber.d("initViewModel: ${TimeUtil.getFullDateTime(millis, true)}")
                birthDate.text = TimeUtil.BIRTH_DATE_FORMAT.format(Date(it))
            }
        })
        viewModel.isContactAttached.observe(this, Observer { isAttached ->
            isAttached?.let {
                container.visibility = if (it) View.VISIBLE else View.GONE
            }
        })
    }

    private fun checkContactPermission(code: Int): Boolean {
        if (!Permissions.ensurePermissions(this, code, Permissions.READ_CONTACTS)) {
            return false
        }
        return true
    }

    private fun pickContact() {
        if (!checkContactPermission(101)) {
            return
        }
        SuperUtil.selectContact(this, Constants.REQUEST_CODE_CONTACTS)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_simple_save_action, menu)
        if (mBirthday != null) {
            menu.add(Menu.NONE, MENU_ITEM_DELETE, 100, getString(R.string.delete))
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                saveBirthday()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            MENU_ITEM_DELETE -> {
                deleteItem()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveBirthday() {
        val contact = birthName.text.toString().trim()
        if (contact == "") {
            birthNameLayout.error = getString(R.string.must_be_not_empty)
            birthNameLayout.isErrorEnabled = true
            return
        }
        var contactId = 0L
        val number = numberView.text.toString().trim()
        if (contactCheck.isChecked) {
            if (TextUtils.isEmpty(number)) {
                numberLayout.error = getString(R.string.you_dont_insert_number)
                numberLayout.isErrorEnabled = true
                return
            }
            if (!checkContactPermission(CONTACT_PERM)) {
                return
            }
            contactId = Contacts.getIdFromNumber(number, this)
        }
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = viewModel.date.value ?: System.currentTimeMillis()
        val birthday = (mBirthday ?: Birthday()).apply {
            this.name = contact
            this.contactId = contactId
            this.date = birthDate.text.toString()
            this.number = number
            this.day = calendar.get(Calendar.DAY_OF_MONTH)
            this.month = calendar.get(Calendar.MONTH)
        }
        viewModel.saveBirthday(birthday)
    }

    private fun closeScreen() {
        sendBroadcast(Intent(this, PermanentBirthdayReceiver::class.java)
                .setAction(PermanentBirthdayReceiver.ACTION_SHOW))
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun deleteItem() {
        mBirthday?.let { viewModel.deleteBirthday(it) }
    }

    private fun dateDialog() {
        val c = Calendar.getInstance()
        c.timeInMillis = viewModel.date.value ?: System.currentTimeMillis()
        TimeUtil.showDatePicker(this, themeUtil.dialogStyle, prefs, c.get(Calendar.YEAR),
                c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), mDateCallBack)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.REQUEST_CODE_CONTACTS) {
            if (resultCode == Activity.RESULT_OK) {
                val name = data?.getStringExtra(Constants.SELECTED_CONTACT_NAME)
                if (birthName.text.toString().matches("".toRegex())) {
                    birthName.setText(name)
                }
                numberView.setText(data?.getStringExtra(Constants.SELECTED_CONTACT_NUMBER) ?: "")
            }
        } else if (requestCode == PinLoginActivity.REQ_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                finish()
            } else {
                viewModel.isLogged = true
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 -> if (Permissions.isAllGranted(grantResults)) {
                SuperUtil.selectContact(this@AddBirthdayActivity, Constants.REQUEST_CODE_CONTACTS)
            }
            CONTACT_PERM -> if (Permissions.isAllGranted(grantResults)) {
                saveBirthday()
            }
        }
    }

    companion object {
        private const val MENU_ITEM_DELETE = 12
        private const val CONTACT_PERM = 102
        private const val ARG_LOGGED = "arg_logged"

        fun openLogged(context: Context, intent: Intent? = null) {
            if (intent == null) {
                context.startActivity(Intent(context, AddBirthdayActivity::class.java)
                        .putExtra(ARG_LOGGED, true))
            } else {
                intent.putExtra(ARG_LOGGED, true)
                context.startActivity(intent)
            }
        }

        fun createBirthDate(day: Int, month: Int, year: Int): String {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            return TimeUtil.BIRTH_DATE_FORMAT.format(calendar.time)
        }
    }
}
