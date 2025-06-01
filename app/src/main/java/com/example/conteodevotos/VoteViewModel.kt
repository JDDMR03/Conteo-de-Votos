package com.example.conteodevotos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class VoteViewModel : ViewModel() {
    private val _votes = MutableLiveData<Vote?>()
    val votes: LiveData<Vote?> = _votes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchLastVote() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getLastVote()
                if (response.status == 200) {
                    _votes.value = response.data
                } else {
                    _error.value = response.message ?: "Error desconocido"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error de conexión"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getSortedParties(vote: Vote): List<Pair<String, Int>> {
        return listOf(
            "PAN" to vote.votesPAN,
            "PT" to vote.votesPT,
            "MOVIMIENTO" to vote.votesMOVIMIENTO,
            "PRI" to vote.votesPRI,
            "MORENA VERDE" to vote.votesMORENAVERDE
        ).sortedByDescending { it.second }
    }

    fun deleteLastVote() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteLastVote()
                if (response.status == 200) {
                    // Actualizamos los datos después de eliminar
                    fetchLastVote()
                    _error.value = "Último voto eliminado correctamente"
                } else {
                    _error.value = response.message ?: "Error al eliminar el voto"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error de conexión"
            } finally {
                _isLoading.value = false
            }
        }
    }

}