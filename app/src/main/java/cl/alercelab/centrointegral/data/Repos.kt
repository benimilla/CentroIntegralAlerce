package cl.alercelab.centrointegral.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import cl.alercelab.centrointegral.domain.*
import cl.alercelab.centrointegral.utils.FcmSender
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class Repos {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // -----------------------------------------------------------
    // 🔹 AUTENTICACIÓN
    // -----------------------------------------------------------

    suspend fun register(nombre: String, email: String, password: String, rol: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return false

            val profile = UserProfile(
                uid = uid,
                nombre = nombre,
                email = email,
                rol = rol,
                aprobado = false,
                estado = "pendiente"
            )

            db.collection("usuarios").document(uid).set(profile).await()
            true
        } catch (e: Exception) {
            Log.e("REGISTER", "Error al registrar: ${e.message}")
            false
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            Log.e("LOGIN", "Error al iniciar sesión: ${e.message}")
            false
        }
    }

    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            Log.e("RESET_PASSWORD", "Error: ${e.message}")
            false
        }
    }

    fun logout() = auth.signOut()

    // -----------------------------------------------------------
    // 🔹 USUARIOS
    // -----------------------------------------------------------

    suspend fun currentUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: return null
        val doc = db.collection("usuarios").document(uid).get().await()
        return doc.toObject<UserProfile>()
    }

    suspend fun listAllUsers(): List<UserProfile> {
        val snap = db.collection("usuarios").get().await()
        return snap.documents.mapNotNull { it.toObject(UserProfile::class.java) }
    }

    suspend fun listPendingUsers(): List<UserProfile> {
        val snap = db.collection("usuarios")
            .whereEqualTo("aprobado", false)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(UserProfile::class.java) }
    }

    suspend fun approveUser(uid: String) {
        db.collection("usuarios").document(uid)
            .update(mapOf("aprobado" to true, "estado" to "activo"))
            .await()
    }

    suspend fun rejectUser(uid: String) {
        db.collection("usuarios").document(uid).delete().await()
    }

    suspend fun updateUserRole(uid: String, newRole: String) {
        db.collection("usuarios").document(uid).update("rol", newRole).await()
    }

    suspend fun deleteUser(uid: String) {
        db.collection("usuarios").document(uid).delete().await()
    }

    suspend fun updateUserData(uid: String, nuevoNombre: String, nuevoEmail: String) {
        val updates = mapOf(
            "nombre" to nuevoNombre,
            "email" to nuevoEmail
        )
        db.collection("usuarios").document(uid).update(updates).await()
    }

    // -----------------------------------------------------------
    // 🔹 ACTIVIDADES
    // -----------------------------------------------------------

    suspend fun crearActividadConCitas(actividad: Actividad, citas: List<Cita>) {
        val ref = db.collection("actividades").document()
        val newAct = actividad.copy(id = ref.id)
        ref.set(newAct).await()

        val citasRef = db.collection("citas")
        for (cita in citas) {
            citasRef.add(cita.copy(actividadId = ref.id)).await()
        }
    }

    suspend fun modificarActividad(actividad: Actividad) {
        db.collection("actividades").document(actividad.id).set(actividad).await()
    }

    suspend fun cancelarActividad(id: String, motivo: String? = null): Boolean {
        return try {
            db.collection("actividades").document(id)
                .update(mapOf("estado" to "cancelada", "motivoCancelacion" to motivo))
                .await()
            true
        } catch (e: Exception) {
            Log.e("CANCELAR_ACTIVIDAD", "Error: ${e.message}")
            false
        }
    }

    suspend fun deleteActividad(id: String) {
        db.collection("actividades").document(id).delete().await()
    }

    suspend fun listAllActividades(): List<Actividad> {
        val snap = db.collection("actividades").get().await()
        return snap.documents.mapNotNull { it.toObject(Actividad::class.java) }
    }

    // -----------------------------------------------------------
    // 🔹 ACTIVIDADES EN RANGO (para el calendario semanal)
    // -----------------------------------------------------------

    suspend fun actividadesEnRango(
        desde: Long,
        hasta: Long,
        tipo: String? = null,
        query: String? = null,
        onlyMine: Boolean = false,
        rol: String? = null,
        uid: String? = null
    ): List<Actividad> {
        return try {
            var ref = db.collection("actividades")
                .whereGreaterThanOrEqualTo("fechaInicio", desde)
                .whereLessThanOrEqualTo("fechaInicio", hasta)

            if (!tipo.isNullOrBlank()) ref = ref.whereEqualTo("tipo", tipo)
            if (!query.isNullOrBlank()) ref = ref.whereEqualTo("nombre", query)
            if (onlyMine && !uid.isNullOrBlank()) ref = ref.whereEqualTo("responsableUid", uid)

            val snapshot = ref.get().await()
            snapshot.documents.mapNotNull { it.toObject(Actividad::class.java) }
        } catch (e: Exception) {
            Log.e("ACTIVIDADES_RANGO", "Error: ${e.message}")
            emptyList()
        }
    }

    // -----------------------------------------------------------
    // 🔹 ACTIVIDADES DE UN USUARIO
    // -----------------------------------------------------------

    suspend fun listUserActividades(uid: String): List<Actividad> {
        return try {
            val snap = db.collection("actividades")
                .whereArrayContains("beneficiarios", uid)
                .get().await()
            val actividades = snap.documents.mapNotNull { it.toObject(Actividad::class.java) }

            if (actividades.isEmpty()) {
                val altSnap = db.collection("actividades")
                    .whereEqualTo("responsableUid", uid)
                    .get().await()
                altSnap.documents.mapNotNull { it.toObject(Actividad::class.java) }
            } else {
                actividades
            }
        } catch (e: Exception) {
            Log.e("LIST_USER_ACT", "Error: ${e.message}")
            emptyList()
        }
    }

    // -----------------------------------------------------------
    // 🔹 CITAS EN RANGO (para el calendario diario)
    // -----------------------------------------------------------

    suspend fun citasEnRango(
        desde: Long,
        hasta: Long,
        tipo: String? = null,
        query: String? = null,
        onlyMine: Boolean = false,
        rol: String? = null,
        uid: String? = null
    ): List<Pair<Cita, Actividad?>> {
        return try {
            val citasSnap = db.collection("citas")
                .whereGreaterThanOrEqualTo("fechaMillis", desde)
                .whereLessThanOrEqualTo("fechaMillis", hasta)
                .get().await()

            val citas = citasSnap.documents.mapNotNull { it.toObject(Cita::class.java) }
            if (citas.isEmpty()) return emptyList()

            val actividadesSnap = db.collection("actividades").get().await()
            val actividades = actividadesSnap.documents.mapNotNull { it.toObject(Actividad::class.java) }
                .associateBy { it.id }

            citas.mapNotNull { cita ->
                val act = actividades[cita.actividadId]
                if (act != null) cita to act else null
            }

        } catch (e: Exception) {
            Log.e("CITAS_RANGO", "Error: ${e.message}")
            emptyList()
        }
    }

    // -----------------------------------------------------------
    // 🔹 CITAS
    // -----------------------------------------------------------

    suspend fun reagendarCita(
        citaId: String,
        nuevaFecha: Long,
        nuevoLugar: String,
        motivo: String? = null
    ): Boolean {
        return try {
            db.collection("citas").document(citaId)
                .update(mapOf("fechaMillis" to nuevaFecha, "lugar" to nuevoLugar, "motivo" to motivo))
                .await()
            true
        } catch (e: Exception) {
            Log.e("REAGENDAR_CITA", "Error: ${e.message}")
            false
        }
    }

    suspend fun updateCitaTiempoYLugar(
        citaId: String,
        nuevaFecha: Long,
        nuevoLugar: String
    ): Boolean {
        return try {
            db.collection("citas").document(citaId)
                .update(mapOf("fechaMillis" to nuevaFecha, "lugar" to nuevoLugar))
                .await()
            true
        } catch (e: Exception) {
            Log.e("UPDATE_CITA", "Error: ${e.message}")
            false
        }
    }

    // -----------------------------------------------------------
    // 🔹 NOTIFICACIONES (FCM)
    // -----------------------------------------------------------

    fun saveDeviceToken() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            db.collection("usuarios").document(uid).update("fcmToken", token)
        }
    }

    suspend fun sendNotificationToUser(uid: String, title: String, body: String) {
        try {
            val userDoc = db.collection("usuarios").document(uid).get().await()
            val token = userDoc.getString("fcmToken")
            if (!token.isNullOrEmpty()) {
                FcmSender.sendPushNotification(token, title, body)
            }
        } catch (e: Exception) {
            Log.e("FCM_SEND", "Error: ${e.message}")
        }
    }
}
