@file:Suppress("unused", "unused", "unused", "ObjectPropertyName", "SetTextI18n", "LocalVariableName", "PrivatePropertyName", "EnumEntryName", "FunctionName", "UNCHECKED_CAST")

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
import com.kana_tutor.rpncalc.RpnToken.Companion.toStorable
import java.io.File

private const val blue_text_default       = 0xff00acc1.toInt()
private const val disabled_text_default   = 0xffae9696.toInt()
private const val green_text_default      = 0xff7bfd35.toInt()
private const val orange_text_default     = 0xffffbb33.toInt()
private const val red_text_default        = 0xfff4511e.toInt()
private const val white_text_default      = 0xffffffff.toInt()
private const val number_bg               = 0xff434343.toInt()
private const val operation_bg            = 0xff636363.toInt()
private const val shift_down_bg           = 0xfF7e7d7d.toInt()

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

    private fun saveRegAndStack() {
        val (stack, errors) =
                RpnParser.rpnCalculate("REG ALL STORABLE".toRpnStack())
        if (errors.isNotEmpty())
            throw java.lang.RuntimeException(
                    "errors occured while restoring registers:$errors")
        var toStore = stack.map{it.toString()}.joinToString("\n")
        toStore += "\n" + rpnStack
                .map{it.toStorable(-1).toString()}
                .joinToString("\n")
        val registersDir = File("${getFilesDir()}/data")
        if (!registersDir.exists() && !registersDir.mkdir())
            throw java.lang.RuntimeException(
                    "saveRegAndStack: directory ${registersDir}\n" +
                            "doesn't exist and mkdir FAILED>")
        val registersFile = File(registersDir, "registers.txt")
        try { registersFile.writeText(toStore) }
        catch (e:java.lang.Exception) {
            throw RuntimeException("saveRegAndStack write $registersFile FAILED")
        }
    }
    private fun restoreRegAndStack() {
        val registersDir = File("${getFilesDir()}/data")
        val registersFile = File(registersDir, "registers.txt")
        if (!registersFile.exists())
            Log.d("restoreRegAndStack", "file $registersFile not found.")
        else {
            val registersAsString = registersFile.readText()
            val (stack, errors) = RpnParser.rpnCalculate(registersAsString.toRpnStack())
            if (errors.isNotEmpty())
                Log.d("restoreRegAndStack", "Errors:$errors")
            rpnStack = stack
        }
    }

    private lateinit var panel_text_view : TextView

    private lateinit var panel_scroll   : ScrollView

    private lateinit var buttonMap     : Map<Int, Button>

    private var rpnStack = RpnStack()
    private var accumulator = ""

    private data class RpnBtn(
        val buttonKey : Int,
        val text:String,
        val rpnToken : String = text,
    )
    private class RpnButtons {
        val buttons = mutableListOf<RpnBtn>()
        val textSize : Int
        val textColor : ColorStateList
        constructor(
            buttonKey:Int,
            text:String,
            token:String = text,
            textColor:ColorStateList = white_text_color,
            textSize: Int = 18,)
        {
            this.buttons.add(RpnBtn(buttonKey, text, token))
            this.textColor = textColor
            this.textSize = textSize
        }
        constructor(
            buttons:List<RpnBtn>,
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
        var setup : (KbdState) -> Boolean
    }
    private enum class KbdState : KbdChanges {
        shiftUp {
            override var preCheck: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
            override var postCheck: (KbdState) -> Boolean =
                fun  (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
            override var setup: (KbdState) -> Boolean =
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
            override var setup: (KbdState) -> Boolean =
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
            override var setup: (KbdState) -> Boolean =
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
            override var setup: (KbdState) -> Boolean =
                fun (state:KbdState):Boolean {
                    Log.d("KdbState:$state", "preCheck:Not implemented")
                    return false
                }
        },
    }
    // Initialize the KdbState enum methods here.  Needs to be here because some
    // the values aren't initialized until the KdbState instance happens.
    private fun kbdStateInitialize() {
        KbdState.shiftUp.setup = fun(state:KbdState) : Boolean {
            setButtons(rpnBtnMerge(shftUpBtns, shftBtn))
            setButtons(rpnBtnMerge(expBtn, piBtn), orange_text_color)
            return state.preCheck(state)
        }
        KbdState.shiftUp.preCheck = fun(_:KbdState) : Boolean {
            if (accumulator.isNotEmpty() || rpnStack.isNotEmpty()) {
                enableButtons()

                if (angleIsDegrees) setButton(delBtn)
                else setButton(radBtn)

                if (accumulator.isNotEmpty()) {
                    setButton(delBtn)
                    // block decimal if it's been pressed or you're in Exp mode.
                    val containsExp = accumulator.contains("[\\d.]E".toRegex())
                    val containsDecimal = accumulator.contains(".")
                    enableButton(!containsDecimal && !containsExp, decimalPtBtn)
                    enableButton(!containsExp, expBtn)
                }
                else {
                    enableButton(false, expBtn)
                    setButton(stkDropBtn)
                }
            }
            else {
                enableButtons(false)
                enableButtons(true, numberPad)
                enableButton(true, piBtn)
            }
            enableButton(
                    RpnParser.registers.size > 0 || accumulator.isNotEmpty(),
                    regBtn)
            enableButton(true, shftBtn)

            return true
        }
        KbdState.shiftUp.postCheck = KbdState.shiftUp.preCheck

        KbdState.shiftDown.setup = fun(state:KbdState) : Boolean {
            setButtons(shftDownBtns)
            setButtons(rpnBtnMerge(shftBtn, stkBtn, arcTrigBtns), red_text_color)
            setButtons(rpnBtnMerge(expBtn, piBtn), orange_text_color)
            return state.preCheck(state)
        }
        KbdState.shiftDown.preCheck = fun(_:KbdState) : Boolean {
            enableButtons(accumulator.isNotEmpty() || rpnStack.isNotEmpty())
            enableButtons(true, numberPad)
            enableButton(rpnStack.isNotEmpty(), stkBtn)
            if (angleIsDegrees) setButton(degBtn)
            else setButton(radBtn)
            if (accumulator.isNotEmpty()) {
                setButton(delBtn)
                // block decimal if it's been pressed or you're in Exp mode.
                val containsExp = accumulator.contains("[\\d.]E".toRegex())
                val containsDecimal = accumulator.contains(".")
                enableButton(!containsDecimal && !containsExp, decimalPtBtn)
                enableButton(!containsExp, expBtn)
            }
            else {
                enableButtons(false, rpnBtnMerge(expBtn))
                setButton(stkDropBtn)
            }
            enableButton(true, shftBtn)
            return true
        }
        KbdState.shiftDown.postCheck = KbdState.shiftDown.preCheck

        KbdState.register.setup = fun(state:KbdState) : Boolean {
            setButtons(rpnBtnMerge(operatorBtns,regBtns)
                    , green_text_color)
            return state.preCheck(state)
        }

        KbdState.register.preCheck = fun(_:KbdState) : Boolean {
            val regIsNotEmpty = RpnParser.registers.size > 0
            val accIsNotEmpty = accumulator.isNotEmpty()
            val stkIsNotEmpty = rpnStack.isNotEmpty()
            enableButtons(false, rpnBtnMerge(chsBtn, decimalPtBtn))
            enableButtons(accIsNotEmpty,
                rpnBtnMerge(regBtns, delBtn, regRclBtn))
            enableButton(regIsNotEmpty, regRclBtn)
            enableButton(accIsNotEmpty && stkIsNotEmpty,
                    regStoBtn)
            enableButtons(accIsNotEmpty && stkIsNotEmpty && regIsNotEmpty,
                    operatorBtns)
            enableButton(regIsNotEmpty || accIsNotEmpty,
                    regClrBtn)
            enableButtons(true, rpnBtnMerge(regBtn, digitBtns))
            enableButton(false, shftBtn)
            return true
        }
        KbdState.register.postCheck = KbdState.register.preCheck

        KbdState.stack.setup = fun(state:KbdState) : Boolean {
            enableButtons(false)
            setButtons(stkBtns, red_text_color)
            return state.preCheck(state)
        }

        KbdState.stack.preCheck = fun(_:KbdState) : Boolean {
            enableButtons(true, rpnBtnMerge(stkBtn, stkBtns))
            return true
        }
        KbdState.stack.postCheck = KbdState.stack.preCheck
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
            // key is row/column where 0 refers to top-right
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
        buttonMap = getButtonViews()
        buttonMap.keys.forEach{ key ->
            val button = buttonMap.getValue(key)
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
            button.tag = RpnBtn(key,
                    button.text.toString(),
                    (if (button.tag == null) button.text else button.tag) as String
            )
        }
        kbdState = KbdState.shiftUp
        kbdStateStack = mutableListOf(KbdState.shiftUp)
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
        panel_text_view = findViewById(R.id.panel_text_view)

        // by default, only the number pad is enabled.
        enableButtons(true, numberPad)

        restoreRegAndStack()
        // this where things get started.
        // restore the keyboard state.
        with (sharedPreferences) {
            accumulator = getString("accumulator", "")!!
            kbdState = KbdState.values()[
                    getInt("kbdState", KbdState.shiftUp.ordinal)]
            kbdStateStack = getString(
                "kbdStateStack", KbdState.shiftUp.ordinal.toString()
                )!!
                .split("\n")
                .filter{it.isNotEmpty()}
                .map{KbdState.values()[it.toInt()]}
                .toMutableList()
        }
        kbdStateInitialize()
        pushKbdState(kbdState)
        updateDisplay()
    }

    override fun onPause() {
        super.onPause()
        saveRegAndStack()
        kbdStateStack.clear()
        sharedPreferences.edit()
            .putBoolean("angleIsDegrees", angleIsDegrees)
            .putString("accumulator", accumulator)
            .putInt("kbdState", kbdState.ordinal)
            .putString("kbdStateStack",
                    kbdStateStack.joinToString("\n") { it.ordinal.toString() })
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
    private fun MutableList<RpnBtn>.addButtonsToList(
        vararg buttons : Any
    ) : MutableList<RpnBtn> {
        val rv = mutableListOf<RpnBtn>()
        rv.addAll(this)
        rv.addAll(rpnBtnMerge(buttons))
        return rv
    }
    private fun rpnBtnMerge(vararg buttons : Any) : List<RpnBtn> {
        val rv = mutableListOf<RpnBtn>()
        for (b in buttons) {
            when (b) {
                is RpnBtn -> rv.add(b)
                is List<*> -> {
                    if (b.filter{it !is RpnBtn}.isNotEmpty())
                        throw RuntimeException(
                                "rpnBtnMerge is only for RpnBtn or RpnButtonList")
                    rv.addAll(b as List<RpnBtn>)
                }
                else -> throw RuntimeException(
                    "rpnBtnMerge is only for RpnBtn or RpnButtonList")
            }
        }
        return rv
    }

    private val shftBtn = RpnBtn(500, "⇳SHFT")

    private val degBtn = RpnBtn(100, "DEG")
    private val radBtn = RpnBtn(100, "RAD")
    private val trigBtns = listOf(
        RpnBtn(101, "SIN"), RpnBtn(102, "COS"),
        RpnBtn(103, "TAN"),
    )
    private val arcTrigBtns = listOf(
        RpnBtn(101, "ASIN"), RpnBtn(102, "ACOS"),
        RpnBtn(103, "ATAN"),
    )
    private val piBtn = RpnBtn(2, "π")
    private val expBtn = RpnBtn(1, "EXP")
    private val operatorBtns = listOf(
        RpnBtn(4, "^"), RpnBtn(104, "÷"),
        RpnBtn(204, "×"), RpnBtn(304, "-"),
        RpnBtn(404, "+"),
    )
    private val regBtn  = RpnBtn(0, "REG")
    private val regStoBtn = RpnBtn(1, "STO")
    private val regClrBtn = RpnBtn(4, "CLR")
    private val regRclBtn =  RpnBtn(2, "RCL")

    private val regBtns = listOf(
        regBtn, regStoBtn, regClrBtn, regRclBtn
    )
    private val stkBtn = RpnBtn(0, "STK")
    private val stkClrBtn = RpnBtn(4, "CLR")
    private val stkDropBtn = RpnBtn(3, "DROP")
    private val stkBtns = listOf(
        RpnBtn(1, "DUP"),
        RpnBtn(2, "SWAP"), stkDropBtn,
        stkClrBtn,
    )
    private val digitBtns = listOf(
            RpnBtn(201, "7"),
            RpnBtn(202, "8"),
            RpnBtn(203, "9"),
            RpnBtn(301, "4"),
            RpnBtn(302, "5"),
            RpnBtn(303, "6"),
            RpnBtn(401, "1"),
            RpnBtn(402, "2"),
            RpnBtn(403, "3"),
            RpnBtn(502, "0"),
    )
    private val decimalPtBtn = RpnBtn(501, ".")
    private val chsBtn = RpnBtn(503, "+/-", "CHS")
    private val numberPad = rpnBtnMerge(decimalPtBtn, chsBtn, digitBtns)

    private var delBtn = RpnBtn(3, "DEL")
    private val shftUpBtns = rpnBtnMerge(
            regBtn, expBtn, piBtn, degBtn, trigBtns, operatorBtns,
            numberPad,
    )
    private val shftDownBtns = rpnBtnMerge(
            stkBtn, expBtn, piBtn, degBtn, arcTrigBtns, operatorBtns,
            numberPad,
    )
    private fun setButton(
        button: RpnBtn,
        textColor: ColorStateList = white_text_color,
        textSize:Int = 18) {
        val b = buttonMap.getValue(button.buttonKey)
        b.text = button.text
        b.tag = button
        b.setTextColor(textColor)
        b.textSize = textSize.toFloat()
    }
    private fun setButtons(buttons:List<RpnBtn>,
        textColor: ColorStateList = white_text_color,
        textSize:Int = 18) {
        buttons.forEach{setButton(it, textColor, textSize)}
    }
    // saved/restored by system.
    private lateinit var kbdState : KbdState
    private lateinit var kbdStateStack:MutableList<KbdState>
    private fun setKbdState(newState:KbdState) : KbdState {
        kbdState = newState
        newState.setup(kbdState)
        newState.preCheck(kbdState)
        updateDisplay()
        return newState
    }
    private fun pushKbdState(newState:KbdState) : KbdState {
        kbdStateStack.add(kbdState)
        return setKbdState(newState)
    }
    private fun popKbdState() : KbdState {
        val newState =  if (kbdStateStack.isNotEmpty())
                            kbdStateStack.removeLast()
                        else KbdState.shiftUp
        return setKbdState(newState)
    }
    // enable/disable buttons using list of buttons.  If no list
    // is supplied, enable/disable all buttons.
    private fun enableButton(enabled: Boolean = true, button: RpnBtn) {
        val b = buttonMap[button.buttonKey]
        if (b != null) {
            b.isEnabled = enabled
            b.tag = button
        }
        else Log.d(
                "enableButton",
                "${button.buttonKey}:${button.text} bad button"
        )
    }
    private fun enableButtons(
        enabled:Boolean = true, buttons : List<RpnBtn>? = null
    ) {
        if (buttons == null) {
            buttonMap.keys.forEach{
                buttonMap.getValue(it).isEnabled = enabled
            }
        }
        else buttons.forEach{enableButton(enabled, it)}
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

        kbdState.preCheck(kbdState)
        var buttonText = (button.tag as RpnBtn).rpnToken
        Log.d("kbdState", "$kbdState")
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
                pushKbdState(
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
                pushKbdState(
                        if (kbdState == KbdState.stack) KbdState.shiftDown
                        else KbdState.stack
                )
            }
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "EXP" -> {
                val containsExp = accumulator.contains("[\\d.]E".toRegex())
                val containsDecimal = accumulator.contains(".")
                enableButton(!containsDecimal && !containsExp, decimalPtBtn)
                enableButton(false, delBtn)
                if (buttonText == "EXP" && containsExp) buttonText = ""
                if (containsDecimal && buttonText == ".") buttonText = ""
                if (buttonText == "EXP") buttonText = "E"

                panelTextAppend(buttonText)
                accumulator += buttonText
            }
            // registers
            "REG" -> {
                if (kbdState != KbdState.register)
                    pushKbdState(KbdState.register)
                else popKbdState()
            }
            // change sign
            "CHS" -> {
                if (accumulator.isNotEmpty()) {
                    // if this is a normal number, change toggle the first char
                    // to/from "-"
                    if ("^-*\\d+(?:\\.\\d+)*$".toRegex().matches(accumulator)) {
                        accumulator = if (accumulator.startsWith("-"))
                            accumulator.drop(1)
                        else "-$accumulator"
                        updateDisplay()
                    }
                    else {
                        // if the number has an exponent, change the number on the
                        // exponent.  eg: 1.5E7 <=> 1.5E-7
                        val m = "^(-*\\d+(?:\\.\\d+)*E)(-*\\d+)$".toRegex().find(accumulator)
                        if (m != null) {
                            val pre = m.groupValues[1]
                            var post = m.groupValues[2]
                            post = if (post.startsWith("-"))
                                post.drop(1)
                            else "-$post"
                            accumulator = "$pre$post"
                            updateDisplay()
                        }
                    }
                }
            }
            "+", "-", "×", "÷", "^" -> {
                if (kbdState == KbdState.register) {
                    val a = accumulator
                    accumulator = ""
                    calculate("REG $a $buttonText")
                    popKbdState()
                }
                else calculate(buttonText)
            }
            "SWAP", "DUP" -> {
                calculate(buttonText)
                popKbdState()
            }
            "STO", "RCL", "CLR" -> {
                if (kbdState == KbdState.register) {
                    var a = accumulator
                    accumulator = ""
                    // for now, parser puts reg values on stack.
                    if (isLongClick &&
                            (buttonText == "RCL" || buttonText == "CLR")) {
                        a = "ALL"
                    }
                    calculate("REG $a $buttonText")
                }
                else if (kbdState == KbdState.stack) {
                    rpnStack.clear()
                    updateDisplay()
                }
                popKbdState()
            }
            "DROP" -> {
                if (kbdState == KbdState.stack) {
                    calculate("DROP")
                    popKbdState()
                }
                else {
                    if (isLongClick) {
                        rpnStack.clear()
                        updateDisplay()
                    }
                    else calculate("DROP")
                }
            }
            "DEL" -> {
                    accumulator =   if (isLongClick) ""
                                    else accumulator.dropLast(1)
                    updateDisplay()
            }
            "SIN", "ASIN", "COS", "ACOS", "TAN", "ATAN" -> {
                val angleUnits = if (angleIsDegrees) "DEG" else "RAD"
                calculate("$angleUnits\n$buttonText")
            }
            "ENTR" -> {
                calculate(buttonText)
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
