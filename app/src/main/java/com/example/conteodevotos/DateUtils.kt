import java.text.SimpleDateFormat
import java.util.*

fun String.formatServerDateToLocalTime(): String {
    return try {
        // Parsear la fecha del servidor (formato ISO)
        val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        utcFormat.timeZone = TimeZone.getTimeZone("UTC") // Indicar que la fecha viene en UTC
        val date = utcFormat.parse(this)

        // Formatear a hora local (GMT-6)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        timeFormat.timeZone = TimeZone.getTimeZone("GMT-6") // Zona horaria deseada

        date?.let { timeFormat.format(it) } ?: this
    } catch (e: Exception) {
        e.printStackTrace()
        this // Si falla, devolver el string original
    }
}