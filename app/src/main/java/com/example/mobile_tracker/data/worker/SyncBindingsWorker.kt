package com.example.mobile_tracker.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.mobile_tracker.data.repository.BindingRepository
import com.example.mobile_tracker.data.local.db.dao.ShiftContextDao
import com.example.mobile_tracker.util.OperatorNotificationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SyncBindingsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val bindingRepository: BindingRepository by inject()
    private val shiftContextDao: ShiftContextDao by inject()
    private val notificationManager: OperatorNotificationManager by inject()

    override suspend fun doWork(): Result {
        Timber.d("SyncBindingsWorker started")

        // 1. Push: отправляем несинхронизированные привязки на бэкенд
        val pushResult = bindingRepository.syncUnsynced()
        pushResult.onSuccess { count ->
            Timber.d("SyncBindingsWorker: pushed $count bindings")
            if (count > 0) {
                notificationManager.notifySyncCompleted(count)
            }
        }.onFailure { e ->
            Timber.e(e, "SyncBindingsWorker: push failed")
        }

        // 2. Pull: загружаем привязки с бэкенда в Room
        val context = shiftContextDao.get()
        if (context != null) {
            bindingRepository.refreshBindings(
                siteId = context.siteId,
                shiftDate = context.shiftDate,
            ).onSuccess { count ->
                Timber.d("SyncBindingsWorker: pulled $count bindings from backend")
            }.onFailure { e ->
                Timber.w(e, "SyncBindingsWorker: pull failed")
            }
        }

        return if (pushResult.isSuccess) {
            Result.success()
        } else {
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val WORK_NAME = "sync_bindings"
        private const val MAX_RETRIES = 5
        private const val REPEAT_INTERVAL_MINUTES = 15L

        fun enqueuePeriodicSync(
            workManager: WorkManager,
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED,
                )
                .build()

            val request =
                PeriodicWorkRequestBuilder<
                    SyncBindingsWorker>(
                    REPEAT_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                )
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30L,
                        TimeUnit.SECONDS,
                    )
                    .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )

            Timber.d(
                "Periodic bindings sync scheduled " +
                    "every $REPEAT_INTERVAL_MINUTES min",
            )
        }
    }
}
