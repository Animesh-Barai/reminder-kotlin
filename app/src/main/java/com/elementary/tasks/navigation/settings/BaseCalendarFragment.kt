package com.elementary.tasks.navigation.settings

import com.elementary.tasks.ReminderApp
import com.elementary.tasks.core.appWidgets.UpdatesHelper
import com.elementary.tasks.core.utils.CalendarUtils
import com.elementary.tasks.navigation.fragments.BaseNavigationFragment
import javax.inject.Inject

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
abstract class BaseCalendarFragment : BaseNavigationFragment() {

    @Inject
    lateinit var updatesHelper: UpdatesHelper
    @Inject
    lateinit var calendarUtils: CalendarUtils

    init {
        ReminderApp.appComponent.inject(this)
    }
}