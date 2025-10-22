package cl.alercelab.centrointegral.domain

data class Actividad(
 val id: String = "",
 val nombre: String = "",
 val descripcion: String = "",
 val tipo: String = "",
 val periodicidad: String = "",
 val cupo: Int? = null,
 val oferente: String? = null,
 val socioComunitario: String? = null,
 val beneficiarios: List<String> = emptyList(),
 val diasAvisoPrevio: Int = 0,
 val lugar: String = "",
 val fechaInicio: Long = 0,
 val fechaFin: Long = 0,
 val estado: String = "activa",
 val motivoCancelacion: String? = null
)
