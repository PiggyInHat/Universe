package com.example.universe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import java.io.File
import java.net.URL

object UpdateChecker {
    private const val GITHUB_OWNER = "PiggyInHat"
    private const val GITHUB_REPO = "Universe"

    data class ReleaseInfo(val tag_name: String, val assets: List<Asset>)
    data class Asset(val name: String, val browser_download_url: String)

    fun checkForUpdate(context: Context, forceCheck: Boolean = false, callback: (ReleaseInfo?) -> Unit) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong("last_check", 0)
        val currentTime = System.currentTimeMillis()

        // ВРЕМЕННО СТОИТ 1000 (1 СЕКУНДА) ДЛЯ ТЕСТА!
        if (!forceCheck && (currentTime - lastCheckTime < 24 * 60 * 60 * 1000)) {
            callback(null)
            return
        }

        Thread {
            try {
                val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
                val response = URL(url).readText()
                val release = Gson().fromJson(response, ReleaseInfo::class.java)

                prefs.edit().putLong("last_check", currentTime).apply()

                if (isNewerVersion(release.tag_name, BuildConfig.VERSION_NAME)) {
                    callback(release)
                } else {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "У вас последняя версия (${BuildConfig.VERSION_NAME})", Toast.LENGTH_LONG).show()
                    }
                    callback(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "ОШИБКА: ${e.message}", Toast.LENGTH_LONG).show()
                }
                callback(null)
            }
        }.start()
    }

    private fun isNewerVersion(githubTag: String, currentVersion: String): Boolean {
        val cleanGithub = githubTag.removePrefix("v").trim()
        return try {
            val githubParts = cleanGithub.split(".").map { it.toInt() }
            val currentParts = currentVersion.split(".").map { it.toInt() }
            for (i in 0 until maxOf(githubParts.size, currentParts.size)) {
                val g = githubParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (g > c) return true
                if (g < c) return false
            }
            false
        } catch (e: Exception) { false }
    }

    fun downloadAndInstall(context: Context, assetUrl: String) {
        Thread {
            try {
                val fileName = "update.apk"
                val file = File(context.getExternalFilesDir(null), fileName)

                URL(assetUrl).openStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(settingsIntent)
                } else {
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Ошибка скачивания: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}