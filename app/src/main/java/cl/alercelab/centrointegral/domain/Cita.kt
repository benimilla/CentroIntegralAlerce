package cl.alercelab.centrointegral.domain
import com.google.firebase.firestore.DocumentId
data class Cita(
 @DocumentId val id: String = "",
 val actividadId: String = "",
 val lugar: String = "",
 val fechaMillis: Long = 0L, // start time
 val duracionMin: Int = 60
)
