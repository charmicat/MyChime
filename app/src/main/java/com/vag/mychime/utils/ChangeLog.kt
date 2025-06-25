package com.vag.mychime.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager.NameNotFoundException
import android.preference.PreferenceManager
import android.util.Log
import android.view.ContextThemeWrapper
import android.webkit.WebView
import com.vag.mychime.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class ChangeLog(private val context: Context, sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)) {
    private var lastVersion: String
    private var thisVersion: String

    fun getLastVersion(): String {
        return lastVersion
    }

    fun getThisVersion(): String {
        return thisVersion
    }

    fun firstRun(): Boolean {
        return lastVersion != thisVersion
    }

    fun firstRunEver(): Boolean {
        return NO_VERSION == lastVersion
    }

    val logDialog: AlertDialog
        get() = getDialog(firstRunEver())

    val fullLogDialog: AlertDialog
        get() = getDialog(true)

    private fun getDialog(full: Boolean): AlertDialog {
        val wv = WebView(context)
        wv.loadDataWithBaseURL(null, getChangeLog(full), "text/html", "UTF-8", null)
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, android.R.style.Theme_Dialog))
        builder.setTitle(context.resources.getString(if (full) R.string.changelog_full_title else R.string.changelog_title))
            .setView(wv)
            .setCancelable(false)
            .setPositiveButton(context.resources.getString(R.string.ok_button)) { _: DialogInterface?, _: Int ->
                updateVersionInPreferences()
            }
        if (!full) {
            builder.setNegativeButton(R.string.changelog_show_full) { _: DialogInterface?, _: Int ->
                fullLogDialog.show()
            }
        }
        return builder.create()
    }

    private fun updateVersionInPreferences() {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sp.edit()
        editor.putString(VERSION_KEY, thisVersion)
        editor.commit()
    }

    fun getChangeLog(): String {
        return getChangeLog(false)
    }

    fun getFullLog(): String {
        return getChangeLog(true)
    }

    private enum class Listmode { NONE, ORDERED, UNORDERED }
    private var listMode = Listmode.NONE
    private var sb: StringBuffer? = null

    private fun getChangeLog(full: Boolean): String {
        sb = StringBuffer()
        try {
            val ins: InputStream = context.resources.openRawResource(R.raw.changelog)
            val br = BufferedReader(InputStreamReader(ins))
            var line: String?
            var advanceToEOVS = false
            while (br.readLine().also { line = it } != null) {
                var lineTrim = line!!.trim { it <= ' ' }
                val marker = if (lineTrim.isNotEmpty()) lineTrim[0] else 0.toChar()
                if (marker == '$') {
                    closeList()
                    val version = lineTrim.substring(1).trim { it <= ' ' }
                    if (!full) {
                        if (lastVersion == version) {
                            advanceToEOVS = true
                        } else if (version == EOCL) {
                            advanceToEOVS = false
                        }
                    }
                } else if (!advanceToEOVS) {
                    when (marker) {
                        '%' -> {
                            closeList()
                            sb!!.append("<div class='title'>" + lineTrim.substring(1).trim { it <= ' ' } + "</div>\n")
                        }
                        '_' -> {
                            closeList()
                            sb!!.append("<div class='subtitle'>" + lineTrim.substring(1).trim { it <= ' ' } + "</div>\n")
                        }
                        '!' -> {
                            closeList()
                            sb!!.append("<div class='freetext'>" + lineTrim.substring(1).trim { it <= ' ' } + "</div>\n")
                        }
                        '#' -> {
                            openList(Listmode.ORDERED)
                            sb!!.append("<li>" + lineTrim.substring(1).trim { it <= ' ' } + "</li>\n")
                        }
                        '*' -> {
                            openList(Listmode.UNORDERED)
                            sb!!.append("<li>" + lineTrim.substring(1).trim { it <= ' ' } + "</li>\n")
                        }
                        else -> {
                            closeList()
                            sb!!.append(lineTrim + "\n")
                        }
                    }
                }
            }
            closeList()
            br.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return sb.toString()
    }

    private fun openList(mode: Listmode) {
        if (listMode != mode) {
            closeList()
            if (mode == Listmode.ORDERED) {
                sb!!.append("<div class='list'><ol>\n")
            } else if (mode == Listmode.UNORDERED) {
                sb!!.append("<div class='list'><ul>\n")
            }
            listMode = mode
        }
    }

    private fun closeList() {
        if (listMode == Listmode.ORDERED) {
            sb!!.append("</ol></div>\n")
        } else if (listMode == Listmode.UNORDERED) {
            sb!!.append("</ul></div>\n")
        }
        listMode = Listmode.NONE
    }

    fun dontuseSetLastVersion(lastVersion: String) {
        this.lastVersion = lastVersion
    }

    companion object {
        private const val VERSION_KEY = "PREFS_VERSION_KEY"
        private const val NO_VERSION = ""
        private const val EOCL = "END_OF_CHANGE_LOG"
        private const val TAG = "ChangeLog"
    }

    init {
        lastVersion = sp.getString(VERSION_KEY, NO_VERSION) ?: NO_VERSION
        Log.d(TAG, "lastVersion: $lastVersion")
        thisVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: NameNotFoundException) {
            Log.e(TAG, "could not get version name from manifest!")
            e.printStackTrace()
            NO_VERSION
        }
        Log.d(TAG, "appVersion: $thisVersion")
    }
}
