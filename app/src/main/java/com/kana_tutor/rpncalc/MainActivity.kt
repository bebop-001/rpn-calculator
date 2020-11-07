@file:Suppress("unused", "unused", "unused", "ObjectPropertyName", "SetTextI18n", "LocalVariableName")

package com.kana_tutor.rpncalc

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kana_tutor.rpncalc.RpnParser.Companion.toFormattedString
import com.kana_tutor.rpncalc.RpnParser.RpnToken
import com.kana_tutor.rpncalc.kanautils.buildInfoDialog
import com.kana_tutor.rpncalc.kanautils.displayReleaseInfo
import com.kana_tutor.rpncalc.kanautils.doubleClickToExit
import com.kana_tutor.rpncalc.kanautils.showAboutDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.keyboard_layout.*
import kotlinx.android.synthetic.main.number_format.view.*
import java.util.*


class MainActivity : AppCompatActivity() , View.OnLongClickListener {
    companion object {
        private lateinit var _sharedPreferences: SharedPreferences
        val sharedPreferences: SharedPreferences
            get() = _sharedPreferences
    }

    private lateinit var panelTextView: TextView
    private var shiftIsUp = true
    private var shiftLock = false
    private var angleIsDegrees = true
    private var numberFormattingEnabled = true
    private var menuNumberFormatString = R.string.number_formatting_enabled

