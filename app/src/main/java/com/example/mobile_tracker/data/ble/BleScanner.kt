package com.example.mobile_tracker.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber

/**
 * BLE-сканер для обнаружения часов Activity Tracker.
 *
 * Фильтрует по service UUID [BleConstants.SERVICE_UUID].
 * Выдаёт поток [ScanResult] до отмены корутины.
 */
class BleScanner(private val context: Context) {

    private val bluetoothManager: BluetoothManager? by lazy {
        context.getSystemService(
            Context.BLUETOOTH_SERVICE,
        ) as? BluetoothManager
    }

    val isBluetoothEnabled: Boolean
        get() = bluetoothManager?.adapter?.isEnabled == true

    /**
     * Запускает BLE-сканирование и выдаёт найденные
     * устройства через Flow.
     *
     * Сканирование останавливается при отмене Flow.
     */
    @SuppressLint("MissingPermission")
    fun scan(): Flow<ScanResult> = callbackFlow {
        val scanner = bluetoothManager?.adapter
            ?.bluetoothLeScanner
            ?: run {
                close(
                    IllegalStateException(
                        "BLE scanner unavailable",
                    ),
                )
                return@callbackFlow
            }

        val filter = ScanFilter.Builder()
            .setServiceUuid(
                ParcelUuid(BleConstants.SERVICE_UUID),
            )
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult,
            ) {
                trySend(result)
            }

            override fun onScanFailed(errorCode: Int) {
                Timber.e("BLE scan failed: $errorCode")
                close(
                    IllegalStateException(
                        "BLE scan failed: $errorCode",
                    ),
                )
            }
        }

        Timber.d("Starting BLE scan")
        scanner.startScan(
            listOf(filter), settings, callback,
        )

        awaitClose {
            Timber.d("Stopping BLE scan")
            scanner.stopScan(callback)
        }
    }
}
