package cl.alercelab.centrointegral.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cl.alercelab.centrointegral.R
import java.util.*
import cl.alercelab.centrointegral.activities.ActivityFormFragment.CitaUI

class AddCitaBottomSheet : BottomSheetDialogFragment() {

    private lateinit var tvFecha: TextView
    private lateinit var tvHora: TextView
    private lateinit var etLugar: EditText
    private lateinit var etHoras: EditText
    private lateinit var etMinutos: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: ImageButton

    private var selectedCal = Calendar.getInstance()
    private var existing: CitaUI? = null
    private var userRole: String = "usuario" // admin/gestor/usuario

    /** Callback al cerrar con Guardar */
    var onSaved: ((CitaUI) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mantendremos existing y role por propiedades (se setean en new()).
        // Si quisieras soportar process death, podrías serializar primitives en arguments.
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.bottomsheet_add_cita, container, false)

        tvFecha = v.findViewById(R.id.tvFecha)
        tvHora = v.findViewById(R.id.tvHora)
        etLugar = v.findViewById(R.id.etLugar)
        etHoras = v.findViewById(R.id.etHoras)
        etMinutos = v.findViewById(R.id.etMinutos)
        btnSave = v.findViewById(R.id.btnSave)
        btnCancel = v.findViewById(R.id.btnClose)

        // Prefills
        existing?.let { ex ->
            selectedCal.timeInMillis = ex.fechaMillis
            etLugar.setText(ex.lugar ?: "")
            val h = ex.duracionMin / 60
            val m = ex.duracionMin % 60
            etHoras.setText(h.toString())
            etMinutos.setText(m.toString())
        } ?: run {
            // Defaults
            etLugar.setText("")
            etHoras.setText("1")
            etMinutos.setText("0")
        }

        updateFechaHoraLabels()

        // Pickers
        tvFecha.setOnClickListener {
            if (isEditable()) pickFecha()
        }
        tvHora.setOnClickListener {
            if (isEditable()) pickHora()
        }

        // Guardar
        btnSave.setOnClickListener {
            if (!isEditable()) return@setOnClickListener

            val lugar = etLugar.text.toString().trim()
            val durH = etHoras.text.toString().toIntOrNull() ?: 0
            val durM = etMinutos.text.toString().toIntOrNull() ?: 0
            val totalMin = durH.coerceAtLeast(0) * 60 + durM.coerceIn(0, 59)

            if (lugar.isEmpty()) {
                Toast.makeText(requireContext(), "Debes ingresar un lugar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (totalMin <= 0) {
                Toast.makeText(requireContext(), "La duración debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val res = CitaUI(
                fechaMillis = selectedCal.timeInMillis,
                lugar = lugar,
                duracionMin = totalMin
            )
            onSaved?.invoke(res)
            dismiss()
        }

        btnCancel.setOnClickListener { dismiss() }

        // Habilitar/Deshabilitar por rol
        applyRoleLock()

        return v
    }

    private fun isEditable(): Boolean = userRole == "admin" || userRole == "gestor"

    private fun applyRoleLock() {
        val editable = isEditable()
        tvFecha.isEnabled = editable
        tvHora.isEnabled = editable
        etLugar.isEnabled = editable
        etHoras.isEnabled = editable
        etMinutos.isEnabled = editable
        btnSave.visibility = if (editable) View.VISIBLE else View.GONE
    }

    private fun pickFecha() {
        val y = selectedCal.get(Calendar.YEAR)
        val m = selectedCal.get(Calendar.MONTH)
        val d = selectedCal.get(Calendar.DAY_OF_MONTH)
        DatePickerDialog(requireContext(), { _, yy, mm, dd ->
            selectedCal.set(Calendar.YEAR, yy)
            selectedCal.set(Calendar.MONTH, mm)
            selectedCal.set(Calendar.DAY_OF_MONTH, dd)
            updateFechaHoraLabels()
        }, y, m, d).show()
    }

    private fun pickHora() {
        val hh = selectedCal.get(Calendar.HOUR_OF_DAY)
        val mm = selectedCal.get(Calendar.MINUTE)
        android.app.TimePickerDialog(requireContext(), { _, h, min ->
            selectedCal.set(Calendar.HOUR_OF_DAY, h)
            selectedCal.set(Calendar.MINUTE, min)
            selectedCal.set(Calendar.SECOND, 0)
            selectedCal.set(Calendar.MILLISECOND, 0)
            updateFechaHoraLabels()
        }, hh, mm, true).show()
    }

    private fun updateFechaHoraLabels() {
        val cal = selectedCal
        val fecha = "%02d-%02d-%04d".format(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
        val hora = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        tvFecha.text = fecha
        tvHora.text = hora
    }

    companion object {
        /**
         * existing: cita a editar (o null para crear)
         * role: "admin" | "gestor" | "usuario"
         */
        fun new(existing: CitaUI?, role: String): AddCitaBottomSheet {
            val f = AddCitaBottomSheet()
            f.existing = existing
            f.userRole = role
            return f
        }
    }
}
