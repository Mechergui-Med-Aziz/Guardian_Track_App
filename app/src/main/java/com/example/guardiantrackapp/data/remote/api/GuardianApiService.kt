package com.example.guardiantrackapp.data.remote.api

import com.example.guardiantrackapp.data.remote.api.dto.IncidentDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST


interface GuardianApiService {

    @POST("incidents")
    suspend fun sendIncident(@Body incident: IncidentDto): Response<ResponseBody>
}
