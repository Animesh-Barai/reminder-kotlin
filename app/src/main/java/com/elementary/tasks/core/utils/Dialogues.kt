package com.elementary.tasks.core.utils

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.elementary.tasks.R
import com.elementary.tasks.databinding.DialogBottomColorSliderBinding
import com.elementary.tasks.databinding.DialogBottomSeekAndTitleBinding
import com.elementary.tasks.databinding.DialogWithSeekAndTitleBinding
import com.elementary.tasks.databinding.ViewColorSliderBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Dialogues {

    fun showColorBottomDialog(activity: Activity, current: Int, colors: IntArray = ThemeUtil.colorsForSlider(activity),
                        onChange: (Int) -> Unit) {
        val dialog = BottomSheetDialog(activity)
        val b = DialogBottomColorSliderBinding.inflate(LayoutInflater.from(activity))
        b.colorSlider.setColors(colors)
        b.colorSlider.setSelectorColorResource(if (ThemeUtil.isDarkMode(activity)) R.color.pureWhite else R.color.pureBlack)
        b.colorSlider.setSelection(current)
        b.colorSlider.setListener { i, _ ->
            onChange.invoke(i)
        }
        dialog.setContentView(b.root)
        dialog.show()
    }

    fun showRadiusBottomDialog(activity: Activity, current: Int, listener: (Int) -> String) {
        val dialog = BottomSheetDialog(activity)
        val b = DialogBottomSeekAndTitleBinding.inflate(LayoutInflater.from(activity))
        b.seekBar.max = MAX_DEF_RADIUS
        if (b.seekBar.max < current && b.seekBar.max < MAX_RADIUS) {
            b.seekBar.max = (current + (b.seekBar.max * 0.2)).toInt()
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
                b.titleView.text = listener.invoke(progress)
                val perc = progress.toFloat() / b.seekBar.max.toFloat() * 100f
                if (perc > 95f && b.seekBar.max < MAX_RADIUS) {
                    b.seekBar.max = (b.seekBar.max + (b.seekBar.max * 0.2)).toInt()
                } else if (perc < 10f && b.seekBar.max > 5000) {
                    b.seekBar.max = (b.seekBar.max - (b.seekBar.max * 0.2)).toInt()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        b.seekBar.progress = current
        b.titleView.text = listener.invoke(current)
        dialog.setContentView(b.root)
        dialog.show()
    }

    fun showColorDialog(activity: Activity, current: Int, title: String,
                        colors: IntArray = ThemeUtil.colorsForSlider(activity),
                        onDone: (Int) -> Unit) {
        val builder = getMaterialDialog(activity)
        builder.setTitle(title)
        val bind = ViewColorSliderBinding.inflate(LayoutInflater.from(activity))
        bind.colorSlider.setColors(colors)
        bind.colorSlider.setSelectorColorResource(if (ThemeUtil.isDarkMode(activity)) R.color.pureWhite else R.color.pureBlack)
        bind.colorSlider.setSelection(current)
        builder.setView(bind.root)
        builder.setPositiveButton(R.string.save) { dialog, _ ->
            val selected = bind.colorSlider.selectedItem
            dialog.dismiss()
            onDone.invoke(selected)
        }
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
        setFullWidthDialog(dialog, activity)
    }

    fun showRadiusDialog(activity: Activity, current: Int, listener: OnValueSelectedListener<Int>) {
        val builder = getMaterialDialog(activity)
        builder.setTitle(R.string.radius)
        val b = DialogWithSeekAndTitleBinding.inflate(LayoutInflater.from(activity))
        b.seekBar.max = MAX_DEF_RADIUS
        if (b.seekBar.max < current && b.seekBar.max < MAX_RADIUS) {
            b.seekBar.max = (current + (b.seekBar.max * 0.2)).toInt()
        }
        if (current > MAX_RADIUS) {
            b.seekBar.max = MAX_RADIUS
        }
        if (current == 0) {
            b.seekBar.max = MAX_DEF_RADIUS
        }
        b.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                b.titleView.text = listener.getTitle(progress)
                val perc = progress.toFloat() / b.seekBar.max.toFloat() * 100f
                if (perc > 95f && b.seekBar.max < MAX_RADIUS) {
                    b.seekBar.max = (b.seekBar.max + (b.seekBar.max * 0.2)).toInt()
                } else if (perc < 10f && b.seekBar.max > 5000) {
                    b.seekBar.max = (b.seekBar.max - (b.seekBar.max * 0.2)).toInt()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        b.seekBar.progress = current
        b.titleView.text = listener.getTitle(current)
        builder.setView(b.root)
        builder.setPositiveButton(R.string.ok) { _, _ -> listener.onSelected(b.seekBar.progress) }
        builder.setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        val dialog = builder.create()
        dialog.show()
        setFullWidthDialog(dialog, activity)
    }

    fun getMaterialDialog(context: Context): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(context)
    }

    fun getNullableDialog(context: Context?): MaterialAlertDialogBuilder? {
        return if (context != null) {
            getMaterialDialog(context)
        } else null
    }

    fun askConfirmation(context: Context, title: String, onAction: (Boolean) -> Unit) {
        getMaterialDialog(context)
                .setTitle(title)
                .setMessage(context.getString(R.string.are_you_sure))
                .setPositiveButton(context.getString(R.string.yes)) { dialog, _ ->
                    dialog.dismiss()
                    onAction.invoke(true)
                }
                .setNegativeButton(context.getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                    onAction.invoke(false)
                }
                .create()
                .show()
    }

    interface OnValueSelectedListener<T> {
        fun onSelected(t: T)
        fun getTitle(t: T): String
    }

    companion object {
        private const val MAX_RADIUS = 100000
        private const val MAX_DEF_RADIUS = 5000

        fun getMaterialDialog(context: Context): MaterialAlertDialogBuilder {
            return MaterialAlertDialogBuilder(context)
        }

        fun showPopup(anchor: View,
                      listener: ((Int) -> Unit)?, vararg actions: String) {
            val popupMenu = PopupMenu(anchor.context, anchor)
            popupMenu.setOnMenuItemClickListener { item ->
                listener?.invoke(item.order)
                true
            }
            for (i in actions.indices) {
                popupMenu.menu.add(1, i + 1000, i, actions[i])
            }
            popupMenu.show()
        }

        fun setFullWidthDialog(dialog: AlertDialog, activity: Activity?) {
            if (activity == null) return
            val window = dialog.window
            window?.setGravity(Gravity.CENTER)
            window?.setLayout((MeasureUtils.dp2px(activity, 380)), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}
