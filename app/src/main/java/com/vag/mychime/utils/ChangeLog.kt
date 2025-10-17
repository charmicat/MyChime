package com.vag.mychime.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Log
import android.view.ContextThemeWrapper
import android.webkit.WebView
import com.vag.mychime.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ChangeLog @JvmOverloads constructor(
    private val context: Context,
    sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
) {

    private var lastVersion: String
    private var thisVersion: String
    private var listMode = Listmode.NONE
    private var sb: StringBuilder = StringBuilder()

    init {
        lastVersion = sp.getString(VERSION_KEY, NO_VERSION) ?: NO_VERSION
        Log.d(TAG, "lastVersion: $lastVersion")
        thisVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: NO_VERSION
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "could not get version name from manifest!", e)
            NO_VERSION
        }
        Log.d(TAG, "appVersion: $thisVersion")
    }

    fun getLastVersion(): String = lastVersion

    fun getThisVersion(): String = thisVersion

    fun firstRun(): Boolean = lastVersion != thisVersion

    fun firstRunEver(): Boolean = lastVersion == NO_VERSION

    val logDialog: AlertDialog
        get() = getDialog(firstRunEver())

    val fullLogDialog: AlertDialog
        get() = getDialog(true)

    private fun getDialog(full: Boolean): AlertDialog {
        val webView = WebView(context).apply {
            loadDataWithBaseURL(null, getChangeLog(full), "text/html", "UTF-8", null)
        }

        val builder = AlertDialog.Builder(ContextThemeWrapper(context, android.R.style.Theme_Dialog))
            .setTitle(
                context.resources.getString(
                    if (full) R.string.changelog_full_title else R.string.changelog_title
                )
            )
            .setView(webView)
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.ok_button)) { _, _ ->
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
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(VERSION_KEY, thisVersion).commit()
    }

    fun getChangeLog(): String = getChangeLog(false)

    fun getFullLog(): String = getChangeLog(true)

    private fun getChangeLog(full: Boolean): String {
        sb = StringBuilder()
        try {
            context.resources.openRawResource(R.raw.changelog).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    var advanceToEovs = false
                    val nowVersion = lastVersion
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line?.trim() ?: continue
                        val marker = if (currentLine.isNotEmpty()) currentLine[0] else 0.toChar()
                        when (marker) {
                            '$' -> {
                                closeList()
                                val version = currentLine.substring(1).trim()
                                if (!full) {
                                    if (nowVersion == version) {
                                        advanceToEovs = true
                                    } else if (version == EOCL) {
                                        advanceToEovs = false
                                    }
                                }
                            }

                            else -> if (!advanceToEovs) {
                                when (marker) {
                                    '%' -> {
                                        closeList()
                                        sb.append("<div class='title'>${currentLine.substring(1).trim()}</div>\n")
                                    }

                                    '_' -> {
                                        closeList()
                                        sb.append("<div class='subtitle'>${currentLine.substring(1).trim()}</div>\n")
                                    }

                                    '!' -> {
                                        closeList()
                                        sb.append("<div class='freetext'>${currentLine.substring(1).trim()}</div>\n")
                                    }

                                    '#' -> {
                                        openList(Listmode.ORDERED)
                                        sb.append("<li>${currentLine.substring(1).trim()}</li>\n")
                                    }

                                    '*' -> {
                                        openList(Listmode.UNORDERED)
                                        sb.append("<li>${currentLine.substring(1).trim()}</li>\n")
                                    }

                                    else -> {
                                        closeList()
                                        sb.append("$currentLine\n")
                                    }
                                }
                            }
                        }
                    }
                    closeList()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read changelog", e)
        }

        return sb.toString()
    }

    private fun openList(mode: Listmode) {
        if (listMode != mode) {
            closeList()
            when (mode) {
                Listmode.ORDERED -> sb.append("<div class='list'><ol>\n")
                Listmode.UNORDERED -> sb.append("<div class='list'><ul>\n")
                else -> {}
            }
            listMode = mode
        }
    }

    private fun closeList() {
        when (listMode) {
            Listmode.ORDERED -> sb.append("</ol></div>\n")
            Listmode.UNORDERED -> sb.append("</ul></div>\n")
            else -> {}
        }
        listMode = Listmode.NONE
    }

    fun dontuseSetLastVersion(lastVersion: String) {
        this.lastVersion = lastVersion
    }

    private enum class Listmode {
        NONE, ORDERED, UNORDERED
    }

    companion object {
        private const val VERSION_KEY = "PREFS_VERSION_KEY"
        private const val NO_VERSION = ""
        private const val EOCL = "END_OF_CHANGE_LOG"
        private const val TAG = "ChangeLog"
    }
}
