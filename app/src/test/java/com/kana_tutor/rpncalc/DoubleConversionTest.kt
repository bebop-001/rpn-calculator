import com.google.gson.Gson
import com.kana_tutor.rpncalc.kanautils.longBitsToDoubleOrNull
import com.kana_tutor.rpncalc.kanautils.toLongBitsString
import kotlin.random.Random.Default.nextDouble

fun Map<Int, Double>.toJsonString() : String{
    val stringMap = mutableMapOf<String, String>()
    this.forEach{
            (i,d)->stringMap .put(i.toString(), d.toLongBitsString())
    }
    return Gson().toJson(stringMap)
}
fun String.fromJsonString() : Map<Int, Double> {
    val rv = mutableMapOf<Int,String>()
    val x = this.split(",")




    return rv as Map<Int, Double>
}

fun main(args: Array<String>) {
    val double = 1.5
	val longBits = java.lang.Double.doubleToLongBits(double)
    val longBitsS = longBits.toString(16)
    val d1: Double = java.lang.Double.longBitsToDouble(longBitsS.toLong(16))
    println("$double, $longBits, $longBitsS, $d1, ${double - d1}")
    val double2 = 1/3.0
    val s1 = double2.toLongBitsString()
    println("$double2, $s1, ${s1.longBitsToDoubleOrNull()?.minus(double2)}")
    println("Empty string:${"".longBitsToDoubleOrNull()}")
    println("Nan:${(1/0.0).toLongBitsString()}")

    val hash = mutableMapOf<Int, Double>()
    (1..20).forEach{it -> hash[it] = nextDouble()}
    println(hash.toJsonString())
}
