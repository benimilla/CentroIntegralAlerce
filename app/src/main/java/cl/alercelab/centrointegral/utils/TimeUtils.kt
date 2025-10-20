package cl.alercelab.centrointegral.utils
import java.time.*
object TimeUtils {
  fun localDateTimeToEpochMillis(ldt: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long =
    ldt.atZone(zone).toInstant().toEpochMilli()
  fun dayBounds(date: LocalDate = LocalDate.now()): Pair<Long, Long> {
    val start = date.atStartOfDay()
    val end = start.plusDays(1)
    return localDateTimeToEpochMillis(start) to localDateTimeToEpochMillis(end)
  }
  fun hourString(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().toString().substring(0,5)
}
