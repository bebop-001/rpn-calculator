@file:Suppress("unused", "unused", "unused", "ObjectPropertyName", "SetTextI18n", "LocalVariableName", "PrivatePropertyName")

package com.kana_tutor.rpncalc

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.*
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.kana_tutor.rpncalc.kanautils.*

import com.kana_tutor.rpncalc.RpnStack.Companion.toRpnStack

private val blue_text_default       = 0xff00acc1.toInt()
private val disabled_text_default   = 0xffae9696.toInt()
private val green_text_default      = 0xff7bfd35.toInt()
private val orange_text_default     = 0xffffbb33.toInt()
private val red_text_default        = 0xfff4511e.toInt()
private val white_text_default      = 0xffffffff.toInt()
private val number_bg               = 0xff434343.toInt()
private val operation_bg            = 0xff636363.toInt()
private val shift_down_bg           = 0xfF7e7d7d.toInt()

private val stateEnableDisable = arrayOf(
        intArrayOf(android.R.attr.state_enabled),  // Enabled
        intArrayOf(-android.R.attr.state_enabled)  // Disabled
)
private val red_text_color = ColorStateList(stateEnableDisable,
        intArrayOf(red_text_default, disabled_text_default)
)
private val white_text_color = ColorStateList(stateEnableDisable,
        intArrayOf(white_text_default, disabled_text_default)
)
private val orange_text_color = ColorStateList(stateEnableDisable,
        intArrayOf(orange_text_default, disabled_text_default)
)
private val green_text_color = ColorStateList(stateEnableDisable,
        intArrayOf(green_text_default, disabled_text_default)
)
private val blue_text_color = ColorStateList(stateEnableDisable,
        intArrayOf(blue_text_default, disabled_text_default)
)
enum class RpnColor(val color:ColorStateList) {
    white(white_text_color), red(red_text_color),
    green(green_text_color), blue(blue_text_color),
    orange(orange_text_color)
}

class MainActivity : AppCompatActivity(){
    companion object {
        private lateinit var _sharedPreferences: SharedPreferences
        val sharedPreferences: SharedPreferences
            get() = _sharedPreferences
    }

    private var angleIsDegrees = true
    private var numberFormattingEnabled = true
    private var menuNumberFormatString = R.string.number_formatting_enabled

    fun displayStoredValues() {
        /*
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

         */
    }
    private fun saveRegisters() {
        /*
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
            throw RuntimeException("saveRegisters write $registersFile FAILED")
        }

         */
    }
    private fun restoreRegisters() {
        /*
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

         */
    }

    private lateinit var panel_text_view : TextView

    private lateinit var panel_scroll   : ScrollView

    private lateinit var buttonMap     : Map<Int, Button>
    private lateinit var numberButtons  : List<Button>
    private lateinit var nonNumberButtons  : List<Button>

    private var rpnStack = RpnStack()
    private var accumulator = ""

