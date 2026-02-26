package com.example.mobile_tracker.data.ble

import java.util.UUID

object BleConstants {

    /** GATT Service UUID (часы Activity Tracker). */
    val SERVICE_UUID: UUID =
        UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb")

    /** Write — передача контекста смены на часы. */
    val CHAR_SHIFT_CONTEXT: UUID =
        UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb")

    /** Write — запрос пакета с часов. */
    val CHAR_PACKET_REQUEST: UUID =
        UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb")

    /** Read / Notify — чанки зашифрованного пакета. */
    val CHAR_PACKET_DATA: UUID =
        UUID.fromString("0000ff04-0000-1000-8000-00805f9b34fb")

    /** Read — метаданные пакета. */
    val CHAR_PACKET_META: UUID =
        UUID.fromString("0000ff05-0000-1000-8000-00805f9b34fb")

    /** Read / Notify — статус часов. */
    val CHAR_STATUS: UUID =
        UUID.fromString("0000ff06-0000-1000-8000-00805f9b34fb")

    /** Write — ACK подтверждение приёма пакета. */
    val CHAR_ACK: UUID =
        UUID.fromString("0000ff07-0000-1000-8000-00805f9b34fb")

    /** CCC descriptor для подписки на Notify. */
    val CCC_DESCRIPTOR: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val CONNECT_TIMEOUT_MS = 30_000L
    const val SCAN_TIMEOUT_MS = 15_000L
    const val REQUESTED_MTU = 512
    const val CHUNK_HEADER_SIZE = 2
}
