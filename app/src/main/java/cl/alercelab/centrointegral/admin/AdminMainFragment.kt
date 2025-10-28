package cl.alercelab.centrointegral.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R

class AdminMainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_admin_main, container, false)

        // 🔹 BOTONES PRINCIPALES
        val btnGestion = v.findViewById<Button>(R.id.btnGestionUsuarios)
        val btnPermitir = v.findViewById<Button>(R.id.btnPermitirUsuarios)

        btnGestion.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo gestión de usuarios...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_adminUsers)
        }

        btnPermitir.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo usuarios pendientes...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_adminPending)
        }

        // 🔹 MANTENEDORES (IDs corregidos para coincidir con tu XML)
        val btnTipoActividad = v.findViewById<Button>(R.id.btnTipoActividad)
        val btnLugares = v.findViewById<Button>(R.id.btnLugares)
        val btnOferentes = v.findViewById<Button>(R.id.btnOferentes)
        val btnSocios = v.findViewById<Button>(R.id.btnSocios)
        val btnProyectos = v.findViewById<Button>(R.id.btnProyectos)

        btnTipoActividad.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Tipos de Actividad...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorTipoActividad)
        }

        btnLugares.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Lugares...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorLugar)
        }

        btnOferentes.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Oferentes...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorOferente)
        }

        btnSocios.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Socios Comunitarios...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorSocio)
        }

        btnProyectos.setOnClickListener {
            Toast.makeText(requireContext(), "Abriendo Mantenedor de Proyectos...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_adminMain_to_mantenedorProyecto)
        }

        return v
    }
}
