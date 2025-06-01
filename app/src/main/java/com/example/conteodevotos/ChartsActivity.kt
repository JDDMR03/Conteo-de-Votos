package com.example.conteodevotos

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class ChartsActivity : AppCompatActivity() {

    private var scaleFactor = 1.0f
    private lateinit var scaleDetector: ScaleGestureDetector
    private var currentChartBitmap: Bitmap? = null

    companion object {
        private const val REQUEST_MEDIA_PERMISSION = 112
        private const val REQUEST_CREATE_FILE = 113
    }

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
            currentChartBitmap = bitmap

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

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.charts_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveChartToGallery()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveChartToGallery() {
        currentChartBitmap?.let { bitmap ->
            if (checkMediaPermissions()) {
                saveImage(bitmap)
            }
        } ?: run {
            Toast.makeText(this, "No hay gráfica para guardar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkMediaPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_MEDIA_PERMISSION
                )
            }
            hasPermission
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11-12 (API 30-32)
            Environment.isExternalStorageManager() ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 10 y anteriores (API 29-)
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun saveImage(bitmap: Bitmap) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> saveImageWithMediaStore(bitmap)
            else -> saveImageLegacy(bitmap)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageWithMediaStore(bitmap: Bitmap) {
        val resolver = contentResolver
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Grafica_votos_$timeStamp.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ConteoVotos")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        try {
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { imageUri ->
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    Toast.makeText(this, "Gráfica guardada en la galería", Toast.LENGTH_LONG).show()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }
                } ?: run {
                    Toast.makeText(this, "Error al crear el archivo", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Error al acceder a la galería", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("SaveImage", "Error al guardar imagen", e)
            Toast.makeText(this, "Error al guardar gráfica: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun saveImageLegacy(bitmap: Bitmap) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Grafica_votos_$timeStamp.png"

            val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val imagesDir = File(storageDir, "ConteoVotos")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val imageFile = File(imagesDir, fileName)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Notificar a la galería
            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imageFile)))

            Toast.makeText(this, "Gráfica guardada en: ${imageFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("SaveImage", "Error al guardar imagen", e)
            Toast.makeText(this, "Error al guardar gráfica: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_MEDIA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    currentChartBitmap?.let { saveImage(it) }
                } else {
                    Toast.makeText(this, "Permiso denegado, no se puede guardar la imagen", Toast.LENGTH_SHORT).show()
                }
            }
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