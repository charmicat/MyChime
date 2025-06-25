package com.vag.mychime.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
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
    private val mBinder: IBinder = MyBinder()
    private val sdkVersion = Build.VERSION.SDK_INT
    private val TAG = "TimeService"
    private var isOn = false
    private var bindCount = 0

    private lateinit var settings: SharedPreferences
    private var minutesTimer: CountDownTimer? = null
    private var tts: TextToSpeech? = null
    private var chime = false
    private var speak = false
    private var vibration = false
    private var hasSpoken = false
    private var scheduledSpeak = false
    private var scheduledChime = false
    private var scheduledVibration = false
    private var clockType: String = ""
    private var iniTimeSpeak: String = ""
    private var endTimeSpeak: String = ""
    private var iniTimeChime: String = ""
    private var endTimeChime: String = ""
    private var scheduleIni: Calendar? = null
    private var scheduleEnd: Calendar? = null
    private var now: Calendar? = null
    private var currentTimeText: String = ""
    private var am_pm: String = ""

    override fun onBind(intent: Intent): IBinder {
        bindCount++
        Log.i(TAG, "Got bound ($bindCount)")
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started. Received start id $startId: $intent flags: $flags")
        startNotification()
        hasSpoken = false
        checkTime()
        minutesTimer = object : CountDownTimer(30000, 30000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                checkTime()
                start()
            }
        }
        minutesTimer!!.start()
        return Service.START_STICKY
    }

    private fun startNotification() {
        Log.d(TAG, "Starting notification")
        val i = Intent(this, MainActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE)
        val NOTIFICATION_CHANNEL_ID = "com.vag.mychime"
        val channelName = "MyChime Time Service"
        val chan = NotificationChannelCompat.Builder(
            NOTIFICATION_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_NONE
        )
        chan.setName(channelName)
        NotificationManagerCompat.from(this).createNotificationChannel(chan.build())
        val notif = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notify_service)
            .setContentTitle("MyChime")
            .setContentText("Service started")
        notif.setContentIntent(pi)
        startForeground(42066, notif.build())
    }

    private fun checkTime() {
        now = Calendar.getInstance()
        val currentMinute = now!!.get(Calendar.MINUTE)
        var currentHour = now!!.get(Calendar.HOUR)
        am_pm = if (now!!.get(Calendar.AM_PM) == 0) "A " else "P "
        if (currentMinute == 0 && !hasSpoken) {
            if (!hasSpoken) {
                hasSpoken = true
                Log.d(TAG, "Time to chime!")
                getSettings()
                if (chime) {
                    HelperFunctions.playAudio(baseContext, R.raw.casiochime)
                }
                if (speak) {
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                    currentTimeText = getString(R.string.speakTimeText_ini)
                    if (clockType == "24-hours")
                        currentHour = now!!.get(Calendar.HOUR_OF_DAY)
                    currentTimeText += if (currentHour == 0) 12 else currentHour
                    startTTS()
                }
                if (vibration) {
                    vibration()
                }
            }
        } else {
            hasSpoken = false
        }
    }

    fun getSettings() {
        Log.d(TAG, "getSettings")
        settings = PreferenceManager.getDefaultSharedPreferences(application)
        val enabledSpeak = settings.getBoolean("enableSpeak", false)
        val speakOnValue = settings.getString("speakOn", "unset")
        Log.i(TAG, "speak=$enabledSpeak mode=$speakOnValue")
        if (enabledSpeak && speakOnValue != "unset") {
            clockType = settings.getString("clockType", "12-hours") ?: "12-hours"
            speak = true
            if (speakOnValue == "speakHeadsetOn" && !isHeadsetPlugged(baseContext)) {
                speak = false
            } else if (speakOnValue == "speakTimeRange") {
                iniTimeSpeak = settings.getString("speakStartTime", "00:00") ?: "00:00"
                endTimeSpeak = settings.getString("speakEndTime", "00:00") ?: "00:00"
                Log.i(TAG, "$iniTimeSpeak  $endTimeSpeak")
                scheduleIni = Calendar.getInstance()
                scheduleIni!!.set(Calendar.HOUR, TimePickerPreference.getHour(iniTimeSpeak))
                scheduleIni!!.set(Calendar.MINUTE, TimePickerPreference.getMinute(iniTimeSpeak))
                scheduleEnd = Calendar.getInstance()
                scheduleEnd!!.set(Calendar.HOUR, TimePickerPreference.getHour(endTimeSpeak))
                scheduleEnd!!.set(Calendar.MINUTE, TimePickerPreference.getMinute(endTimeSpeak))
                speak = isScheduledTime(scheduleIni!!, scheduleEnd!!)
            }
        } else {
            speak = false
        }
        val enabledChime = settings.getBoolean("enableChime", false)
        val chimeOnValue = settings.getString("chimeOn", "unset")
        Log.i(TAG, "chime=$enabledChime mode=$chimeOnValue")
        if (enabledChime && chimeOnValue != "unset") {
            chime = true
            if (chimeOnValue == "chimeHeadsetOn" && !isHeadsetPlugged(baseContext)) {
                chime = false
            } else if (chimeOnValue == "chimeTimeRange") {
                iniTimeChime = settings.getString("chimeStartTime", "00:00") ?: "00:00"
                endTimeChime = settings.getString("chimeEndTime", "00:00") ?: "00:00"
                Log.i(TAG, "$iniTimeChime  $endTimeChime")
                scheduleIni = Calendar.getInstance()
                scheduleIni!!.set(Calendar.HOUR, TimePickerPreference.getHour(iniTimeChime))
                scheduleIni!!.set(Calendar.MINUTE, TimePickerPreference.getMinute(iniTimeChime))
                scheduleEnd = Calendar.getInstance()
                scheduleEnd!!.set(Calendar.HOUR, TimePickerPreference.getHour(endTimeChime))
                scheduleEnd!!.set(Calendar.MINUTE, TimePickerPreference.getMinute(endTimeChime))
                chime = isScheduledTime(scheduleIni!!, scheduleEnd!!)
            }
        } else {
            chime = false
        }
        val enabledVibration = settings.getBoolean("enableVibration", false)
        val vibrationOnValue = settings.getString("vibrationOn", "unset")
        Log.i(TAG, "vibration=$enabledVibration mode=$vibrationOnValue")
        if (enabledVibration && vibrationOnValue != "unset") {
            vibration = true
            if (vibrationOnValue == "vibrationHeadsetOn" && !isHeadsetPlugged(baseContext)) {
                vibration = false
            } else if (vibrationOnValue == "vibrationTimeRange") {
                iniTimeChime = settings.getString("vibrationStartTime", "00:00") ?: "00:00"
                endTimeChime = settings.getString("vibrationEndTime", "00:00") ?: "00:00"
                Log.i(TAG, "$iniTimeChime  $endTimeChime")
                scheduleIni = Calendar.getInstance()
                scheduleIni!!.set(Calendar.HOUR, TimePickerPreference.getHour(iniTimeChime))
                scheduleIni!!.set(Calendar.MINUTE, TimePickerPreference.getMinute(iniTimeChime))
                scheduleEnd = Calendar.getInstance()
                scheduleEnd!!.set(Calendar.HOUR, TimePickerPreference.getHour(endTimeChime))
                scheduleEnd!!.set(Calendar.MINUTE, TimePickerPreference.getMinute(endTimeChime))
                vibration = isScheduledTime(scheduleIni!!, scheduleEnd!!)
            }
        } else {
            vibration = false
        }
    }

    private fun isScheduledTime(ini: Calendar, end: Calendar): Boolean {
        return ini.time <= now!!.time && end.time >= now!!.time
    }

    private fun startTTS() {
        tts = TextToSpeech(this, ttsListener, "com.svox.pico")
    }

    private fun stopTTS() {
        tts?.shutdown()
        tts = null
    }

    override fun onDestroy() {
        super.onDestroy()
        settings = PreferenceManager.getDefaultSharedPreferences(application)
        val isSpeakTimeOn = settings.getBoolean("enableSpeak", false)
        val isChimeOn = settings.getBoolean("enableChime", false)
        val shouldRestart = isSpeakTimeOn || isChimeOn
        if (shouldRestart)
            sendBroadcast(Intent("RestartTimeService"))
        else {
            Log.d(TAG, "Service destroyed")
            minutesTimer?.cancel()
            stopTTS()
            stopForeground(true)
        }
    }

    private val ttsListener = TextToSpeech.OnInitListener { status ->
        Log.d(TAG, "TTS engine started")
        isOn = status == TextToSpeech.SUCCESS
        val current = resources.configuration.locale
        Log.i(TAG, "Current locale " + current.displayName)
        tts?.language = current
        if (tts?.speak(currentTimeText, TextToSpeech.QUEUE_ADD, null, "") != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TTS queueing failed. Trying again")
            tts?.speak(currentTimeText, TextToSpeech.QUEUE_ADD, null, "")
        }
        if (clockType == "12-hours") {
            tts?.speak(am_pm, TextToSpeech.QUEUE_ADD, null, "mychime")
            tts?.speak("M", TextToSpeech.QUEUE_ADD, null, "mychime")
        }
    }

    inner class MyBinder : Binder() {
        val service: TimeService
            get() = this@TimeService
    }

    private fun isHeadsetPlugged(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            ?: return false
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            ) {
                return true
            }
        }
        return false
    }

    private fun vibration() {
        val pattern = longArrayOf(0, 500, 100, 500, 100, 500, 100)
        Log.d(TAG, "vibrating")
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(pattern, -1)
    }
}
