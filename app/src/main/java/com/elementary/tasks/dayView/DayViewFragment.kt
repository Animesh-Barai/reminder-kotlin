package com.elementary.tasks.dayView

import android.app.AlarmManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.elementary.tasks.R
import com.elementary.tasks.core.calendar.InfinitePagerAdapter
import com.elementary.tasks.core.calendar.InfiniteViewPager
import com.elementary.tasks.core.utils.GlobalButtonObservable
import com.elementary.tasks.core.utils.Module
import com.elementary.tasks.core.utils.TimeUtil
import com.elementary.tasks.dayView.pager.DayPagerAdapter
import com.elementary.tasks.navigation.fragments.BaseCalendarFragment
import kotlinx.android.synthetic.main.fragment_day_view.*
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
class DayViewFragment : BaseCalendarFragment() {

    lateinit var dayPagerAdapter: DayPagerAdapter
    private val datePageChangeListener = DatePageChangeListener()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = arguments
        if (intent != null) {
            dateMills = intent.getLong(DATE_KEY, 0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.day_view_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_voice -> {
                buttonObservable.fireAction(view!!, GlobalButtonObservable.Action.VOICE)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun layoutRes(): Int = R.layout.fragment_day_view

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fab.setOnClickListener { showActionDialog(false) }

        initPager()
    }

    private fun initPager() {
        dayPagerAdapter = DayPagerAdapter(if (Module.isJellyMR2) childFragmentManager else fragmentManager!!)
        pager.adapter = InfinitePagerAdapter(dayPagerAdapter)
    }

    private fun updateMenuTitles(): String {
        val dayString = if (dateMills != 0L) TimeUtil.getDate(dateMills) else TimeUtil.getDate(System.currentTimeMillis())
        callback?.onTitleChange(dayString)
        return dayString
    }

    override fun onResume() {
        super.onResume()
        callback?.onMenuSelect(R.id.nav_day_view)
        loadData()
    }

    override fun getTitle(): String = updateMenuTitles()

    private fun loadData() {
        initProvider()
        if (dateMills != 0L) {
            showEvents(dateMills)
        } else {
            showEvents(System.currentTimeMillis())
        }
    }

    private fun fromMills(mills: Long): EventsPagerItem {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = mills
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        return EventsPagerItem(day, month, year)
    }

    private fun showEvents(date: Long) {
        var mills = date - AlarmManager.INTERVAL_DAY
        dayPagerAdapter.fragments[0].setModel(fromMills(mills))

        mills += AlarmManager.INTERVAL_DAY
        dateMills = mills
        dayPagerAdapter.fragments[1].setModel(fromMills(mills))

        mills += AlarmManager.INTERVAL_DAY
        dayPagerAdapter.fragments[2].setModel(fromMills(mills))

        updateMenuTitles()

        datePageChangeListener.setCurrentDateTime(dateMills)
        pager.isEnabled = true
        pager.addOnPageChangeListener(datePageChangeListener)
        pager.currentItem = InfiniteViewPager.OFFSET + 1
    }

    private inner class DatePageChangeListener : ViewPager.OnPageChangeListener {

        var currentPage = InfiniteViewPager.OFFSET + 1
            private set
        private var currentDateTime: Long = System.currentTimeMillis()

        internal fun setCurrentDateTime(dateTime: Long) {
            this.currentDateTime = dateTime
        }

        private fun getNext(position: Int): Int {
            return (position + 1) % NUMBER_OF_PAGES
        }

        private fun getPrevious(position: Int): Int {
            return (position + 3) % NUMBER_OF_PAGES
        }

        internal fun getCurrent(position: Int): Int {
            return position % NUMBER_OF_PAGES
        }

        override fun onPageScrollStateChanged(position: Int) {

        }

        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {

        }

        private fun refreshAdapters(position: Int) {
            val currentFragment = dayPagerAdapter.fragments[getCurrent(position)]
            val prevFragment = dayPagerAdapter.fragments[getPrevious(position)]
            val nextFragment = dayPagerAdapter.fragments[getNext(position)]
            when {
                position == currentPage -> {
                    currentFragment.setModel(fromMills(currentDateTime))
                    prevFragment.setModel(fromMills(currentDateTime - AlarmManager.INTERVAL_DAY))
                    nextFragment.setModel(fromMills(currentDateTime + AlarmManager.INTERVAL_DAY))
                }
                position > currentPage -> {
                    currentDateTime += AlarmManager.INTERVAL_DAY
                    nextFragment.setModel(fromMills(currentDateTime + AlarmManager.INTERVAL_DAY))
                }
                else -> {
                    currentDateTime -= AlarmManager.INTERVAL_DAY
                    prevFragment.setModel(fromMills(currentDateTime - AlarmManager.INTERVAL_DAY))
                }
            }
            currentPage = position
        }

        override fun onPageSelected(position: Int) {
            refreshAdapters(position)
            val item = dayPagerAdapter.fragments[getCurrent(position)].getModel() ?: return
            val calendar = Calendar.getInstance()
            calendar.set(item.year, item.month, item.day)
            dateMills = calendar.timeInMillis
            updateMenuTitles()
        }
    }

    companion object {

        private const val DATE_KEY = "date"
        private const val POS_KEY = "position"
        const val NUMBER_OF_PAGES = 4

        fun newInstance(date: Long, position: Int): DayViewFragment {
            val pageFragment = DayViewFragment()
            val arguments = Bundle()
            arguments.putLong(DATE_KEY, date)
            arguments.putInt(POS_KEY, position)
            pageFragment.arguments = arguments
            return pageFragment
        }
    }
}