package cl.alercelab.centrointegral.calendar

data class WeekCell(
    val dayIndex: Int,   // 0..6 (L..D)
    val hour: String,    // "08:00"
    val titulo: String? = null,
    val lugar: String? = null,
    val citaId: String? = null
)
