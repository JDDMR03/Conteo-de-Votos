package com.example.conteodevotos

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @GET("votos")
    suspend fun getAllVotes(): ApiResponse<List<Vote>>

    @POST("votos")
    suspend fun createVote(@Body vote: VoteRequest): ApiResponse<Unit>

    @GET("votos/{id}")
    suspend fun getVoteById(@Path("id") id: Int): ApiResponse<Vote>

    @GET("voto/ultimo")
    suspend fun getLastVote(): ApiResponse<Vote>

    @DELETE("voto/ultimo")
    suspend fun deleteLastVote(): ApiResponse<Unit>
}