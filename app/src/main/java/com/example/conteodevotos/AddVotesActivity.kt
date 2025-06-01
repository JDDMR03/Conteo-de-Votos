package com.example.conteodevotos

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddVotesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_votes)

        // Configurar Toolbar idéntica a MainActivity
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Agregar Votos"

        findViewById<Button>(R.id.btnSubmitVotes).setOnClickListener {
            submitVotes()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish() // Cierra la actividad al presionar la flecha de retroceso
        return true
    }

    private fun submitVotes() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val submitButton = findViewById<Button>(R.id.btnSubmitVotes)

        // Deshabilitar botón y mostrar progreso
        submitButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener y sumar votos
                val newVotes = VoteRequest(
                    votesPAN = intent.getIntExtra("PAN_VOTES", 0) + (findViewById<EditText>(R.id.etPanVotes).text.toString().toIntOrNull() ?: 0),
                    votesPT = intent.getIntExtra("PT_VOTES", 0) + (findViewById<EditText>(R.id.etPtVotes).text.toString().toIntOrNull() ?: 0),
                    votesMOVIMIENTO = intent.getIntExtra("MOVIMIENTO_VOTES", 0) + (findViewById<EditText>(R.id.etMovimientoVotes).text.toString().toIntOrNull() ?: 0),
                    votesPRI = intent.getIntExtra("PRI_VOTES", 0) + (findViewById<EditText>(R.id.etPriVotes).text.toString().toIntOrNull() ?: 0),
                    votesMORENAVERDE = intent.getIntExtra("MORENA_VERDE_VOTES", 0) + (findViewById<EditText>(R.id.etMorenaVerdeVotes).text.toString().toIntOrNull() ?: 0)
                )

                // Enviar al servidor
                val response = RetrofitClient.apiService.createVote(newVotes)

                withContext(Dispatchers.Main) {
                    if (response.status == 201) {
                        Toast.makeText(
                            this@AddVotesActivity,
                            "✓ Votos agregados correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish() // Cierra la actividad y regresa a MainActivity
                    } else {
                        Toast.makeText(
                            this@AddVotesActivity,
                            "Error: ${response.message ?: "Error desconocido"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@AddVotesActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    submitButton.isEnabled = true
                }
            }
        }
    }
}