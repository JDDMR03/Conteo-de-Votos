package com.example.conteodevotos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: VoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Configurar Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Historial de Conteos"

        // Configurar RecyclerView
        adapter = VoteAdapter { vote ->
            val intent = Intent(this, VoteDetailActivity::class.java).apply {
                putExtra("VOTE_ID", vote.id)
                putExtra("VOTE_TIME", vote.time)
            }
            startActivity(intent)
        }

        findViewById<RecyclerView>(R.id.rvVotes).apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = this@HistoryActivity.adapter
            addItemDecoration(DividerItemDecoration(this@HistoryActivity, DividerItemDecoration.VERTICAL))
        }

        // Observar cambios
        viewModel.votes.observe(this) { votes ->
            adapter.submitList(votes)
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
        viewModel.fetchAllVotes()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}