package io.getstream.webrtc.sample.compose

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi


class ScreenCaptureService : Service() {
  @RequiresApi(Build.VERSION_CODES.O)
  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    // Create a pending intent for the notification
    val notificationIntent = Intent(this, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_MUTABLE)

    // Create the notification
    val notification: Notification = Notification.Builder(this, CHANNEL_ID)
      .setContentTitle("Screen Capture Service")
      .setContentText("Capturing screen...")
      .setSmallIcon(R.drawable.ic_notification)
      .setContentIntent(pendingIntent)
      .build()

    // Start the foreground service
    startForeground(NOTIFICATION_ID, notification)
    return START_STICKY
  }

  override fun onBind(intent: Intent): IBinder? {
    return null
  }

  companion object {
    private const val NOTIFICATION_ID = 1
    private const val CHANNEL_ID = "ScreenCaptureChannel"
  }
}