package com.example.conteodevotos

data class VoteRequest(
    val votesPAN: Int,
    val votesPT: Int,
    val votesMOVIMIENTO: Int,
    val votesPRI: Int,
    val votesMORENAVERDE: Int
)