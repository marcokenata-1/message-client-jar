package com.marcokenata.messagefetcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.rabbitmq.client.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.*

class Fetcher(
    private val iconId: Int,
    private val appName: String,
    private val activity: Intent,
    private val context: Context
) {

    private val connectionFactory = ConnectionFactory()
    lateinit var channel: Channel
    lateinit var consumer: DefaultConsumer
    lateinit var connection: Connection

    private lateinit var declareOk: AMQP.Queue.DeclareOk

    private suspend fun connectionFactorySetup(routingKey: String) = withContext(Dispatchers.IO) {
        val stringUri =
            "amqp://hazpfnog:k6nyGD8xWN9o-8q1qy1Y8YvJkLkjBfjA@shark.rmq.cloudamqp.com/hazpfnog"
        try {
            Log.d("thread", "setup")
            connectionFactory.isAutomaticRecoveryEnabled = true
            connectionFactory.setUri(stringUri)
            connectionFactory.isTopologyRecoveryEnabled = true
            connection = connectionFactory.newConnection()
            channel = connection.createChannel()
            channel.basicQos(0)
            declareOk = channel.queueDeclare()!!
            Log.d("thread", "getting messages...")
            channel.queueBind(declareOk.queue, "notifications12", routingKey)
            channel.queueBind(declareOk.queue, "allmessage", routingKey)
            Log.d("thread", "$channel ${declareOk.queue}")
            Log.d("thread", "running...")
            consumer = object : DefaultConsumer(channel) {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun handleDelivery(
                    consumerTag: String?,
                    envelope: Envelope?,
                    properties: AMQP.BasicProperties?,
                    body: ByteArray?
                ) {
                    super.handleDelivery(consumerTag, envelope, properties, body)
                    val routingKey = envelope?.routingKey
                    body?.let { String(it) }?.let {
                        if (routingKey != null) {
                            NotifCreator().notificationCreator(
                                context, appName,
                                it, activity, iconId, routingKey
                            )
                        }
                    }
                }
            }
            channel.basicConsume(declareOk.queue, true, consumer)
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        } catch (e: Exception) {
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                connection.close()
                return@withContext Log.d("thread", "interupsi")
            }
        } catch (e : TimeoutCancellationException){
            connection.close()
        }
    }

    fun getChannel(routingKey: String) {
        runBlocking {
            val jobA = async { connectionFactorySetup(routingKey) }
            run {
                jobA.await()
            }
        }
    }

    class FetcherWork(
        context: Context,
        workerParameters: WorkerParameters,
        fetcher: Fetcher
    ) :
        CoroutineWorker(context, workerParameters) {

        override suspend fun doWork(): Result {

            Log.d("thread", "workmanager jalan")
            return Result.success()

        }

    }

    class NotifCreator {

        private var builder: NotificationCompat.Builder? = null

        @RequiresApi(Build.VERSION_CODES.O)
        fun notificationCreator(
            p0: Context?,
            appName: String,
            message: String,
            intent: Intent,
            iconId: Int,
            routingKey: String
        ) {


            val messageJson = JSONObject(message)

            val notificationManager =
                p0?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelId: String
            val channelName = "$appName Channel"
            val importance: Int
            val notificationChannel: NotificationChannel

            val priority = messageJson["priority"]
            if (priority == "All Devices") {
                Log.d("thread", "importance none - all devices")
                importance = NotificationManager.IMPORTANCE_NONE
                channelId = "101"
                notificationChannel =
                    NotificationChannel(channelId, "$channelName none", importance)
                notificationManager.createNotificationChannel(notificationChannel)
            } else if (priority == "Targeted") {
                Log.d("thread", "importance min - targeted")
                importance = NotificationManager.IMPORTANCE_MIN
                channelId = "102"
                notificationChannel =
                    NotificationChannel(channelId, "$channelName min", importance)
                notificationManager.createNotificationChannel(notificationChannel)
            } else if (priority == "Important") {
                Log.d("thread", "importance high - important")
                importance = NotificationManager.IMPORTANCE_HIGH
                channelId = "103"
                notificationChannel =
                    NotificationChannel(channelId, "$channelName high", importance)
                notificationManager.createNotificationChannel(notificationChannel)
            } else {
                Log.d("thread", "importance default - general")
                importance = NotificationManager.IMPORTANCE_DEFAULT
                channelId = "104"
                notificationChannel =
                    NotificationChannel(channelId, "$channelName default", importance)
                notificationManager.createNotificationChannel(notificationChannel)
            }

            val pendingIntent = PendingIntent.getActivity(
                p0,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )


            if (messageJson["imageUrl"] == "/media/noImage") {
                builder =
                    NotificationCompat.Builder(p0, channelId)
                        .setContentIntent(pendingIntent)
                        .setSmallIcon(iconId)
                        .setContentTitle(appName)
                        .setContentText(messageJson["message"] as String)
                        .setGroup(routingKey)
            } else {
                builder =
                    NotificationCompat.Builder(p0, channelId)
                        .setContentIntent(pendingIntent)
                        .setSmallIcon(iconId)
                        .setContentTitle(appName)
                        .setContentText(messageJson["message"] as String)
                        .setGroup(routingKey)

                //adding image function
                val target = Glide.with(p0)
                    .asBitmap()
                    .load(APIClient().httpUrl + messageJson["imageUrl"])
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
            val notif = builder?.build()
            notif?.flags = Notification.FLAG_AUTO_CANCEL
            val m = (Date().time / 1000L % Int.MAX_VALUE).toInt()
            notificationManager.notify(m, notif)
        }
    }
}



