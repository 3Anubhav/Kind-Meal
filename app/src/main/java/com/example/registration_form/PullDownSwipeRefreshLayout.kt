package com.example.registration_form

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class PullDownSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private var targetView: View? = null
    private var isPulling = false
    private var startY = 0f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        targetView = getChildAt(0)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            startY = ev.y
            isPulling = false
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (!canChildScrollUp() && !isRefreshing) {
            when (ev.action) {
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.y - startY
                    if (dy > 0) {
                        isPulling = true
                        val shift = (dy / 2f).coerceAtMost(720f)
                        targetView?.translationY = shift

                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isPulling && !isRefreshing) {
                        resetTranslation()
                    }
                    // Always reset state
                    isPulling = false
                    startY = 0f
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun setRefreshing(refreshing: Boolean) {
        super.setRefreshing(refreshing)

        // Reset visual offset after refresh
        if (!refreshing) {
            resetTranslation()
        }
    }

    private fun resetTranslation() {
        targetView?.animate()?.translationY(0f)?.setDuration(300)?.start()
    }
}
