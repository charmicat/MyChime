package com.vag.mychime.activity

import android.app.Activity
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
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import com.vag.mychime.R
import com.vag.mychime.preferences.MyPreferences
import com.vag.mychime.service.TimeService
import com.vag.mychime.utils.ChangeLog
import io.github.charmicat.vaghelper.HelperFunctions

class MainActivity : AppCompatActivity(), MyPreferences.OnConfigurationChangedListener {

    private val tag = "MainActivity"
    private val prefFragmentTag = "preference_fragment"

    private lateinit var serviceIntent: Intent
    private lateinit var settings: SharedPreferences
    private var service: TimeService? = null
    private var isChimeOn = false
    private var isSpeakTimeOn = false
    private var isVibrationOn = false
    private var isTtsAvailable = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            Log.d(tag, "onServiceConnected")
            service = (binder as TimeService.MyBinder).service
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.d(tag, "onServiceDisconnected")
            service = null
        }
    }

    private val ttsActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(tag, "onActivityResult ${result.resultCode}")
            when (result.resultCode) {
                RESULT_OK, TextToSpeech.Engine.CHECK_VOICE_DATA_PASS -> {
                    isTtsAvailable = true
                }

                TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL -> {
                    isTtsAvailable = false
                    showInstallTtsDialog()
                }

                else -> {
                    if (result.resultCode != RESULT_CANCELED) {
                        isTtsAvailable = false
                        showInstallTtsDialog()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(tag, "onCreate")
        setContentView(R.layout.settings_activity)
        settings = PreferenceManager.getDefaultSharedPreferences(application)
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        with(settings.edit()) {
            putBoolean("hasVibration", vibrator.hasVibrator())
            putBoolean("enableVibration", false)
            apply()
        }
        Log.d(tag, "${vibrator.hasVibrator()} onCreate ${settings.all}")

        if (savedInstanceState == null) {
            Log.d(tag, "onCreate replacing MyPreferences fragment")
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, MyPreferences(), prefFragmentTag)
                .commit()
        }

        if (HelperFunctions.isIntentAvailable(this, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
            val intent = Intent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)
            ttsActivityResultLauncher.launch(intent)
        }

        serviceIntent = Intent(this, TimeService::class.java)
        controlService()

        val myToolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(myToolbar)
        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun showInstallTtsDialog() {
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
        val isEnabled = getState()
        val isServiceRunning = HelperFunctions.isServiceRunning(
            application,
            "com.vag.mychime.service.TimeService",
            false
        )
        Log.d(tag, "controlService: isEnabled $isEnabled")

        if (!isServiceRunning) {
            Log.d(tag, "Service is not running")
            if (isEnabled) {
                Log.d(tag, "controlService: Starting service")
                startService(serviceIntent)
            }
        } else if (!isEnabled) {
            stopService(serviceIntent)
        }
    }

    private fun getState(): Boolean {
        return if (!isNewInstall()) {
            isSpeakTimeOn = settings.getBoolean("enableSpeak", false)
            isChimeOn = settings.getBoolean("enableChime", false)
            isVibrationOn = settings.getBoolean("enableVibration", false)
            isSpeakTimeOn || isChimeOn || isVibrationOn
        } else {
            false
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(tag, "onStop isSpeakTimeOn=$isSpeakTimeOn isChimeOn=$isChimeOn isVibrateOn=$isVibrationOn")

        val isServiceRunning = HelperFunctions.isServiceRunning(
            application,
            "com.vag.mychime.service.TimeService",
            true
        )

        if (!isServiceRunning) {
            Log.d(tag, "Service is not running")
            Toast.makeText(this, resources.getString(R.string.serviceStoped), Toast.LENGTH_LONG).show()
        } else {
            Log.d(tag, "Service is running")
            Toast.makeText(application, resources.getString(R.string.serviceStarted), Toast.LENGTH_LONG)
                .show()
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
            val rateIntent = rateIntentForUrl("market://details")
            startActivity(rateIntent)
        } catch (e: ActivityNotFoundException) {
            val rateIntent = rateIntentForUrl("http://play.google.com/store/apps/details")
            startActivity(rateIntent)
        }
    }

    private fun rateIntentForUrl(url: String): Intent {
        val uri = Uri.parse("$url?id=$packageName")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT
            )
        }
    }

    private fun isNewInstall(): Boolean {
        return if (settings.contains("installFlag")) {
            Log.d(tag, "isNewInstall: false, showing changelog")
            false
        } else {
            Log.d(tag, "isNewInstall: true, showing welcome")
            true
        }
    }
}
