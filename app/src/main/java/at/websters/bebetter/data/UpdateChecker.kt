package at.websters.bebetter.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Self-update for sideloaded installs (no Play Store): checks the public
// GitHub releases feed, downloads the phone APK and fires the installer.
// No auth needed — the repo's releases are public.
object UpdateChecker {
    const val RELEASES_URL = "https://api.github.com/repos/Michi4/BeBetter-android/releases/latest"
    private const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

    data class UpdateInfo(
        val tag: String,
        val notes: String,
        val apkUrl: String,
        val size: Long
    )

    fun compareVersions(a: String, b: String): Int {
        fun parts(v: String) = v.trim().trimStart('v', 'V')
            .split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val pa = parts(a)
        val pb = parts(b)
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val d = (pa.getOrElse(i) { 0 }).compareTo(pb.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    /** Returns an update if the latest public release is newer than currentVersion. Null otherwise. */
    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 12000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "BeBetter-Android")
            }
            if (conn.responseCode != 200) return@withContext null
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            val tag = json.optString("tag_name").trim()
            if (tag.isBlank() || compareVersions(tag, currentVersion) <= 0) return@withContext null
            val assets = json.optJSONArray("assets") ?: return@withContext null
            for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                val name = a.optString("name")
                if (name.endsWith(".apk", ignoreCase = true) && name.contains("phone", ignoreCase = true)) {
                    val url = a.optString("browser_download_url")
                    if (url.isNotBlank()) {
                        return@withContext UpdateInfo(
                            tag = tag,
                            notes = json.optString("body").take(2000),
                            apkUrl = url,
                            size = a.optLong("size")
                        )
                    }
                }
            }
            null
        } catch (_: Exception) {
            null // offline / API hiccup: silently skip, retry tomorrow
        }
    }

    /** Downloads the APK to cache and fires the system installer. Returns false if it couldn't start. */
    suspend fun downloadAndInstall(context: Context, info: UpdateInfo, onProgress: (Int) -> Unit = {}): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 60000
                    setRequestProperty("User-Agent", "BeBetter-Android")
                    instanceFollowRedirects = true
                }
                if (conn.responseCode !in 200..299) return@withContext false
                val total = conn.contentLengthLong.takeIf { it > 0 }
                val out = File(context.cacheDir, "bebetter-update.apk")
                conn.inputStream.use { ins ->
                    out.outputStream().use { outs ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            outs.write(buf, 0, n)
                            done += n
                            if (total != null) onProgress(((done * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
                val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", out)
                val install = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(install)
                true
            } catch (_: Exception) {
                false
            }
        }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }
}
