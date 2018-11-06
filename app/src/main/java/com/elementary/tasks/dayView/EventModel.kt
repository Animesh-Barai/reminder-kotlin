package com.elementary.tasks.dayView

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
data class EventModel(
        val viewType: Int,
        var model: Any,
        val day: Int,
        val month: Int,
        val year: Int,
        val color: Int
) {
    companion object {
        const val REMINDER = 0
        const val SHOPPING = 1
        const val BIRTHDAY = 2
    }
}