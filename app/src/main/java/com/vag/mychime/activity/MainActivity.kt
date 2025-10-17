package com.vag.mychime.activity

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Vibrator
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.vag.mychime.R
import com.vag.mychime.preferences.MyPreferences
import com.vag.mychime.service.TimeService
import com.vag.mychime.utils.ChangeLog
import io.github.charmicat.vaghelper.HelperFunctions

class MainActivity : AppCompatActivity(), MyPreferences.OnConfigurationChangedListener {

    private val tag = "MainActivity"
    private val debug = true
    private val isTtsAvailableIntentCode = 666
    private val prefFragmentTag = "preference_fragment"

    private lateinit var serviceIntent: Intent
    private lateinit var settings: SharedPreferences
    private var service: TimeService? = null
    private var isServiceBound = false
    private var isChimeOn = false
    private var isSpeakTimeOn = false
    private var isVibrationOn = false
    private var isTtsAvailable = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            Log.d(tag, "onServiceConnected")
            service = (binder as TimeService.MyBinder).getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.d(tag, "onServiceDisconnected")
            service = null
        }
    }

    private val ttsActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(tag, "onActivityResult ${result.resultCode}")
            isTtsAvailable = result.resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS
            if (!isTtsAvailable) {
                showTtsInstallDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(tag, "onCreate")
        setContentView(R.layout.settings_activity)
        settings = PreferenceManager.getDefaultSharedPreferences(application)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        settings.edit()
            .putBoolean("hasVibration", vibrator.hasVibrator())
            .putBoolean("enableVibration", false)
            .apply()
        Log.d(tag, "${vibrator.hasVibrator()} onCreate ${settings.all}")

        if (savedInstanceState == null) {
            Log.d(tag, "onCreate replacing MyPreferences fragment")
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings, MyPreferences(), prefFragmentTag)
                .commit()
        }

        if (HelperFunctions.isIntentAvailable(this, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
            val intent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
            ttsActivityResultLauncher.launch(intent)
        }

        serviceIntent = Intent(this, TimeService::class.java)
        controlService()

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()
        isServiceBound = bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        Log.d(tag, "onStop isSpeakTimeOn=$isSpeakTimeOn isChimeOn=$isChimeOn isVibrateOn=$isVibrationOn")
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }

        val isServiceRunning = HelperFunctions.isServiceRunning(
            application,
            "com.vag.mychime.service.TimeService",
            debug
        )

        if (!isServiceRunning) {
            Log.d(tag, "Service is not running")
            Toast.makeText(this, getString(R.string.serviceStoped), Toast.LENGTH_LONG).show()
        } else {
            Log.d(tag, "Service is running")
            Toast.makeText(application, getString(R.string.serviceStarted), Toast.LENGTH_LONG).show()
        }
    }

    private fun showTtsInstallDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.titleMsg)
            .setMessage(R.string.installMsg)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                startActivity(installIntent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun controlService() {
        if (!settings.contains("installFlag")) {
            settings.edit().putInt("installFlag", 1).apply()
        }
        val isEnabled = state
        val isServiceRunning = HelperFunctions.isServiceRunning(
            application,
            "com.vag.mychime.service.TimeService",
            false
        )
        Log.d(tag, "controlService: isEnabled $isEnabled")

        if (!isServiceRunning && isEnabled) {
            Log.d(tag, "controlService: Starting service")
            startService(serviceIntent)
        } else if (isServiceRunning && !isEnabled) {
            stopService(serviceIntent)
        }
    }

    private val state: Boolean
        get() = if (!isNewInstall()) {
            isSpeakTimeOn = settings.getBoolean("enableSpeak", false)
            isChimeOn = settings.getBoolean("enableChime", false)
            isVibrationOn = settings.getBoolean("enableVibration", false)
            isSpeakTimeOn || isChimeOn || isVibrationOn
        } else {
            false
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(tag, "onActivityResult: requestCode$requestCode resultCode $resultCode")

        if (requestCode == isTtsAvailableIntentCode) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                isTtsAvailable = true
            } else {
                isTtsAvailable = false
                showTtsInstallDialog()
            }
        }
    }

    override fun onConfigurationChanged() {
        Log.d(tag, "onConfigurationChanged nopar")
        controlService()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(tag, "onConfigurationChanged")
        controlService()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(tag, "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.rate -> {
                Log.d(tag, "Clicked on rate")
                rateApp()
                true
            }

            R.id.about -> {
                ChangeLog(this).fullLogDialog.show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun rateApp() {
        try {
            startActivity(rateIntentForUrl("market://details"))
        } catch (e: ActivityNotFoundException) {
            startActivity(rateIntentForUrl("http://play.google.com/store/apps/details"))
        }
    }

    private fun rateIntentForUrl(url: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse("$url?id=$packageName")).apply {
            val flags = Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT
            addFlags(flags)
        }
    }

    fun isNewInstall(): Boolean {
        return if (settings.contains("installFlag")) {
            Log.d(tag, "isNewInstall: false, showing changelog")
            false
        } else {
            Log.d(tag, "isNewInstall: true, showing welcome")
            true
        }
    }
}
