package com.elementary.tasks.core.utils

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.widget.Toast
import com.elementary.tasks.R

import java.io.File

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

object TelephonyUtil {

    fun sendNote(file: File, context: Context, message: String?) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        var title = "Note"
        var note = ""
        if (message != null) {
            if (message.length > 100) {
                title = message.substring(0, 48)
                title = "$title..."
            }
            if (message.length > 150) {
                note = message.substring(0, 135)
                note = "$note..."
            }
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, title)
        intent.putExtra(Intent.EXTRA_TEXT, note)
        val uri = UriUtil.getUri(context, file)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            context.startActivity(Intent.createChooser(intent, "Send email..."))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMail(context: Context, email: String, subject: String,
                 message: String, filePath: String?) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, message)
        if (filePath != null) {
            val uri = UriUtil.getUri(context, filePath)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Send email..."))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSms(number: String, context: Context) {
        if (TextUtils.isEmpty(number)) {
            return
        }
        val smsIntent = Intent(Intent.ACTION_VIEW)
        smsIntent.data = Uri.parse("sms:$number")
        try {
            context.startActivity(smsIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    fun makeCall(number: String, context: Context) {
        if (TextUtils.isEmpty(number)) {
            return
        }
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$number")
        try {
            context.startActivity(callIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    fun openApp(appPackage: String, context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(appPackage)
        try {
            context.startActivity(launchIntent)
        } catch (ignored: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    fun openLink(link: String, context: Context) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        try {
            context.startActivity(browserIntent)
        } catch (ignored: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    fun skypeCall(number: String, context: Context) {
        val uri = "skype:$number?call"
        val sky = Intent("android.intent.action.VIEW")
        sky.data = Uri.parse(uri)
        try {
            context.startActivity(sky)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    fun skypeVideoCall(number: String, context: Context) {
        val uri = "skype:$number?call&video=true"
        val sky = Intent("android.intent.action.VIEW")
        sky.data = Uri.parse(uri)
        try {
            context.startActivity(sky)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    fun skypeChat(number: String, context: Context) {
        val uri = "skype:$number?chat"
        val sky = Intent("android.intent.action.VIEW")
        sky.data = Uri.parse(uri)
        try {
            context.startActivity(sky)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
        }
    }
}