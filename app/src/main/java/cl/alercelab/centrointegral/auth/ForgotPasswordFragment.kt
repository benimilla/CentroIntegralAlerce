package cl.alercelab.centrointegral.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R
import cl.alercelab.centrointegral.data.Repos
import cl.alercelab.centrointegral.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val repos = Repos()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Ingrese su correo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val ok = repos.resetPassword(email)
                if (ok) {
                    Toast.makeText(
                        requireContext(),
                        "Correo de recuperación enviado a $email",
                        Toast.LENGTH_LONG
                    ).show()
                    // ✅ Navegar de vuelta al login después del envío
                    findNavController().navigate(R.id.loginFragment)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo enviar el correo. Verifique el correo ingresado.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Opcional: botón extra de volver al login (si lo tienes en el layout)
        binding.btnBackToLogin?.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/*
Resumen:
Este fragmento muestra una interfaz para restablecer la contraseña del usuario.
El usuario ingresa su correo electrónico y al presionar el botón,
se envía una solicitud a Firebase para enviar un correo de recuperación de contraseña.
*/