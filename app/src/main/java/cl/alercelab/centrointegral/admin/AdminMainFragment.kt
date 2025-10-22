package cl.alercelab.centrointegral.admin

import android.os.Bundle
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import cl.alercelab.centrointegral.R

class AdminMainFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_admin_main, container, false)

        val btnGestion = v.findViewById<Button>(R.id.btnGestionUsuarios)
        val btnPermitir = v.findViewById<Button>(R.id.btnPermitirUsuarios)

        btnGestion.setOnClickListener {
            findNavController().navigate(R.id.action_adminMain_to_adminUsers)
        }

        btnPermitir.setOnClickListener {
            findNavController().navigate(R.id.action_adminMain_to_adminPending)
        }

        return v
    }
}
