package com.vag.mychime.utils

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.util.Log
import android.view.ContextThemeWrapper
import android.webkit.WebView
import com.vag.mychime.R
import androidx.preference.PreferenceManager
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class ChangeLog @JvmOverloads constructor(
    private val context: Context,
    sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
) {

    private val lastVersion: String
    private val thisVersion: String

    init {
        lastVersion = sp.getString(VERSION_KEY, NO_VERSION) ?: NO_VERSION
        Log.d(TAG, "lastVersion: $lastVersion")
        thisVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: NO_VERSION
        } catch (e: Exception) {
            Log.e(TAG, "could not get version name from manifest!", e)
            NO_VERSION
        }
        Log.d(TAG, "appVersion: $thisVersion")
    }

    val logDialog: AlertDialog
        get() = getDialog(firstRunEver())

    val fullLogDialog: AlertDialog
        get() = getDialog(true)

    fun getLastVersion(): String = lastVersion

    fun getThisVersion(): String = thisVersion

    fun firstRun(): Boolean = lastVersion != thisVersion

    fun firstRunEver(): Boolean = lastVersion == NO_VERSION

    fun getChangeLog(): String = getChangeLog(false)

    fun getFullLog(): String = getChangeLog(true)

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
            .setPositiveButton(context.resources.getString(R.string.ok_button)) { _, _ ->
                updateVersionInPreferences()
            }

        if (!full) {
            builder.setNegativeButton(R.string.changelog_show_full) { _: DialogInterface, _: Int ->
                fullLogDialog.show()
            }
        }

        return builder.create()
    }

    private fun updateVersionInPreferences() {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        sp.edit().putString(VERSION_KEY, thisVersion).commit()
    }

    private fun getChangeLog(full: Boolean): String {
        val sb = StringBuilder()
        try {
            context.resources.openRawResource(R.raw.changelog).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    var advanceToEovs = false
                    while (reader.readLine().also { line = it } != null) {
                        val content = line?.trim().orEmpty()
                        if (content.isEmpty()) continue
                        val marker = content[0]
                        if (marker == '$') {
                            closeList(sb)
                            val version = content.substring(1).trim()
                            if (!full) {
                                if (lastVersion == version) {
                                    advanceToEovs = true
                                } else if (version == EOCL) {
                                    advanceToEovs = false
                                }
                            }
                        } else if (!advanceToEovs) {
                            when (marker) {
                                '%' -> {
                                    closeList(sb)
                                    sb.append("<div class='title'>${content.substring(1).trim()}</div>\n")
                                }

                                '_' -> {
                                    closeList(sb)
                                    sb.append("<div class='subtitle'>${content.substring(1).trim()}</div>\n")
                                }

                                '!' -> {
                                    closeList(sb)
                                    sb.append("<div class='freetext'>${content.substring(1).trim()}</div>\n")
                                }

                                '#' -> {
                                    openList(sb, ListMode.ORDERED)
                                    sb.append("<li>${content.substring(1).trim()}</li>\n")
                                }

                                '*' -> {
                                    openList(sb, ListMode.UNORDERED)
                                    sb.append("<li>${content.substring(1).trim()}</li>\n")
                                }

                                else -> {
                                    closeList(sb)
                                    sb.append("$content\n")
                                }
                            }
                        }
                    }
                    closeList(sb)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading changelog", e)
        }

        return sb.toString()
    }

    private fun openList(sb: StringBuilder, listMode: ListMode) {
        if (this.listMode != listMode) {
            closeList(sb)
            when (listMode) {
                ListMode.ORDERED -> sb.append("<div class='list'><ol>\n")
                ListMode.UNORDERED -> sb.append("<div class='list'><ul>\n")
                else -> {}
            }
            this.listMode = listMode
        }
    }

    private fun closeList(sb: StringBuilder) {
        when (listMode) {
            ListMode.ORDERED -> sb.append("</ol></div>\n")
            ListMode.UNORDERED -> sb.append("</ul></div>\n")
            else -> {}
        }
        listMode = ListMode.NONE
    }

    private enum class ListMode {
        NONE, ORDERED, UNORDERED
    }

    private var listMode = ListMode.NONE

    companion object {
        private const val TAG = "ChangeLog"
        private const val VERSION_KEY = "PREFS_VERSION_KEY"
        private const val NO_VERSION = ""
        private const val EOCL = "END_OF_CHANGE_LOG"
    }
}
