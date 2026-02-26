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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SyncBindingsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val bindingRepository: BindingRepository by inject()

    override suspend fun doWork(): Result {
        Timber.d("SyncBindingsWorker started")
        return bindingRepository.syncUnsynced().fold(
            onSuccess = { count ->
                Timber.d(
                    "SyncBindingsWorker: synced $count",
                )
                Result.success()
            },
            onFailure = { e ->
                Timber.e(
                    e,
                    "SyncBindingsWorker failed",
                )
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            },
        )
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
