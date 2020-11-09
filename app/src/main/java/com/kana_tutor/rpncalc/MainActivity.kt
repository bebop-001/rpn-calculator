@file:Suppress("unused", "unused", "unused", "ObjectPropertyName", "SetTextI18n", "LocalVariableName")

package com.kana_tutor.rpncalc

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.*
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.kana_tutor.rpncalc.RpnParser.Companion.toFormattedString
import com.kana_tutor.rpncalc.RpnParser.RpnToken
import com.kana_tutor.rpncalc.kanautils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.keyboard_layout.*
import kotlinx.android.synthetic.main.number_format.view.*
import java.io.File
import java.lang.RuntimeException
import java.util.*


class MainActivity : AppCompatActivity() , View.OnLongClickListener {
    companion object {
        private lateinit var _sharedPreferences: SharedPreferences
        val sharedPreferences: SharedPreferences
            get() = _sharedPreferences
    }

    private lateinit var panelTextView: TextView
    private var shiftIsUp = true
    private var angleIsDegrees = true
    private var numberFormattingEnabled = true
    private var menuNumberFormatString = R.string.number_formatting_enabled

    fun displayStoredValues() {
        val sortedKeys = RpnParser.registers.keys.sorted()
        val mess =
                if (sortedKeys.isEmpty()) "Empty"
                else sortedKeys
                    .map { "%2d) %s".format(
                        it, RpnParser.registers[it]!!.toFormattedString()
                    )}
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
    fun saveRegisters() {
        val asString = RpnParser.registers.map{ (key,value)->
            "$key ${value.toLongBitsString()}"}
                .joinToString("\n")
        val registersDir = File("${getFilesDir()}/registers")
        if (!registersDir.exists()) {
            try {registersDir.mkdir()}
            catch (e:Exception) {
                throw RuntimeException("saveRegisters:mkdir $registersDir FAILED:$e")
            }
        }
        val registersFile = File(registersDir, "registers.txt")
        try { registersFile.writeText(asString) }
        catch (e:java.lang.Exception) {
            RuntimeException("saveRegisters write $registersFile FAILD")
        }
    }
    fun restoreRegisters() {
        val registers = mutableMapOf<Int, Double>()
        val registersDir = File("${getFilesDir()}/registers")
        val registersFile = File(registersDir, "registers.txt")
        if (registersFile.exists()) {
            val registerData = registersFile.readText()
            registerData.split("\n").forEach{
                val(idx, longBits) = it.split("\\s+".toRegex())
                val value = longBits.longBitStringToDouble()
                registers[idx.toInt()] = value
            }
        }
        else {
            Log.d("restoreRegisters", "$registersFile not found")
        }
        RpnParser.registers = registers
    }
    private var shiftLock = false
    override fun onLongClick(v: View): Boolean {
        fun click() = (getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                .playSoundEffect(AudioManager.FX_KEY_CLICK)
        when (v.id) {
            R.id.shift_button -> {
                click()
                shiftLock = !shiftLock
                shiftIsUp = !shiftLock
                setShiftedButtons(shiftIsUp)
                return true
            }
            R.id.registers_button -> {
                click()
                displayStoredValues()
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        _sharedPreferences = getSharedPreferences(
                "user_prefs.txt", MODE_PRIVATE)

        shift_button.setOnLongClickListener(this)
        registers_button.setOnLongClickListener(this)
    }

    override fun onResume() {
        super.onResume()
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
        restoreRegisters()
    }

    override fun onPause() {
        super.onPause()
        saveRegisters()
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

    val buttonInfo = hashMapOf<Int,Pair<String,String>>(
            R.id.sine_button    to Pair("SIN", "ASIN"),
            R.id.cosine_button  to Pair("COS", "ACOS"),
            R.id.tangent_button to Pair("TAN", "ATAN"),
            R.id.shift_button   to Pair("⇳SHFT", "⇳SHFT"),
            R.id.sto_rcl_button to Pair("STO", "RCL"),
            R.id.del_clr_button to Pair("DEL", "CLR")
    )

    private fun setShiftedButton(isUp:Boolean, resId:Int) {
        val button = findViewById<Button>(resId)!!
        val textColor = ContextCompat.getColorStateList(
                this,
                if (isUp) R.color.white_text_color
                else R.color.red_text_color
        )!!
        val p = buttonInfo[resId]
        if (p == null)
            throw RuntimeException(
                "setShiftedButton: unrecognized resource id:${"0x%08x".format(resId)}"
            )
        val (buttonUp, buttonDown) = buttonInfo[resId]!!
        button.text = if(isUp) buttonUp else buttonDown
        button.setTextColor(textColor)
    }
    private fun setShiftedButtons(isUp: Boolean) {
        buttonInfo.map{setShiftedButton(isUp, it.key)}
    }
    val useRegisterKeys = arrayOf(R.id.pow_button, R.id.div_button,
            R.id.mult_button, R.id.plus_button, R.id.minus_button,
            R.id.registers_button, R.id.del_clr_button)
    private var useRegisterLock = false
    private fun useRegisterButton() {
        fun getAllChildren(v: View): ArrayList<View>? {
            if (v !is ViewGroup) {
                val viewArrayList = ArrayList<View>()
                viewArrayList.add(v)
                return viewArrayList
            }
            val result = ArrayList<View>()
            val vg = v
            for (i in 0 until vg.childCount) {
                val child = vg.getChildAt(i)
                val viewArrayList = ArrayList<View>()
                viewArrayList.add(v)
                viewArrayList.addAll(getAllChildren(child)!!)
                result.addAll(viewArrayList)
            }
            return result
        }
        // find all the buttons under our keyboard root.
        val keyboardRoot =
                this.findViewById<LinearLayout>(R.id.keyboard_root)
        val buttons  =
            getAllChildren(keyboardRoot)?.filter{it is Button} as List<Button>
        useRegisterLock = !useRegisterLock
        // enable/disable keys not used for the REG operator.
        for (b in buttons) {
            if (!useRegisterKeys.contains(b.id)) {
                val color = b.textColors
                b.isEnabled = !useRegisterLock
            }
        }
        // clear register in register mode.  clear/delete otherwise.
        if (useRegisterLock) {
            this.findViewById<Button>(R.id.del_clr_button).text = "CLR"
        }
        else setShiftedButton(shiftIsUp, R.id.del_clr_button)

        val buttonColor =
            if (useRegisterLock) R.color.green_text_color
            else R.color.white_text_color
        for (button in useRegisterKeys)
            findViewById<Button>(button)
                .setTextColor(
                        ContextCompat.getColorStateList(this, buttonColor))
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
        // If useRegisterLock is set and this isn't a math operation
        // key (eg:+-^...) ignore it.
        if (useRegisterLock && !useRegisterKeys.contains(v.id))
            return
        try {
            when (buttonText) {
                "DEG" -> {
                    angleIsDegrees = false
                    (v as Button).setTextColor(AppCompatResources
                            .getColorStateList(this, R.color.red_text_color))
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
                        setShiftedButtons(shiftIsUp)
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
                // retisters
                "REG" -> {
                    if (!useRegisterLock) {
                        calculate(buttonText)
                    }
                    useRegisterButton()
                }
                // change sign
                "CHS", "+", "-", "×", "÷", "^",
                "CLR", "SWAP", "DROP", "DUP",
                "ENTR", "STO", "RCL" -> {
                    if (v.id == R.id.del_clr_button) {
                        if (accumulator.isEmpty() && rpnStack.isNotEmpty()) {
                            calculate("DROP")
                        }
                        else if (panelTextView.text.toString().isNotEmpty()) {
                            accumulator = ""
                            var t = panelTextView.text.toString()
                            while (!t.endsWith("\n") && t.isNotEmpty())
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
            if (buttonInfo.containsKey(v.id) && !shiftIsUp && v.id != R.id.shift_button) {
                shiftIsUp = true
                setShiftedButtons(shiftIsUp)
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

private fun getAllChildren(v: View): ArrayList<View>? {
    if (v !is ViewGroup) {
        val viewArrayList = ArrayList<View>()
        viewArrayList.add(v)
        return viewArrayList
    }
    val result = ArrayList<View>()
    val vg = v
    for (i in 0 until vg.childCount) {
        val child = vg.getChildAt(i)
        val viewArrayList = ArrayList<View>()
        viewArrayList.add(v)
        viewArrayList.addAll(getAllChildren(child)!!)
        result.addAll(viewArrayList)
    }
    return result
}
