package com.elementary.tasks.notes.list

import com.elementary.tasks.core.data.models.Note
import com.elementary.tasks.reminder.lists.filters.AbstractFilter
import com.elementary.tasks.reminder.lists.filters.FilterCallback
import com.elementary.tasks.reminder.lists.filters.FilterValue
import com.elementary.tasks.reminder.lists.filters.ObjectFilter

import java.util.ArrayList

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
class NoteFilterController(private val mCallback: FilterCallback<Note>?) {

    private val searchValue = FilterValue<String>()

    var original: List<Note> = ArrayList()
        set(original) {
            field = original
            onChanged()
        }
    private var mFilter: ObjectFilter<Note>? = null

    init {
        initFilters()
    }

    private fun initFilters() {
        val filter = object : AbstractFilter<String, Note>(null) {
            private var query: String? = null

            override fun filter(item: Note): Boolean {
                return query == null || query!!.length == 0 || item.summary!!.toLowerCase().contains(query!!.toLowerCase())
            }

            @Throws(Exception::class)
            override fun accept(s: String) {
                this.query = s
            }
        }
        searchValue.subscribe(filter)
        this.mFilter = filter
    }

    fun setSearchValue(value: String?) {
        if (value == null) {
            searchValue.setValue("")
        } else {
            searchValue.setValue(value)
        }
        onChanged()
    }

    private fun onChanged() {
        val list = ArrayList<Note>()
        for (item in this.original) {
            if (mFilter != null) {
                if (mFilter!!.filter(item)) list.add(item)
            } else
                list.add(item)
        }
        mCallback?.onChanged(list)
    }
}
