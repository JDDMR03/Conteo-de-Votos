package com.example.conteodevotos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import formatServerDateToLocalTime

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var voteViewModel: VoteViewModel
    private lateinit var drawerLayout: DrawerLayout

    companion object {
        const val ADD_VOTES_REQUEST_CODE = 1001  // Puede ser cualquier número único
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        // Configurar Navigation Drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar),
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Inicializar ViewModel
        voteViewModel = ViewModelProvider(this).get(VoteViewModel::class.java)

        // Observar cambios en los votos
        voteViewModel.votes.observe(this) { vote ->
            vote?.let {
                updateUI(it)
            }
        }

        // Observar cambios en el estado de carga
        voteViewModel.isLoading.observe(this) { isLoading ->
            findViewById<ProgressBar>(R.id.progressBar).visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }

        // Observar errores
        voteViewModel.error.observe(this) { error ->
            error?.let {
                findViewById<TextView>(R.id.errorTextView).text = it
                findViewById<TextView>(R.id.errorTextView).visibility = android.view.View.VISIBLE
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            } ?: run {
                findViewById<TextView>(R.id.errorTextView).visibility = android.view.View.GONE
            }
        }

        findViewById<Button>(R.id.btnAddVotes).setOnClickListener {
            voteViewModel.votes.value?.let { currentVotes ->
                val intent = Intent(this, AddVotesActivity::class.java).apply {
                    putExtra("PAN_VOTES", currentVotes.votesPAN)
                    putExtra("PT_VOTES", currentVotes.votesPT)
                    putExtra("MOVIMIENTO_VOTES", currentVotes.votesMOVIMIENTO)
                    putExtra("PRI_VOTES", currentVotes.votesPRI)
                    putExtra("MORENA_VERDE_VOTES", currentVotes.votesMORENAVERDE)
                }
                startActivityForResult(intent, ADD_VOTES_REQUEST_CODE)
            } ?: run {
                Toast.makeText(this, "Espera a que se carguen los votos actuales", Toast.LENGTH_SHORT).show()
            }
        }

        // Cargar datos iniciales
        voteViewModel.fetchLastVote()
    }

    private fun updateUI(vote: Vote) {
        val sortedParties = voteViewModel.getSortedParties(vote)

        // Obtén referencias a tus vistas
        val panLayout = findViewById<LinearLayout>(R.id.panLayout)
        val ptLayout = findViewById<LinearLayout>(R.id.ptLayout)
        val movimientoLayout = findViewById<LinearLayout>(R.id.movimientoLayout)
        val priLayout = findViewById<LinearLayout>(R.id.priLayout)
        val morenaVerdeLayout = findViewById<LinearLayout>(R.id.morenaVerdeLayout)

        val partyLayouts = listOf(panLayout, ptLayout, movimientoLayout, priLayout, morenaVerdeLayout)

        // Ordena las vistas según el orden de los partidos
        val sortedLayouts = sortedParties.map { party ->
            when (party.first) {
                "PAN" -> panLayout to R.id.panVotes
                "PT" -> ptLayout to R.id.ptVotes
                "MOVIMIENTO" -> movimientoLayout to R.id.movimientoVotes
                "PRI" -> priLayout to R.id.priVotes
                "MORENA VERDE" -> morenaVerdeLayout to R.id.morenaVerdeVotes
                else -> throw IllegalStateException("Partido desconocido")
            }
        }

        // Actualiza los textos y el orden en el layout padre
        val container = findViewById<LinearLayout>(R.id.partiesContainer)
        container.removeAllViews()

        sortedLayouts.forEachIndexed { index, (layout, votesId) ->
            val party = sortedParties[index]
            val votesTextView = layout.findViewById<TextView>(votesId)
            votesTextView.text = "${party.first}: ${party.second}"
            container.addView(layout)
        }

        // Formatear la hora
        val formattedTime = vote.time.formatServerDateToLocalTime()
        findViewById<TextView>(R.id.timeTextView).text = "Hora de actualización: $formattedTime"
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_history -> {
                Toast.makeText(this, "Mostrar historial", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HistoryActivity::class.java))
            }
            R.id.nav_charts -> {
                Toast.makeText(this, "Mostrar gráficas", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ChartsActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        // Esto se ejecutará cada vez que la actividad se vuelva visible
        voteViewModel.fetchLastVote()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_VOTES_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Recargar los datos cuando regresamos de AddVotesActivity
            voteViewModel.fetchLastVote()
            Toast.makeText(this, "Datos actualizados", Toast.LENGTH_SHORT).show()
        }
    }
}