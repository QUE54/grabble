package com.qin.r4rickroll

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.app.NotificationCompat
import java.util.UUID

class BleScanService : Service() {

    // ===================== ตั้งค่า (ต้องตรงกับฝั่ง Arduino) =====================
    private val SERVICE_UUID: UUID = UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214")
    private val CHAR_UUID: UUID = UUID.fromString("19b10001-e8f2-537e-4f6c-d104768a1214")

    // ระยะ ~30cm โดยประมาณ: ปรับค่านี้ตามการทดสอบจริง (ยิ่งใกล้ 0 = ยิ่งใกล้ตัว)
    // ค่าทั่วไป: -40 ถึง -55 คือใกล้มาก (ไม่กี่สิบ ซม.), -70 ลงไปคือเริ่มไกล (หลายเมตร)
    private val RSSI_THRESHOLD = -50

    // กันเปิดซ้ำถี่ๆ ขณะยังอยู่ใกล้กัน (มิลลิวินาที)
    private val COOLDOWN_MS = 30_000L

    // ลิงก์ที่จะเปิด — เปลี่ยนเป็นวิดีโอ/URL ที่ต้องการได้
    private val YOUTUBE_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    // ================================================================================

    private var lastTriggerTime = 0L
    private var gatt: BluetoothGatt? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val bluetoothManager by lazy { getSystemService(BluetoothManager::class.java) }
    private val adapter: BluetoothAdapter? by lazy { bluetoothManager?.adapter }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("กำลังค้นหา R4..."))
        startScanning()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "R4 Key",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startScanning() {
        val scanner = adapter?.bluetoothLeScanner ?: return

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
        } catch (e: SecurityException) {
            // ผู้ใช้ยังไม่ได้ให้สิทธิ์ BLUETOOTH_SCAN — service จะหยุดตัวเอง
            stopSelf()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.rssi >= RSSI_THRESHOLD) {
                maybeTrigger(result)
            }
        }
    }

    private fun maybeTrigger(result: ScanResult) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < COOLDOWN_MS) return
        lastTriggerTime = now

        updateNotification("พบ R4 แล้ว! (RSSI ${result.rssi}) กำลังเปิด YouTube...")
        openYoutubeInBrowser()
        connectForFeedback(result)
    }

    private fun openYoutubeInBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    // เชื่อมต่อ GATT สั้นๆ เพื่อ "ยืนยันตัว" กับบอร์ด ทำให้ไฟ LED บน R4 ติด
    // (ไม่บังคับ — ถ้าไม่ต้องการฟีเจอร์นี้ลบฟังก์ชันนี้ทิ้งได้ ไม่กระทบการเปิด YouTube)
    private fun connectForFeedback(result: ScanResult) {
        try {
            gatt = result.device.connectGatt(this, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        g.discoverServices()
                    }
                }

                override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                    val characteristic: BluetoothGattCharacteristic? =
                        g.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID)
                    if (characteristic != null) {
                        characteristic.value = byteArrayOf(1)
                        g.writeCharacteristic(characteristic)
                    }
                    // ปิดการเชื่อมต่อหลังจากนั้นไม่นาน ไม่ต้องค้างไว้
                    mainHandler.postDelayed({ g.disconnect(); g.close() }, 2000)
                }
            })
        } catch (e: SecurityException) {
            // ไม่มีสิทธิ์ BLUETOOTH_CONNECT — ข้ามส่วนนี้ไป การเปิด YouTube ยังทำงานปกติ
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("R4 Key")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) { /* ไม่มีสิทธิ์แล้ว ไม่ต้องทำอะไรต่อ */ }
        gatt?.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "r4_key_channel"
        private const val NOTIFICATION_ID = 1
    }
}
