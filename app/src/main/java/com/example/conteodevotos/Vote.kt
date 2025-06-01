package com.example.conteodevotos

data class Vote(
    val id: Int,
    val time: String,
    val votesPAN: Int,
    val votesPT: Int,
    val votesMOVIMIENTO: Int,
    val votesPRI: Int,
    val votesMORENAVERDE: Int
)