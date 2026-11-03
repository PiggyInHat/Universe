package com.example.universe

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import java.io.File
import java.net.URL

object UpdateChecker {
    // ⚠️ ВАЖНО: Укажите ваш точный ник и название репозитория на GitHub (как на скриншоте)
    private const val GITHUB_OWNER = "PiggylnHat"
    private const val GITHUB_REPO = "Universe"

    // Классы для парсинга ответа от GitHub API
    data class ReleaseInfo(val tag_name: String, val assets: List<Asset>)
    data class Asset(val name: String, val browser_download_url: String)

    // Проверка обновлений с кэшированием (раз в 24 часа)
    fun checkForUpdate(context: Context, forceCheck: Boolean = false, callback: (ReleaseInfo?) -> Unit) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong("last_check", 0)
        val currentTime = System.currentTimeMillis()

        // Если прошло меньше 24 часов и это не принудительная проверка — тихо выходим
        if (!forceCheck && (currentTime - lastCheckTime < 24 * 60 * 60 * 1000)) {
            callback(null)
            return
        }

        Thread {
            try {
                val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
                val response = URL(url).readText()
                val release = Gson().fromJson(response, ReleaseInfo::class.java)

                // Сохраняем время успешной проверки
                prefs.edit().putLong("last_check", currentTime).apply()

                // Сравниваем версии (текущую из BuildConfig и новую с GitHub)
                if (isNewerVersion(release.tag_name, BuildConfig.VERSION_NAME)) {
                    callback(release)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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

    // Скачивание и запуск установки
    fun downloadAndInstall(context: Context, assetUrl: String) {
        Thread {
            try {
                val fileName = "update.apk"
                val file = File(context.getExternalFilesDir(null), fileName)

                // Скачиваем APK
                URL(assetUrl).openStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }

                // Запускаем системный установщик через FileProvider
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                // Проверка разрешения на установку неизвестных приложений (Android 8+)
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
            }
        }.start()
    }
}