package com.yourapp.sqliautohunter.service

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.yourapp.sqliautohunter.data.repository.QueueRepository
import com.yourapp.sqliautohunter.engine.concurrency.ScanWorkerPool
import com.yourapp.sqliautohunter.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class ScanWorkManagerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val queueRepository: QueueRepository,
    private val scanWorkerPool: ScanWorkerPool
) : CoroutineWorker(context, params) {
    companion object {
        const val WORKER_TAG = "sqli_scan_worker"
        const val INPUT_KEY_KEYWORD = "keyword"
        const val INPUT_KEY_BATCH_SIZE = "batch_size"
    }
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val queueStats = queueRepository.getQueueStats().first()
                if (queueStats.pending <= 0 && queueStats.testing <= 0) return@withContext Result.success()
                if (!scanWorkerPool.getStats().isRunning) scanWorkerPool.start()
                Result.success()
            } catch (e: Exception) { Result.retry() }
        }
    }
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationController.createForegroundNotification(applicationContext, "SQLi Hunter Background Scan", "Processing URLs in background")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ForegroundInfo(Constants.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(Constants.NOTIFICATION_ID, notification)
    }
    suspend fun startScanWithKeyword(keyword: String, batchSize: Int = 10) {}
}
