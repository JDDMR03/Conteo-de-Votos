package com.example.conteodevotos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ChartsActivity : AppCompatActivity() {

    private var scaleFactor = 1.0f
    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_charts)

        // Configurar Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Evolución de Votos"

        // Inicializar detector de gestos
        scaleDetector = ScaleGestureDetector(this, ScaleListener())

        // Inicializar Python
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Cargar datos
        loadChartData()
    }

    private fun loadChartData() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val chartImage = findViewById<ImageView>(R.id.ivChart)

        progressBar.visibility = View.VISIBLE
        chartImage.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val votes = withContext(Dispatchers.IO) {
                    try {
                        val response = RetrofitClient.apiService.getAllVotes()
                        if (response.status == 200) response.data else null
                    } catch (e: Exception) {
                        Log.e("API", "Error al obtener votos", e)
                        null
                    }
                }

                if (votes.isNullOrEmpty()) {
                    showError("No hay datos disponibles")
                    return@launch
                }

                generateChart(votes)
            } catch (e: Exception) {
                Log.e("ChartsActivity", "Error general", e)
                showError("Error al cargar datos")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun generateChart(votes: List<Vote>) {
        try {
            val jsonData = convertVotesToJson(votes)
            val py = Python.getInstance()
            val module = py.getModule("charts")

            val result = module.callAttr("generate_votes_timeline", jsonData)

            when {
                result == null -> {
                    showError("Error al generar gráfica")
                }
                else -> {
                    val chartBase64 = result.toString()
                    if (chartBase64.isBlank()) {
                        showError("No se pudo generar la gráfica")
                    } else {
                        displayChartImage(chartBase64)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChartGen", "Error al generar gráfica", e)
            showError("Error al procesar gráfica")
        }
    }

    private fun convertVotesToJson(votes: List<Vote>): String {
        return try {
            JSONArray().apply {
                votes.forEach { vote ->
                    put(JSONObject().apply {
                        put("time", vote.time ?: "")
                        put("PAN", vote.votesPAN)
                        put("PT", vote.votesPT)
                        put("MOVIMIENTO", vote.votesMOVIMIENTO)
                        put("PRI", vote.votesPRI)
                        put("MORENA_VERDE", vote.votesMORENAVERDE)
                    })
                }
            }.toString()
        } catch (e: Exception) {
            Log.e("JSON", "Error al convertir datos", e)
            "[]"
        }
    }

    private fun displayChartImage(chartBase64: String) {
        try {
            val imageBytes = Base64.decode(chartBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            findViewById<ImageView>(R.id.ivChart).apply {
                setImageBitmap(bitmap)
                visibility = View.VISIBLE

                setOnTouchListener { v, event ->
                    scaleDetector.onTouchEvent(event)
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("Image", "Error al mostrar imagen", e)
            showError("Error al mostrar gráfica")
            showErrorImage()
        }
    }

    private fun showError(message: String) {
        Log.w("Charts", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        showErrorImage()
    }

    private fun showErrorImage() {
        try {
            findViewById<ImageView>(R.id.ivChart).apply {
                setImageResource(R.drawable.chart_error_backup)
                visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e("ErrorImage", "No se pudo mostrar imagen de error", e)
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(0.5f, 5.0f)

            findViewById<ImageView>(R.id.ivChart).apply {
                scaleX = scaleFactor
                scaleY = scaleFactor
            }
            return true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}