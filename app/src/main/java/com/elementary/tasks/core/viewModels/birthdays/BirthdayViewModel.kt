package com.elementary.tasks.core.viewModels.birthdays

import android.app.Application

import com.elementary.tasks.core.data.models.Birthday

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

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
class BirthdayViewModel private constructor(application: Application, id: Int) : BaseBirthdaysViewModel(application) {

    var birthday: LiveData<Birthday>

    init {
        birthday = appDb.birthdaysDao().loadById(id)
    }

    class Factory(private val application: Application, private val key: Int) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BirthdayViewModel(application, key) as T
        }
    }
}
