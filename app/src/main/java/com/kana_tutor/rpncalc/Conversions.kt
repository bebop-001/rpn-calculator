package com.kana_tutor.rpncalc

class Conversions private constructor(){
    open class Unit (        // grams, farenheit or celcius for exmple
        val name:String,
            // for completeness.
        val description:String,
            // mtr or ft for example.
        val tag:String,
            // gram for weight or cmtr for example
    )
    class UnitInstance(
        name:String,
        description:String,
        tag:String,
        // gram for weight or cmtr for example
        val baseTag:String,
        // this contains string RPN code to convert to/from this instance.
        // conversion is accomplished by "Chaining" conversions, so
        // the conversion from feet to nextInstance will be
        // ft -> cmtr, cmtr -> in
        val convertTo:String,
        val convertFrom:String,
        // list link.
        var nextInstance : UnitInstance? = null
    ) : Unit(name, description, tag)
    class UnitType (
        name:String,
        description:String,
        tag:String,
        var nextInstance : UnitInstance? = null,
        var nextUnit : UnitType? = null,
    ) : Unit(name, description, tag)
    fun UnitType.append(newUnit:UnitType) : UnitType {
        var unit = this
        while(unit.nextUnit != null)
            unit = unit.nextUnit!!
        unit.nextUnit = newUnit
        return newUnit
    }
    fun UnitType.appendInstance(name: String, newInstance:UnitInstance) : Boolean {
        var curType : UnitType? = this
        while (curType != null && curType.name != name)
            curType = curType.nextUnit
        if (curType != null) {
            if (curType.nextInstance == null)
                curType.nextInstance = newInstance
            else {
                var instance: UnitInstance = curType.nextInstance!!
                while (instance.nextInstance != null)
                    instance = instance.nextInstance!!
                instance.nextInstance = newInstance
            }
        }
        return curType != null
    }
    val root : UnitType
    init{
        root = UnitType("Temperature", "", "temp")
        root.appendInstance("Temperature", UnitInstance("Centegrade", "", "°C",
                "°C", "1 *", "1 *"))
        root.appendInstance("Temperature", UnitInstance("Kelvin", "", "°K",
                "°C", "273.15 +", "253.15 -"))
        root.appendInstance("Temperature",  UnitInstance("Fahrenheit", "", "°F",
        "°C", "32 - 5 * 9 /", "5 / 9 * 32 +"))
        root.append(UnitType("Length", "", "dst"))
        root.appendInstance("Length", UnitInstance("Foot", "", "ft",
            "mtr", "0.348 /", "0.348 *"))
        root.appendInstance("Length", UnitInstance("Inch", "", "in",
            "mtr", "0.0254 /", "0.0254 *"))
        root.appendInstance("Length", UnitInstance("Yard", "", "yd",
            "mtr", "0.9144 /", "0.9144 *"))
        root.appendInstance("Length", UnitInstance("Mile", "", "mi",
            "mtr", "1609.34 /", "1609.34 *"))
        root.appendInstance("Length", UnitInstance("Nautical Mile", "", "Nmi",
            "mtr", "1852 /", "1852 *"))
        root.appendInstance("Length", UnitInstance("Astronomical Unit", "", "AU",
            "mtr", "1.496E11 /", "1.496E11 *"))
        root.appendInstance("Length", UnitInstance("Furlong (US)", "", "fur",
            "mtr", "201.168 /", "201.168 *"))
        root.appendInstance("Length", UnitInstance("Centimeter", "", "cmtr",
            "mtr", "1E2 /", "1E2 *"))
        root.appendInstance("Length", UnitInstance("Kilometer", "", "kmtr",
            "mtr", "1E3 *", "1E3 /"))
        root.appendInstance("Length", UnitInstance("Millimeter", "", "mmtr",
            "mtr", "1E-3 *", "1E3 *"))
        root.appendInstance("Length", UnitInstance("Meter", "", "mtr",
            "mtr", "1 *", "1 *"))
}

    companion object {
        fun traverse(unitNode : UnitType) : Map<String, Pair<UnitType, Map<String, UnitInstance>>> {
            fun trav(node:UnitType) : Map<String, UnitInstance> {
                val rv = mutableMapOf<String, UnitInstance>()
                var instance = node.nextInstance
                while (instance != null) {
                    rv[instance.name] = instance
                    instance = instance.nextInstance
                }
                return rv
            }
            val rv = mutableMapOf<String, Pair<UnitType, Map<String, UnitInstance>>>()
            var un : UnitType? = unitNode
            while (un != null) {
                rv[un.name] = Pair(un, trav(un))
                un = un.nextUnit
            }
            return rv
        }
        private val myInstance = Conversions()
        val conversionsMap = traverse(myInstance.root)
        fun getInstance(): Conversions {
            return myInstance
        }
    }
}
