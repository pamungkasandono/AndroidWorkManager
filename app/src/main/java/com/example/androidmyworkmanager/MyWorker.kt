package com.example.androidmyworkmanager

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.lang.Exception
import java.text.DecimalFormat

class MyWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    companion object {
        const val APP_ID = "api_key"
        const val EXTRA_CITY = "Purbalingga"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "channel_01"
        const val CHANNEL_NAME = "dicoding channel"
    }

    private var resultStatus: Result? = null

    override fun doWork(): Result {
        val dataCity = inputData.getString(EXTRA_CITY)
        return getCurrentWeather(dataCity)
    }

    private fun getCurrentWeather(city: String?): Result {
        Log.d("Debug", "getCurrentWeather : Mulai....")
        Looper.prepare()
        val client = SyncHttpClient()
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$APP_ID"
        Log.d("Debug", "getCurrentWeather : $url")
        client.get(url, object : AsyncHttpResponseHandler() {
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?
            ) {
                val result = String(responseBody!!)
                Log.d("Debug", result)
                try {
                    val resOb = JSONObject(result)
                    val currentWeather: String =
                        resOb.getJSONArray("weather").getJSONObject(0).getString("main")
                    val description =
                        resOb.getJSONArray("weather").getJSONObject(0).getString("description")
                    val tempKelvin = resOb.getJSONObject("main").getDouble("temp")

                    val tempCelsius = tempKelvin - 273
                    val temperature = DecimalFormat("##.#").format(tempCelsius)

                    val title = "Cuaca hari ini di $city"
                    val message = "$currentWeather, $description with $temperature ℃"

                    showNotification(title, message)
                    Log.d("Debug", "onSuccess : Selesai...")
                    resultStatus = Result.success()
                } catch (e: Exception) {
                    showNotification("Gagal mendapatkan update cuaca", e.message)
                    Log.d("Debug", "onSuccess : Gagal...")
                    resultStatus = Result.failure()
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?,
                error: Throwable?
            ) {
                Log.d("Debug", "onFailure : Gagal...")

                // ketika proses gagal, maka jobFinished diset dengan parameter true. Yang artinya job perlu di reschedule
                showNotification("Gagal mendapkan update cuaca", error?.message)
                resultStatus = Result.failure()
            }
        })
        return resultStatus as Result
    }

    @SuppressLint("NewApi")
    private fun showNotification(title: String, description: String?) {
        val smallIcon = R.drawable.ic_launcher_background
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: NotificationCompat.Builder =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notification.setChannelId(CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build())

    }
}