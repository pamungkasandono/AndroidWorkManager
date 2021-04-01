package com.example.androidmyworkmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.work.*
import androidx.work.WorkInfo.State
import com.example.androidmyworkmanager.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var binding: ActivityMainBinding? = null
    private lateinit var workManager: WorkManager
    private lateinit var periodicWorkRequest: PeriodicWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        workManager = WorkManager.getInstance(this)

        binding?.btnOneTimeTask?.setOnClickListener(this)
        binding?.btnPeriodicTask?.setOnClickListener(this)
        binding?.btnCancelTask?.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnOneTimeTask -> startOneTimeTask()
            R.id.btnPeriodicTask -> startPeriodicTask()
            R.id.btnCancelTask -> cancelPeriodicTask()
        }
    }

    private fun startOneTimeTask() {
        binding?.textStatus?.text = getString(R.string.status)
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding?.editCity?.text.toString())
            .build()
        val constraits = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val oneTimeWorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .setInputData(data)
            .setConstraints(constraits)
            .addTag("just_tag_for_canceled_the_work_manager_process")
            .build()
        workManager.enqueue(oneTimeWorkRequest)
        workManager.getWorkInfoByIdLiveData(oneTimeWorkRequest.id)
            .observe(this@MainActivity, {
                val status = it.state.name
                binding?.textStatus?.append("\n $status")
            })
    }

    private fun startPeriodicTask() {
        binding?.textStatus?.text = getString(R.string.status)
        val data = Data.Builder()
            .putString(MyWorker.EXTRA_CITY, binding?.editCity?.text?.toString())
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        periodicWorkRequest =
            PeriodicWorkRequest.Builder(MyWorker::class.java, 15, TimeUnit.MINUTES)
                .setInputData(data)
                .setConstraints(constraints)
                .addTag("just_tag_for_canceled_the_work_manager_process")
                .build()

        workManager.enqueue(periodicWorkRequest)
        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.id)
            .observe(this@MainActivity, {
                val status = it.state.name
                Log.d("Debug state", it.state.toString())
                Log.d("Debug State enque", State.ENQUEUED.toString())
                binding?.textStatus?.append("\n $status")
                binding?.btnCancelTask?.isEnabled = false
                if (it.state == State.ENQUEUED) {
                    binding?.btnCancelTask?.isEnabled = true
                }
            })
    }

    private fun cancelPeriodicTask() {
        workManager.cancelWorkById(periodicWorkRequest.id)
        workManager.cancelAllWorkByTag("just_tag_for_canceled_the_work_manager_process")
    }
}
