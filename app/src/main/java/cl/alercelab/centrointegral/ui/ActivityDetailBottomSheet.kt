package cl.alercelab.centrointegral.ui

import android.os.Bundle
import android.view.*
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import cl.alercelab.centrointegral.R

class ActivityDetailBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.bs_activity_detail, container, false)
        val tv = v.findViewById<TextView>(R.id.tvInfo)

        val rol = requireArguments().getString("rol") ?: "usuario"
        val nombre = requireArguments().getString("nombre") ?: "Actividad"
        val lugar = requireArguments().getString("lugar") ?: "-"
        val hora = requireArguments().getString("hora") ?: "-"

        when (rol) {
            "admin" -> tv.text = "Detalle completo de «" + nombre + "»\nLugar: " + lugar + "\nHora: " + hora + "\n(Adjuntos, beneficiarios, acciones…)"
            "gestor" -> tv.text = "Detalle limitado de «" + nombre + "»\nLugar: " + lugar + "\nHora: " + hora + "\n(Puede editar/cancelar)"
            else -> tv.text = "Detalle de «" + nombre + "»\nLugar: " + lugar + "\nHora: " + hora
        }

        return v
    }

    companion object {
        fun new(
            rol: String,
            nombre: String,
            lugar: String,
            hora: String
        ): ActivityDetailBottomSheet {
            val b = ActivityDetailBottomSheet()
            b.arguments = Bundle().apply {
                putString("rol", rol)
                putString("nombre", nombre)
                putString("lugar", lugar)
                putString("hora", hora)
            }
            return b
        }
    }
}
