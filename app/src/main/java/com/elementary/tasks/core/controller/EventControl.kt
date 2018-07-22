package com.elementary.tasks.core.controller

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

interface EventControl {

    val isRepeatable: Boolean

    val isActive: Boolean

    fun start(): Boolean

    fun stop(): Boolean

    fun pause(): Boolean

    fun skip(): Boolean

    fun resume(): Boolean

    operator fun next(): Boolean

    fun onOff(): Boolean

    fun canSkip(): Boolean

    fun setDelay(delay: Int)

    fun calculateTime(isNew: Boolean): Long
}