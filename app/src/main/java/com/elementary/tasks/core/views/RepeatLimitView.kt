package com.elementary.tasks.core.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import com.elementary.tasks.R
import com.elementary.tasks.core.binding.views.RepeatLimitViewBinding

class RepeatLimitView : LinearLayout {

    private lateinit var binding: RepeatLimitViewBinding
    var onLevelUpdateListener: ((level: Int) -> Unit)? = null
    var level: Int = 0
        get() {
            return field - 1
        }
        private set(value) {
            field = value
            if (value > 0) {
                binding.labelView.text = "${value - 1}"
            } else {
                binding.labelView.text = context.getString(R.string.no_limits)
            }
        }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        binding.sliderView.isEnabled = enabled
    }

    fun setLimit(level: Int) {
        binding.sliderView.progress = level + 1
        this.level = binding.sliderView.progress
    }

    private fun init(context: Context) {
        View.inflate(context, R.layout.view_repeat_limit, this)
        orientation = HORIZONTAL
        binding = RepeatLimitViewBinding(this)

        binding.hintIcon.setOnLongClickListener {
            Toast.makeText(context, context.getString(R.string.repeat_limit), Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }
        TooltipCompat.setTooltipText(binding.hintIcon, context.getString(R.string.repeat_limit))
        binding.sliderView.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                level = progress
                onLevelUpdateListener?.invoke(level)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })
    }
}
