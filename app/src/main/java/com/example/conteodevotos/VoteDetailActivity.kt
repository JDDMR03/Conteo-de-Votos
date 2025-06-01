package com.example.conteodevotos

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import formatServerDateToLocalTime
import androidx.activity.viewModels

class VoteDetailActivity : AppCompatActivity() {

    private val viewModel: VoteDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vote_detail)

        // Configurar Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val voteId = intent.getIntExtra("VOTE_ID", -1)
        val voteTime = intent.getStringExtra("VOTE_TIME") ?: ""

        supportActionBar?.title = "Conteo hasta ${voteTime.formatServerDateToLocalTime()}"

        if (voteId == -1) {
            Toast.makeText(this, "ID de conteo inválido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Observar cambios
        viewModel.vote.observe(this) { vote ->
            vote?.let { updateUI(it) }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            findViewById<ProgressBar>(R.id.progressBar).visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        // Cargar datos
        viewModel.fetchVoteById(voteId)
    }

    private fun updateUI(vote: Vote) {
        // Obtener partidos ordenados
        val sortedParties = listOf(
            PartyInfo("PAN", vote.votesPAN, R.drawable.pan_logo),
            PartyInfo("PT", vote.votesPT, R.drawable.pt_logo),
            PartyInfo("MOVIMIENTO", vote.votesMOVIMIENTO, R.drawable.movimiento_logo),
            PartyInfo("PRI", vote.votesPRI, R.drawable.pri_logo),
            PartyInfo("MORENA VERDE", vote.votesMORENAVERDE, R.drawable.morena_verde_logo)
        ).sortedByDescending { it.votes }

        val container = findViewById<LinearLayout>(R.id.partiesContainer)
        container.removeAllViews()

        sortedParties.forEach { party ->
            val view = layoutInflater.inflate(R.layout.item_party_detail, container, false).apply {
                findViewById<ImageView>(R.id.ivPartyLogo).setImageResource(party.logoRes)
                findViewById<TextView>(R.id.tvPartyName).text = party.name
                findViewById<TextView>(R.id.tvPartyVotes).text = "Votos: ${party.votes}"
            }
            container.addView(view)
        }
    }

    private data class PartyInfo(
        val name: String,
        val votes: Int,
        val logoRes: Int
    )
}