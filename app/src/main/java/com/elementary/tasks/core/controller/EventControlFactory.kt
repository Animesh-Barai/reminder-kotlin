package com.elementary.tasks.core.controller

import com.elementary.tasks.core.data.models.Reminder
import com.elementary.tasks.core.utils.LogUtil

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
object EventControlFactory {

    private const val TAG = "EventControlFactory"

    fun getController(reminder: Reminder): EventControl {
        val control: EventControl = when {
            Reminder.isSame(reminder.type, Reminder.BY_DATE_SHOP) -> ShoppingEvent(reminder)
            Reminder.isBase(reminder.type, Reminder.BY_DATE) -> DateEvent(reminder)
            Reminder.isBase(reminder.type, Reminder.BY_LOCATION) -> LocationEvent(reminder)
            Reminder.isBase(reminder.type, Reminder.BY_MONTH) -> MonthlyEvent(reminder)
            Reminder.isBase(reminder.type, Reminder.BY_WEEK) -> WeeklyEvent(reminder)
            Reminder.isBase(reminder.type, Reminder.BY_OUT) -> LocationEvent(reminder)
            Reminder.isBase(reminder.type, Reminder.BY_PLACES) -> LocationEvent(reminder)
            Reminder.isSame(reminder.type, Reminder.BY_TIME) -> TimerEvent(reminder)
            Reminder.isBase(reminder.type, Reminder.BY_DAY_OF_YEAR) -> YearlyEvent(reminder)
            else -> DateEvent(reminder)
        }
        LogUtil.d(TAG, "getController: $control")
        return control
    }
}