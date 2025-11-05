package cl.alercelab.centrointegral.calendar

data class WeekCell(
    val dayIndex: Int,   // 0..6 (L..D)
    val hour: String,    // "08:00"
    val titulo: String? = null,
    val lugar: String? = null,
    val citaId: String? = null
)

// Representa una celda en la vista semanal del calendario,
// indicando el día, hora y datos opcionales de una cita (título, lugar e identificador).