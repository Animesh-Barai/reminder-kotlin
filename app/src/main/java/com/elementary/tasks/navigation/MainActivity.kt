package com.elementary.tasks.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProviders
import com.elementary.tasks.R
import com.elementary.tasks.ReminderApp
import com.elementary.tasks.birthdays.list.BirthdaysFragment
import com.elementary.tasks.core.ThemedActivity
import com.elementary.tasks.core.async.BackupSettingTask
import com.elementary.tasks.core.cloud.GTasks
import com.elementary.tasks.core.utils.*
import com.elementary.tasks.core.viewModels.conversation.ConversationViewModel
import com.elementary.tasks.core.viewModels.notes.NoteViewModel
import com.elementary.tasks.dayView.DayViewFragment
import com.elementary.tasks.googleTasks.GoogleTasksFragment
import com.elementary.tasks.groups.list.GroupsFragment
import com.elementary.tasks.monthView.CalendarFragment
import com.elementary.tasks.navigation.fragments.BaseFragment
import com.elementary.tasks.navigation.fragments.FeedbackFragment
import com.elementary.tasks.navigation.fragments.HelpFragment
import com.elementary.tasks.navigation.fragments.MapFragment
import com.elementary.tasks.navigation.settings.BaseSettingsFragment
import com.elementary.tasks.navigation.settings.SettingsFragment
import com.elementary.tasks.notes.QuickNoteCoordinator
import com.elementary.tasks.notes.list.NotesFragment
import com.elementary.tasks.reminder.lists.ArchiveFragment
import com.elementary.tasks.reminder.lists.RemindersFragment
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import timber.log.Timber
import javax.inject.Inject

