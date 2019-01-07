package com.elementary.tasks.core.appWidgets.buttons

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.elementary.tasks.ReminderApp
import com.elementary.tasks.core.services.PermanentReminderReceiver
import com.elementary.tasks.core.utils.*
import com.elementary.tasks.core.viewModels.conversation.ConversationViewModel
import javax.inject.Inject

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
class VoiceWidgetDialog : FragmentActivity() {

    private lateinit var viewModel: ConversationViewModel
    @Inject
    lateinit var themeUtil: ThemeUtil
    @Inject
    lateinit var notifier: Notifier
    @Inject
    lateinit var prefs: Prefs
    @Inject
    lateinit var language: Language

    init {
        ReminderApp.appComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(themeUtil.dialogStyle)
        viewModel = ViewModelProviders.of(this).get(ConversationViewModel::class.java)
        startVoiceRecognitionActivity()
    }

    private fun startVoiceRecognitionActivity() {
        SuperUtil.startVoiceRecognitionActivity(this, VOICE_RECOGNITION_REQUEST_CODE, false, prefs, language)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?: ArrayList()
            viewModel.parseResults(matches, true, this)
        }
        if (prefs.isSbNotificationEnabled) {
            notifier.updateReminderPermanent(PermanentReminderReceiver.ACTION_SHOW)
        }
        finish()
    }

    override fun onBackPressed() {
        finish()
    }

    companion object {

        const val VOICE_RECOGNITION_REQUEST_CODE = 109
    }
}