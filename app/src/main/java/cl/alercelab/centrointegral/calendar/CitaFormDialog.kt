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

        // Si se está editando una cita existente, se rellenan los campos con los datos previos
        citaExistente?.let {
            fechaMillis = it.fechaInicioMillis
            horaMillis = it.fechaInicioMillis
            lugarEt.setText(it.lugar)
            observacionesEt.setText(it.observaciones ?: "")
            fechaBtn.text = Date(it.fechaInicioMillis).toString()
            horaBtn.text = Date(it.fechaInicioMillis).hours.toString() + ":00"
        }

        // Botones para elegir fecha y hora de la cita
        fechaBtn.setOnClickListener { pickDate() }
        horaBtn.setOnClickListener { pickTime() }

        // Guarda la cita al presionar el botón correspondiente
        btnGuardar.setOnClickListener {
            guardarCita()
        }

        return v
    }

    private fun pickDate() {
        // Muestra un selector de fecha y guarda el valor elegido
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d, 0, 0)
            fechaMillis = cal.timeInMillis
            fechaBtn.text = "$d/${m + 1}/$y"
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime() {
        // Muestra un selector de hora y calcula el timestamp completo de la cita
        val cal = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, h, min ->
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, min)
            horaMillis = (fechaMillis ?: 0) + (h * 3600000L) + (min * 60000L)
            horaBtn.text = String.format("%02d:%02d", h, min)
        }, 10, 0, true).show()
    }

    private fun guardarCita() {
        // Verifica que se haya seleccionado fecha y hora
        val fechaInicio = horaMillis ?: return Toast.makeText(requireContext(), "Selecciona fecha y hora", Toast.LENGTH_SHORT).show()
        val fechaFin = fechaInicio + duracionMin * 60 * 1000

        // Crea una nueva cita o actualiza una existente
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
            // Comprueba si existe conflicto con otra cita en el mismo horario
            val hayConflicto = repos.existeConflictoCita(nuevaCita)
            if (hayConflicto) {
                Toast.makeText(requireContext(), "Ese horario ya está ocupado.", Toast.LENGTH_LONG).show()
            } else {
                if (citaExistente != null) {
                    // Actualiza (reagenda) una cita existente
                    repos.reagendarCita(nuevaCita.id, fechaInicio, fechaFin, nuevaCita.lugar)

                    // Muestra notificación local informando del cambio
                    NotificationHelper.showSimpleNotification(
                        requireContext(),
                        "Cita reagendada",
                        "Tu cita ha sido reagendada para el ${Date(fechaInicio)}."
                    )
                } else {
                    // Crea una nueva cita y muestra notificación de confirmación
                    repos.crearCita(nuevaCita)

                    NotificationHelper.showSimpleNotification(
                        requireContext(),
                        "Nueva cita creada",
                        "Se agendó una cita el ${Date(fechaInicio)} a las ${
                            String.format("%02d:%02d", Date(fechaInicio).hours, Date(fechaInicio).minutes)
                        }."
                    )
                }
                // Devuelve la cita creada al componente que llamó el diálogo
                onCitaCreada(nuevaCita)
                dismiss()
            }
        }
    }
}

// Diálogo de formulario para crear o editar una cita. Permite seleccionar fecha, hora y lugar,
// valida conflictos de horario y muestra notificaciones cuando la cita es creada o modificada.