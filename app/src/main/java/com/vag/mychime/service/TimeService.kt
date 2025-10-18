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
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.vag.mychime.R
import com.vag.mychime.activity.MainActivity
import com.vag.mychime.preferences.TimePickerPreference
import io.github.charmicat.vaghelper.HelperFunctions
import java.util.Calendar

class TimeService : Service() {
    private val binder = MyBinder()

    private val tag = "TimeService"

    private var bindCount = 0

    private var minutesTimer: CountDownTimer? = null
    private var tts: TextToSpeech? = null
    private var chime = false
    private var speak = false
    private var vibration = false
    private var hasSpoken = false
    private var clockType: String? = null
    private var scheduleIni: Calendar? = null
    private var scheduleEnd: Calendar? = null
    private var now: Calendar = Calendar.getInstance()
    private var currentTimeText: String = ""
    private var amPm: String = ""

    override fun onBind(intent: Intent): IBinder {
        bindCount++
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

        val channelId = "com.vag.mychime"
        val channelName = "MyChime Time Service"

        val channel = NotificationChannelCompat.Builder(channelId, NotificationManagerCompat.IMPORTANCE_NONE)
            .setName(channelName)
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)

        val notificationId = 42066

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notify_service)
            .setContentTitle("MyChime")
            .setContentText("Service started")
            .setContentIntent(pendingIntent)
            .build()

        startForeground(notificationId, notification)
    }

    private fun checkTime() {
        now = Calendar.getInstance()

        var currentMinute = now.get(Calendar.MINUTE)
        var currentHour = now.get(Calendar.HOUR)
        amPm = if (now.get(Calendar.AM_PM) == 0) "A " else "P "

        if (currentMinute == 0 && !hasSpoken) {
            if (!hasSpoken) {
                hasSpoken = true

                Log.d(tag, "Time to chime!")

                getSettings()

                if (chime) {
                    HelperFunctions.playAudio(baseContext, R.raw.casiochime)
                }

                if (speak) {
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
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
                    vibration()
                }
            }
        } else {
            hasSpoken = false
        }
    }

    private fun getSettings() {
        Log.d(tag, "getSettings")
        val prefs = PreferenceManager.getDefaultSharedPreferences(application)

        val enabledSpeak = prefs.getBoolean("enableSpeak", false)
        val speakOnValue = prefs.getString("speakOn", "unset") ?: "unset"

        Log.i(tag, "speak=$enabledSpeak mode=$speakOnValue")

        if (enabledSpeak && speakOnValue != "unset") {
            clockType = prefs.getString("clockType", "12-hours")
            speak = true

            speak = when (speakOnValue) {
                "speakHeadsetOn" -> isHeadsetPlugged(baseContext)
                "speakTimeRange" -> {
                    val iniTimeSpeak = prefs.getString("speakStartTime", "00:00") ?: "00:00"
                    val endTimeSpeak = prefs.getString("speakEndTime", "00:00") ?: "00:00"
                    Log.i(tag, "$iniTimeSpeak  $endTimeSpeak")
                    scheduleIni = Calendar.getInstance().apply {
                        set(Calendar.HOUR, TimePickerPreference.getHour(iniTimeSpeak))
                        set(Calendar.MINUTE, TimePickerPreference.getMinute(iniTimeSpeak))
                    }
                    scheduleEnd = Calendar.getInstance().apply {
                        set(Calendar.HOUR, TimePickerPreference.getHour(endTimeSpeak))
                        set(Calendar.MINUTE, TimePickerPreference.getMinute(endTimeSpeak))
                    }
                    isScheduledTime(scheduleIni, scheduleEnd)
                }

                else -> true
            }
        } else {
            speak = false
        }

        val enabledChime = prefs.getBoolean("enableChime", false)
        val chimeOnValue = prefs.getString("chimeOn", "unset") ?: "unset"

        Log.i(tag, "chime=$enabledChime mode=$chimeOnValue")

        if (enabledChime && chimeOnValue != "unset") {
            chime = when (chimeOnValue) {
                "chimeHeadsetOn" -> isHeadsetPlugged(baseContext)
                "chimeTimeRange" -> {
                    val iniTimeChime = prefs.getString("chimeStartTime", "00:00") ?: "00:00"
                    val endTimeChime = prefs.getString("chimeEndTime", "00:00") ?: "00:00"
                    Log.i(tag, "$iniTimeChime  $endTimeChime")

                    scheduleIni = Calendar.getInstance().apply {
                        set(Calendar.HOUR, TimePickerPreference.getHour(iniTimeChime))
                        set(Calendar.MINUTE, TimePickerPreference.getMinute(iniTimeChime))
                    }

                    scheduleEnd = Calendar.getInstance().apply {
                        set(Calendar.HOUR, TimePickerPreference.getHour(endTimeChime))
                        set(Calendar.MINUTE, TimePickerPreference.getMinute(endTimeChime))
                    }

                    isScheduledTime(scheduleIni, scheduleEnd)
                }

                else -> true
            }
        } else {
            chime = false
        }

        val enabledVibration = prefs.getBoolean("enableVibration", false)
        val vibrationOnValue = prefs.getString("vibrationOn", "unset") ?: "unset"

        Log.i(tag, "vibration=$enabledVibration mode=$vibrationOnValue")

        if (enabledVibration && vibrationOnValue != "unset") {
            vibration = when (vibrationOnValue) {
                "vibrationHeadsetOn" -> isHeadsetPlugged(baseContext)
                "vibrationTimeRange" -> {
                    val iniTimeVibration = prefs.getString("vibrationStartTime", "00:00") ?: "00:00"
                    val endTimeVibration = prefs.getString("vibrationEndTime", "00:00") ?: "00:00"
                    Log.i(tag, "$iniTimeVibration  $endTimeVibration")

                    scheduleIni = Calendar.getInstance().apply {
                        set(Calendar.HOUR, TimePickerPreference.getHour(iniTimeVibration))
                        set(Calendar.MINUTE, TimePickerPreference.getMinute(iniTimeVibration))
                    }

                    scheduleEnd = Calendar.getInstance().apply {
                        set(Calendar.HOUR, TimePickerPreference.getHour(endTimeVibration))
                        set(Calendar.MINUTE, TimePickerPreference.getMinute(endTimeVibration))
                    }

                    isScheduledTime(scheduleIni, scheduleEnd)
                }

                else -> true
            }
        } else {
            vibration = false
        }
    }

    private fun isScheduledTime(ini: Calendar?, end: Calendar?): Boolean {
        val iniTime = ini?.time ?: return false
        val endTime = end?.time ?: return false
        val current = now.time
        return iniTime <= current && endTime >= current
    }

    private fun startTts() {
        if (tts == null) {
            tts = TextToSpeech(this, ttsListener, "com.svox.pico")
        } else {
            ttsListener.onInit(TextToSpeech.SUCCESS)
        }
    }

    private fun stopTts() {
        tts?.shutdown()
        tts = null
    }

    override fun onDestroy() {
        super.onDestroy()

        val prefs = PreferenceManager.getDefaultSharedPreferences(application)
        val isSpeakTimeOn = prefs.getBoolean("enableSpeak", false)
        val isChimeOn = prefs.getBoolean("enableChime", false)

        val shouldRestart = isSpeakTimeOn || isChimeOn

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

        val current = resources.configuration.locale
        Log.i(tag, "Current locale ${current.displayName}")

        val languageResult = tts?.setLanguage(current)
        Log.d(tag, "Language set result: $languageResult")

        val speakResult = tts?.speak(currentTimeText, TextToSpeech.QUEUE_ADD, null, "")
        if (speakResult != TextToSpeech.SUCCESS) {
            Log.e(tag, "TTS queueing failed. Trying again")
            tts?.speak(currentTimeText, TextToSpeech.QUEUE_ADD, null, "")
        }

        if (clockType == "12-hours") {
            tts?.speak(amPm, TextToSpeech.QUEUE_ADD, null, "mychime")
            tts?.speak("M", TextToSpeech.QUEUE_ADD, null, "mychime")
        }
    }

    inner class MyBinder : Binder() {
        val service: TimeService
            get() = this@TimeService
    }

    private fun isHeadsetPlugged(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

        return devices.any { device ->
            when (device.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true

                else -> false
            }
        }
    }

    private fun vibration() {
        val pattern = longArrayOf(0, 500, 100, 500, 100, 500, 100)
        Log.d(tag, "vibrating")
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, -1)
            }
        }
    }

}
