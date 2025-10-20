package cl.alercelab.centrointegral.data

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cl.alercelab.centrointegral.domain.*
import cl.alercelab.centrointegral.notifications.AlertWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.TimeUnit

class Repos(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    fun logout() = auth.signOut()
    fun currentUid(): String? = auth.currentUser?.uid

    // ------------------ AUTH ----------------------

    suspend fun login(email: String, pass: String): UserProfile? {
        auth.signInWithEmailAndPassword(email, pass).await()
        return currentUserProfile()
    }

    fun resetPassword(email: String) = auth.sendPasswordResetEmail(email)

    suspend fun register(nombre: String, email: String, pass: String, rol: String) {
        val cred = auth.createUserWithEmailAndPassword(email, pass).await()
        val uid = cred.user?.uid ?: return
        val perfil = UserProfile(
            uid = uid,
            nombre = nombre,
            email = email,
            rol = rol,
            estado = "pendiente"
        )
        db.collection("usuarios").document(uid).set(perfil).await()
    }

    suspend fun currentUserProfile(): UserProfile? {
        val uid = currentUid() ?: return null
        val snap = db.collection("usuarios").document(uid).get().await()

        if (!snap.exists()) {
            println("❌ DEBUG: No existe documento Firestore para UID=$uid")
            return null
        }

        val perfil = snap.toObject<UserProfile>()
        println("✅ DEBUG: Perfil Firestore cargado = $perfil")
        return perfil?.copy(uid = uid)
    }

    // ------------------ ADMIN ----------------------

    suspend fun listPendingUsers(): List<UserProfile> =
        db.collection("usuarios")
            .whereEqualTo("estado", "pendiente")
            .get().await()
            .toObjects()

    suspend fun approveUser(uid: String) {
        db.collection("usuarios").document(uid).update("estado", "activo").await()
    }

    suspend fun rejectUser(uid: String) {
        db.collection("usuarios").document(uid).update("estado", "rechazado").await()
    }

    // ------------------ ACTIVIDADES / CITAS ----------------------

    suspend fun crearActividadConCitas(
        base: Actividad,
        slots: List<Triple<Long, String, Int>>,
        contextForAlerts: Context? = null
    ): String {
        val ref = db.collection("actividades").document()
        val actividadId = ref.id
        ref.set(base.copy(id = actividadId)).await()

        for ((fecha, lugar, duracion) in slots) {
            val conflicto = checkConflicts(lugar, fecha, duracion, null)
            if (conflicto) throw IllegalStateException("Conflicto de horario en $lugar")

            val citaRef = db.collection("citas").document()
            val cita = Cita(
                id = citaRef.id,
                actividadId = actividadId,
                lugar = lugar,
                fechaMillis = fecha,
                duracionMin = duracion
            )
            citaRef.set(cita).await()

            val diasAviso = base.diasAvisoPrevio
            if (contextForAlerts != null && diasAviso > 0) {
                val trigger = fecha - diasAviso * 24L * 60L * 60L * 1000L
                scheduleAlert(contextForAlerts, actividadId, cita.id, trigger)
            }
        }
        return actividadId
    }

    suspend fun modificarActividad(actividad: Actividad) {
        db.collection("actividades").document(actividad.id).set(actividad).await()
    }

    suspend fun cancelarActividad(actividadId: String, motivo: String?) {
        db.collection("actividades").document(actividadId)
            .update("estado", "cancelada").await()

        val snaps = db.collection("citas")
            .whereEqualTo("actividadId", actividadId)
            .get().await()

        snaps.documents.forEach { doc ->
            val c = doc.toObject<Cita>() ?: return@forEach
            db.collection("citas").document(c.id).update(
                mapOf(
                    "estado" to "cancelada",
                    "motivoCambio" to (motivo ?: "")
                )
            ).await()
        }
    }

    suspend fun reagendarCita(
        citaId: String,
        nuevoInicio: Long,
        nuevoLugar: String,
        motivo: String?
    ): Boolean {
        val ok = updateCitaTiempoYLugar(citaId, nuevoInicio, nuevoLugar)
        if (ok) {
            db.collection("citas").document(citaId).update(
                mapOf(
                    "estado" to "reagendada",
                    "motivoCambio" to (motivo ?: "")
                )
            ).await()
        }
        return ok
    }

    suspend fun actividadesEnRango(
        start: Long,
        end: Long,
        tipo: String?,
        query: String?,
        onlyMine: Boolean,
        rol: String,
        uid: String
    ): List<Pair<Cita, Actividad?>> {

        val base = db.collection("citas")
            .whereGreaterThanOrEqualTo("fechaMillis", start)
            .whereLessThan("fechaMillis", end)
            .get().await()
            .toObjects<Cita>()

        val citasFiltradas = mutableListOf<Cita>()

        for (c in base) {
            val actSnap = db.collection("actividades").document(c.actividadId).get().await()
            val act = actSnap.toObject<Actividad>()?.copy(id = actSnap.id)

            val visible = when (rol) {
                "admin", "gestor" -> true
                else -> act?.beneficiarios?.contains(uid) == true
            }

            val byTipo = tipo?.let { act?.tipo.equals(it, ignoreCase = true) } ?: true
            val byText = query?.takeIf { it.isNotBlank() }
                ?.let { act?.nombre?.contains(it, true) == true } ?: true

            val mine = if (onlyMine) {
                act?.beneficiarios?.contains(uid) == true ||
                        act?.oferente == uid || act?.socioComunitario == uid
            } else true

            if (visible && byTipo && byText && mine) citasFiltradas += c
        }

        val results = mutableListOf<Pair<Cita, Actividad?>>()
        for (c in citasFiltradas) {
            val act = db.collection("actividades").document(c.actividadId)
                .get().await()
                .toObject<Actividad>()
            results += c to act
        }
        return results
    }

    suspend fun checkConflicts(
        lugar: String,
        startMillis: Long,
        duracionMin: Int,
        excludeCitaId: String? = null
    ): Boolean {
        val end = startMillis + duracionMin * 60_000L
        val snaps = db.collection("citas")
            .whereEqualTo("lugar", lugar)
            .whereGreaterThanOrEqualTo("fechaMillis", startMillis - 6 * 60 * 60 * 1000L)
            .get().await()

        for (doc in snaps.documents) {
            if (excludeCitaId != null && doc.id == excludeCitaId) continue
            val c = doc.toObject<Cita>() ?: continue
            val cEnd = c.fechaMillis + c.duracionMin * 60_000L
            if (startMillis < cEnd && end > c.fechaMillis) return true
        }
        return false
    }

    suspend fun updateCitaTiempoYLugar(
        citaId: String,
        nuevoInicio: Long,
        nuevoLugar: String
    ): Boolean {
        val citaRef = db.collection("citas").document(citaId)
        val cita = citaRef.get().await().toObject<Cita>() ?: return false
        val conflict = checkConflicts(nuevoLugar, nuevoInicio, cita.duracionMin, citaId)
        if (conflict) return false

        citaRef.update(
            mapOf(
                "fechaMillis" to nuevoInicio,
                "lugar" to nuevoLugar,
                "estado" to "vigente"
            )
        ).await()
        return true
    }

    // ------------------ ADJUNTOS ----------------------

    suspend fun adjuntarArchivoActividad(
        actividadId: String,
        bytes: ByteArray,
        mime: String = "application/octet-stream"
    ): String {
        // 1) Subir a Storage
        val path = "adjuntos/$actividadId/${java.util.UUID.randomUUID()}"
        val ref = storage.reference.child(path)
        ref.putBytes(
            bytes,
            com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(mime)
                .build()
        ).await()
        val url = ref.downloadUrl.await().toString()

        // 2) Leer lista existente de forma segura (puede no existir / ser null)
        val docRef = db.collection("actividades").document(actividadId)
        val snap = docRef.get().await()
        val current = (snap.get("adjuntos") as? List<*>)            // puede venir como Any
            ?.filterIsInstance<String>()                            // nos quedamos con Strings válidos
            ?: emptyList()

        // 3) Escribir nueva lista
        val nuevaLista = current + url
        docRef.update("adjuntos", nuevaLista).await()

        return url
    }


    // ------------------ MANTENEDORES ----------------------

    suspend fun listTiposActividad(): List<TipoActividad> =
        db.collection("tiposActividad").get().await().toObjects()

    suspend fun upsertTipoActividad(item: TipoActividad) {
        db.collection("tiposActividad").document(item.nombre).set(item).await()
    }

    suspend fun deleteTipoActividad(id: String) {
        db.collection("tiposActividad").document(id).delete().await()
    }

    suspend fun listLugares(): List<Lugar> =
        db.collection("lugares").get().await().toObjects()

    suspend fun upsertLugar(item: Lugar) {
        val id = if (item.id.isBlank()) item.nombre else item.id
        db.collection("lugares").document(id).set(item.copy(id = id)).await()
    }

    suspend fun deleteLugar(id: String) {
        db.collection("lugares").document(id).delete().await()
    }

    suspend fun listOferentes(): List<Oferente> =
        db.collection("oferentes").get().await().toObjects()

    suspend fun upsertOferente(item: Oferente) {
        val id = if (item.id.isBlank()) item.nombre else item.id
        db.collection("oferentes").document(id).set(item.copy(id = id)).await()
    }

    suspend fun deleteOferente(id: String) {
        db.collection("oferentes").document(id).delete().await()
    }

    suspend fun listSocios(): List<SocioComunitario> =
        db.collection("socios").get().await().toObjects()

    suspend fun upsertSocio(item: SocioComunitario) {
        val id = if (item.id.isBlank()) item.nombre else item.id
        db.collection("socios").document(id).set(item.copy(id = id)).await()
    }

    suspend fun deleteSocio(id: String) {
        db.collection("socios").document(id).delete().await()
    }

    suspend fun listProyectos(): List<Proyecto> =
        db.collection("proyectos").get().await().toObjects()

    suspend fun upsertProyecto(item: Proyecto) {
        val id = if (item.id.isBlank()) item.nombre else item.id
        db.collection("proyectos").document(id).set(item.copy(id = id)).await()
    }

    suspend fun deleteProyecto(id: String) {
        db.collection("proyectos").document(id).delete().await()
    }

    // ------------------ ALERTAS ----------------------

    fun scheduleAlert(
        context: Context,
        actividadId: String,
        citaId: String,
        triggerAtMillis: Long
    ) {
        val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        val data = Data.Builder()
            .putString("actividadId", actividadId)
            .putString("citaId", citaId)
            .build()

        val req = OneTimeWorkRequestBuilder<AlertWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(req)
    }
}
