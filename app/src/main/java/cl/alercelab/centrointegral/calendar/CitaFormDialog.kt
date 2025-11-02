package cl.alercelab.centrointegral.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.domain.Cita
import kotlinx.coroutines.launch
import java.util.*
import cl.alercelab.centrointegral.notifications.NotificationHelper
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class CitaFormDialog(
    private val actividadId: String,
    private val duracionMin: Int,
    private val citaExistente: Cita? = null,
    private val onCitaCreada: (Cita) -> Unit
) : DialogFragment() {

    private val repos = Repos()
    private lateinit var fechaBtn: Button
    private lateinit var horaBtn: Button
    private lateinit var lugarEt: EditText
    private lateinit var observacionesEt: EditText
    private lateinit var btnGuardar: Button

    private var fechaMillis: Long? = null
    private var horaMillis: Long? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.dialog_cita_form, container, false)

        fechaBtn = v.findViewById(R.id.btnFecha)
        horaBtn = v.findViewById(R.id.btnHora)
        lugarEt = v.findViewById(R.id.etLugar)
        observacionesEt = v.findViewById(R.id.etObservaciones)
        btnGuardar = v.findViewById(R.id.btnGuardar)

        citaExistente?.let {
            fechaMillis = it.fechaInicioMillis
            horaMillis = it.fechaInicioMillis
            lugarEt.setText(it.lugar)
            observacionesEt.setText(it.observaciones ?: "")
            fechaBtn.text = Date(it.fechaInicioMillis).toString()
            horaBtn.text = Date(it.fechaInicioMillis).hours.toString() + ":00"
        }

        fechaBtn.setOnClickListener { pickDate() }
        horaBtn.setOnClickListener { pickTime() }

        btnGuardar.setOnClickListener {
            guardarCita()
        }

        return v
    }

    private fun pickDate() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d, 0, 0)
            fechaMillis = cal.timeInMillis
            fechaBtn.text = "$d/${m + 1}/$y"
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime() {
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, h, min ->
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, min)
            horaMillis = (fechaMillis ?: 0) + (h * 3600000L) + (min * 60000L)
            horaBtn.text = String.format("%02d:%02d", h, min)
        }, 10, 0, true).show()
    }

    private fun guardarCita() {
        val fechaInicio = horaMillis ?: return Toast.makeText(requireContext(), "Selecciona fecha y hora", Toast.LENGTH_SHORT).show()
        val fechaFin = fechaInicio + duracionMin * 60 * 1000

        val nuevaCita = citaExistente?.copy(
            fechaInicioMillis = fechaInicio,
            fechaFinMillis = fechaFin,
            lugar = lugarEt.text.toString(),
            observaciones = observacionesEt.text.toString()
        ) ?: Cita(
            actividadId = actividadId,
            fechaInicioMillis = fechaInicio,
            fechaFinMillis = fechaFin,
            lugar = lugarEt.text.toString(),
            observaciones = observacionesEt.text.toString()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val hayConflicto = repos.existeConflictoCita(nuevaCita)
            if (hayConflicto) {
                Toast.makeText(requireContext(), "⚠️ Ese horario ya está ocupado.", Toast.LENGTH_LONG).show()
            } else {
                if (citaExistente != null) {
                    repos.reagendarCita(nuevaCita.id, fechaInicio, fechaFin, nuevaCita.lugar)

                    NotificationHelper.showSimpleNotification(
                        requireContext(),
                        "Cita reagendada",
                        "Tu cita ha sido reagendada para el ${Date(fechaInicio)}."
                    )
                } else {
                    repos.crearCita(nuevaCita)

                    NotificationHelper.showSimpleNotification(
                        requireContext(),
                        "Nueva cita creada",
                        "Se agendó una cita el ${Date(fechaInicio)} a las ${
                            String.format("%02d:%02d", Date(fechaInicio).hours, Date(fechaInicio).minutes)
                        }."
                    )
                }
                onCitaCreada(nuevaCita)
                dismiss()
            }
        }
    }
}
