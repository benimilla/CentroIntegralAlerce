package cl.alercelab.centrointegral.domain

data class Actividad(
 val id: String = "",
 val nombre: String? = null,
 val tipo: String? = null,
 val periodicidad: String? = null,            // <-- SE GUARDA COMO TEXTO
 val cupo: Int? = null,
 val oferente: String? = null,
 val socioComunitario: String? = null,
 val beneficiarios: List<String> = emptyList(),
 val diasAvisoPrevio: Int = 0,
 val adjuntos: List<String> = emptyList(),
 val estado: String = "vigente"
)


