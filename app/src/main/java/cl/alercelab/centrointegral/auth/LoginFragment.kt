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

            //  Verifica que los campos de correo y contraseña no estén vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                //  Intenta iniciar sesión usando el repositorio (Firebase Auth)
                val ok = repos.login(email, password)
                if (!ok) {
                    Toast.makeText(requireContext(), "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                //  Cargar el perfil del usuario actual desde la base de datos
                val profile = repos.currentUserProfile()
                if (profile == null) {
                    Toast.makeText(requireContext(), "Error al cargar usuario", Toast.LENGTH_SHORT).show()
                    FirebaseAuth.getInstance().signOut()
                    return@launch
                }

                //  Validar si el usuario está aprobado y activo antes de continuar
                if (!profile.aprobado || profile.estado.lowercase() != "activo") {
                    FirebaseAuth.getInstance().signOut()

                    //  Mostrar mensaje visual en pantalla indicando el motivo
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
                    //  Si todo está correcto, asegurarse de ocultar mensajes previos
                    binding.tvPending.visibility = View.GONE
                }

                //  Usuario validado → navegar directamente al calendario principal
                findNavController().navigate(R.id.action_login_to_calendar)
            }
        }

        //  Navegar a la pantalla de recuperación de contraseña
        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot)
        }

        //  Navegar a la pantalla de registro
        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null //  Limpieza del binding para evitar fugas de memoria
    }
}