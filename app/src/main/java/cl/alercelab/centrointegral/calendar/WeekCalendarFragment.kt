package cl.alercelab.centrointegral.calendar

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Actividad
import cl.alercelab.centrointegral.domain.Cita
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

class WeekCalendarFragment : Fragment() {

    private lateinit var containerWeek: LinearLayout
    private val repos = Repos()
    private val db = FirebaseFirestore.getInstance()

    private val sdfDay = SimpleDateFormat("EEEE dd/MM", Locale.getDefault())
    private val sdfHour = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val sdfFull = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_calendar_week, container, false)
        containerWeek = v.findViewById(R.id.containerWeek)
        loadCurrentWeek()
        return v
    }

    private fun loadCurrentWeek() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val (startMs, endMs) = weekBounds(System.currentTimeMillis())

                // 1️⃣ Obtener todas las actividades de la semana
                val actividades = repos.actividadesEnRango(startMs, endMs)
                val actsById = actividades.associateBy { it.id }

                // 2️⃣ Obtener citas dentro del rango semanal
                val citasSnap = db.collection("citas")
                    .whereGreaterThanOrEqualTo("fechaInicioMillis", startMs)
                    .whereLessThan("fechaInicioMillis", endMs)
                    .get()
                    .await()

                val citas = citasSnap.documents.mapNotNull { d ->
                    d.toObject(Cita::class.java)?.copy(id = d.id)
                }.sortedBy { it.fechaInicioMillis }

                // 3️⃣ Limpiar vista y agrupar citas por día
                containerWeek.removeAllViews()
                val days = daysOfWeek(startMs)

                days.forEach { dayStart ->
                    val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1
                    val dayCitas = citas.filter { it.fechaInicioMillis in dayStart..dayEnd }

                    val header = TextView(requireContext()).apply {
                        text = sdfDay.format(Date(dayStart)).replaceFirstChar { it.uppercase() }
                        textSize = 16f
                        setPadding(12, 24, 12, 8)
                    }
                    containerWeek.addView(header)

                    if (dayCitas.isEmpty()) {
                        val empty = TextView(requireContext()).apply {
                            text = "— Sin actividades —"
                            setPadding(24, 0, 24, 8)
                        }
                        containerWeek.addView(empty)
                    } else {
                        dayCitas.forEach { c ->
                            val a = actsById[c.actividadId]
                            val row = layoutInflater.inflate(R.layout.item_week_activity, containerWeek, false)

                            val tvNombre = row.findViewById<TextView>(R.id.tvNombre)
                            val tvTipo = row.findViewById<TextView>(R.id.tvTipo)
                            val tvLugar = row.findViewById<TextView>(R.id.tvLugar)
                            val tvFecha = row.findViewById<TextView>(R.id.tvFecha)

                            tvNombre.text = a?.nombre ?: "(Actividad desconocida)"
                            tvTipo.text = a?.tipo ?: "-"
                            tvLugar.text = "Lugar: ${c.lugar}"
                            tvFecha.text = "${sdfHour.format(Date(c.fechaInicioMillis))} - ${sdfHour.format(Date(c.fechaFinMillis))}"

                            // Mostrar detalle al tocar
                            row.setOnClickListener { showDetailDialog(a, c) }
                            containerWeek.addView(row)
                        }
                    }
                }

            } catch (e: Exception) {
                containerWeek.removeAllViews()
                val tv = TextView(requireContext()).apply {
                    text = "Error al cargar: ${e.message}"
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    textSize = 16f
                    setPadding(12, 20, 12, 20)
                }
                containerWeek.addView(tv)
            }
        }
    }

    /** 📋 Diálogo de detalle de la cita */
    private fun showDetailDialog(a: Actividad?, c: Cita) {
        val duracionMin = ((c.fechaFinMillis - c.fechaInicioMillis) / (1000 * 60)).toInt()

        val msg = buildString {
            appendLine("📘 Actividad: ${a?.nombre ?: "(sin nombre)"}")
            appendLine("Tipo: ${a?.tipo ?: "-"}")
            appendLine("Periodicidad: ${a?.periodicidad ?: "-"}")
            appendLine("Lugar: ${c.lugar}")
            appendLine("Inicio: ${sdfFull.format(Date(c.fechaInicioMillis))}")
            appendLine("Fin: ${sdfFull.format(Date(c.fechaFinMillis))}")
            appendLine("Duración: ${duracionMin} min")
            appendLine("Observaciones: ${c.observaciones ?: "-"}")
            appendLine("Estado: ${c.estado}")
            if (c.asistentes.isNotEmpty())
                appendLine("Asistentes: ${c.asistentes.joinToString(", ")}")
            a?.let {
                appendLine()
                appendLine("Oferente: ${it.oferente ?: "-"}")
                appendLine("Socio Comunitario: ${it.socioComunitario ?: "-"}")
                appendLine("Cupo: ${it.cupo ?: "-"}")
                appendLine("Beneficiarios: ${if (it.beneficiarios.isEmpty()) "-" else it.beneficiarios.joinToString()}")
            }
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Detalle de la Cita")
            .setMessage(msg)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    /** 📆 Retorna (inicioSemana, finSemana) en millis */
    private fun weekBounds(nowMs: Long): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val today = Date(nowMs).toInstant().atZone(zone).toLocalDate()
        val monday = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong()) // lunes
        val start = monday.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = monday.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }

    /** 📅 Devuelve lista con inicios de día (lunes..domingo) */
    private fun daysOfWeek(weekStartMs: Long): List<Long> =
        List(7) { i -> weekStartMs + i * 24 * 60 * 60 * 1000 }
}
