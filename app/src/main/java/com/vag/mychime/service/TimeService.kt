package com.vag.mychime.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vag.mychime.R
import com.vag.mychime.activity.MainActivity
import com.vag.mychime.preferences.TimePickerPreference
import io.github.charmicat.vaghelper.HelperFunctions
import java.util.Calendar
import java.util.Locale

class TimeService : Service() {

    private val binder = MyBinder()
    private val tag = "TimeService"

    private var isOn = false
    private var bindCount = 0

    private var minutesTimer: CountDownTimer? = null
    private var tts: TextToSpeech? = null
    private var chime = false
    private var speak = false
    private var vibration = false
    private var hasSpoken = false
    private var clockType: String = "12-hours"
    private var currentTimeText: String = ""
    private var amPm: String = ""

    override fun onBind(intent: Intent): IBinder {
        bindCount += 1
        Log.i(tag, "Got bound ($bindCount)")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(tag, "Service started. Received start id $startId: $intent flags: $flags")

        startNotification()

        hasSpoken = false
        checkTime()

        minutesTimer = object : CountDownTimer(30000, 30000) {
            override fun onTick(millisUntilFinished: Long) {
                // no-op
            }

            override fun onFinish() {
                checkTime()
                start()
            }
        }.also { it.start() }

        return START_STICKY
    }

    private fun startNotification() {
        Log.d(tag, "Starting notification")

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notificationChannelId = "com.vag.mychime"
        val channelName = "MyChime Time Service"

        val channel = NotificationChannelCompat.Builder(
            notificationChannelId,
            NotificationManagerCompat.IMPORTANCE_NONE
        ).setName(channelName).build()

        NotificationManagerCompat.from(this).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_stat_notify_service)
            .setContentTitle("MyChime")
            .setContentText("Service started")
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(42066, notification)
    }

    private fun checkTime() {
        val now = Calendar.getInstance()
        val currentMinute = now.get(Calendar.MINUTE)
        var currentHour = now.get(Calendar.HOUR)
        amPm = if (now.get(Calendar.AM_PM) == 0) "A " else "P "

        if (currentMinute == 0 && !hasSpoken) {
            hasSpoken = true
            Log.d(tag, "Time to chime!")

            getSettings(now)

            if (chime) {
                HelperFunctions.playAudio(baseContext, R.raw.casiochime)
            }

            if (speak) {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    Log.e(tag, "Sleep interrupted", e)
                    Thread.currentThread().interrupt()
                }

                currentTimeText = getString(R.string.speakTimeText_ini)

                if (clockType == "24-hours") {
                    currentHour = now.get(Calendar.HOUR_OF_DAY)
                }

                currentTimeText += if (currentHour == 0) 12 else currentHour

                startTts()
            }

            if (vibration) {
                vibrate()
            }
        } else if (currentMinute != 0) {
            hasSpoken = false
        }
    }

    private fun getSettings(now: Calendar) {
        val settings = PreferenceManager.getDefaultSharedPreferences(application)

        val speakOnValue = settings.getString("speakOn", "unset") ?: "unset"
        speak = if (settings.getBoolean("enableSpeak", false) && speakOnValue != "unset") {
            clockType = settings.getString("clockType", "12-hours") ?: "12-hours"
            when (speakOnValue) {
                "speakHeadsetOn" -> isHeadsetPlugged(baseContext)
                "speakTimeRange" -> {
                    val start = settings.getString("speakStartTime", "00:00") ?: "00:00"
                    val end = settings.getString("speakEndTime", "00:00") ?: "00:00"

                    isScheduledTime(now, start, end)
                }

                else -> true
            }
        } else {
            false
        }

        val chimeOnValue = settings.getString("chimeOn", "unset") ?: "unset"
        chime = if (settings.getBoolean("enableChime", false) && chimeOnValue != "unset") {
            when (chimeOnValue) {
                "chimeHeadsetOn" -> isHeadsetPlugged(baseContext)
                "chimeTimeRange" -> {
                    val start = settings.getString("chimeStartTime", "00:00") ?: "00:00"
                    val end = settings.getString("chimeEndTime", "00:00") ?: "00:00"
                    isScheduledTime(now, start, end)
                }

                else -> true
            }
        } else {
            false
        }

        val vibrationOnValue = settings.getString("vibrationOn", "unset") ?: "unset"
        vibration = if (settings.getBoolean("enableVibration", false) && vibrationOnValue != "unset") {
            when (vibrationOnValue) {
                "vibrationHeadsetOn" -> isHeadsetPlugged(baseContext)
                "vibrationTimeRange" -> {
                    val start = settings.getString("vibrationStartTime", "00:00") ?: "00:00"
                    val end = settings.getString("vibrationEndTime", "00:00") ?: "00:00"
                    isScheduledTime(now, start, end)
                }

                else -> true
            }
        } else {
            false
        }
    }

    private fun isScheduledTime(now: Calendar, start: String, end: String): Boolean {
        val startTime = Calendar.getInstance().apply {
            time = now.time
            set(Calendar.HOUR, TimePickerPreference.getHour(start))
            set(Calendar.MINUTE, TimePickerPreference.getMinute(start))
        }

        val endTime = Calendar.getInstance().apply {
            time = now.time
            set(Calendar.HOUR, TimePickerPreference.getHour(end))
            set(Calendar.MINUTE, TimePickerPreference.getMinute(end))
        }

        val current = now.time
        return startTime.time <= current && endTime.time >= current
    }

    private fun startTts() {
        tts = TextToSpeech(this, ttsListener, "com.svox.pico")
    }

    private fun stopTts() {
        tts?.shutdown()
        tts = null
    }

    override fun onDestroy() {
        super.onDestroy()

        val settings = PreferenceManager.getDefaultSharedPreferences(application)
        val shouldRestart = settings.getBoolean("enableSpeak", false) ||
                settings.getBoolean("enableChime", false)

        if (shouldRestart) {
            sendBroadcast(Intent("RestartTimeService"))
        } else {
            Log.d(tag, "Service destroyed")
            minutesTimer?.cancel()
            stopTts()
            stopForeground(true)
        }
    }

    private val ttsListener = TextToSpeech.OnInitListener { status ->
        Log.d(tag, "TTS engine started")
        isOn = status == TextToSpeech.SUCCESS

        val currentLocale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }

        Log.i(tag, "Current locale ${currentLocale.displayName}")

        tts?.language = currentLocale

        tts?.let { engine ->
            if (engine.speak(currentTimeText, TextToSpeech.QUEUE_ADD, null, "") != TextToSpeech.SUCCESS) {
                Log.e(tag, "TTS queueing failed. Trying again")
                engine.speak(currentTimeText, TextToSpeech.QUEUE_ADD, null, "")
            }

            if (clockType == "12-hours") {
                engine.speak(amPm, TextToSpeech.QUEUE_ADD, null, "mychime")
                engine.speak("M", TextToSpeech.QUEUE_ADD, null, "mychime")
            }
        }
    }

    inner class MyBinder : Binder() {
        fun getService(): TimeService = this@TimeService
    }

    private fun isHeadsetPlugged(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { device ->
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    }

    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        val pattern = longArrayOf(0, 500, 100, 500, 100, 500, 100)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
