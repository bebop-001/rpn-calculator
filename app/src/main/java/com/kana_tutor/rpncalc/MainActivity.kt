@file:Suppress("unused", "unused", "unused", "ObjectPropertyName")

package com.kana_tutor.rpncalc

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kana_tutor.rpncalc.kanautils.buildInfoDialog
import com.kana_tutor.rpncalc.kanautils.displayReleaseInfo
import com.kana_tutor.rpncalc.kanautils.showAboutDialog
import kotlinx.android.synthetic.main.keyboard_layout.*

class MainActivity : AppCompatActivity() {
    companion object {
        private lateinit var _sharedPreferences : SharedPreferences
        val sharedPreferences : SharedPreferences
            get() = _sharedPreferences
    }
    private lateinit var panelTextView: TextView
    private var shiftIsUp = true
    private var angleIsDegrees = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        _sharedPreferences = getSharedPreferences(
                "user_prefs.txt", MODE_PRIVATE)
        shiftIsUp = sharedPreferences.getBoolean("shiftIsUp", true)
        angleIsDegrees = sharedPreferences.getBoolean("angleIsDegrees", true)
        panelTextView = findViewById(R.id.panelTextView)
    }
    private fun calculate(toCalculate: String) : String {
        return RpnParser.rpnCalculate(toCalculate)
    }

    @SuppressLint("SetTextI18n")
    fun btnOnClick(v: View) {
        fun Button.setButton(textIn: String, textColor: Int, tag: String = "") {
            text = textIn
            setTextColor(textColor)
            if (tag.isNotEmpty())
                this.tag = tag
        }
        val cls = v.javaClass.simpleName.toString()
        val buttonText : String
        buttonText = when (cls) {
            "AppCompatImageButton" -> v.tag.toString()
            "AppCompatButton" -> if (v.tag != null) v.tag.toString()
            else (v as Button).text.toString()
            else -> ""
        }

        try {
            when (buttonText) {
                "DEG" -> {
                    angleIsDegrees = false
                    (v as Button).setTextColor(ContextCompat.getColor(
                            this, R.color.shift_down_text))
                    v.setBackgroundColor(
                            ContextCompat.getColor(
                                    this, R.color.shift_down_bg))
                    v.text = "RAD"
                    sharedPreferences.edit()
                            .putBoolean("angleIsDegrees", angleIsDegrees)
                            .apply()
                }
                "RAD" -> {
                    angleIsDegrees = true
                    (v as Button).setTextColor(ContextCompat.getColor(
                            this, android.R.color.white))
                    v.setBackgroundColor(
                            ContextCompat.getColor(
                                    this, R.color.operation_button))
                    v.text = "DEG"
                    sharedPreferences.edit()
                            .putBoolean("angleIsDegrees", angleIsDegrees)
                            .apply()
                }
                "⇳SHFT" -> {
                    if (shiftIsUp) {
                        val textColor = ContextCompat.getColor(
                                this, R.color.shift_down_text)
                        shiftIsUp = false
                        sharedPreferences.edit()
                                .putBoolean("shiftIsUp", shiftIsUp)
                                .apply()

                        tangent_button.setButton("ATAN", textColor)
                        sine_button.setButton("ASIN", textColor)
                        cosine_button.setButton("ACOS", textColor)
                        drop_button.setButton("π", textColor, "PI")
                        (v as Button).setTextColor(textColor)

                        v.setBackgroundColor(
                                ContextCompat.getColor(
                                        this, R.color.shift_down_bg))
                    }
                    else {
                        shiftIsUp = true
                        sharedPreferences.edit()
                                .putBoolean("shiftIsUp", shiftIsUp)
                                .apply()
                        val textColor = ContextCompat.getColor(
                                this, android.R.color.white)
                        tangent_button.setButton("TAN", textColor)
                        sine_button.setButton("SIN", textColor)
                        cosine_button.setButton("COS", textColor)
                        drop_button.setButton("⇩", textColor, "DROP")

                        tangent_button.setTextColor(textColor)
                        sine_button.setTextColor(textColor)
                        cosine_button.setTextColor(textColor)
                        (v as Button).setTextColor(textColor)

                        tangent_button.text = "TAN"
                        sine_button.text = "SIN"
                        cosine_button.text = "COS"
                        v.setBackgroundColor(
                                ContextCompat.getColor(
                                        this, R.color.operation_button))
                    }
                }
                "PI" -> panelTextView.text = panelTextView.text.toString() + " PI\n"
                "CLR", "ENTR", "SWAP", "DROP" -> {
                    panelTextView.text = calculate(
                            panelTextView.text.toString() + " $buttonText")
                }
                "DEL" -> {
                    val l = panelTextView.text.toString()
                    if (!l.endsWith("\n") &&
                            "[\\d.-]$".toRegex().find(l) != null)
                        panelTextView.text = l.dropLast(1)
                }
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "." -> {
                    panelTextView.text = panelTextView.text.toString() + buttonText
                }
                // change sign
                "CHS" -> {
                    panelTextView.text = calculate(
                            panelTextView.text.toString() + " $buttonText ")
                }
                "+", "-", "×", "÷", "^" -> {
                    panelTextView.text = calculate(
                            panelTextView.text.toString() + " $buttonText ENTR")
                }
                "SIN", "ASIN", "COS", "ACOS", "TAN", "ATAN" -> {
                    val modeText = if (angleIsDegrees) "DEG" else "RAD"
                    panelTextView.text = calculate(
                            panelTextView.text.toString() + " $modeText $buttonText ENTR")
                }
                else -> Log.d("btnOnClick", "$buttonText ignored")
            }
        }
        catch (e: Exception) {
            Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()
        }
    }
        // Default menu.  Unless a class implements its own onCreateOptionsMenu
    // method, it gets menu items defined in the menu/base_activity
    override fun onCreateOptionsMenu(menu: Menu) :Boolean {
        getMenuInflater().inflate(R.menu.main_menu, menu)
        setTitle(getString(R.string.app_label))
        return true
    }
    private fun settingsDialog() : Boolean {
        return true
    }
    // Default menu handler.  As long as a menu item has an ID here, it
    // gets handled here.
    override fun onOptionsItemSelected(item: MenuItem) :Boolean {
        when (item.itemId) {
            R.id.get_app_settings -> return settingsDialog()
            R.id.build_info -> return buildInfoDialog()
            R.id.release_info_item -> return displayReleaseInfo(false)
            R.id.menu_about -> return showAboutDialog()
            else            -> {             // Currently nested menu items aren't caught in switch above
                // and show up here.
                Log.i("MainActivity", String.format(
                        "onOptionsItemSelected: unhandled id: 0x%08x", item.itemId)
                )
            }
        }
        return false
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Show the release info if this is an upgrade.
        val CURRENT_VERSION = "currentVersion"
        val curVersion: Int = com.kana_tutor.rpncalc.MainActivity.sharedPreferences.getInt(CURRENT_VERSION, 0)
        Log.d("showUpdateReleaseInfo",
                java.lang.String.format(
                        "current:%s, new:%s",
                        curVersion, BuildConfig.VERSION_CODE)
        )
        if (curVersion != BuildConfig.VERSION_CODE) {
            displayReleaseInfo(true)
            com.kana_tutor.rpncalc.MainActivity.sharedPreferences.edit()
                    .putInt(CURRENT_VERSION, BuildConfig.VERSION_CODE)
                    .apply()
        }
    }

}
