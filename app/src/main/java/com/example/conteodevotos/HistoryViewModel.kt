package com.example.conteodevotos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val _votes = MutableLiveData<List<Vote>>()
    val votes: LiveData<List<Vote>> = _votes

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
                    _votes.value = response.data ?: emptyList()
                } else {
                    _error.value = response.message ?: "Error al obtener historial"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error de conexión"
            } finally {
                _isLoading.value = false
            }
        }
    }
}