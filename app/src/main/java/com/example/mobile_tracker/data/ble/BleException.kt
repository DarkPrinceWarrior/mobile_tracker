package com.example.mobile_tracker.data.ble

sealed class BleException(
    override val message: String,
) : Exception(message) {

    class NotConnected(message: String) :
        BleException(message)

    class ServiceNotFound(message: String) :
        BleException(message)

    class CharacteristicNotFound(message: String) :
        BleException(message)

    class ConnectionLost(message: String) :
        BleException(message)

    class ScanTimeout(message: String) :
        BleException(message)

    class ConnectTimeout(message: String) :
        BleException(message)

    class GattError(
        message: String,
        val status: Int,
    ) : BleException("$message (status=$status)")

    class IncompletePacket(
        message: String,
        val received: Int,
        val expected: Int,
    ) : BleException(message)

    class PermissionDenied(message: String) :
        BleException(message)
}
