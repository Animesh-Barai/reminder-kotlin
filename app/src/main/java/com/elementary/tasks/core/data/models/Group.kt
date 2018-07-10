package com.elementary.tasks.core.data.models

import com.elementary.tasks.core.utils.SuperUtil
import com.elementary.tasks.core.utils.TimeUtil
import com.google.gson.annotations.SerializedName

import java.util.UUID

import androidx.room.Entity
import androidx.room.PrimaryKey

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
@Entity
class Group {

    @SerializedName("title")
    var title: String? = null
    @SerializedName("uuId")
    @PrimaryKey
    var uuId: String? = null
    @SerializedName("color")
    var color: Int = 0
    @SerializedName("dateTime")
    var dateTime: String? = null

    constructor(title: String, color: Int) {
        this.title = title
        this.uuId = UUID.randomUUID().toString()
        this.color = color
        this.dateTime = TimeUtil.gmtDateTime
    }

    constructor(title: String, uuId: String, color: Int, dateTime: String) {
        this.title = title
        this.uuId = uuId
        this.color = color
        this.dateTime = dateTime
    }

    override fun toString(): String {
        return SuperUtil.getObjectPrint(this, Group::class.java)
    }
}
