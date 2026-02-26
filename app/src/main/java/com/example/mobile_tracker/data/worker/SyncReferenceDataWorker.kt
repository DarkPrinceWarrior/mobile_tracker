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
import com.example.mobile_tracker.data.local.datastore.UserPreferencesManager
import com.example.mobile_tracker.data.repository.ReferenceRepository
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SyncReferenceDataWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val repository: ReferenceRepository by inject()
    private val prefs: UserPreferencesManager by inject()

    override suspend fun doWork(): Result {
        Timber.d("SyncReferenceDataWorker started")

        val userPrefs = prefs.preferencesFlow.first()
        if (!userPrefs.isLoggedIn) {
            Timber.d("Not logged in, skipping sync")
            return Result.success()
        }

        val siteId = userPrefs.scopeIds.firstOrNull()
        if (siteId.isNullOrBlank()) {
            Timber.w("No site scope, skipping sync")
            return Result.success()
        }

        return repository.syncAll(siteId).fold(
            onSuccess = {
                Timber.d(
                    "Reference sync completed",
                )
                Result.success()
            },
            onFailure = { e ->
                Timber.e(e, "Reference sync failed")
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            },
        )
    }

    companion object {
        const val WORK_NAME =
            "sync_reference_data"
        private const val MAX_RETRIES = 3
        private const val REPEAT_INTERVAL_HOURS = 4L

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
                    SyncReferenceDataWorker>(
                    REPEAT_INTERVAL_HOURS,
                    TimeUnit.HOURS,
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
                "Periodic reference sync scheduled " +
                    "every $REPEAT_INTERVAL_HOURS hours",
            )
        }

        fun enqueueSingleSync(
            workManager: WorkManager,
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED,
                )
                .build()

            val request = androidx.work.OneTimeWorkRequestBuilder<
                SyncReferenceDataWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueue(request)
            Timber.d(
                "Single reference sync enqueued",
            )
        }
    }
}
