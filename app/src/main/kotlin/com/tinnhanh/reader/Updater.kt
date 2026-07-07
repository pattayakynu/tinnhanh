package com.tinnhanh.reader

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.tinnhanh.core.AppConfig
import com.tinnhanh.core.UpdateAction
import com.tinnhanh.core.decideUpdate
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** Kiểm tra + tải + cài bản mới. Gọi trên main thread với config đã lấy được. */
object Updater {
    fun check(activity: MainActivity, config: AppConfig, httpForDownload: OkHttpClient) {
        val current = BuildConfig.VERSION_CODE
        when (decideUpdate(current, config)) {
            UpdateAction.NONE -> {}
            UpdateAction.SOFT -> showDialog(activity, config, httpForDownload, force = false)
            UpdateAction.FORCE -> showDialog(activity, config, httpForDownload, force = true)
        }
    }

    private fun showDialog(activity: MainActivity, config: AppConfig, http: OkHttpClient, force: Boolean) {
        val b = AlertDialog.Builder(activity)
            .setTitle(if (force) "Cần cập nhật" else "Có bản mới")
            .setMessage(
                if (force) "Phiên bản này đã cũ, cần cập nhật để tiếp tục dùng."
                else "Đã có phiên bản mới hơn. Cập nhật ngay?"
            )
            .setCancelable(!force)
            .setPositiveButton("Cập nhật") { _, _ -> download(activity, config, http) }
        if (!force) b.setNegativeButton("Để sau", null)
        b.show()
    }

    private fun download(activity: MainActivity, config: AppConfig, http: OkHttpClient) {
        Toast.makeText(activity, "Đang tải bản mới...", Toast.LENGTH_SHORT).show()
        Thread {
            val apk = File(activity.cacheDir, "update.apk")
            val ok = try {
                http.newCall(Request.Builder().url(config.latestApk).build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@use false
                    resp.body?.byteStream()?.use { input ->
                        apk.outputStream().use { output -> input.copyTo(output) }
                    }
                    true
                }
            } catch (e: Exception) {
                false
            }
            activity.runOnUiThread {
                if (ok) install(activity, apk)
                else Toast.makeText(activity, "Tải thất bại, thử lại sau.", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun install(activity: MainActivity, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(activity, "Không mở được trình cài đặt.", Toast.LENGTH_LONG).show()
        }
    }
}
