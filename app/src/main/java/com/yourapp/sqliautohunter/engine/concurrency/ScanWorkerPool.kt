package com.yourapp.sqliautohunter.engine.concurrency

import com.yourapp.sqliautohunter.data.repository.QueueRepository
import com.yourapp.sqliautohunter.domain.usecase.CheckUrlHashUseCase
import com.yourapp.sqliautohunter.domain.usecase.ScanUrlForVulnerabilityUseCase
import com.yourapp.sqliautohunter.engine.browser.PayloadInjector
import com.yourapp.sqliautohunter.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ScanWorkerPool(
    private val queueRepository: QueueRepository,
    private val scanUrlForVulnerabilityUseCase: ScanUrlForVulnerabilityUseCase,
    private val checkUrlHashUseCase: CheckUrlHashUseCase,
    private val payloadInjector: PayloadInjector,
    private val dynamicConcurrencyController: DynamicConcurrencyController,
    private val scope: CoroutineScope
) {

    private val mutex = Mutex()
    private var isRunning = false
    private var jobs = mutableListOf<Job>()
    private var processedCount = 0L
    private var vulnerableCount = 0L
    private var errorCount = 0L
    private var lastError: String? = null

    private var pauseRequested = false
    private var stopRequested = false

    suspend fun start() {
        mutex.withLock {
            if (isRunning) return
            isRunning = true
            pauseRequested = false
            stopRequested = false
        }

        launchWorkers()
    }

    suspend fun pause() {
        mutex.withLock {
            pauseRequested = true
        }
        // Wait for current tasks to finish
        jobs.forEach { job ->
            job.cancel()
        }
        jobs.clear()
    }

    suspend fun resume() {
        mutex.withLock {
            pauseRequested = false
        }
        launchWorkers()
    }

    suspend fun stop() {
        mutex.withLock {
            stopRequested = true
            isRunning = false
        }
        
        // Cancel all jobs
        jobs.forEach { job ->
            job.cancel()
        }
        jobs.clear()
        
        resetCounters()
    }

    private suspend fun launchWorkers() {
        if (!isRunning) return

        val maxConcurrency = dynamicConcurrencyController.getCurrentMaxConcurrency()
        
        repeat(maxConcurrency) { workerId ->
            val job = scope.launch(Dispatchers.IO) {
                workerLoop(workerId)
            }
            jobs.add(job)
        }
    }

    private suspend fun workerLoop(workerId: Int) {
        while (isRunning && !stopRequested && currentCoroutineContext().isActive) {
            // Check if paused
            if (pauseRequested) {
                kotlinx.coroutines.delay(1000)
                continue
            }

            // Acquire slot
            if (!dynamicConcurrencyController.acquireSlot()) {
                kotlinx.coroutines.delay(100)
                continue
            }

            try {
                // Get next URL from queue
                val batch = queueRepository.getNextBatch(1)
                
                if (batch.isEmpty()) {
                    dynamicConcurrencyController.releaseSlot()
                    kotlinx.coroutines.delay(500)
                    continue
                }

                val queueItem = batch.first()
                
                // Mark as testing
                queueRepository.markAsTesting(queueItem.id)
                
                // Check if already tested
                val alreadyTested = checkUrlHashUseCase(queueItem.url)
                if (alreadyTested) {
                    // Get the cached result
                    val testResult = checkUrlHashUseCase.getTestResult(queueItem.url) ?: "not_vulnerable"
                    
                    if (testResult == "vulnerable") {
                        queueRepository.markAsVulnerable(queueItem.id)
                        vulnerableCount++
                    } else {
                        queueRepository.markAsNotVulnerable(queueItem.id)
                    }
                    
                    processedCount++
                    dynamicConcurrencyController.releaseSlot()
                    continue
                }

                // Perform the scan
                val scanResult = scanUrlForVulnerabilityUseCase(queueItem.url, queueItem.keywordSource)
                
                // Mark the URL as tested
                checkUrlHashUseCase.checkAndInsert(
                    queueItem.url,
                    if (scanResult.isVulnerable) "vulnerable" else "not_vulnerable",
                    scanResult.vulnerabilityTypes.joinToString(",")
                )

                // Update queue status
                if (scanResult.isVulnerable) {
                    queueRepository.markAsVulnerable(queueItem.id)
                    vulnerableCount++
                } else {
                    queueRepository.markAsNotVulnerable(queueItem.id)
                }

                processedCount++
                
            } catch (e: Exception) {
                errorCount++
                lastError = e.message
                // Mark as error in queue if we have the ID
                // Note: We don't have the queueItem here, so we'll just count the error
            } finally {
                dynamicConcurrencyController.releaseSlot()
            }
            
            // Small delay to prevent overwhelming
            kotlinx.coroutines.delay(100)
        }
    }

    private fun resetCounters() {
        processedCount = 0
        vulnerableCount = 0
        errorCount = 0
        lastError = null
    }

    suspend fun getStats(): WorkerPoolStats {
        return WorkerPoolStats(
            isRunning = isRunning,
            isPaused = pauseRequested,
            processedCount = processedCount,
            vulnerableCount = vulnerableCount,
            errorCount = errorCount,
            lastError = lastError,
            activeWorkers = jobs.count { it.isActive },
            maxWorkers = dynamicConcurrencyController.getCurrentMaxConcurrency()
        )
    }

    suspend fun getQueueStats(): QueueRepository.QueueStats {
        return queueRepository.getQueueStats().first()
    }

    data class WorkerPoolStats(
        val isRunning: Boolean,
        val isPaused: Boolean,
        val processedCount: Long,
        val vulnerableCount: Long,
        val errorCount: Long,
        val lastError: String?,
        val activeWorkers: Int,
        val maxWorkers: Int
    )
}
