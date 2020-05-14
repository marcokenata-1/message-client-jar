package com.marcokenata.messagefetcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.QueueingConsumer
import java.net.URISyntaxException
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.util.*

class Fetcher(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    private var builder: NotificationCompat.Builder? = null
    private var subscribeThread: Thread? = null

    private var connectionFactory = ConnectionFactory()

    private lateinit var declareOk: AMQP.Queue.DeclareOk
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
        appName: String
//        intent: Intent
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
                        notificationCreator(p0, appName, message, iconId, routingKey)
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
        subscribeThread
            ?.start()
    }

    private fun notificationCreator(
        p0: Context?,
        appName: String,
        message: String,
//        intent: Intent,
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


//        val pendingIntent = PendingIntent.getActivity(
//            p0,
//            100,
////            intent,
//            PendingIntent.FLAG_UPDATE_CURRENT
//        )


        if (messageSplit[1] == "/media/noImage") {
            builder =
                NotificationCompat.Builder(p0, channelId)
//                    .setContentIntent(pendingIntent)
                    .setSmallIcon(iconId)
                    .setContentTitle(appName)
                    .setContentText(messageSplit[0])
                    .setGroup(routingKey)
        } else {
            builder =
                NotificationCompat.Builder(p0, channelId)
//                    .setContentIntent(pendingIntent)
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
//        intent: Intent,
        routingKey: String
    ) {
        Log.d("handlerNotification", "notification Handler go")
        connectionFactorySetup()
        subscriber(routingKey, p0, iconId, appName)
    }

    override fun doWork(): Result {

        Log.d("thread","workmanager jalan")

        val appName = inputData.keyValueMap["appName"]
        val iconId = inputData.keyValueMap["id"]
        val routingKey = inputData.keyValueMap["routingKey"]
//        val activity = inputData.keyValueMap["activity"]

        appName?.let { app ->
            notificationHandler(
                applicationContext,
                iconId as Int,
                app as String,
                routingKey as String
            )
        }



        Log.d("thread","workmanager mati")
        return Result.retry()
    }

//    private val TAG = "FetcherJobIntentService"

//    fun enqueueWork(context: Context, intent: Intent){
//        Log.d("thread","enqueue berfungsi")
//        enqueueWork(context, Fetcher::class.java, 1000, intent)
//    }
//
//    override fun onHandleWork(intent: Intent) {
//        if (id == null){
//            id = intent.extras?.get("id")
//            appName = intent.extras?.get("appName")
//            routingKey = intent.extras?.get("routingKey")
//            activity = intent.extras?.get("activity")
//        }
//
//        intentAlarm = Intent(this, Broadcaster::class.java)
//        intentAlarm?.putExtra("id",id as Int)
//        intentAlarm?.putExtra("appName",appName as String)
//        intentAlarm?.putExtra("routingKey",routingKey as String)
//        intentAlarm?.putExtra("activity",activity as Class<*>)
//
//        Log.d("thread","idnya sama dengan "+id.toString())
//
//        Log.d("thread","onHandleWork berfungsi")
//
//        notificationHandler(
//            this,
//            id as Int,
//            appName as String,
//            Intent(this, activity as Class<*>),
//            routingKey as String
//        )
//    }

//    override fun onCreate() {
//        super.onCreate()
//        Log.d(TAG, "onCreate")
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        Log.d(TAG, "onDestroy")
//    }
//
//    override fun onStopCurrentWork(): Boolean {
//        Log.d(TAG, "onStopCurrentWork")
//        return super.onStopCurrentWork()
//    }
}