    private data class RpnButton(
        val buttonKey : Int,
        val text:String,
        val rpnToken : String = text,
    )
    private class RpnButtons {
        val buttons = mutableListOf<RpnButton>()
        val textSize : Int
        val textColor : ColorStateList
        constructor(
            buttonKey:Int,
            text:String,
            token:String = text,
            textColor:ColorStateList = white_text_color,
            textSize: Int = 18,)
        {
            this.buttons.add(RpnButton(buttonKey, text, token))
            this.textColor = textColor
            this.textSize = textSize
        }
        constructor(
            buttons:List<RpnButton>,
            textColor:ColorStateList = white_text_color,
            textSize: Int = 18,)
        {
            this.buttons.addAll(buttons)
            this.textColor = textColor
            this.textSize = textSize
        }
    }
    private interface KbdChanges {
        var preCheck : (KbdState) -> Boolean
        var postCheck : (KbdState) -> Boolean
    }
    private enum class KbdState : KbdChanges {
        shiftUp {
            override var preCheck: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
            override var postCheck: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
        },
        shiftDown{
            override var preCheck: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
            override var postCheck: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
        },
        register{
            override var preCheck: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
            override var postCheck: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
        },
        stack{
            override var preCheck: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
            override var postCheck: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
        },
    }
    private var kbdState = KbdState.shiftUp
    private fun kdbStateInitialize() {
        KbdState.shiftUp.preCheck = fun (kbdState:KbdState) : Boolean {
            val del_clr_button = buttonMap[300]!!
            // if accumulator and stack are empty, disable all but number keys.
            nonNumberButtons.forEach{
                it.isEnabled = !(accumulator.isEmpty() && rpnStack.isEmpty())
                Log.d("loop", "${it.text}:${it.isEnabled}")
            }
            if (accumulator.isNotEmpty())
                del_clr_button.text = "DEL"
            else if (rpnStack.isNotEmpty())
                del_clr_button.text = "DROP"
            for (key in buttonMap.keys.sorted()) {
                Log.d("buttonsList", "$key:${buttonMap[key]!!.text}")
            }
            return true
        }
    }

    // get a list of lists of all buttons under the keyboard by
    // row x column. 0,0 is top left, rr[lastIndex][lastIndex]
    // bottom right.
    private fun getButtonViews () : Map<Int, Button> {
        var minor = 0
        val m = hashMapOf<Int, Button>()
        fun getViewChildren(vg: ViewGroup) {
            val l = arrayListOf<Button>()
            (0 until vg.childCount).forEach{i ->
                val v = vg.getChildAt(i)
                if (v is ViewGroup) getViewChildren(v)
                else l.add(v as Button)
            }
            // key is row/colum where 0 refers to top-right
            // button and 504 is bottom right.
            l.forEachIndexed{ index, button ->
                m[(index * 100) + minor] = button
            }
            minor++
        }
        getViewChildren(findViewById<LinearLayout>(R.id.keyboard_root))
        return m
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        _sharedPreferences = getSharedPreferences(
                "user_prefs.txt", MODE_PRIVATE)

        panel_text_view = findViewById(R.id.panel_text_view)
        panel_scroll   = findViewById(R.id.panel_scroll)

        // get a list of all the buttons from the root view
        // then establish our on-click listeners.
        val num = mutableListOf<Button>()
        val notNum = mutableListOf<Button>()
        val operators = mutableListOf<Button>()
        buttonMap = getButtonViews()
        buttonMap.keys.forEach{ key ->
            val button = buttonMap[key]!!
            button.setOnLongClickListener{
                // Play a click sound.
                (getSystemService(Context.AUDIO_SERVICE) as AudioManager)
                        .playSoundEffect(AudioManager.FX_KEY_CLICK)
                buttonClickHandler(it as Button, true)
                true
            }
            button.setOnClickListener{
                buttonClickHandler(it as Button, false)
            }
            if ("^[0-9]$".toRegex().matches(button.text.toString())) num.add(button)
            else notNum.add(button)
            if ("^[\\^+-×÷]$".toRegex().matches(button.text)) operators.add(button)
        }
        numberButtons = num
        nonNumberButtons = notNum

        kdbStateInitialize()
        KbdState.shiftUp.postCheck = KbdState.shiftUp.preCheck
    }

    override fun onResume() {
        super.onResume()
        angleIsDegrees = sharedPreferences.getBoolean("angleIsDegrees", true)
        findViewById<Button>(R.id.deg_rad_button).text = if (angleIsDegrees) "DEG" else "RAD"
        // restore the keyboard state.
        kbdState = KbdState.values()[
                sharedPreferences.getInt("kbdState", KbdState.shiftUp.ordinal)]
        kbdState.preCheck(kbdState)
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
        panel_text_view = findViewById(R.id.panel_text_view)

        // by default, only the number pad is enabled.
        numberPad.addAll(digitButtons)
        enableButtons(false)
        enableButtons(true, numberPad)

        restoreRegisters()
    }

