package com.example.conteodevotos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class VoteDetailViewModel : ViewModel() {
    private val _vote = MutableLiveData<Vote?>()
    val vote: LiveData<Vote?> = _vote

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchVoteById(id: Int) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getVoteById(id)
                if (response.status == 200) {
                    _vote.value = response.data
                } else {
                    _error.value = response.message ?: "Error al obtener conteo"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error de conexión"
            } finally {
                _isLoading.value = false
            }
        }
    }
}