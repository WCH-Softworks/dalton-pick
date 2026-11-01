package com.wchsoftworks.daltonpick

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import com.wchsoftworks.daltonpick.capture.ProjectionCapturer
import com.wchsoftworks.daltonpick.overlay.OverlayRuntime

/**
 * Fallback host: a foreground service holding the MediaProjection session and a regular
 * TYPE_APPLICATION_OVERLAY window. Android 14+ needs fresh user consent for every session.
 * Runs the same [OverlayRuntime] as the accessibility host, behind the same capture interface.
 */
class ProjectionOverlayService : Service() {
    private var runtime: OverlayRuntime? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (runtime != null) return START_NOT_STICKY

        // Must be in the foreground with the mediaProjection type before getMediaProjection().
        startForeground(
            OverlayNotification.ID,
            OverlayNotification.build(this, tabVisible = true, withStopAction = true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        val projection = data?.let {
            getSystemService(MediaProjectionManager::class.java).getMediaProjection(resultCode, it)
        }
        if (projection == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Android 14 requires a callback before the virtual display is created.
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() = stopSelf()
        }, Handler(Looper.getMainLooper()))

        runtime = OverlayRuntime(
            this,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            ProjectionCapturer(this, projection),
            withStopAction = true,
        ).also { it.start() }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        runtime?.onScreenChanged()
    }

    override fun onDestroy() {
        runtime?.stop()
        runtime = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.wchsoftworks.daltonpick.STOP"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"

        fun start(context: Context, resultCode: Int, data: Intent) {
            context.startForegroundService(
                Intent(context, ProjectionOverlayService::class.java)
                    .putExtra(EXTRA_RESULT_CODE, resultCode)
                    .putExtra(EXTRA_RESULT_DATA, data),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ProjectionOverlayService::class.java))
        }
    }
}
