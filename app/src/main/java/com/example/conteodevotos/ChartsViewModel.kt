package com.example.conteodevotos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ChartsViewModel : ViewModel() {
    private val _votesData = MutableLiveData<List<Vote>>()
    val votesData: LiveData<List<Vote>> = _votesData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchAllVotes() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllVotes()
                if (response.status == 200) {
                    _votesData.value = response.data ?: emptyList()
                } else {
                    _error.value = response.message ?: "Error al obtener datos para gráficas"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}