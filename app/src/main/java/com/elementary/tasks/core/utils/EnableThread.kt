package com.elementary.tasks.core.utils

import android.content.Context
import com.elementary.tasks.core.controller.EventControlFactory
import com.elementary.tasks.core.data.AppDb
import com.elementary.tasks.core.utils.launchDefault

/**
 * Copyright 2017 Nazar Suhovich
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
object EnableThread {

    fun run(context: Context) {
        launchDefault {
            for (item in AppDb.getAppDatabase(context).reminderDao().getAll(true, false)) {
                EventControlFactory.getController(item).start()
            }
        }
    }
}