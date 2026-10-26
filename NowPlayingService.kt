package com.aurapulse.studio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle

/**
 * Keeps the app alive while music plays and shows the notification / lock-screen player.
 * The audio itself is still played by the HTML page; this service only mirrors its state
 * and sends button presses (play, pause, next, previous, seek) back to the page.
 */
class NowPlayingService : Service() {

    companion object {
        const val CHANNEL_ID = "aurapulse_playback"
        const val NOTIF_ID = 7
        const val ACTION_TOGGLE = "com.aurapulse.studio.TOGGLE"
        const val ACTION_NEXT = "com.aurapulse.studio.NEXT"
        const val ACTION_PREV = "com.aurapulse.studio.PREV"

        @Volatile var instance: NowPlayingService? = null
        @Volatile var title: String = "AuraPulse Studio Pro"
        @Volatile var artist: String = ""
        @Volatile var playing: Boolean = false
        @Volatile var posMs: Long = 0
        @Volatile var durMs: Long = 0
        @Volatile var art: Bitmap? = null
    }

    private lateinit var session: MediaSessionCompat
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Music playback", NotificationManager.IMPORTANCE_LOW)
        channel.description = "AuraPulse Studio player controls"
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        channel.setShowBadge(false)
        nm.createNotificationChannel(channel)

        session = MediaSessionCompat(this, "AuraPulseStudio")
        session.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() { MainActivity.js("playAudio()") }
            override fun onPause() { MainActivity.js("pauseAudio()") }
            override fun onStop() { MainActivity.js("pauseAudio()") }
            override fun onSkipToNext() { MainActivity.js("playNextSong()") }
            override fun onSkipToPrevious() { MainActivity.js("playPreviousSong()") }
            override fun onSeekTo(pos: Long) { MainActivity.js("audioElement.currentTime=" + (pos / 1000.0)) }
        })
        session.isActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> MainActivity.js(if (playing) "pauseAudio()" else "playAudio()")
            ACTION_NEXT -> MainActivity.js("playNextSong()")
            ACTION_PREV -> MainActivity.js("playPreviousSong()")
        }
        applyState()
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        return START_NOT_STICKY
    }

    /** Called by the page (through the JS bridge) whenever the song, position or play state changes. */
    fun refresh() {
        applyState()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun applyState() {
        val meta = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "AuraPulse Studio Pro")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durMs)
        art?.let { meta.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) }
        session.setMetadata(meta.build())

        val actions = PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP
        val st = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(st, posMs, if (playing) 1f else 0f, SystemClock.elapsedRealtime())
                .build()
        )

        // Keep the CPU awake only while music is actually playing.
        if (playing) {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "aurapulse:playback").apply { setReferenceCounted(false) }
            }
            if (wakeLock?.isHeld == false) wakeLock?.acquire(6 * 60 * 60 * 1000L)
        } else if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun servicePi(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code,
            Intent(this, NowPlayingService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(open)
            .setLargeIcon(art)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(playing)
            .addAction(android.R.drawable.ic_media_previous, "Previous", servicePi(ACTION_PREV, 1))
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Pause" else "Play", servicePi(ACTION_TOGGLE, 2)
            )
            .addAction(android.R.drawable.ic_media_next, "Next", servicePi(ACTION_NEXT, 3))
            .setStyle(MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
        return b.build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        instance = null
        if (wakeLock?.isHeld == true) wakeLock?.release()
        session.isActive = false
        session.release()
        super.onDestroy()
    }
}
