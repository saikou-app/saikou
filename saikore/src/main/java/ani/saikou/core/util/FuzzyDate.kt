package ani.saikou.core.util

import java.io.Serializable
import java.text.DateFormatSymbols
import java.util.*

data class FuzzyDate(
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
) : Serializable {
    companion object {
        val today by lazy {
            with(Calendar.getInstance()) {
                FuzzyDate(
                    get(Calendar.YEAR),
                    get(Calendar.MONTH) + 1,
                    get(Calendar.DAY_OF_MONTH)
                )
            }
        }
    }

    val variableString by lazy {
        buildString {
            append("{")
            if (year != null) append("year:$year")
            if (month != null) append(",month:$month")
            if (day != null) append(",day:$day")
            append("}")
        }
    }

    override fun toString(): String {
        val a = if (month != null) DateFormatSymbols().months[month - 1] else ""
        return (if (day != null) "$day " else "") + a + (if (year != null) ", $year" else "")
    }
}