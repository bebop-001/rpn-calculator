package com.kana_tutor.rpncalc

import java.lang.RuntimeException

object Conversions {
    open class Unit(
            // grams, farenheit or celcius for exmple
        val name: String,
            // for completeness.
        val description: String,
            // m or ft for example.
        val tag: String,
            // gram for weight or cm for example
    )

    class UnitInstance(
        val unitId:String,
        name: String,
        description: String,
        tag: String,
            // gram for weight or cm for example
        val base: String,
            // this contains string RPN code to convert to/from this instance.
            // conversion is accomplished by "Chaining" conversions, so
            // the conversion from feet to nextInstance will be
            // ft -> cm, cm -> in
        val toBase: String,
        val fromBase: String,
    ) : Unit(name, description, tag)

    class UnitType(
        name: String,
        description: String,
        base: String,
    ) : Unit(name, description, base)

    val conversions = mutableMapOf<String, Pair<UnitType, HashMap<String, UnitInstance>>>()
    fun listConversions() : List<String> {
        val rv = mutableListOf<String>()
        conversions.keys.forEach { typeKey ->
            val (unitType, instanceMap) = conversions[typeKey]!!
            val typeStr = "${unitType.name}:$typeKey"
            instanceMap.keys.forEach{instanceKey ->
                rv.add("$typeStr -> ${instanceMap[instanceKey]!!.name}:$instanceKey")
            }
        }
        return rv
    }
    fun getFromTo(type:String, fromUnits:String, toUnits:String) :Pair<String, String>?{
        var rv : Pair<String, String>? = null
        if (conversions[type] != null) {
           //  val p : Pair<UnitType, HashMap<String, UnitInstance>> = conversions[type]!!
            val (unitType, cvtMap) = conversions[type]!!
            if (cvtMap[fromUnits] != null && cvtMap[toUnits] != null)
                rv = Pair(cvtMap[fromUnits]!!.toBase, cvtMap[toUnits]!!.fromBase)
        }
        return rv
    }

    init {
        arrayOf<Any>(
            UnitType("Temperature", "", "temp"),
            UnitInstance("temp", "Centegrade",
                "°C", "C", "C", "1 *", "1 *"),
            UnitInstance("temp", "Kelvin",
                "°K", "K", "C", "273.15 +", "273.15 -"),
            UnitInstance("temp", "Fahrenheit",
                "°F", "F", "C", "5 / 9 * 32 +", "32 - 5 * 9 /"),

            UnitType("Distance", "", "dst"),
            UnitInstance("dst", "Foot",
				 "", "ft", "m", "0.3048 /", "0.3048 *"),
            UnitInstance("dst", "Inch",
				 "", "in", "m", "0.0254 /", "0.0254 *"),
            UnitInstance("dst", "Yard",
				 "", "yd", "m", "0.9144 /", "0.9144 *"),
            UnitInstance("dst", "Mile",
				 "", "mi", "m", "1609.344 /", "1609.344 *"),
            UnitInstance("dst", "Nautical Mile",
				 "", "Nmi", "m", "1852 /", "1852 *"),
            UnitInstance("dst", "Astronomical Unit",
                    "", "AU", "m", "149597871E3 /", "149597871E3 *"),
            UnitInstance("dst", "Light Years",
                    "", "LY", "m", "9.4607305e15 /", "9.4607305e15 *"),
            UnitInstance("dst", "Furlong (US)",
				 "", "fur", "m", "201.168 /", "201.168 *"),
            UnitInstance("dst", "Centimeter",
				 "", "cm", "m", "1E2 /", "1E2 *"),
            UnitInstance("dst", "Kilometer",
				 "", "km", "m", "1E3 *", "1E3 /"),
            UnitInstance("dst", "Millimeter",
				 "", "mm", "m", "1E-3 *", "1E3 *"),
            UnitInstance("dst", "Meter",
				 "", "m", "m", "1 *", "1 *"),
        ).forEach{
            if(it is UnitType) {
                conversions[it.tag] = Pair(it, hashMapOf<String, UnitInstance>())
            }
            else if (it is UnitInstance) {
                val (obj:UnitType,instanceMap:HashMap<String,UnitInstance>) = conversions[it.unitId]!!
                instanceMap[it.tag] = it
            }
            else throw RuntimeException(
                    "Conversions:init:${it::class.java.simpleName}:unexpected class.")
        }
    }
}
