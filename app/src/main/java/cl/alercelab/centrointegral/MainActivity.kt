package cl.alercelab.centrointegral

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import cl.alercelab.centrointegral.data.Repos
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Setup inicial ---
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val bottom = findViewById<BottomNavigationView>(R.id.bottom_nav)
        val navController = findNavController(R.id.nav_host_fragment)
        setSupportActionBar(toolbar)

        // --- Configuración del AppBar ---
        val appBarConfig = AppBarConfiguration(
            setOf(
                R.id.calendarDayFragment,
                R.id.activitiesFragment,
                R.id.adminFragment,
                R.id.settingsFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfig)
        bottom.setupWithNavController(navController)

        // --- Ocultar bottom nav en pantallas de autenticación ---
        navController.addOnDestinationChangedListener { _, dest, _ ->
            bottom.visibility = when (dest.id) {
                R.id.loginFragment, R.id.registerFragment, R.id.forgotFragment -> View.GONE
                else -> View.VISIBLE
            }
        }

        // --- Control de visibilidad por rol ---
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            lifecycleScope.launch {
                val perfil = Repos().currentUserProfile()
                if (perfil?.rol == "usuario") {
                    bottom.menu.findItem(R.id.adminFragment)?.isVisible = false
                    bottom.menu.findItem(R.id.activitiesFragment)?.isVisible = false

                    // Si el usuario está en una pestaña oculta, redirigir al calendario
                    val current = navController.currentDestination?.id
                    if (current == R.id.adminFragment || current == R.id.activitiesFragment) {
                        navController.navigate(R.id.calendarDayFragment)
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.nav_host_fragment).navigateUp() || super.onSupportNavigateUp()
}
