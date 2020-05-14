package com.marcokenata.messagefetcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.util.Log

//class Broadcaster : BroadcastReceiver() {
//
//    var cdt: CountDownTimer? = null
//
//    override fun onReceive(context: Context?, intent: Intent?) {
//        Log.d("thread", "broadcaster jalan")
//        Log.d("thread",intent?.extras?.getString("appName").toString())
//        val fetcher = Fetcher()
//
//
//        cdt = object : CountDownTimer(10000,1000){
//            override fun onFinish() {
//                context?.let {
//                    intent?.let { intent ->
//                        fetcher.enqueueWork(it, intent)
//                    }
//                }
//            }
//
//            override fun onTick(millisUntilFinished: Long) {
//                Log.d("thread",millisUntilFinished.toString())
//            }
//        }
//
//        (cdt as CountDownTimer).start()
//
//
//
//
//    }
//}