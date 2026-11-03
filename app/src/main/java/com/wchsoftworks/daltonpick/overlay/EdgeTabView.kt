package com.wchsoftworks.daltonpick.overlay

import android.animation.ValueAnimator
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
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import com.wchsoftworks.daltonpick.R
import com.wchsoftworks.daltonpick.overlay.TabGesture.Mode
import com.wchsoftworks.daltonpick.overlay.TabGesture.Release

/**
 * The idle "hidden away" tab docked to a screen edge (spec §4.1).
 * Drag along the edge to move it, pull it inward to bring out the lens, tap it to put an open lens
 * away, and hold then drag to carry it to the other edge. [TabGesture] decides which is which.
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
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val accessibility = context.getSystemService(AccessibilityManager::class.java)

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt() }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
        color = 0xCC202020.toInt()
    }
    private val grip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCC202020.toInt() }
    private val pill = RectF()

    private var mode = Mode.NONE
    private var downX = 0f
    private var downY = 0f
    private var startTabY = 0

    /** 0..1: how far the pill is nudged inward by [hintPull]. */
    private var nudge = 0f
    private var nudgeAnimator: ValueAnimator? = null

    private val startCarry = Runnable {
        if (mode == Mode.NONE) {
            mode = Mode.CARRY
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            controller.beginCarry()
        }
    }

    init {
        contentDescription = context.getString(R.string.tab_description)
        // So TalkBack's double-tap arrives as ACTION_CLICK (performClick) rather than a made-up touch.
        isClickable = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // With gesture navigation, edge swipes are the system back gesture. Opt the tab out of it
        // (Android allows up to 200 dp of exclusion per edge; the tab is 56 dp tall).
        systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
    }

    override fun onDraw(canvas: Canvas) {
        val w = VISUAL_WIDTH_DP * density
        val inset = nudge * NUDGE_DP * density
        val left = if (onRightEdge) width - w - inset else inset
        pill.set(left, 0f, left + w, height.toFloat())
        val r = w / 2
        canvas.drawRoundRect(pill, r, r, fill)
        canvas.drawRoundRect(pill, r, r, outline)
        // Three grip dots, so the pill reads as a handle and not as a scrollbar.
        val dot = GRIP_DOT_DP * density
        val gap = GRIP_GAP_DP * density
        for (i in -1..1) canvas.drawCircle(pill.centerX(), pill.centerY() + i * gap, dot, grip)
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
                val before = mode
                mode = TabGesture.classify(mode, inward, vertical, touchSlop, pullThreshold)
                if (mode != Mode.NONE) removeCallbacks(startCarry)
                if (mode == Mode.PULL && before != Mode.PULL) controller.startPull(event.rawX, event.rawY)
                when (mode) {
                    Mode.MOVE -> controller.moveTabTo(startTabY + vertical.toInt())
                    Mode.PULL -> controller.dragPull(event.rawX, event.rawY)
                    Mode.CARRY -> controller.carryTabTo(event.rawX, event.rawY)
                    Mode.NONE, Mode.UNDECIDED -> Unit
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(startCarry)
                val release = TabGesture.release(
                    mode,
                    cancelled = event.actionMasked == MotionEvent.ACTION_CANCEL,
                    lensOpen = controller.lensOpen,
                    touchExploration = accessibility.isTouchExplorationEnabled,
                )
                when (release) {
                    Release.END_PULL -> controller.endPull()
                    Release.SAVE_POSITION -> controller.saveTabPosition()
                    Release.END_CARRY -> controller.endCarry(event.rawX, event.rawY)
                    Release.CLOSE_LENS, Release.OPEN_LENS -> performClick()
                    Release.HINT_PULL -> hintPull()
                    Release.NOTHING -> Unit
                }
                mode = Mode.NONE
                controller.onTabReleased()
            }
        }
        return true
    }

    /** Tap and TalkBack's double-tap: open or put away the lens. */
    override fun performClick(): Boolean {
        super.performClick()
        controller.tapTab()
        return true
    }

    /** A tap doesn't open the lens: nudge the pill inward and back, the way it is meant to be pulled. */
    private fun hintPull() {
        controller.onTapHinted()
        if (!ValueAnimator.areAnimatorsEnabled()) return
        nudgeAnimator?.cancel()
        nudgeAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = NUDGE_MS
            addUpdateListener {
                nudge = it.animatedValue as Float
                invalidate()
            }
            start()
        }
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
        const val VISUAL_WIDTH_DP = 10
        const val TOUCH_WIDTH_DP = 28
        const val HEIGHT_DP = 56

        /** How far a tap nudges the pill inward (it fits inside the 28 dp touch width). */
        private const val NUDGE_DP = 10
        private const val NUDGE_MS = 420L
        private const val GRIP_DOT_DP = 1.25f
        private const val GRIP_GAP_DP = 6f
        private val ACTION_SWITCH_EDGE = R.id.action_switch_edge
    }
}
