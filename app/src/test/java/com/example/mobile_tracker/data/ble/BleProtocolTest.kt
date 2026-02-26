package com.example.mobile_tracker.data.ble

import android.bluetooth.le.ScanResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BleProtocolTest {

    private lateinit var scanner: BleScanner
    private lateinit var gattClient: GattClient
    private lateinit var protocol: BleProtocol

    @Before
    fun setup() {
        scanner = mockk(relaxed = true)
        gattClient = mockk(relaxed = true)
        protocol = BleProtocol(scanner, gattClient)
    }

    // region findDevice

    @Test
    fun `findDevice returns first matching result`() =
        runTest {
            val scanResult = mockk<ScanResult>(
                relaxed = true,
            )
            val device = mockk<android.bluetooth.BluetoothDevice>(
                relaxed = true,
            )
            every { scanResult.device } returns device
            every { device.name } returns "dev_abc123"
            every { device.address } returns "AA:BB:CC"

            every { scanner.scan() } returns
                flowOf(scanResult)

            val result = protocol.findDevice(
                "dev_abc123",
            )
            assertEquals(scanResult, result)
        }

    @Test(expected = BleException.ScanTimeout::class)
    fun `findDevice throws ScanTimeout when no match`() =
        runTest {
            val scanResult = mockk<ScanResult>(
                relaxed = true,
            )
            val device = mockk<android.bluetooth.BluetoothDevice>(
                relaxed = true,
            )
            every { scanResult.device } returns device
            every { device.name } returns "other_device"
            every { device.address } returns "XX:YY:ZZ"

            every { scanner.scan() } returns
                flowOf(scanResult)

            protocol.findDevice("dev_abc123")
        }

    // endregion

    // region connectAndSetup

    @Test
    fun `connectAndSetup calls connect, discover, mtu`() =
        runTest {
            val scanResult = mockk<ScanResult>(
                relaxed = true,
            )
            val gatt = mockk<android.bluetooth.BluetoothGatt>(
                relaxed = true,
            )

            coEvery {
                gattClient.connect(any())
            } returns gatt
            coEvery {
                gattClient.discoverServices()
            } returns Unit
            coEvery {
                gattClient.requestMtu(any())
            } returns 512

            protocol.connectAndSetup(scanResult)

            coVerify { gattClient.connect(any()) }
            coVerify { gattClient.discoverServices() }
            coVerify { gattClient.requestMtu(any()) }
        }

    // endregion

    // region sendShiftContext

    @Test
    fun `sendShiftContext writes JSON to characteristic`() =
        runTest {
            coEvery {
                gattClient.writeCharacteristic(
                    any(), any(),
                )
            } returns Unit

            val payload = BleProtocol.ShiftContextPayload(
                shiftId = "shift-1",
                employeeId = "emp-1",
                siteId = "site-1",
                startTsMs = 1000L,
                plannedEndTsMs = 2000L,
            )

            protocol.sendShiftContext(payload)

            coVerify {
                gattClient.writeCharacteristic(
                    BleConstants.CHAR_SHIFT_CONTEXT,
                    any(),
                )
            }
        }

    // endregion

    // region requestPacketMeta

    @Test
    fun `requestPacketMeta writes command and reads meta`() =
        runTest {
            val metaJson = """
                {
                    "packet_id": "pkt-1",
                    "device_id": "dev-1",
                    "shift_start_ts": 1000,
                    "shift_end_ts": 2000,
                    "schema_version": 1,
                    "payload_hash": "abc123",
                    "total_chunks": 10,
                    "chunk_size": 512,
                    "total_size_bytes": 5120,
                    "payload_key_enc": "key==",
                    "iv": "iv=="
                }
            """.trimIndent()

            coEvery {
                gattClient.writeCharacteristic(
                    any(), any(),
                )
            } returns Unit
            coEvery {
                gattClient.readCharacteristic(
                    BleConstants.CHAR_PACKET_META,
                )
            } returns metaJson.toByteArray(
                Charsets.UTF_8,
            )

            val meta = protocol.requestPacketMeta()

            assertEquals("pkt-1", meta.packetId)
            assertEquals("dev-1", meta.deviceId)
            assertEquals(10, meta.totalChunks)
            assertEquals(5120, meta.totalSizeBytes)
            assertEquals("key==", meta.payloadKeyEnc)
            assertEquals("iv==", meta.iv)

            coVerify {
                gattClient.writeCharacteristic(
                    BleConstants.CHAR_PACKET_REQUEST,
                    any(),
                )
            }
        }

    // endregion

    // region readPacketChunks

    @Test
    fun `readPacketChunks assembles all chunks`() =
        runTest {
            val totalChunks = 3
            val chunkDataSize = 4
            val totalSize = totalChunks * chunkDataSize
            val meta = PacketMeta(
                packetId = "pkt-1",
                deviceId = "dev-1",
                shiftStartTs = 1000L,
                shiftEndTs = 2000L,
                payloadHash = "hash",
                totalChunks = totalChunks,
                chunkSize = chunkDataSize +
                    BleConstants.CHUNK_HEADER_SIZE,
                totalSizeBytes = totalSize,
                payloadKeyEnc = "key==",
                iv = "iv==",
            )

            val channel = Channel<ByteArray>(
                Channel.BUFFERED,
            )
            every { gattClient.notifyChannel } returns
                channel
            every {
                gattClient.enableNotify(any())
            } returns Unit

            // Создаём чанки: [index: 2 bytes][data: N bytes]
            val chunks = (0 until totalChunks).map { i ->
                val buf = ByteBuffer.allocate(
                    BleConstants.CHUNK_HEADER_SIZE +
                        chunkDataSize,
                )
                buf.order(ByteOrder.BIG_ENDIAN)
                buf.putShort(i.toShort())
                buf.put(
                    ByteArray(chunkDataSize) {
                        (i + 1).toByte()
                    },
                )
                buf.array()
            }

            // Отправляем чанки в канал параллельно
            kotlinx.coroutines.launch {
                for (chunk in chunks) {
                    channel.send(chunk)
                }
            }

            val result = protocol.readPacketChunks(meta)

            assertNotNull(result)
            // Результат — Base64 строка
            assert(result.isNotBlank())
        }

    @Test(
        expected = BleException.IncompletePacket::class,
    )
    fun `readPacketChunks throws on incomplete`() =
        runTest {
            val meta = PacketMeta(
                packetId = "pkt-1",
                deviceId = "dev-1",
                shiftStartTs = 1000L,
                shiftEndTs = 2000L,
                payloadHash = "hash",
                totalChunks = 10,
                chunkSize = 514,
                totalSizeBytes = 5120,
                payloadKeyEnc = "key==",
                iv = "iv==",
            )

            val channel = Channel<ByteArray>(
                Channel.BUFFERED,
            )
            every { gattClient.notifyChannel } returns
                channel
            every {
                gattClient.enableNotify(any())
            } returns Unit

            // Отправляем только 2 из 10 чанков
            kotlinx.coroutines.launch {
                repeat(2) { i ->
                    val buf = ByteBuffer.allocate(6)
                    buf.order(ByteOrder.BIG_ENDIAN)
                    buf.putShort(i.toShort())
                    buf.put(ByteArray(4) { 0x01 })
                    channel.send(buf.array())
                }
                channel.close()
            }

            protocol.readPacketChunks(meta)
        }

    // endregion

    // region sendAck

    @Test
    fun `sendAck writes ACK to characteristic`() =
        runTest {
            coEvery {
                gattClient.writeCharacteristic(
                    any(), any(),
                )
            } returns Unit

            protocol.sendAck("pkt-1", 10)

            coVerify {
                gattClient.writeCharacteristic(
                    BleConstants.CHAR_ACK,
                    any(),
                )
            }
        }

    // endregion

    // region sendNack

    @Test
    fun `sendNack writes NACK to characteristic`() =
        runTest {
            coEvery {
                gattClient.writeCharacteristic(
                    any(), any(),
                )
            } returns Unit

            protocol.sendNack("pkt-1")

            coVerify {
                gattClient.writeCharacteristic(
                    BleConstants.CHAR_ACK,
                    any(),
                )
            }
        }

    // endregion

    // region disconnect

    @Test
    fun `disconnect delegates to gattClient`() {
        protocol.disconnect()
        verify { gattClient.disconnect() }
    }

    // endregion
}
