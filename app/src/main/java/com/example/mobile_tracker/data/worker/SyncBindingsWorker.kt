package com.example.mobile_tracker.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mobile_tracker.data.repository.BindingRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

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
    }
}
