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
import android.preference.PreferenceManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.vag.mychime.R
import com.vag.mychime.preferences.MyPreferences
import com.vag.mychime.service.TimeService
import com.vag.mychime.utils.ChangeLog
import io.github.charmicat.vaghelper.HelperFunctions

class MainActivity : AppCompatActivity(), MyPreferences.OnConfigurationChangedListener {
    private val TAG = "MainActivity"
    private val isTTSAvailableIntentCode = 666
    private val prefFragmentTag = "preference_fragment"
    private lateinit var serviceIntent: Intent
    private lateinit var settings: SharedPreferences
    private var service: TimeService? = null
    private lateinit var chimeCheck: CheckBox
    private var isChimeOn = false
    private var isSpeakTimeOn = false
    private var isVibrationOn = false
    private var isTTSAvailable = false
    private var uncommittedChanges = false
    private lateinit var speakTime: ToggleButton
    private lateinit var chime: ToggleButton
    private lateinit var myToolbar: Toolbar

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            Log.d(TAG, "onServiceConnected")
            service = (binder as TimeService.MyBinder).service
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.d(TAG, "onServiceDisconnected")
            service = null
        }
    }

    private val ttsActivityResultCB: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ActivityResultCallback<ActivityResult> { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "onActivityResult " + result.resultCode)
                    isTTSAvailable = result.resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS
                    if (!isTTSAvailable) {
                        val builder = AlertDialog.Builder(applicationContext)
                        builder.setTitle(R.string.titleMsg)
                        builder.setMessage(R.string.installMsg)
                        builder.setPositiveButton(android.R.string.ok) { _, _ ->
                            val installIntent = Intent()
                            installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                            startActivity(installIntent)
                        }
                        builder.setNegativeButton(android.R.string.cancel, null)
                        builder.create()
                        builder.show()
                    }
                }
            })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContentView(R.layout.settings_activity)
        settings = PreferenceManager.getDefaultSharedPreferences(application)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val editor = settings.edit()
        editor.putBoolean("hasVibration", vibrator.hasVibrator())
        editor.putBoolean("enableVibration", false)
        editor.commit()
        Log.d(TAG, vibrator.hasVibrator().toString() + " onCreate " + settings.all)
        if (savedInstanceState == null) {
            Log.d(TAG, "onCreate replacing MyPreferences fragment")
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings, MyPreferences(), prefFragmentTag)
                .commit()
        }
        if (HelperFunctions.isIntentAvailable(this, TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
            val intent = Intent()
            intent.action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA
            ttsActivityResultCB.launch(intent)
        }
        serviceIntent = Intent(this, TimeService::class.java)
        controlService()
        myToolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(myToolbar)
        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun controlService() {
        if (!settings.contains("installFlag")) {
            val editor = settings.edit()
            editor.putInt("installFlag", 1)
            editor.commit()
        }
        val isEnabled = getState()
        val isServiceRunning = HelperFunctions.isServiceRunning(application, "com.vag.mychime.service.TimeService", false)
        Log.d(TAG, "controlService: isEnabled $isEnabled")
        if (!isServiceRunning) {
            if (isEnabled) {
                Log.d(TAG, "controlService: Starting service")
                startService(serviceIntent)
            }
        } else {
            if (!isEnabled) {
                stopService(serviceIntent)
            }
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

    override fun onConfigurationChanged() {
        Log.d(TAG, "onConfigurationChanged nopar")
        controlService()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "onConfigurationChanged")
        controlService()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.rate) {
            Log.d(TAG, "Clicked on rate")
            rateApp()
            true
        } else if (item.itemId == R.id.about) {
            val cl = ChangeLog(this)
            cl.fullLogDialog.show()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    fun rateApp() {
        try {
            val rateIntent = rateIntentForUrl("market://details")
            startActivity(rateIntent)
        } catch (e: ActivityNotFoundException) {
            val rateIntent = rateIntentForUrl("http://play.google.com/store/apps/details")
            startActivity(rateIntent)
        }
    }

    private fun rateIntentForUrl(url: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(String.format("%s?id=%s", url, packageName)))
        val flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        intent.addFlags(flags)
        return intent
    }

    fun isNewInstall(): Boolean {
        return if (settings.contains("installFlag")) {
            Log.d(TAG, "isNewInstall: false, showing changelog")
            false
        } else {
            Log.d(TAG, "isNewInstall: true, showing welcome")
            true
        }
    }
}
