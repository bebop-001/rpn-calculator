
@file:Suppress("MoveLambdaOutsideParentheses")

package com.kana_tutor.rpncalc.kanautils

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.kana_tutor.rpncalc.BuildConfig
import com.kana_tutor.rpncalc.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


private const val DOUBLE_CLICK_TIMEOUT = 2500L
private var clickTimer: Long = 0
private var kanaUtilsToast: Toast? = null

// read in each info assets file.  file name includes any directory
// info relative to assets.  eg: assets/release_info/Info_00.html is
// release_info/Info_00.html
private fun Activity.assetFileLines(assetsFile:String) : String {
    val sb = StringBuffer()
    val reader = assets.open(assetsFile).bufferedReader()
    var line : String? = reader.readLine()
    while (line != null) {
        sb.append(line).append("\n")
        line = reader.readLine()
    }
    reader.close()
    return sb.toString()
}

fun Activity.webViewAlert (webView : WebView, title:String) {
    androidx.appcompat.app.AlertDialog.Builder(this)
        .setTitle(title)
        .setView(webView)
        .setNegativeButton(R.string.KT_done,
            {dialogInterface, _ -> dialogInterface.dismiss()})
        .setCancelable(false)
        .show()
}
// Display release info.  If latestOnly is true show only the most recent.
fun Activity.displayReleaseInfo(latestOnly: Boolean) : Boolean {
    // release info files are in assets/releaseInfo and should be
    // Info_nn.html where nn is the number of the major release.
    val releaseInfo: String = try {
        val infoDir = "release_info"
        val sb = StringBuffer()
        val infoFiles = assets.list(infoDir)!!
            .toList()
            .map { "$infoDir/$it" }
            .reversed()
        latestOnly@ for (f in infoFiles) {
            sb.append(this.assetFileLines(f))
            if (latestOnly) break@latestOnly
        }
        sb.toString()
    }
    catch (e: Exception) {
        Log.d("displayReleaseInfo", "Exception:${e.message}")
        ""
    }
    val htmlOut = getString(R.string.KT_html_body,
            getString(R.string.KT_release_info_font_px),
            getString(R.string.KT_html_bg),
            getString(R.string.KT_html_text_color),
            releaseInfo)

    val webView = WebView(this)
    webView.loadData(htmlOut, "text/html", "utf-8")
    webViewAlert(webView, "Release Info")
    return true
}
fun doubleClickToExit(activity: Activity) {
    val now = System.currentTimeMillis()
    if (now - clickTimer < DOUBLE_CLICK_TIMEOUT) {
        kanaUtilsToast!!.cancel()
        if (Build.VERSION.SDK_INT >= 21) {
            activity.finishAndRemoveTask()
        } else {
            activity.finish()
        }
    } else {
        clickTimer = now
        if (kanaUtilsToast != null) kanaUtilsToast!!.cancel()
        kanaUtilsToast = Toast.makeText(
                activity, R.string.KT_click_again, Toast.LENGTH_SHORT)
        kanaUtilsToast!!.show()
    }
}

private fun Activity.getInstallTimestamp(): Long {
    var rv = 0L
    try {
        val pm = this.packageManager
        val appInfo = pm.getApplicationInfo(BuildConfig.APPLICATION_ID, 0)
        rv = File(appInfo.sourceDir).lastModified()
    } catch (e: PackageManager.NameNotFoundException) {
        Log.d("getInstallTimestamp",
                "Exception while getting install timestamp:" +
                        e.message)
    }
    return rv
}


/*
 * Display the 'about' dialog for the app.  This will show apk build time
 * and install time and a link that will call google play or amazon page for
 * the app to get the user more info.
 * @param c  application context
 */
fun Activity.buildInfoDialog() : Boolean {
    val installTimestamp = getInstallTimestamp()
    val htmlOut = getString(R.string.KT_build_html,
        BuildConfig.VERSION_NAME +
            "-" + BuildConfig.BUILD_TYPE,
        SimpleDateFormat.getInstance().format(
            Date(BuildConfig.BUILD_TIMESTAMP)
        ),
        SimpleDateFormat.getInstance().format(
            Date(installTimestamp)
        )
    )
    val webView = WebView(this)
    webView.loadData(htmlOut, "text/html", "utf-8")
    this.webViewAlert(webView, "BuildInfo")
    return true
}
fun Activity.showAboutDialog() : Boolean{
    val webView = WebView(this)
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Log.i("WebView", "Attempting to load URL: $url")
            // if external link, start with a browser.  Should only hit
            // from the about google play link.
            if (url.startsWith("https://") || url.startsWith("http://")) {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
            else
                view.loadUrl(url)
            return true
        }
    }
    webView.loadUrl("file:///android_asset/www/about.html")
    webViewAlert(webView, "About...")
    return true
}
