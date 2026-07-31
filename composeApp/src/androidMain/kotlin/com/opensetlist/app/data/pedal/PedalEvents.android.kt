package com.opensetlist.app.data.pedal

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.UUID

private class BleMidiPedalController(
    private val context: Context,
    private val onEvent: (PedalEvent) -> Unit
) {
    private val midiServiceUuid = UUID.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700")
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var adapter: BluetoothAdapter? = null
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var scanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let { device ->
                stopScan()
                connect(device)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                this@BleMidiPedalController.gatt?.close()
                this@BleMidiPedalController.gatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(midiServiceUuid) ?: return
            service.characteristics.forEach { characteristic ->
                val props = characteristic.properties
                val wantsNotify =
                    props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 ||
                        props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
                if (wantsNotify) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(cccdUuid)
                    if (descriptor != null) {
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value ?: return
            parseBleMidi(data) { note ->
                mapNoteToEvent(note)?.let { onEvent(it) }
            }
        }
    }

    fun start() {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: return
        adapter = manager.adapter ?: return
        scanner = adapter?.bluetoothLeScanner ?: return
        startScan()
    }

    fun stop() {
        stopScan()
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    private fun startScan() {
        if (scanning) return
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(midiServiceUuid))
            .build()
        scanning = true
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private fun stopScan() {
        if (scanning) {
            scanner?.stopScan(scanCallback)
            scanning = false
        }
    }

    private fun connect(device: BluetoothDevice) {
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private fun mapNoteToEvent(note: Int): PedalEvent? = when (note) {
        0, 60 -> PedalEvent.PREVIOUS
        1, 61 -> PedalEvent.NEXT
        2, 62 -> PedalEvent.PLAY_PAUSE
        else -> null
    }
}

private fun parseBleMidi(data: ByteArray, onNoteOn: (Int) -> Unit) {
    var i = 0
    var lastStatus = 0
    while (i < data.size) {
        val first = data[i].toInt() and 0xFF
        if (first and 0x80 == 0) {
            i++
            continue
        }
        i++
        var status = 0
        var note = -1
        var velocity = -1
        var count = 0
        while (i < data.size && count < 3) {
            val mb = data[i].toInt() and 0xFF
            if (mb and 0x80 == 0) break
            val value = mb and 0x7F
            when (count) {
                0 -> status = value
                1 -> note = value
                2 -> velocity = value
            }
            count++
            i++
        }
        if (status != 0) lastStatus = status
        val effectiveStatus = if (status != 0) status else lastStatus
        if ((effectiveStatus and 0xF0) == 0x90 && velocity > 0 && note != -1) {
            onNoteOn(note)
        }
    }
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

@Composable
actual fun rememberPedalEvents(onEvent: (PedalEvent) -> Unit): PedalState {
    val context = LocalContext.current
    val currentOnEvent = rememberUpdatedState(onEvent)
    var enabled by remember { mutableStateOf(false) }

    val controller = remember {
        BleMidiPedalController(context.applicationContext) { event ->
            currentOnEvent.value(event)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (granted) controller.start() else enabled = false
    }

    LaunchedEffect(enabled) {
        if (enabled) {
            val missing = requiredPermissions().filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                permissionLauncher.launch(missing.toTypedArray())
            } else {
                controller.start()
            }
        } else {
            controller.stop()
        }
    }

    DisposableEffect(Unit) {
        onDispose { controller.stop() }
    }

    return PedalState(
        isEnabled = enabled,
        setEnabled = { enabled = it }
    )
}
