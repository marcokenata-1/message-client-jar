package com.marcokenata.messagefetcher

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface APICaller {

    @POST("/home/add-message-api/")
    fun publishMessage(@Body requestBody: RequestBody): Call<ResponseBody>
}