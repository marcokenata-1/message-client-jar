package com.marcokenata.messageclient

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.marcokenata.messagefetcher.Broadcaster
import com.marcokenata.messagefetcher.Fetcher
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var evArrayUri = ArrayList<Uri>()

    private var imageEncoded: String? = null

    private var x = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissions()
        }

        val appName: String = applicationInfo.loadLabel(packageManager).toString()
        val fetcher = Fetcher()

//        fetcher.notificationHandler(
//            applicationContext,
//            R.drawable.ic_launcher_foreground,
//            appName,
//            Intent(applicationContext, MainActivity::class.java),
//            "test.*"
//        )

        val intent = Intent(this, Fetcher::class.java)
        intent.putExtra("id", R.drawable.ic_launcher_foreground)
        intent.putExtra("appName", appName)
        intent.putExtra("routingKey", "test.*")
        intent.putExtra("activity", MainActivity::class.java)

        fetcher.enqueueWork(this, intent)

        val myCalendar = Calendar.getInstance()

        val date = DatePickerDialog.OnDateSetListener { _, year, month, dateOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dateOfMonth)

            val myFormat = "dd.MM.yyyy" // mention the format you need
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            et_date.text = sdf.format(myCalendar.time)
        }

        et_date.setOnClickListener {
            DatePickerDialog(
                this,
                date,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val time = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            myCalendar.set(Calendar.HOUR_OF_DAY, hour)
            myCalendar.set(Calendar.MINUTE, minute)
            myCalendar.set(Calendar.SECOND, 0)
            val string = "$hour:$minute"
            editText2.text = string

            val sdf1 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
            x = sdf1.format(myCalendar.time)
        }

        editText2.setOnClickListener {
            TimePickerDialog(
                this,
                time,
                myCalendar.get(Calendar.HOUR_OF_DAY),
                myCalendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        bt_image.setOnClickListener {
            val chooseFile = Intent(Intent.ACTION_PICK)
            chooseFile.type = "image/*"
            chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            startActivityForResult(Intent.createChooser(chooseFile, "Choose a file"), 2)
        }

        bt_publisher.setOnClickListener {
            if (et_channel.length() != 0 && et_date.text != "Click to Select Date"
                && editText2.text != "Click to Select Hour and Minute" && et_message.length() != 0
            ) {
                if (imageEncoded.isNullOrEmpty()) {
                    fetcher.publisher(
                        et_message.text.toString(), x, et_channel.text.toString(),
                        context = this
                    )
                } else {
                    imageEncoded?.let { image ->
                        fetcher.publisher(
                            et_message.text.toString(), x, et_channel.text.toString(),
                            image, this
                        )
                    }
                }
            } else {
                Toast.makeText(this, "There are unfinished data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            evArrayUri.clear()
            val mClipData = data.clipData
            val filePathColumn =
                arrayOf(MediaStore.Images.Media.DATA)
            val dataCount = data.clipData?.itemCount
            tv_image_selected.text = "$dataCount items selected"

            for (i in 0 until data.clipData!!.itemCount) {
                val item: ClipData.Item? = mClipData?.getItemAt(i)
                val uri = item?.uri
                if (uri != null) {
                    evArrayUri.add(uri)
                }
                // Get the cursor
                val cursor: Cursor? =
                    uri?.let { this.contentResolver.query(it, filePathColumn, null, null, null) }
                // Move to first row
                cursor?.moveToFirst()
                val columnIndex: Int? = cursor?.getColumnIndex(filePathColumn[0])
                imageEncoded = columnIndex?.let { cursor.getString(it) }
                cursor?.close()
            }
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun checkPermissions() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            != PackageManager.PERMISSION_GRANTED ||

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                1052
            )
        }
    }

    override fun onDestroy() {

        Log.d("thread","destroyed")
        val appName: String = applicationInfo.loadLabel(packageManager).toString()
        val intent = Intent(this, Broadcaster::class.java)

        intent.putExtra("id", R.drawable.ic_launcher_foreground)
        intent.putExtra("appName", appName)
        intent.putExtra("routingKey", "test.*")
        intent.putExtra("activity", MainActivity::class.java)

        sendBroadcast(intent)

        Log.d("thread","intent send")

        super.onDestroy()
    }

}
