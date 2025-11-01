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
import cl.alercelab.centrointegral.databinding.FragmentLoginBinding
import cl.alercelab.centrointegral.data.Repos
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val repos = Repos()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validar campos vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // Intentar login con Repos
                val ok = repos.login(email, password)
                if (!ok) {
                    Toast.makeText(requireContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Cargar perfil actual
                val profile = repos.currentUserProfile()
                if (profile == null) {
                    Toast.makeText(requireContext(), "Error al cargar usuario", Toast.LENGTH_SHORT).show()
                    FirebaseAuth.getInstance().signOut()
                    return@launch
                }

                // 🔒 Validar aprobación y estado antes de continuar
                if (!profile.aprobado || profile.estado.lowercase() != "activo") {
                    FirebaseAuth.getInstance().signOut()

                    // Mostrar mensaje visual en pantalla
                    binding.tvPending.visibility = View.VISIBLE
                    binding.tvPending.text = if (!profile.aprobado)
                        "Tu cuenta está pendiente de aprobación por el administrador."
                    else
                        "Tu cuenta fue desactivada por el administrador."

                    Toast.makeText(
                        requireContext(),
                        "Tu cuenta aún no ha sido aprobada o fue desactivada.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                } else {
                    // Si todo está OK, ocultar mensaje por si se mostró antes
                    binding.tvPending.visibility = View.GONE
                }

                // ✅ Usuario aprobado → siempre navegar al calendario
                findNavController().navigate(R.id.action_login_to_calendar)
            }
        }

        // 🔹 Recuperar contraseña
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot)
        }

        // 🔹 Ir al registro
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
