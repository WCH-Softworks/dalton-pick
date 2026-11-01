package com.wchsoftworks.daltonpick.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import com.wchsoftworks.daltonpick.R
import kotlin.math.abs

/**
 * The idle "hidden away" tab docked to a screen edge (spec §4.1).
 * Drag along the edge to move it, pull it inward to bring out the lens, tap it to open or close
 * the lens, and hold then drag to carry it to the other edge.
 */
@SuppressLint("ViewConstructor")
class EdgeTabView(context: Context, private val controller: OverlayController) : View(context) {
    var onRightEdge = true
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val pullThreshold = 24 * density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt() }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = 0xCC202020.toInt()
    }
    private val pill = RectF()

    private enum class Mode { NONE, MOVE, PULL, CARRY }

    private var mode = Mode.NONE
    private var downX = 0f
    private var downY = 0f
    private var startTabY = 0

    private val startCarry = Runnable {
        if (mode == Mode.NONE) {
            mode = Mode.CARRY
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            controller.beginCarry()
        }
    }

    init {
        contentDescription = context.getString(R.string.tab_description)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // With gesture navigation, edge swipes are the system back gesture. Opt the tab out of it
        // (Android allows up to 200 dp of exclusion per edge; the tab is 56 dp tall).
        systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
    }

    override fun onDraw(canvas: Canvas) {
        val w = VISUAL_WIDTH_DP * density
        val left = if (onRightEdge) width - w else 0f
        pill.set(left, 0f, left + w, height.toFloat())
        val r = w / 2
        canvas.drawRoundRect(pill, r, r, fill)
        canvas.drawRoundRect(pill, r, r, outline)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mode = Mode.NONE
                downX = event.rawX
                downY = event.rawY
                startTabY = controller.tabY
                controller.onTabTouched()
                postDelayed(startCarry, ViewConfiguration.getLongPressTimeout().toLong())
            }

            MotionEvent.ACTION_MOVE -> {
                val inward = if (onRightEdge) downX - event.rawX else event.rawX - downX
                val vertical = event.rawY - downY
                if (mode == Mode.NONE) {
                    if (inward > pullThreshold) {
                        removeCallbacks(startCarry)
                        mode = Mode.PULL
                        controller.startPull(event.rawX, event.rawY)
                    } else if (abs(vertical) > touchSlop && abs(vertical) > inward) {
                        removeCallbacks(startCarry)
                        mode = Mode.MOVE
                    }
                }
                when (mode) {
                    Mode.MOVE -> controller.moveTabTo(startTabY + vertical.toInt())
                    Mode.PULL -> controller.dragPull(event.rawX, event.rawY)
                    Mode.CARRY -> controller.carryTabTo(event.rawX, event.rawY)
                    Mode.NONE -> Unit
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(startCarry)
                when (mode) {
                    Mode.PULL -> controller.endPull()
                    Mode.MOVE -> controller.saveTabPosition()
                    Mode.CARRY -> controller.endCarry(event.rawX, event.rawY)
                    Mode.NONE -> if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                }
                mode = Mode.NONE
                controller.onTabReleased()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        controller.tapTab()
        return true
    }

    // TalkBack users can't drag the tab: a double-tap opens the lens at screen centre, and a custom
    // action switches the edge.
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(ACTION_SWITCH_EDGE, context.getString(R.string.tab_action_switch_edge)))
    }

    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        if (action == ACTION_SWITCH_EDGE) {
            controller.switchEdge()
            return true
        }
        return super.performAccessibilityAction(action, arguments)
    }

    companion object {
        const val VISUAL_WIDTH_DP = 8
        const val TOUCH_WIDTH_DP = 28
        const val HEIGHT_DP = 56
        private val ACTION_SWITCH_EDGE = R.id.action_switch_edge
    }
}
