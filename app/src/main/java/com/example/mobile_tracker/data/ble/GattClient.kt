package com.example.mobile_tracker.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Обёртка над Android BluetoothGatt, превращающая
 * callback-API в suspend-функции.
 */
class GattClient(private val context: Context) {

    private var gatt: BluetoothGatt? = null

    /** Канал для получения Notify-данных. */
    val notifyChannel = Channel<ByteArray>(
        Channel.BUFFERED,
    )

    private var connectCont:
        CancellableContinuation<BluetoothGatt>? = null
    private var discoverCont:
        CancellableContinuation<Unit>? = null
    private var mtuCont:
        CancellableContinuation<Int>? = null
    private var writeCont:
        CancellableContinuation<Unit>? = null
    private var readCont:
        CancellableContinuation<ByteArray>? = null

    private val gattCallback = object :
        BluetoothGattCallback() {

        override fun onConnectionStateChange(
            g: BluetoothGatt,
            status: Int,
            newState: Int,
        ) {
            Timber.d(
                "GATT state: status=$status " +
                    "newState=$newState",
            )
            if (newState == BluetoothProfile.STATE_CONNECTED
                && status == BluetoothGatt.GATT_SUCCESS
            ) {
                connectCont?.resume(g)
                connectCont = null
            } else if (
                newState ==
                    BluetoothProfile.STATE_DISCONNECTED
            ) {
                val err = BleException.ConnectionLost(
                    "GATT disconnected, status=$status",
                )
                connectCont?.resumeWithException(err)
                connectCont = null
                notifyChannel.close(err)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(
            g: BluetoothGatt,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                discoverCont?.resume(Unit)
            } else {
                discoverCont?.resumeWithException(
                    BleException.GattError(
                        "Service discovery failed",
                        status,
                    ),
                )
            }
            discoverCont = null
        }

        override fun onMtuChanged(
            g: BluetoothGatt,
            mtu: Int,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("MTU negotiated: $mtu")
                mtuCont?.resume(mtu)
            } else {
                mtuCont?.resumeWithException(
                    BleException.GattError(
                        "MTU request failed",
                        status,
                    ),
                )
            }
            mtuCont = null
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeCont?.resume(Unit)
            } else {
                writeCont?.resumeWithException(
                    BleException.GattError(
                        "Write failed: " +
                            "${characteristic.uuid}",
                        status,
                    ),
                )
            }
            writeCont = null
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCont?.resume(
                    characteristic.value ?: byteArrayOf(),
                )
            } else {
                readCont?.resumeWithException(
                    BleException.GattError(
                        "Read failed: " +
                            "${characteristic.uuid}",
                        status,
                    ),
                )
            }
            readCont = null
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            val data = characteristic.value
                ?: return
            notifyChannel.trySend(data)
        }
    }

    /**
     * Подключение к BLE-устройству с таймаутом.
     * Возвращает [BluetoothGatt] после успешного
     * подключения.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(
        device: BluetoothDevice,
        timeoutMs: Long = BleConstants.CONNECT_TIMEOUT_MS,
    ): BluetoothGatt = withTimeout(timeoutMs) {
        suspendCancellableCoroutine { cont ->
            connectCont = cont
            gatt = device.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE,
            )
            cont.invokeOnCancellation { disconnect() }
        }
    }

    /** Обнаружение GATT-сервисов. */
    @SuppressLint("MissingPermission")
    suspend fun discoverServices() {
        suspendCancellableCoroutine { cont ->
            discoverCont = cont
            if (gatt?.discoverServices() != true) {
                cont.resumeWithException(
                    BleException.GattError(
                        "discoverServices() returned false",
                        -1,
                    ),
                )
                discoverCont = null
            }
        }
    }

    /** Запрос максимального MTU. */
    @SuppressLint("MissingPermission")
    suspend fun requestMtu(
        mtu: Int = BleConstants.REQUESTED_MTU,
    ): Int = suspendCancellableCoroutine { cont ->
        mtuCont = cont
        if (gatt?.requestMtu(mtu) != true) {
            cont.resumeWithException(
                BleException.GattError(
                    "requestMtu() returned false",
                    -1,
                ),
            )
            mtuCont = null
        }
    }

    /**
     * Запись данных в характеристику.
     * Использует WRITE_TYPE_DEFAULT (с подтверждением).
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    suspend fun writeCharacteristic(
        charUuid: UUID,
        data: ByteArray,
    ) {
        val g = requireGatt()
        val svc = g.getService(BleConstants.SERVICE_UUID)
            ?: throw BleException.ServiceNotFound(
                "Service not found",
            )
        val char = svc.getCharacteristic(charUuid)
            ?: throw BleException.CharacteristicNotFound(
                "Characteristic $charUuid not found",
            )
        char.value = data
        char.writeType =
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        suspendCancellableCoroutine { cont ->
            writeCont = cont
            if (!g.writeCharacteristic(char)) {
                cont.resumeWithException(
                    BleException.GattError(
                        "writeCharacteristic failed",
                        -1,
                    ),
                )
                writeCont = null
            }
        }
    }

    /** Чтение данных из характеристики. */
    @SuppressLint("MissingPermission")
    suspend fun readCharacteristic(
        charUuid: UUID,
    ): ByteArray {
        val g = requireGatt()
        val svc = g.getService(BleConstants.SERVICE_UUID)
            ?: throw BleException.ServiceNotFound(
                "Service not found",
            )
        val char = svc.getCharacteristic(charUuid)
            ?: throw BleException.CharacteristicNotFound(
                "Characteristic $charUuid not found",
            )

        return suspendCancellableCoroutine { cont ->
            readCont = cont
            if (!g.readCharacteristic(char)) {
                cont.resumeWithException(
                    BleException.GattError(
                        "readCharacteristic failed",
                        -1,
                    ),
                )
                readCont = null
            }
        }
    }

    /**
     * Подписка на Notify для характеристики.
     * Данные приходят в [notifyChannel].
     */
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    fun enableNotify(charUuid: UUID) {
        val g = requireGatt()
        val svc = g.getService(BleConstants.SERVICE_UUID)
            ?: throw BleException.ServiceNotFound(
                "Service not found",
            )
        val char = svc.getCharacteristic(charUuid)
            ?: throw BleException.CharacteristicNotFound(
                "Characteristic $charUuid not found",
            )

        g.setCharacteristicNotification(char, true)

        val descriptor = char.getDescriptor(
            BleConstants.CCC_DESCRIPTOR,
        )
        descriptor?.let {
            it.value =
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(it)
        }
        Timber.d("Notify enabled for $charUuid")
    }

    /** Закрытие GATT-соединения. */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        Timber.d("GATT disconnected")
    }

    private fun requireGatt(): BluetoothGatt =
        gatt ?: throw BleException.NotConnected(
            "GATT not connected",
        )
}
