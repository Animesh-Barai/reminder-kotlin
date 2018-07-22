package com.elementary.tasks.core.utils

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar

import com.elementary.tasks.R

import androidx.appcompat.widget.PopupMenu
import com.elementary.tasks.core.utils.ThemeUtil.Companion.THEME_AMOLED

import kotlinx.android.synthetic.main.dialog_with_seek_and_title.view.*

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
object Dialogues {

    private const val MAX_RADIUS = 100000
    private const val MAX_DEF_RADIUS = 5000

    fun showRadiusDialog(context: Context, current: Int, listener: OnValueSelectedListener<Int>) {
        val builder = Dialogues.getDialog(context)
        builder.setTitle(R.string.radius)
        val b = LayoutInflater.from(context).inflate(R.layout.dialog_with_seek_and_title, null, false)
        b.seekBar.max = MAX_DEF_RADIUS
        while (b.seekBar.max < current && b.seekBar.max < MAX_RADIUS) {
            b.seekBar.max = b.seekBar.max + 1000
        }
        if (current > MAX_RADIUS) {
            b.seekBar.max = MAX_RADIUS
        }
        b.seekBar.max = current * 2
        if (current == 0) {
            b.seekBar.max = MAX_DEF_RADIUS
        }
        b.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                b.titleView.text = listener.getTitle(progress)
                val perc = progress.toFloat() / b.seekBar.max.toFloat() * 100f
                if (perc > 95f && b.seekBar.max < MAX_RADIUS) {
                    b.seekBar.max = b.seekBar.max + 1000
                } else if (perc < 15f && b.seekBar.max > 5000) {
                    b.seekBar.max = b.seekBar.max - 1000
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        b.seekBar.progress = current
        b.titleView.text = listener.getTitle(current)
        builder.setView(b)
        builder.setPositiveButton(R.string.ok) { _, _ -> listener.onSelected(b.seekBar.progress) }
        builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    fun getDialog(context: Context): AlertDialog.Builder {
        return if (Prefs.getInstance(context).appTheme == THEME_AMOLED) {
            AlertDialog.Builder(context, ThemeUtil.getInstance(context).dialogStyle)
        } else {
            AlertDialog.Builder(context)
        }
    }

    fun showLCAM(context: Context, listener: ((Int) -> Unit)?, vararg actions: String) {
        val builder = getDialog(context)
        builder.setItems(actions) { dialog, item ->
            dialog.dismiss()
            listener?.invoke(item)
        }
        val alert = builder.create()
        alert.show()
    }

    fun showPopup(context: Context, anchor: View,
                  listener: ((Int) -> Unit)?, vararg actions: String) {
        val popupMenu = PopupMenu(context, anchor)
        popupMenu.setOnMenuItemClickListener { item ->
            listener?.invoke(item.order)
            true
        }
        for (i in actions.indices) {
            popupMenu.menu.add(1, i + 1000, i, actions[i])
        }
        popupMenu.show()
    }

    interface OnValueSelectedListener<T> {
        fun onSelected(t: T)
        fun getTitle(t: T): String
    }
}