    fun displayStoredValues() {
        val sortedRegisters = RpnParser.getRigisters()
                .toList()
                .sortedBy { it.key }

        val mess =
                if (sortedRegisters.isEmpty()) "Empty"
                else sortedRegisters
                    .map { "%2d) %s".format(it.key, it.value.toFormattedString()) }
                    .joinToString("\n")
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.register_contents)
            .setNegativeButton(R.string.done, { d, i ->
                d.cancel()
            })
            .setPositiveButton(R.string.clear_registers, { d, i ->
                RpnParser.clearRegisters()
                d.cancel()
            })
            .setMessage(mess)
            .show()
        var tv  = dialog.findViewById<TextView>(android.R.id.message)!!
        tv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
    }
    override fun onLongClick(v: View): Boolean {
        Toast.makeText(this, "Long click detected",
                Toast.LENGTH_SHORT).show()
        when (v.id) {
            R.id.shift_button -> {
                shiftLock = !shiftLock
                shiftIsUp = !shiftLock
                setShiftKey(shiftIsUp)
                return true
            }
            R.id.registers_button -> displayStoredValues()
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        _sharedPreferences = getSharedPreferences(
                "user_prefs.txt", MODE_PRIVATE)
        angleIsDegrees = sharedPreferences.getBoolean("angleIsDegrees", true)
        numberFormattingEnabled =
                sharedPreferences.getBoolean("numberFormattingEnabled", true)
        RpnParser.setDigitsFormatting(
                numberFormattingEnabled,
                sharedPreferences.getInt("digitsAfterDecimal", 3),
                sharedPreferences.getBoolean("commasEnabled", true)
        )
        menuNumberFormatString =
                if (numberFormattingEnabled)
                    R.string.number_formatting_enabled
                else
                    R.string.number_formatting_disabled
        panelTextView = findViewById(R.id.panelTextView)

        shift_button.setOnLongClickListener(this)
        registers_button.setOnLongClickListener(this)
    }

    private var rpnStack = Stack<RpnToken>()
    private var accumulator = ""

    // used to return result of context menu operation
    // set in onCreateContextMenu, used/cleared in onContextItemSelected
    var returnResult: ((String) -> Unit)? = null
    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_item -> returnResult?.invoke("CLR\n")
            R.id.swap_item -> returnResult?.invoke("SWAP\n")
            R.id.drop_item -> returnResult?.invoke("DROP")
            R.id.duplicate_item -> returnResult?.invoke("DUP")
            else                -> return super.onContextItemSelected(item)
        }
        returnResult = null
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val buttonText =
                if (v.tag != null) v.tag.toString()
                else (v as Button).text.toString()
        if (buttonText == "STK") { // stack operation
            menuInflater.inflate(R.menu.stack_operations_menu, menu)
            menu.setHeaderTitle("Stack operations")
        }
        else {
            Log.d("onCreateContextMenu", "No text or tag")
        }
    }

    fun getStackOperation(view: View, toReturn: (String) -> Unit) {
        registerForContextMenu(view)
        openContextMenu(view)
        returnResult = toReturn // to return the result of the operation.
    }

    private fun calculate(text: String = "") {
        rpnStack.addAll("$accumulator\n$text"
                .split("\n")
                .filter { !"^\\s*#".toRegex().matches(it) }
                .joinToString("\n")
                .split("\\s+".toRegex())
                .filter { it.isNotEmpty() }
                .map { RpnToken(it) })

        accumulator = ""
        try {
            rpnStack = RpnParser.rpnCalculate(rpnStack)
        }
        catch (e: RpnParserException) {
            Toast.makeText(this,
                    "$e", Toast.LENGTH_LONG)
                    .show()
        }
        panelTextView.text = rpnStack.joinToString("\n") { it.token } + "\n"
        panel_scroll.post { // scroll to the bottom of the screen.
            panel_scroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    data class ButtonInfo(val id: Int, val button_up_txt: String, val button_down_txt: String)

    val buttonInfo = arrayOf(
            ButtonInfo(R.id.sine_button, "SIN", "ASIN"),
            ButtonInfo(R.id.cosine_button, "COS", "ACOS"),
            ButtonInfo(R.id.tangent_button, "TAN", "ATAN"),
            ButtonInfo(R.id.shift_button, "⇳SHFT", "⇳SHFT"),
            ButtonInfo(R.id.sto_rcl_button, "STO", "RCL"),
            ButtonInfo(R.id.del_clr_button, "DEL", "CLR")
    )

    private fun setShiftKey(isUp: Boolean) {
        val textColor = ContextCompat.getColor(
                this,
                if (isUp) android.R.color.white
                else R.color.shift_down_text
        )

        fun Button.setButton(textIn: String, textColor: Int, tag: String = "") {
            text = textIn
            setTextColor(textColor)
            if (tag.isNotEmpty())
                this.tag = tag
        }
        shift_button.setBackgroundColor(
                ContextCompat.getColor(
                        this,
                        if (isUp) R.color.operation_button
                        else R.color.shift_down_bg
                )
        )
        for (b in buttonInfo) {
            // use the resource id to find the button then
            // apply the appropriate text and color.
            val buttonTxt =
                    if (isUp) b.button_up_txt
                    else b.button_down_txt
            findViewById<Button>(b.id)!!.setButton(buttonTxt, textColor)
        }
    }

    @SuppressLint("SetTextI18n")
    fun btnOnClick(v: View) {
        @SuppressLint("SetTextI18n")
        fun panelTextAppend(str: String): String {
            val t = panelTextView.text.toString()
            panelTextView.text = t + str
            panel_scroll.post { // scroll to the bottom of the screen.
                panel_scroll.fullScroll(ScrollView.FOCUS_DOWN)
            }
            return str
        }

        var buttonText = if (v.tag != null) v.tag.toString()
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
                "STK" -> {
                    getStackOperation(v) { result ->
                        calculate(result)
                    }
                }
                "DEL" -> {
                    if (accumulator.isNotEmpty()) {
                        accumulator = accumulator.dropLast(1)
                        panelTextView.text =
                                panelTextView.text.toString().dropLast(1)
                    }
                }
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "E" -> {
                    if (buttonText == "E") {
                        // unless the accumulator contans 1 or more digits
                        // and doesn't already contain an "E", ignore it.
                        if (!accumulator.contains("\\d+".toRegex())
                                || accumulator.contains("E"))
                            buttonText = ""
                    }
                    panelTextAppend(buttonText)
                    accumulator += buttonText
                }
                // change sign
                "CHS", "+", "-", "×", "÷", "^",
                "CLR", "SWAP", "DROP", "DUP",
                "ENTR", "STO", "RCL", "REG" -> {
                    if (v.id == R.id.del_clr_button) {
                        if (accumulator.isEmpty() && rpnStack.isNotEmpty()) {
                            calculate("DROP")
                        }
                        else if (panelTextView.text.toString().isNotEmpty()) {
                            accumulator = ""
                            var t =  panelTextView.text.toString()
                            while(!t.endsWith("\n") && t.isNotEmpty())
                                t = t.dropLast(1)
                            panelTextView.text = t
                        }
                    }
                    else {
                        calculate(buttonText)
                    }
                }
                "SIN", "ASIN", "COS", "ACOS", "TAN", "ATAN" -> {
                    val angleUnits = if (angleIsDegrees) "DEG" else "RAD"
                    calculate("$angleUnits\n$buttonText")
                }
                else                                                       -> Log.d("btnOnClick", "$buttonText ignored")
            }
            // if this a shifted key and shift is down and shift lock is off,
            // turn shift off.
            if (!shiftLock && !shiftIsUp && R.id.shift_button != v.id &&
                    buttonInfo.map { it.id }.contains(v.id)) {
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
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        title = getString(R.string.app_label)
        return true
    }

    private fun numberFormatControl(item: MenuItem): Boolean {
        fun setNewFormat(digits: Int, commas: Boolean) {
            sharedPreferences.edit()
                    .putBoolean("numberFormattingEnabled", true)
                    .putInt("digitsAfterDecimal", digits)
                    .putBoolean("commasEnabled", commas)
                    .apply()
            numberFormattingEnabled = true
            RpnParser.setDigitsFormatting(true, digits, commas)
        }

        val newFormat =
                if (menuNumberFormatString == R.string.number_formatting_disabled)
                    R.string.number_formatting_enabled
                else
                    R.string.number_formatting_disabled
        menuNumberFormatString = newFormat
        item.setTitle(newFormat)
        if (newFormat == R.string.number_formatting_enabled) {
            val v = layoutInflater.inflate(R.layout.number_format, null)
            val sb = v.digits_after_decimal!!
            sb.progress = RpnParser.digitsAfterDecimal
            val digitsFormat = getString(R.string.digits_after_decimal)
            v.scroll_title.text = digitsFormat.format(RpnParser.digitsAfterDecimal)
            // read digits.
            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    v.scroll_title.text = digitsFormat.format(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
            val cb = v.commas_enabled!!
            cb.isChecked = RpnParser.commasEnabled
            cb.setOnClickListener { view ->
                with(view as CheckedTextView) {
                    isChecked = !isChecked
                }
            }
            AlertDialog.Builder(this)
                    .setTitle(R.string.set_number_format)
                    .setView(v)
                    .setPositiveButton(R.string.done) { dialog, _ ->
                        setNewFormat(
                                v.digits_after_decimal.progress, v.commas_enabled.isChecked
                        )
                        dialog.dismiss()
                    }
                    .show()
        }
        else {
            RpnParser.setDigitsFormatting(false)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu!!.findItem(R.id.number_formatting)!!.setTitle(menuNumberFormatString)
        return true
    }

    // Default menu handler.  As long as a menu item has an ID here, it
    // gets handled here.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.number_formatting -> return numberFormatControl(item)
            R.id.build_info -> return buildInfoDialog()
            R.id.release_info_item -> return displayReleaseInfo(false)
            R.id.menu_about -> return showAboutDialog()
            else                   -> {             // Currently nested menu items aren't caught in switch above
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