    override fun onPause() {
        super.onPause()
        saveRegisters()
        sharedPreferences.edit()
                .putBoolean("angleIsDegrees", angleIsDegrees)
                .putInt("kbdState", kbdState.ordinal)
                .apply()
    }

    // used to return result of context menu operation
    // set in onCreateContextMenu, used/cleared in onContextItemSelected
    private var returnResult: ((String) -> Unit)? = null
    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear_item -> returnResult?.invoke("STACK\nCLR\n")
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

    private fun getStackOperation(view: View, toReturn: (String) -> Unit) {
        registerForContextMenu(view)
        openContextMenu(view)
        returnResult = toReturn // to return the result of the operation.
    }
    private fun updateDisplay() {
        panel_text_view.text = rpnStack.joinToString("\n")
        { it.token } + "\n" + accumulator
        panel_scroll.post { // scroll to the bottom of the screen.
            panel_scroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
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
        val (stack, error) = RpnParser.rpnCalculate(rpnStack)
        rpnStack = stack
        if (error.isNotEmpty())
            Toast.makeText(this,
                    "ERROR: $error", Toast.LENGTH_LONG)
                    .show()
        updateDisplay()
    }

    private val trigButtons = listOf(
        RpnButton(101, "SIN"), RpnButton(102, "COS"),
        RpnButton(103, "TAN"),
    )
    private val arcTrigButtons = listOf(
        RpnButton(101, "ASIN"), RpnButton(102, "ACOS"),
        RpnButton(103, "ATAN"),
    )
    private val operatorButtons = listOf(
        RpnButton(4, "^"), RpnButton(104, "÷"),
        RpnButton(204, "×"), RpnButton(304, "-"),
        RpnButton(404, "+"),
    )
    private val registerButtons = listOf(
        RpnButton(0, "REG"), RpnButton(1, "STO"),
        RpnButton(2, "RCL"), RpnButton(3, "CLR"),
    )
    private val stackButtons = listOf(
        RpnButton(0, "STK"), RpnButton(1, "DUP"),
        RpnButton(2, "SWP"), RpnButton(3, "DROP"),
        RpnButton(4, "CLR"),
    )
    private val digitButtons = listOf(
            RpnButton(201, "7"),
            RpnButton(202, "8"),
            RpnButton(203, "9"),
            RpnButton(301, "4"),
            RpnButton(302, "5"),
            RpnButton(303, "6"),
            RpnButton(401, "1"),
            RpnButton(402, "2"),
            RpnButton(403, "3"),
            RpnButton(502, "0"),
    )
    private var numberPad = mutableListOf (
            RpnButton(501, "."),
            RpnButton(503, "+/-", "CHS"),
    )

    private fun setKeyboardState(newState:KbdState) {
        val shiftUp = listOf(
            RpnButtons(0, "REG"),
            RpnButtons(trigButtons),
            RpnButtons(500,"⇳SHFT"),
        )
        val shiftDown = listOf(
            RpnButtons(0, "STK", textColor = red_text_color),
            RpnButtons(arcTrigButtons, textColor = red_text_color),
            RpnButtons(500, "⇳SHFT", textColor = red_text_color),
        )
        val regUp = listOf(
            RpnButtons(0, "REG"),
            RpnButtons(1, "EXP", textColor = orange_text_color),
            RpnButtons(2,"π", textColor = orange_text_color),
            RpnButtons(3, "DEL"),
            RpnButtons(operatorButtons, textSize = 26),
        )
        val regDown = listOf(
            RpnButtons(registerButtons, textColor = green_text_color),
            RpnButtons(operatorButtons, textColor = green_text_color,
                textSize = 26),
        )
        val stackUp = listOf(
            RpnButtons(0, "STK", textColor = red_text_color),
            RpnButtons(1, "EXP", textColor = orange_text_color),
            RpnButtons(2, "π", textColor = orange_text_color),
            RpnButtons(3, "DEL"),
            RpnButtons(4, "^", textSize = 26),
        )
        val stackDown = listOf(
            RpnButtons(stackButtons, textColor = blue_text_color),
        )


        val stateMap = hashMapOf<Pair<KbdState,KbdState>,List<RpnButtons>>(
            Pair(KbdState.shiftUp,KbdState.shiftDown) to shiftDown,
            Pair(KbdState.shiftDown,KbdState.shiftUp) to shiftUp,

            Pair(KbdState.shiftUp,KbdState.register) to regDown,
            Pair(KbdState.register,KbdState.shiftUp) to regUp,

            Pair(KbdState.shiftDown,KbdState.stack) to stackDown,
            Pair(KbdState.stack,KbdState.shiftDown) to stackUp,
        )

        val buttonInfo = stateMap[Pair(kbdState, newState)]
        kbdState = newState
        for (rpnButtons in buttonInfo!!) {
            for (rpnButton in rpnButtons.buttons) {
                val button = buttonMap[rpnButton.buttonKey]!!
                button.text = rpnButton.text
                button.setTextColor(rpnButtons.textColor)
                button.textSize = rpnButtons.textSize.toFloat()
            }
        }
    }
    // enable/disable buttons using list of buttons.  If no list
    // is supplied, enable/disable all buttons.
    private fun enableButtons(
        enabled:Boolean = true, buttons : List<RpnButton>? = null
    ) {
        if (buttons == null) {
            buttonMap.keys.forEach{buttonMap[it]!!.isEnabled = enabled}
        }
        else {
            buttons.forEach{
                val b = buttonMap[it.buttonKey]
                if (b != null)
                    b.isEnabled = enabled
                else Log.d(
                        "enableButtons",
                        "${it.buttonKey}:${it.text} bad button"
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun buttonClickHandler (button: Button, isLongClick:Boolean = false) {
        @SuppressLint("SetTextI18n")
        fun panelTextAppend(str: String): String {
            val t = panel_text_view.text.toString()
            panel_text_view.text = t + str
            panel_scroll.post { // scroll to the bottom of the screen.
                panel_scroll.fullScroll(ScrollView.FOCUS_DOWN)
            }
            return str
        }

        var buttonText = if (button.tag != null) button.tag.toString()
        else button.text.toString()
        Log.d("kbdState", "$kbdState")
        kbdState.preCheck(kbdState)
        when (buttonText) {
            "DEG" -> {
                angleIsDegrees = false
                button.setTextColor(shift_down_bg)
                button.setBackgroundColor(shift_down_bg)
                button.text = "RAD"
                sharedPreferences.edit()
                        .putBoolean("angleIsDegrees", angleIsDegrees)
                        .apply()
            }
            "RAD" -> {
                angleIsDegrees = true
                button.setTextColor(white_text_color)
                button.setBackgroundColor(operation_bg)
                button.text = "DEG"
                sharedPreferences.edit()
                        .putBoolean("angleIsDegrees", angleIsDegrees)
                        .apply()
            }
            "⇳SHFT" -> {
                setKeyboardState(
                    if (kbdState == KbdState.shiftUp) KbdState.shiftDown
                    else KbdState.shiftUp
                )
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
                setKeyboardState(
                    if(kbdState == KbdState.stack) KbdState.shiftDown
                    else KbdState.stack
                )
            }
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "E" -> {
                if (buttonText == "EXP") {
                    // unless the accumulator contains 1 or more digits
                    // and doesn't already contain an "E", ignore it.
                    if (!accumulator.contains("\\d+".toRegex())
                            || accumulator.contains("E"))
                        buttonText = ""
                }
                panelTextAppend(buttonText)
                accumulator += buttonText
            }
            // registers
            "REG" -> {
                setKeyboardState(
                    if(kbdState == KbdState.register) KbdState.shiftUp
                    else KbdState.register
                )
            }
            // change sign
            "CHS" -> {
                if (accumulator.isNotEmpty())
                    try {
                        // we get an exception if this isn't a valid number.
                        accumulator.toDouble()
                        accumulator =   if (accumulator.startsWith("-"))
                                            accumulator.removePrefix("-")
                                        else "-$accumulator"
                        updateDisplay()
                    }
                    catch (e:java.lang.Exception) {
                        // ignore.  We used toDouble to test for accumulator
                        // being a number.
                    }
                else calculate(buttonText)
            }
            "+", "-", "×", "÷", "^", "SWAP", "DUP", "ENTR", "STO", "RCL" -> {
                calculate(buttonText)
            }
            "DROP" -> {
                if (isLongClick) {
                    rpnStack.clear()
                    updateDisplay()
                }
                else calculate("DROP")
            }
            "DEL" -> {
                    accumulator =   if (isLongClick) ""
                                    else accumulator.dropLast(1)
                    updateDisplay()
            }
            "CLR" -> {
                calculate("\nREG\nCLR\n")
            }
            "SIN", "ASIN", "COS", "ACOS", "TAN", "ATAN" -> {
                val angleUnits = if (angleIsDegrees) "DEG" else "RAD"
                calculate("$angleUnits\n$buttonText")
            }
            else -> Log.d("btnOnClick", "$buttonText ignored")
        }
        kbdState.postCheck(kbdState)
    }

    // Default menu.  Unless a class implements its own onCreateOptionsMenu
    // method, it gets menu items defined in the menu/base_activity
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        title = getString(R.string.app_label)
        return true
    }
    //set the display format and update the display.
    private fun updateParserFormat(formatString:String) {
        val stk = "$formatString FORMAT STO".toRpnStack()
        stk.addAll(rpnStack)
        val(stack, error) = RpnParser.rpnCalculate(stk)
        if (error.isNotEmpty())
            Log.d("updateParserFormat", "error:$error")
        rpnStack = stack
        panel_text_view.text =
            rpnStack.joinToString("\n") { it.token } +
            "\n" + accumulator
        sharedPreferences.edit()
                .putString("rpnDigitFormat", formatString)
                .apply()
    }

    private fun numberFormatControl(item: MenuItem): Boolean {
        fun setNewFormat(digits: Int, commas: Boolean) {
            val formatString = "format:fixed:${if(commas) "on" else "off"}:$digits"
            sharedPreferences.edit()
                    .putBoolean("numberFormattingEnabled", true)
                    .putInt("digitsAfterDecimal", digits)
                    .putBoolean("commasEnabled", commas)
                    .putString("rpnDigitFormat", formatString)
                    .apply()
            numberFormattingEnabled = true
            updateParserFormat(formatString)
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
            val scroll_title = v.findViewById<TextView>(R.id.scroll_title)
            val commas_enabled = v.findViewById<CheckedTextView>(R.id.commas_enabled)
            val digits_after_decimal:SeekBar = v.findViewById(R.id.digits_after_decimal)
            digits_after_decimal.progress = RpnParser.digitsAfterDecimal
            val digitsFormat = getString(R.string.digits_after_decimal)
            scroll_title.text = digitsFormat.format(RpnParser.digitsAfterDecimal)
            // read digits.
            digits_after_decimal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    scroll_title.text = digitsFormat.format(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
            commas_enabled.isChecked = RpnParser.commasEnabled
            commas_enabled.setOnClickListener { view ->
                with(view as CheckedTextView) {
                    isChecked = !isChecked
                }
            }
            AlertDialog.Builder(this)
                    .setTitle(R.string.set_number_format)
                    .setView(v)
                    .setPositiveButton(R.string.done) { dialog, _ ->
                        setNewFormat(
                                digits_after_decimal.progress, commas_enabled.isChecked
                        )
                        dialog.dismiss()
                    }
                    .show()
        }
        else {
            updateParserFormat("format:off")
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
