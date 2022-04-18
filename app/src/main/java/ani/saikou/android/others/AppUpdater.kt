package ani.saikou.android.others

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import ani.saikou.android.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object AppUpdater {
    fun check(activity: Activity) {
        try {
            val version =
                if (!BuildConfig.DEBUG)
                    OkHttpClient().newCall(
                        Request.Builder()
                            .url("https://raw.githubusercontent.com/saikou-app/mal-id-filler-list/main/stable.txt")
                            .build()
                    ).execute().body?.string()?.replace("\n", "") ?: return
                else {
                    OkHttpClient().newCall(
                        Request.Builder()
                            .url("https://raw.githubusercontent.com/saikou-app/saikou/main/app/build.gradle")
                            .build()
                    ).execute().body?.string()?.substringAfter("versionName \"")
                        ?.substringBefore('"') ?: return
                }
            val dontShow = loadData("dont_ask_for_update_$version") ?: false
            if (compareVersion(version) && !dontShow && !activity.isDestroyed) activity.runOnUiThread {
                AlertDialog.Builder(activity, R.style.DialogTheme)
                    .setTitle("A new update is available, do you want to check it out?").apply {
                        setMultiChoiceItems(
                            arrayOf("Don't show again for version $version"),
                            booleanArrayOf(false)
                        ) { _, _, isChecked ->
                            if (isChecked) {
                                saveData("dont_ask_for_update_$version", isChecked)
                            }
                        }
                        setPositiveButton("Let's Go") { _: DialogInterface, _: Int ->
                            if (!BuildConfig.DEBUG) {
                                MainScope().launch(Dispatchers.IO) {
                                    try {
                                        OkHttpClient().newCall(
                                            Request.Builder()
                                                .url("https://api.github.com/repos/saikou-app/saikou/releases/tags/v$version")
                                                .build()
                                        ).execute().body?.string()?.apply {
                                            substringAfter("\"browser_download_url\":\"").substringBefore(
                                                '"'
                                            ).apply {
                                                if (endsWith("apk")) activity.downloadUpdate(this)
                                                else openLinkInBrowser("https://github.com/saikou-app/saikou/releases/")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        toastString(e.toString())
                                    }
                                }
                            } else openLinkInBrowser("https://discord.com/channels/902174389351620629/946852010198728704")
                        }
                        setNegativeButton("Cope") { dialogInterface: DialogInterface, _: Int ->
                            dialogInterface.dismiss()
                        }
                    }.show()
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
    }

    private fun compareVersion(version: String): Boolean {
        return try {
            version.replace(".", "").toInt() > BuildConfig.VERSION_NAME.replace(".", "").toInt()
        } catch (e: Exception) {
            false
        }
    }


    //Blatantly kanged from https://github.com/LagradOst/CloudStream-3/blob/master/app/src/main/java/com/lagradost/cloudstream3/utils/InAppUpdater.kt
    private fun Activity.downloadUpdate(url: String): Boolean {
        val downloadManager = this.getSystemService<DownloadManager>()!!

        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType("application/vnd.android.package-archive")
            .setTitle("Saikou Update")
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Saikou.apk"
            )
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            toastString(e.toString())
            -1
        }
        if (id == -1L) return true
        registerReceiver(
            object : BroadcastReceiver() {
                @SuppressLint("Range")
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        val downloadId = intent?.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, id
                        ) ?: id

                        val query = DownloadManager.Query()
                        query.setFilterById(downloadId)
                        val c = downloadManager.query(query)

                        if (c.moveToFirst()) {
                            val columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            if (DownloadManager.STATUS_SUCCESSFUL == c
                                    .getInt(columnIndex)
                            ) {
                                c.getColumnIndex(DownloadManager.COLUMN_MEDIAPROVIDER_URI)
                                val uri = Uri.parse(
                                    c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                                )
                                openApk(this@downloadUpdate, uri)
                            }
                        }
                    } catch (e: Exception) {
                        toastString(e.toString())
                    }
                }
            }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        return true
    }

    fun openApk(context: Context, uri: Uri) {
        try {
            uri.path?.let {
                val contentUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(it)
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    data = contentUri
                }
                context.startActivity(installIntent)
            }
        } catch (e: Exception) {
            toastString(e.toString())
        }
    }
}