package cl.alercelab.centrointegral.domain

data class Cita(
 val id: String = "",
 val actividadId: String = "",
 val fechaInicioMillis: Long = 0L,
 val fechaFinMillis: Long = 0L,
 val lugar: String = "",
 val motivo: String? = null
) {
 val duracionMin: Int
  get() = ((fechaFinMillis - fechaInicioMillis) / 60000).toInt()
}

