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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.wchsoftworks.daltonpick.colorengine.ColorLocalization
import com.wchsoftworks.daltonpick.colorengine.ColorNamer
import com.wchsoftworks.daltonpick.colorengine.Lang
import com.wchsoftworks.daltonpick.colorengine.Srgb
import com.wchsoftworks.daltonpick.overlay.OverlayRuntime
import com.wchsoftworks.daltonpick.settings.Edge
import com.wchsoftworks.daltonpick.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.wchsoftworks.daltonpick.settings.Settings as AppSettings

/**
 * Temporary screen until the real app (M4): permissions, the fallback capture backend, the
 * settings, and known color swatches to compare against what the lens reads.
 */
class MainActivity : Activity() {
    private lateinit var repo: SettingsRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var overlayStatus: TextView
    private lateinit var a11yStatus: TextView
    private val density get() = resources.displayMetrics.density

    /** Pushes the stored settings into the widgets without triggering their change listeners. */
    private val syncers = mutableListOf<(AppSettings) -> Unit>()
    private var syncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = SettingsRepository.get(this)
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)

        val pad = (16 * density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        column.addView(TextView(this).apply {
            text = getString(R.string.main_title)
            textSize = 22f
        })

        overlayStatus = TextView(this).also { column.addView(it) }
        a11yStatus = TextView(this).also { column.addView(it) }

        column.addView(button(R.string.btn_open_a11y) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        column.addView(button(R.string.btn_grant_overlay) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        column.addView(button(R.string.btn_start_projection) {
            if (DaltonAccessibilityService.instance != null) return@button toast(R.string.accessibility_active)
            if (!Settings.canDrawOverlays(this)) return@button toast(R.string.overlay_not_granted)
            val mpm = getSystemService(MediaProjectionManager::class.java)
            @Suppress("DEPRECATION")
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_PROJECTION)
        })
        column.addView(button(R.string.btn_stop) { ProjectionOverlayService.stop(this) })

        addSettings(column, pad)

        column.addView(TextView(this).apply {
            text = getString(R.string.swatches_title)
            setPadding(0, pad, 0, pad / 2)
        })
        addSwatches(column)

        setContentView(ScrollView(this).apply { addView(column) })

        scope.launch {
            repo.flow.collect { s ->
                syncing = true
                syncers.forEach { it(s) }
                syncing = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val on = getString(R.string.status_on)
        val off = getString(R.string.status_off)
        overlayStatus.text = getString(R.string.status_overlay, if (Settings.canDrawOverlays(this)) on else off)
        a11yStatus.text = getString(R.string.status_a11y, if (DaltonAccessibilityService.instance != null) on else off)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    @Deprecated("Platform Activity result API; the real app moves to AndroidX in M4.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PROJECTION && resultCode == RESULT_OK && data != null) {
            ProjectionOverlayService.start(this, resultCode, data)
        }
    }

    // --- Settings (the full settings screen comes with M4)

    private fun update(transform: (AppSettings) -> AppSettings) {
        if (!syncing) scope.launch { repo.update(transform) }
    }

    private fun addSettings(column: LinearLayout, pad: Int) {
        column.addView(TextView(this).apply {
            text = getString(R.string.settings_title)
            textSize = 18f
            setPadding(0, pad, 0, 0)
        })
        column.addView(checkBox(R.string.setting_tab_visible, { it.tabVisible }) { s, v -> s.copy(tabVisible = v) })
        column.addView(checkBox(R.string.setting_edge_right, { it.edge == Edge.RIGHT }) { s, v ->
            s.copy(edge = if (v) Edge.RIGHT else Edge.LEFT)
        })
        column.addView(checkBox(R.string.setting_auto_dim, { it.autoDim }) { s, v -> s.copy(autoDim = v) })
        column.addView(slider(
            R.string.setting_lens_size, AppSettings.LENS_SIZE_RANGE_DP.first, AppSettings.LENS_SIZE_RANGE_DP.last,
            { it.lensSizeDp }, { s, v -> s.copy(lensSizeDp = v) },
        ))
        column.addView(slider(
            R.string.setting_zoom, AppSettings.ZOOM_RANGE.first, AppSettings.ZOOM_RANGE.last,
            { it.zoom }, { s, v -> s.copy(zoom = v) },
        ))
        column.addView(Button(this).apply {
            var current = AppSettings.DEFAULT_SAMPLE_SIZE
            syncers += {
                current = it.sampleSize
                text = getString(R.string.setting_sample_size, it.sampleSize)
            }
            setOnClickListener {
                val sizes = AppSettings.SAMPLE_SIZES
                val next = sizes[(sizes.indexOf(current) + 1) % sizes.size]
                update { s -> s.copy(sampleSize = next) }
            }
        })
        column.addView(checkBox(R.string.setting_show_hex, { it.showHex }) { s, v -> s.copy(showHex = v) })
        column.addView(checkBox(R.string.setting_haptics, { it.haptics }) { s, v -> s.copy(haptics = v) })
        column.addView(TextView(this).apply {
            text = getString(R.string.setting_developer)
            setPadding(0, pad / 2, 0, 0)
        })
        column.addView(checkBox(R.string.setting_show_debug, { it.showDebug }) { s, v -> s.copy(showDebug = v) })
    }

    private fun checkBox(
        textRes: Int,
        read: (AppSettings) -> Boolean,
        write: (AppSettings, Boolean) -> AppSettings,
    ) = CheckBox(this).apply {
        text = getString(textRes)
        syncers += { isChecked = read(it) }
        setOnCheckedChangeListener { _, checked -> update { write(it, checked) } }
    }

    private fun slider(
        labelRes: Int,
        min: Int,
        max: Int,
        read: (AppSettings) -> Int,
        write: (AppSettings, Int) -> AppSettings,
    ) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val label = TextView(this@MainActivity)
        val bar = SeekBar(this@MainActivity).apply { this.max = max - min }
        syncers += {
            label.text = getString(labelRes, read(it))
            bar.progress = read(it) - min
        }
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                label.text = getString(labelRes, progress + min)
                // Writes while dragging are cheap and let the open lens resize live.
                update { write(it, progress + min) }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
        addView(label)
        addView(bar)
    }

    // --- Test swatches

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
