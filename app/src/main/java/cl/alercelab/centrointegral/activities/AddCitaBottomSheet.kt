package cl.alercelab.centrointegral.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.domain.Cita
import java.util.*

class AddCitaBottomSheet : BottomSheetDialogFragment() {

    private lateinit var tvFecha: TextView
    private lateinit var tvHoraInicio: TextView
    private lateinit var tvHoraFin: TextView
    private lateinit var etLugar: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: ImageButton

    private var inicioCal = Calendar.getInstance()
    private var finCal = Calendar.getInstance()
    private var existing: Cita? = null
    private var userRole: String = "usuario" // admin / gestor / usuario

    var onSaved: ((Cita) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.bottomsheet_add_cita, container, false)

        tvFecha = v.findViewById(R.id.tvFecha)
        tvHoraInicio = v.findViewById(R.id.tvHoraInicio)
        tvHoraFin = v.findViewById(R.id.tvHoraFin)
        etLugar = v.findViewById(R.id.etLugar)
        btnSave = v.findViewById(R.id.btnSave)
        btnCancel = v.findViewById(R.id.btnClose)

        existing?.let { ex ->
            inicioCal.timeInMillis = ex.fechaInicioMillis
            finCal.timeInMillis = ex.fechaFinMillis
            etLugar.setText(ex.lugar)
        } ?: run {
            etLugar.setText("")
        }

        updateLabels()

        tvFecha.setOnClickListener { if (isEditable()) pickFecha() }
        tvHoraInicio.setOnClickListener { if (isEditable()) pickHora(true) }
        tvHoraFin.setOnClickListener { if (isEditable()) pickHora(false) }

        btnSave.setOnClickListener {
            if (!isEditable()) return@setOnClickListener
            val lugar = etLugar.text.toString().trim()
            if (lugar.isEmpty()) {
                Toast.makeText(requireContext(), "Debe ingresar un lugar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (finCal.timeInMillis <= inicioCal.timeInMillis) {
                Toast.makeText(requireContext(), "La hora final debe ser posterior a la inicial", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cita = Cita(
                id = existing?.id ?: UUID.randomUUID().toString(),
                fechaInicioMillis = inicioCal.timeInMillis,
                fechaFinMillis = finCal.timeInMillis,
                lugar = lugar
            )
            onSaved?.invoke(cita)
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }
        applyRoleLock()

        return v
    }

    private fun pickFecha() {
        val y = inicioCal.get(Calendar.YEAR)
        val m = inicioCal.get(Calendar.MONTH)
        val d = inicioCal.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(requireContext(), { _, yy, mm, dd ->
            inicioCal.set(yy, mm, dd)
            finCal.set(yy, mm, dd)
            updateLabels()
        }, y, m, d).show()
    }

    private fun pickHora(isStart: Boolean) {
        val cal = if (isStart) inicioCal else finCal
        val hh = cal.get(Calendar.HOUR_OF_DAY)
        val mm = cal.get(Calendar.MINUTE)
        TimePickerDialog(requireContext(), { _, h, min ->
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, min)
            updateLabels()
        }, hh, mm, true).show()
    }

    private fun updateLabels() {
        val fecha = "%02d-%02d-%04d".format(
            inicioCal.get(Calendar.DAY_OF_MONTH),
            inicioCal.get(Calendar.MONTH) + 1,
            inicioCal.get(Calendar.YEAR)
        )
        val hInicio = "%02d:%02d".format(inicioCal.get(Calendar.HOUR_OF_DAY), inicioCal.get(Calendar.MINUTE))
        val hFin = "%02d:%02d".format(finCal.get(Calendar.HOUR_OF_DAY), finCal.get(Calendar.MINUTE))
        tvFecha.text = fecha
        tvHoraInicio.text = hInicio
        tvHoraFin.text = hFin
    }

    private fun applyRoleLock() {
        val editable = isEditable()
        tvFecha.isEnabled = editable
        tvHoraInicio.isEnabled = editable
        tvHoraFin.isEnabled = editable
        etLugar.isEnabled = editable
        btnSave.visibility = if (editable) View.VISIBLE else View.GONE
    }

    private fun isEditable(): Boolean = userRole == "admin" || userRole == "gestor"

    companion object {
        fun new(existing: Cita?, role: String): AddCitaBottomSheet {
            val f = AddCitaBottomSheet()
            f.existing = existing
            f.userRole = role
            return f
        }
    }
}