class MainActivity : ThemedActivity(), NavigationView.OnNavigationItemSelectedListener, FragmentCallback,
        RemotePrefs.SaleObserver, RemotePrefs.UpdateObserver, (View, GlobalButtonObservable.Action) -> Unit {
    override fun invoke(view: View, action: GlobalButtonObservable.Action) {
        if (action == GlobalButtonObservable.Action.QUICK_NOTE) {
            mNoteView?.switchQuickNote()
        } else if (action == GlobalButtonObservable.Action.VOICE) {
            SuperUtil.startVoiceRecognitionActivity(this, VOICE_RECOGNITION_REQUEST_CODE, false, prefs, language)
        }
    }

    @Inject
    lateinit var remotePrefs: RemotePrefs
    @Inject
    lateinit var buttonObservable: GlobalButtonObservable

    private var fragment: BaseFragment? = null
    private var mNoteView: QuickNoteCoordinator? = null

    private lateinit var viewModel: ConversationViewModel

    private var prevItem: Int = 0
    private var beforeSettings: Int = 0
    private var isBackPressed: Boolean = false
    private var pressedTime: Long = 0

    init {
        ReminderApp.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.addOnBackStackChangedListener { onStackChanged() }
        initActionBar()
        initNavigation()
        initViewModel()
        initQuickNote(savedInstanceState)
    }

    private fun onStackChanged() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            val f = supportFragmentManager.findFragmentById(R.id.main_container)
            if (f != null && f is BaseFragment && f.isResumed) f.onBackStackResume()
        }
    }

    private fun initQuickNote(savedInstanceState: Bundle?) {
        val noteViewModel = ViewModelProviders.of(this, NoteViewModel.Factory(application, "")).get(NoteViewModel::class.java)
        mNoteView = QuickNoteCoordinator(this, quickNoteContainer, quickNoteView,
                noteViewModel, prefs, notifier)
        when {
            savedInstanceState != null -> openScreen(savedInstanceState.getInt(CURRENT_SCREEN, R.id.nav_current))
            intent.getIntExtra(Constants.INTENT_POSITION, 0) != 0 -> {
                prevItem = intent.getIntExtra(Constants.INTENT_POSITION, 0)
                nav_view.setCheckedItem(prevItem)
                openScreen(prevItem)
            }
            else -> initStartFragment()
        }
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(ConversationViewModel::class.java)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_SCREEN, prevItem)
        super.onSaveInstanceState(outState)
    }

    private fun initStartFragment() {
        prevItem = R.id.nav_current
        nav_view.setCheckedItem(prevItem)
        replaceFragment(RemindersFragment(), getString(R.string.events))
    }

    private fun initActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp)
        toolbar.setNavigationOnClickListener { onDrawerClick() }
    }

    private fun onDrawerClick() {
        if (this.fragment is BaseSettingsFragment) {
            onBackPressed()
        } else {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    private fun replaceFragment(fragment: BaseFragment, title: String) {
        clearBackStack()
        this.fragment = fragment
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.main_container, fragment, title)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        ft.addToBackStack(title)
        ft.commit()
    }

    override fun onResume() {
        super.onResume()
        buttonObservable.addObserver(GlobalButtonObservable.Action.QUICK_NOTE, this)
        buttonObservable.addObserver(GlobalButtonObservable.Action.VOICE, this)
        if (prefs.isUiChanged) {
            prefs.isUiChanged = false
            recreate()
        }
        if (!prefs.isBetaWarmingShowed) {
            showBetaDialog()
        }
        remotePrefs.addUpdateObserver(this)
        if (!Module.isPro) {
            remotePrefs.addSaleObserver(this)
        }
    }

    override fun onPause() {
        super.onPause()
        buttonObservable.removeObserver(GlobalButtonObservable.Action.QUICK_NOTE, this)
        buttonObservable.removeObserver(GlobalButtonObservable.Action.VOICE, this)
        if (!Module.isPro) {
            remotePrefs.removeSaleObserver(this)
        }
        remotePrefs.removeUpdateObserver(this)
    }

    private fun showBetaDialog() {
        prefs.isBetaWarmingShowed = true
        var appVersion = ""
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            appVersion = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        if (!appVersion.contains("beta")) {
            return
        }
        val builder = dialogues.getDialog(this)
        builder.setTitle("Beta")
        builder.setMessage("This version of application may work unstable!")
        builder.setPositiveButton(getString(R.string.ok)) { dialogInterface, _ -> dialogInterface.dismiss() }
        builder.create().show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (prefs.isAutoBackupEnabled && prefs.isSettingsBackupEnabled
                && Permissions.checkPermission(this, Permissions.WRITE_EXTERNAL, Permissions.READ_EXTERNAL)) {
            BackupSettingTask().execute()
        }
    }

    override fun onTitleChange(title: String) {
        toolbar.title = title
    }

    override fun onFragmentSelect(fragment: BaseFragment) {
        this.fragment = fragment
        if (this.fragment is BaseSettingsFragment) {
            if (isDark) {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
            } else {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
            }
        } else {
            if (isDark) {
                toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp)
            } else {
                toolbar.setNavigationIcon(R.drawable.ic_menu_black_24dp)
            }
        }
    }

    private fun clearBackStack() {
        val fm = supportFragmentManager
        for (i in 0 until fm.backStackEntryCount) {
            fm.popBackStack()
        }
    }

    override fun openFragment(fragment: BaseFragment, tag: String, replace: Boolean) {
        if (replace) {
            replaceFragment(fragment, tag)
        } else {
            openFragment(fragment, tag)
        }
    }

    override fun openFragment(fragment: BaseFragment, tag: String) {
        val ft = supportFragmentManager.beginTransaction()
        ft.add(R.id.main_container, fragment, tag)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        ft.addToBackStack(tag)
        ft.commit()
    }

    override fun refreshMenu() {
        setMenuVisible()
    }

    override fun onScrollUpdate(y: Int) {
        Timber.d("onScrollUpdate: %s", y)
        appBar.isSelected = y > 0
    }

    override fun onMenuSelect(menu: Int) {
        prevItem = menu
        nav_view.setCheckedItem(prevItem)
    }

    private fun initNavigation() {
        nav_view.isVerticalScrollBarEnabled = false
        nav_view.setNavigationItemSelectedListener(this)
        val view = nav_view.getHeaderView(0)
        view.sale_badge.visibility = View.GONE
        view.update_badge.visibility = View.GONE
        val nameView = view.findViewById<TextView>(R.id.appNameBannerPro)
        if (Module.isPro) {
            nameView.visibility = View.VISIBLE
        } else {
            nameView.visibility = View.GONE
        }
        setMenuVisible()
    }

    private fun setMenuVisible() {
        val menu = nav_view.menu
        menu.getItem(5)?.isVisible = GTasks.getInstance(this)?.isLogged ?: false
        menu.getItem(11)?.isVisible = !Module.isPro && !SuperUtil.isAppInstalled(this, "com.cray.software.justreminderpro")
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else if (mNoteView != null && mNoteView!!.isNoteVisible) {
            mNoteView?.hideNoteView()
        } else {
            moveBack()
        }
    }

    private fun moveBack() {
        if (fragment != null) {
            if (fragment is SettingsFragment) {
                if (beforeSettings != 0) {
                    prevItem = beforeSettings
                    nav_view.setCheckedItem(beforeSettings)
                    openScreen(beforeSettings)
                } else {
                    initStartFragment()
                }
                return
            } else if (fragment is BaseSettingsFragment && fragment?.canGoBack()!!) {
                super.onBackPressed()
                return
            }
        }
        if (isBackPressed) {
            if (System.currentTimeMillis() - pressedTime < PRESS_AGAIN_TIME) {
                finish()
            } else {
                isBackPressed = false
                onBackPressed()
            }
        } else {
            isBackPressed = true
            pressedTime = System.currentTimeMillis()
            Toast.makeText(this, getString(R.string.press_again_to_exit), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) ?: return
            viewModel.parseResults(matches, false)
        }
        fragment?.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (Module.isMarshmallow) {
            fragment?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawer_layout.closeDrawer(GravityCompat.START)
        Handler().postDelayed({
            if (prevItem == item.itemId && (item.itemId != R.id.nav_feedback || item.itemId != R.id.nav_help && item.itemId != R.id.nav_pro)) {
                return@postDelayed
            }
            openScreen(item.itemId)
            if (item.itemId != R.id.nav_feedback && item.itemId != R.id.nav_help && item.itemId != R.id.nav_pro) {
                prevItem = item.itemId
            }
        }, 250)
        return true
    }

    private fun openScreen(itemId: Int) {
        beforeSettings = 0
        when (itemId) {
            R.id.nav_current -> replaceFragment(RemindersFragment(), getString(R.string.tasks))
            R.id.nav_notes -> replaceFragment(NotesFragment(), getString(R.string.notes))
            R.id.nav_birthdays -> replaceFragment(BirthdaysFragment(), getString(R.string.birthdays))
            R.id.nav_calendar -> replaceFragment(CalendarFragment(), getString(R.string.calendar))
            R.id.nav_day_view -> replaceFragment(DayViewFragment(), getString(R.string.events))
            R.id.nav_tasks -> replaceFragment(GoogleTasksFragment(), getString(R.string.google_tasks))
            R.id.nav_groups -> replaceFragment(GroupsFragment(), getString(R.string.groups))
            R.id.nav_map -> replaceFragment(MapFragment(), getString(R.string.map))
            R.id.nav_archive -> replaceFragment(ArchiveFragment(), getString(R.string.trash))
            R.id.nav_settings -> {
                beforeSettings = prevItem
                replaceFragment(SettingsFragment(), getString(R.string.action_settings))
            }
            R.id.nav_feedback -> replaceFragment(FeedbackFragment(), getString(R.string.feedback))
            R.id.nav_help -> replaceFragment(HelpFragment(), getString(R.string.help))
            R.id.nav_pro -> showProDialog()
        }
    }

    private fun openMarket() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("market://details?id=" + "com.cray.software.justreminderpro")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.could_not_launch_market, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showProDialog() {
        dialogues.getDialog(this)
                .setTitle(getString(R.string.buy_pro))
                .setMessage(getString(R.string.pro_advantages) + "\n" +
                        getString(R.string.different_settings_for_birthdays) + "\n" +
                        getString(R.string.additional_reminder) + "\n" +
                        getString(R.string._led_notification_) + "\n" +
                        getString(R.string.led_color_for_each_reminder) + "\n" +
                        getString(R.string.styles_for_marker) + "\n" +
                        getString(R.string.option_for_image_blurring) + "\n" +
                        getString(R.string.additional_app_themes))
                .setPositiveButton(R.string.buy) { dialog, _ ->
                    dialog.dismiss()
                    openMarket()
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .create().show()
    }

    override fun onSale(discount: String, expiryDate: String) {
        val expiry = TimeUtil.getFireFormatted(prefs, expiryDate)
        val view = nav_view.getHeaderView(0)
        if (TextUtils.isEmpty(expiry)) {
            view.sale_badge.visibility = View.GONE
        } else {
            view.sale_badge.visibility = View.VISIBLE
            view.sale_badge.text = "SALE" + " " + getString(R.string.app_name_pro) + " -" + discount + getString(R.string.p_until) + " " + expiry
        }
    }

    override fun noSale() {
        val view = nav_view.getHeaderView(0)
        view.sale_badge.visibility = View.GONE
    }

    override fun onUpdate(version: String) {
        val view = nav_view.getHeaderView(0)
        view.update_badge.visibility = View.VISIBLE
        view.update_badge.text = getString(R.string.update_available) + ": " + version
        view.update_badge.setOnClickListener { SuperUtil.launchMarket(this@MainActivity) }
    }

    override fun noUpdate() {
        val view = nav_view.getHeaderView(0)
        view.update_badge.visibility = View.GONE
    }

    companion object {

        const val VOICE_RECOGNITION_REQUEST_CODE = 109
        private const val PRESS_AGAIN_TIME = 2000
        private const val CURRENT_SCREEN = "current_screen"
    }
}
