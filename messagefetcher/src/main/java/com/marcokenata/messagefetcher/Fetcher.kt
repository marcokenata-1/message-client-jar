package com.marcokenata.messagefetcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.QueueingConsumer
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.*

class Fetcher : JobIntentService() {

    private var builder: NotificationCompat.Builder? = null
    private var subscribeThread: Thread? = null
    private var id : Any? = null
    private var appName : Any? = null
    private var activity : Any? = null
    private var routingKey : Any? = null

    private var connectionFactory = ConnectionFactory()

    private lateinit var declareOk : AMQP.Queue.DeclareOk
    private fun connectionFactorySetup() {
        val stringUri =
            "amqp://hazpfnog:k6nyGD8xWN9o-8q1qy1Y8YvJkLkjBfjA@shark.rmq.cloudamqp.com/hazpfnog"
        try {
            Log.d("thread", "setup")
            connectionFactory.isAutomaticRecoveryEnabled = false
            connectionFactory.setUri(stringUri)
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    private fun subscriber(
        routingKey: String,
        p0: Context?,
        iconId: Int,
        appName: String,
        intent: Intent
    ) {
        subscribeThread = Thread(Runnable {
            while (true) {
                Log.d("thread", "running...")
                val connection = connectionFactory.newConnection()
                val channel = connection.createChannel()
                channel.basicQos(0)
                declareOk = channel.queueDeclare()
                Log.d("thread", "getting messages...")
                channel.queueBind(declareOk.queue, "notifications12", routingKey)
                val defaultConsumer = QueueingConsumer(channel)
                channel.basicConsume(declareOk.queue, true, defaultConsumer)
                Log.d("thread", "$channel ${declareOk.queue}")
                try {
                    while (true) {
                        Log.d("thread", "getting messages decoded")
                        val delivery = defaultConsumer.nextDelivery()
                        val message = String(delivery.body)
                        Log.d("thread", message)
                        notificationCreator(p0, appName, message, intent, iconId, routingKey)
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e1: Exception) {
                    try {
                        Log.d("thread", "sleeping")
                        Thread.sleep(5000)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }
        })

    }

    private fun notificationCreator(
        p0: Context?,
        appName: String,
        message: String,
        intent: Intent,
        iconId: Int,
        routingKey: String
    ) {
        val messageSplit = message.split("----")

        val notificationManager =
            p0?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "101"
        val channelName = "$appName Channel"


        val routingSplit = routingKey.split('.')
        val priority = routingSplit[routingSplit.size - 1]
        if (priority == "*" && priority == "Targeted") {
            val importance = NotificationManager.IMPORTANCE_MAX
            val notificationChannel =
                NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        } else {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel =
                NotificationChannel(channelId, channelName, importance)
            notificationManager.createNotificationChannel(notificationChannel)
        }


        val pendingIntent = PendingIntent.getActivity(
            p0,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )


        if (messageSplit[1] == "/media/noImage") {
            builder =
                NotificationCompat.Builder(p0, channelId)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(iconId)
                    .setContentTitle(appName)
                    .setContentText(messageSplit[0])
                    .setGroup(routingKey)
        } else {
            builder =
                NotificationCompat.Builder(p0, channelId)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(iconId)
                    .setContentTitle(appName)
                    .setContentText(messageSplit[0])
                    .setGroup(routingKey)

            //adding image function
            val target = Glide.with(p0)
                .asBitmap()
                .load(APIClient().httpUrl + messageSplit[1])
                .submit()

            val bitmap = target.get()
            builder!!
                .setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigLargeIcon(null)
                        .bigPicture(bitmap)
                )


        }
        val m = (Date().time / 1000L % Int.MAX_VALUE).toInt()
        notificationManager.notify(m, builder?.build())
    }

    fun notificationHandler(
        p0: Context?,
        iconId: Int,
        appName: String,
        intent: Intent,
        routingKey: String
    ) {
        Log.d("handlerNotification", "notification Handler go")
        connectionFactorySetup()
        subscriber(routingKey, p0, iconId, appName, intent)
    }

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

    private val TAG = "FetcherJobIntentService"

    fun enqueueWork(context: Context, intent: Intent){
        Log.d("thread","enqueue berfungsi")
        enqueueWork(context, Fetcher::class.java, 1000, intent)
    }

    override fun onHandleWork(intent: Intent) {
        id = intent.extras?.get("id")
        appName = intent.extras?.get("appName")
        routingKey = intent.extras?.get("routingKey")
        activity = intent.extras?.get("activity")

        Log.d("thread","onHandleWork berfungsi")

        notificationHandler(
            this,
            id as Int,
            appName as String,
            Intent(this, activity as Class<*>),
            routingKey as String
        )

        subscribeThread
            ?.start()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }

    override fun onStopCurrentWork(): Boolean {
        Log.d(TAG, "onStopCurrentWork")
        return super.onStopCurrentWork()
    }

}
