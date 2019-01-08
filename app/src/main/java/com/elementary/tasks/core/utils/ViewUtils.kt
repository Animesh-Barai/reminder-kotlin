package com.elementary.tasks.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.view.*
import android.view.animation.*
import android.widget.ScrollView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.elementary.tasks.R

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
object ViewUtils {

    fun isHorizontal(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun tintOverflowButton(toolbar: Toolbar, isDark: Boolean): Boolean {
        val overflowIcon = toolbar.overflowIcon ?: return false
        val color = if (isDark) {
            ContextCompat.getColor(toolbar.context, R.color.whitePrimary)
        } else {
            ContextCompat.getColor(toolbar.context, R.color.pureBlack)
        }
        val colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY)
        overflowIcon.colorFilter = colorFilter
        return true
    }

    fun backIcon(context: Context, isDark: Boolean): Drawable? {
        return tintIcon(context, R.drawable.ic_twotone_arrow_back_24px, isDark)
    }

    fun createIcon(context: Context, @DrawableRes res: Int, @ColorInt color: Int): Bitmap? {
        var icon = ContextCompat.getDrawable(context, res)
        if (icon != null) {
            if (Module.isLollipop) {
                icon = (DrawableCompat.wrap(icon)).mutate()
                DrawableCompat.setTint(icon, color)
                DrawableCompat.setTintMode(icon, PorterDuff.Mode.SRC_IN)
            }
            if (icon != null) {
                val bitmap = Bitmap.createBitmap(icon.intrinsicWidth, icon.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                icon.setBounds(0, 0, canvas.width, canvas.height)
                icon.draw(canvas)
                return bitmap
            }
        }
        return null
    }

    fun tintIcon(context: Context, @DrawableRes resource: Int, isDark: Boolean): Drawable? {
        var icon = ContextCompat.getDrawable(context, resource)
        if (icon != null) {
            if (Module.isLollipop) {
                icon = (DrawableCompat.wrap(icon)).mutate()
            }
            if (icon == null) return null
            val color = if (isDark) {
                ContextCompat.getColor(context, R.color.whitePrimary)
            } else {
                ContextCompat.getColor(context, R.color.pureBlack)
            }
            DrawableCompat.setTint(icon, color)
            DrawableCompat.setTintMode(icon, PorterDuff.Mode.SRC_IN)
            return icon
        }
        return null
    }

    fun tintMenuIcon(context: Context, menu: Menu?, index: Int, @DrawableRes resource: Int, isDark: Boolean) {
        menu?.getItem(index)?.icon = tintIcon(context, resource, isDark)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun listenScrollableView(scrollView: ScrollView, listener: ((x: Int) -> Unit)?) {
        val onScrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
            listener?.invoke(scrollView.scrollY)
        }
        scrollView.setOnTouchListener(object : View.OnTouchListener {
            private var observer: ViewTreeObserver? = null
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (observer == null) {
                    observer = scrollView.viewTreeObserver
                    observer?.addOnScrollChangedListener(onScrollChangedListener)
                } else if (!observer!!.isAlive) {
                    observer?.removeOnScrollChangedListener(onScrollChangedListener)
                    observer = scrollView.viewTreeObserver
                    observer?.addOnScrollChangedListener(onScrollChangedListener)
                }
                return false
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    fun listenScrollableView(scrollView: NestedScrollView, listener: ((y: Int) -> Unit)?) {
        val onScrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
            listener?.invoke(scrollView.scrollY)
        }
        scrollView.setOnTouchListener(object : View.OnTouchListener {
            private var observer: ViewTreeObserver? = null
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (observer == null) {
                    observer = scrollView.viewTreeObserver
                    observer?.addOnScrollChangedListener(onScrollChangedListener)
                } else if (!observer!!.isAlive) {
                    observer?.removeOnScrollChangedListener(onScrollChangedListener)
                    observer = scrollView.viewTreeObserver
                    observer?.addOnScrollChangedListener(onScrollChangedListener)
                }
                return false
            }
        })
    }

    fun listenScrollableView(recyclerView: RecyclerView, listener: ((y: Int) -> Unit)?) {
        if (Module.isMarshmallow) {
            recyclerView.setOnScrollChangeListener { _, _, _, _, _ ->
                listener?.invoke(if (recyclerView.canScrollVertically(-1)) 1 else 0)
            }
        } else {
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    listener?.invoke(if (recyclerView.canScrollVertically(-1)) 1 else 0)
                }
            })
        }
    }

    fun slideInUp(context: Context, view: View) {
        val animation = AnimationUtils.loadAnimation(context, R.anim.slide_up)
        view.startAnimation(animation)
        view.visibility = View.VISIBLE
    }

    fun slideOutDown(context: Context, view: View) {
        val animation = AnimationUtils.loadAnimation(context, R.anim.slide_down)
        view.startAnimation(animation)
        view.visibility = View.GONE
    }

    fun slideOutUp(context: Context, view: View) {
        val animation = AnimationUtils.loadAnimation(context, R.anim.slide_up_out)
        view.startAnimation(animation)
        view.visibility = View.GONE
    }

    fun slideInDown(context: Context, view: View) {
        val animation = AnimationUtils.loadAnimation(context, R.anim.slide_down_in)
        view.startAnimation(animation)
        view.visibility = View.VISIBLE
    }

    fun fadeInAnimation(view: View) {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.startOffset = 400
        fadeIn.duration = 400
        view.animation = fadeIn
        view.visibility = View.VISIBLE
    }

    fun fadeOutAnimation(view: View) {
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator() //and this
        fadeOut.duration = 400
        view.animation = fadeOut
        view.visibility = View.GONE
    }

    fun showOver(view: View) {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = OvershootInterpolator()
        fadeIn.duration = 300
        view.animation = fadeIn
        view.visibility = View.VISIBLE
    }

    fun hideOver(view: View) {
        val fadeIn = AlphaAnimation(1f, 0f)
        fadeIn.interpolator = OvershootInterpolator()
        fadeIn.duration = 300
        view.animation = fadeIn
        view.visibility = View.GONE
    }

    fun expand(v: View) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val targetHeight = v.measuredHeight
        v.layoutParams.height = 0
        v.visibility = View.VISIBLE
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height = if (interpolatedTime == 1f)
                    ViewGroup.LayoutParams.WRAP_CONTENT
                else
                    (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        // 1dp/ms
        a.duration = (targetHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }

    fun collapse(v: View) {
        val initialHeight = v.measuredHeight
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1f) {
                    v.visibility = View.GONE
                } else {
                    v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        // 1dp/ms
        a.duration = (initialHeight / v.context.resources.displayMetrics.density).toInt().toLong()
        v.startAnimation(a)
    }
}
