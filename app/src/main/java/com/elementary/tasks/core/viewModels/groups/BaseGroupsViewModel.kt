package com.elementary.tasks.core.viewModels.groups

import android.app.Application
import androidx.lifecycle.LiveData
import com.elementary.tasks.core.data.models.Group
import com.elementary.tasks.core.viewModels.BaseDbViewModel
import com.elementary.tasks.core.viewModels.Commands
import com.elementary.tasks.groups.DeleteGroupFilesAsync
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext

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
abstract class BaseGroupsViewModel(application: Application) : BaseDbViewModel(application) {

    var allGroups: LiveData<List<Group>>

    init {
        allGroups = appDb.groupDao().loadAll()
    }

    fun deleteGroup(group: Group) {
        isInProgress.postValue(true)
        launch(CommonPool) {
            appDb.groupDao().delete(group)
            withContext(UI) {
                isInProgress.postValue(false)
                result.postValue(Commands.DELETED)
            }
            DeleteGroupFilesAsync(getApplication()).execute(group.uuId)
        }
    }
}