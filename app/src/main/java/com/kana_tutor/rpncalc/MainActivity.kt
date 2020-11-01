@file:Suppress("unused", "unused", "unused", "ObjectPropertyName", "SetTextI18n", "LocalVariableName")

package com.kana_tutor.rpncalc

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Typeface
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
import java.util.*

import com.kana_tutor.rpncalc.RpnParser.*
import com.kana_tutor.rpncalc.kanautils.doubleClickToExit

class MainActivity : AppCompatActivity() {
    companion object {
        private lateinit var _sharedPreferences : SharedPreferences
        val sharedPreferences : SharedPreferences
            get() = _sharedPreferences
    }
    private lateinit var panelTextView: TextView
    private var shiftIsUp = true
    private var shiftLock = false
    private var angleIsDegrees = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        _sharedPreferences = getSharedPreferences(
                "user_prefs.txt", MODE_PRIVATE)
        angleIsDegrees = sharedPreferences.getBoolean("angleIsDegrees", true)
        panelTextView = findViewById(R.id.panelTextView)

        shift_key.setOnLongClickListener {
            Toast.makeText(this, "Long click detected", Toast.LENGTH_SHORT).show()
            shiftLock = !shiftLock
            shiftIsUp = !shiftLock
            setShiftKey(shiftIsUp)
            true
        }
    }

    private var rpnStack = Stack<RpnToken>()
    private var accumulator = ""

    private fun calculate(toCalculate: Stack<RpnToken>) : Stack<RpnToken> {
        val newStack =  RpnParser.rpnCalculate(toCalculate)
        panelTextView.text = newStack.joinToString("\n") { it.token } + "\n"
        accumulator = ""
        return newStack
    }
    private val shiftedButtonIds = arrayOf(R.id.sine_button,
        R.id.cosine_button, R.id.tangent_button, R.id.drop_button)
    private fun setShiftKey(isUp: Boolean) {
        fun Button.setButton(textIn: String, textColor: Int, tag: String = "") {
            text = textIn
            setTextColor(textColor)
            if (tag.isNotEmpty())
                this.tag = tag
        }
        val textColor = ContextCompat.getColor(
                this,
                if (isUp) android.R.color.white
                else R.color.shift_down_text
        )
        shift_key.setBackgroundColor(
                ContextCompat.getColor(
                        this,
                        if (isUp) R.color.operation_button
                        else R.color.shift_down_bg
                )
        )
        tangent_button.setTextColor(textColor)
        sine_button.setTextColor(textColor)
        cosine_button.setTextColor(textColor)
        drop_button.setTextColor(textColor)
        shift_key.setTextColor(textColor)

        if (isUp) {
            tangent_button.text = "TAN"
            sine_button.text = "SIN"
            cosine_button.text = "COS"
            drop_button.setButton("⇩", textColor, "DROP")
            drop_button.setTypeface(null, Typeface.NORMAL)
        }
        else {
            tangent_button.setButton("ATAN", textColor)
            sine_button.setButton("ASIN", textColor)
            cosine_button.setButton("ACOS", textColor)
            drop_button.setButton("π", textColor, "π")
            drop_button.setTypeface(null, Typeface.BOLD)
        }
    }
    @SuppressLint("SetTextI18n")
    fun btnOnClick(v: View) {
        @SuppressLint("SetTextI18n")
        fun panelTextAppend(str : String) : String {
            val t = panelTextView.text.toString()
            panelTextView.text = t + str
            return str
        }
        val buttonText =  if (v.tag != null) v.tag.toString()
            else (v as Button).text.toString()

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
                    if (!shiftLock) {
                        shiftIsUp = !shiftIsUp
                        setShiftKey(shiftIsUp)
                    }
                }
                "PI", "π" -> {
                    if (accumulator.isNotEmpty()) {
                        rpnStack.push(RpnToken(accumulator))
                        accumulator = ""
                    }
                    rpnStack.push(RpnToken(buttonText))
                    panelTextAppend(buttonText)
                }
                "CLR", "SWAP", "DROP" -> {
                    if (accumulator.isNotEmpty()) {
                        rpnStack.push(RpnToken(accumulator))
                        accumulator = ""
                    }
                    rpnStack.push(RpnToken(buttonText))
                    rpnStack = calculate(rpnStack)
                }
                "ENTR" -> {
                    if (accumulator.isNotEmpty())
                        rpnStack.push(RpnToken(accumulator))
                    rpnStack = calculate(rpnStack)
                }
                "DEL" -> {
                    if (accumulator.isNotEmpty())
                        accumulator.dropLast(1)
                }
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "." -> {
                    panelTextAppend(buttonText)
                    accumulator += buttonText
                }
                // change sign
                "CHS" -> {
                    rpnStack.push(RpnToken(buttonText))
                    rpnStack = calculate(rpnStack)
                }
                "+", "-", "×", "÷", "^" -> {
                    if (accumulator.isNotEmpty()) rpnStack.push(RpnToken(accumulator))
                    rpnStack.push(RpnToken(buttonText))
                    rpnStack = calculate(rpnStack)
                }
                "SIN", "ASIN", "COS", "ACOS", "TAN", "ATAN" -> {
                    if (accumulator.isNotEmpty()) rpnStack.push(RpnToken(accumulator))
                    rpnStack.push(RpnToken(if (angleIsDegrees) "DEG" else "RAD"))
                    rpnStack.push(RpnToken(buttonText))
                    rpnStack = calculate(rpnStack)
                }
                else -> Log.d("btnOnClick", "$buttonText ignored")
            }
            // if this a shifted key and shift is down and shift lock is off,
            // turn shift off.
            if (!shiftLock && !shiftIsUp && shiftedButtonIds.contains(v.id)) {
                shiftIsUp = true
                setShiftKey(shiftIsUp)
            }
        }
        catch (e: Exception) {
            Toast.makeText(this, "Error: $e", Toast.LENGTH_LONG).show()
        }
    }
        // Default menu.  Unless a class implements its own onCreateOptionsMenu
    // method, it gets menu items defined in the menu/base_activity
    override fun onCreateOptionsMenu(menu: Menu) :Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
            title = getString(R.string.app_label)
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
        val curVersion: Int = sharedPreferences.getInt(CURRENT_VERSION, 0)
        Log.d("showUpdateReleaseInfo",
                java.lang.String.format(
                        "current:%s, new:%s",
                        curVersion, BuildConfig.VERSION_CODE)
        )
        if (curVersion != BuildConfig.VERSION_CODE) {
            displayReleaseInfo(true)
            sharedPreferences.edit()
                    .putInt(CURRENT_VERSION, BuildConfig.VERSION_CODE)
                    .apply()
        }
    }
    override fun onBackPressed() {
        doubleClickToExit(this)
    }
}
