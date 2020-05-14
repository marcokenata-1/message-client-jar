package com.marcokenata.messagefetcher

import android.content.Context
import android.util.Log
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class Publisher {

    fun publisher(
        message: String,
        dateTime: String,
        channel: String,
        imageUri: String = "noImage",
        context: Context
    ) {
        Log.d("publisher", imageUri)

        val apiInterface = APIClient().getClient()

        val imageFile = File(imageUri)

        val requestBody = MultipartBody.Builder()

        if (imageUri == "noImage") {
            requestBody
                .setType(MultipartBody.FORM)
                .addFormDataPart("message", message)
                .addFormDataPart("dateTime", dateTime)
                .addFormDataPart("channel", channel)
                .addFormDataPart(
                    "imageUri",
                    imageUri
                )

        } else {
            Log.d("publisher", imageFile.toString())
            requestBody
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "imageUri",
                    imageFile.name,
                    RequestBody.create("multipart/form-data".toMediaTypeOrNull(), imageFile)
                )
                .addFormDataPart("message", message)
                .addFormDataPart("dateTime", dateTime)
                .addFormDataPart("channel", channel)

        }

        val callback = apiInterface.publishMessage(requestBody.build())

        callback.enqueue(object : Callback<ResponseBody> {
            override fun onFailure(call: Call<ResponseBody>?, t: Throwable) {
                Log.d("Failure", "try again")
                Toast.makeText(context, "Fail to send message", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>) {
                Log.d("Success", "Sended")
                Toast.makeText(context, "Sending message success!", Toast.LENGTH_SHORT).show()
            }
        })

    }
}