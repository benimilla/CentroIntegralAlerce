package cl.alercelab.centrointegral.utils

import java.time.*

object TimeUtils {

  // ✅ Ya lo tenías: se mantiene tal cual
  fun localDateTimeToEpochMillis(
    ldt: LocalDateTime,
    zone: ZoneId = ZoneId.systemDefault()
  ): Long = ldt.atZone(zone).toInstant().toEpochMilli()

  // 🆕 Convertir epoch → LocalDateTime (útil para formateos)
  fun epochMillisToLocalDateTime(
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault()
  ): LocalDateTime = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()

  // ✅ Se mantiene, sin romper firmas
  fun dayBounds(date: LocalDate = LocalDate.now()): Pair<Long, Long> {
    val start = date.atStartOfDay()
    val end = start.plusDays(1)
    return localDateTimeToEpochMillis(start) to localDateTimeToEpochMillis(end)
  }

  // 🆕 Límites de la semana [Lunes 00:00, próximo Lunes 00:00)
  fun weekBounds(
    date: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
  ): Pair<Long, Long> {
    val start = date.with(DayOfWeek.MONDAY).atStartOfDay()
    val end = start.plusWeeks(1)
    return localDateTimeToEpochMillis(start, zone) to localDateTimeToEpochMillis(end, zone)
  }

  // 🆕 Límites del mes [1 del mes 00:00, 1 del siguiente mes 00:00)
  fun monthBounds(
    date: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
  ): Pair<Long, Long> {
    val start = date.withDayOfMonth(1).atStartOfDay()
    val end = start.plusMonths(1)
    return localDateTimeToEpochMillis(start, zone) to localDateTimeToEpochMillis(end, zone)
  }

  // 🆕 Límites del año [1 de enero 00:00, 1 de enero del siguiente año 00:00)
  fun yearBounds(
    date: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault()
  ): Pair<Long, Long> {
    val start = date.withDayOfYear(1).atStartOfDay()
    val end = start.plusYears(1)
    return localDateTimeToEpochMillis(start, zone) to localDateTimeToEpochMillis(end, zone)
  }

  // ✅ Mejorada: formatea HH:mm sin substring frágil
  fun hourString(
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault()
  ): String {
    val ldt = epochMillisToLocalDateTime(millis, zone)
    return "%02d:%02d".format(ldt.hour, ldt.minute)
  }

  // 🆕 Combina una fecha + "HH:mm" → epochMillis (útil para drag & drop / reschedule)
  fun combineToMillis(
    date: LocalDate,
    hhmm: String,
    zone: ZoneId = ZoneId.systemDefault()
  ): Long {
    val (h, m) = hhmm.split(":").map { it.toInt() }
    val ldt = date.atTime(h, m)
    return localDateTimeToEpochMillis(ldt, zone)
  }
}
