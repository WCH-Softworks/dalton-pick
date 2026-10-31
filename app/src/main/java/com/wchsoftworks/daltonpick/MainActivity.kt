package com.wchsoftworks.daltonpick

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.wchsoftworks.daltonpick.colorengine.ColorLocalization
import com.wchsoftworks.daltonpick.colorengine.ColorNamer
import com.wchsoftworks.daltonpick.colorengine.Lang
import com.wchsoftworks.daltonpick.colorengine.Srgb

/**
 * M0 test screen: permissions, a start button per capture backend, capture options, and
 * known color swatches to compare against what the lens reads (spikes S1–S4).
 */
class MainActivity : Activity() {
    private lateinit var prefs: SpikePrefs
    private lateinit var overlayStatus: TextView
    private lateinit var a11yStatus: TextView
    private lateinit var lastCapture: TextView
    private val density get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = SpikePrefs(this)
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)

        val pad = (16 * density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        column.addView(TextView(this).apply {
            text = getString(R.string.spike_title)
            textSize = 22f
        })

        overlayStatus = TextView(this).also { column.addView(it) }
        a11yStatus = TextView(this).also { column.addView(it) }

        column.addView(button(R.string.btn_grant_overlay) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        column.addView(button(R.string.btn_open_a11y) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        column.addView(button(R.string.btn_start_a11y) {
            val service = DaltonAccessibilityService.instance
            if (service == null) toast(R.string.a11y_not_enabled) else service.showOverlay()
        })
        column.addView(button(R.string.btn_start_projection) {
            if (!Settings.canDrawOverlays(this)) return@button toast(R.string.overlay_not_granted)
            val mpm = getSystemService(MediaProjectionManager::class.java)
            @Suppress("DEPRECATION")
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_PROJECTION)
        })
        column.addView(button(R.string.btn_stop) {
            DaltonAccessibilityService.instance?.hideOverlay()
            ProjectionOverlayService.stop(this)
        })

        column.addView(CheckBox(this).apply {
            text = getString(R.string.opt_hide_before_capture)
            isChecked = prefs.hideBeforeCapture
            setOnCheckedChangeListener { _, checked -> prefs.hideBeforeCapture = checked }
        })
        column.addView(CheckBox(this).apply {
            text = getString(R.string.opt_tab_right)
            isChecked = prefs.tabOnRight
            setOnCheckedChangeListener { _, checked -> prefs.tabOnRight = checked }
        })
        column.addView(Button(this).apply {
            fun refreshText() { text = getString(R.string.opt_extra_delay, prefs.extraDelayMs) }
            refreshText()
            setOnClickListener {
                val steps = SpikePrefs.EXTRA_DELAY_STEPS
                prefs.extraDelayMs = steps[(steps.indexOf(prefs.extraDelayMs) + 1) % steps.size]
                refreshText()
            }
        })

        lastCapture = TextView(this).apply { setPadding(0, pad / 2, 0, pad / 2) }.also { column.addView(it) }

        column.addView(TextView(this).apply {
            text = getString(R.string.swatches_title)
            setPadding(0, pad, 0, pad / 2)
        })
        addSwatches(column)

        setContentView(ScrollView(this).apply { addView(column) })
    }

    override fun onResume() {
        super.onResume()
        val on = getString(R.string.status_on)
        val off = getString(R.string.status_off)
        overlayStatus.text = getString(R.string.status_overlay, if (Settings.canDrawOverlays(this)) on else off)
        a11yStatus.text = getString(R.string.status_a11y, if (DaltonAccessibilityService.instance != null) on else off)
        lastCapture.text = getString(R.string.last_capture, prefs.lastCapture ?: getString(R.string.no_capture_yet))
    }

    @Deprecated("Platform Activity result API; the real app moves to AndroidX in M4.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PROJECTION && resultCode == RESULT_OK && data != null) {
            ProjectionOverlayService.start(this, resultCode, data)
        }
    }

    private fun addSwatches(column: LinearLayout) {
        val namer = ColorNamer()
        val lang = Lang.fromTag(resources.configuration.locales[0].toLanguageTag())
        SWATCHES.toList().chunked(3).forEach { rowColors ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (rgb in rowColors) {
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding((4 * density).toInt(), (4 * density).toInt(), (4 * density).toInt(), (8 * density).toInt())
                }
                cell.addView(View(this).apply { setBackgroundColor(0xFF000000.toInt() or rgb) }, LinearLayout.LayoutParams(MATCH_PARENT, (72 * density).toInt()))
                val label = ColorLocalization.label(namer.match(rgb), lang)
                cell.addView(TextView(this).apply {
                    text = "${label.name}\n${Srgb.toHex(rgb)}"
                    textSize = 12f
                    gravity = Gravity.CENTER_HORIZONTAL
                })
                row.addView(cell, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            }
            column.addView(row)
        }
        // A red-to-green gradient for checking that the label doesn't flicker (hysteresis).
        column.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt()))
        }, LinearLayout.LayoutParams(MATCH_PARENT, (72 * density).toInt()))
    }

    private fun button(textRes: Int, onClick: () -> Unit) = Button(this).apply {
        text = getString(textRes)
        setOnClickListener { onClick() }
    }

    private fun toast(textRes: Int) = Toast.makeText(this, textRes, Toast.LENGTH_SHORT).show()

    companion object {
        private const val REQUEST_PROJECTION = 1

        private val SWATCHES = intArrayOf(
            0xFF0000, 0x00FF00, 0x0000FF,
            0xFFFF00, 0x00FFFF, 0xFF00FF,
            0x000000, 0x808080, 0xFFFFFF,
            0x000080, 0x808000, 0xD8A1A4,
            0x228B22, 0x964B00, 0xFFC0CB,
            // Material colors that colorblind users commonly confuse: red/green, blue/purple, brown/green.
            0xF44336, 0x4CAF50, 0x795548,
            0x2196F3, 0x9C27B0, 0x8BC34A,
        )
    }
}
