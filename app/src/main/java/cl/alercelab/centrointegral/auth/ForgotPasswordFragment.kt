package cl.alercelab.centrointegral.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val repos = Repos() // Instancia del repositorio que maneja la lógica de Firebase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Infla el layout usando ViewBinding
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configura el botón para enviar correo de recuperación
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString()
            if (email.isNotEmpty()) {
                // Ejecuta la llamada en una corrutina para no bloquear el hilo principal
                lifecycleScope.launch {
                    val ok = repos.resetPassword(email)
                    if (ok) {
                        Toast.makeText(requireContext(), "Correo de recuperación enviado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Error al enviar el correo", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Ingrese su correo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evita fugas de memoria al liberar la referencia del binding
    }
}

/*
Resumen:
Este fragmento muestra una interfaz para restablecer la contraseña del usuario.
El usuario ingresa su correo electrónico y al presionar el botón,
se envía una solicitud a Firebase para enviar un correo de recuperación de contraseña.
*/