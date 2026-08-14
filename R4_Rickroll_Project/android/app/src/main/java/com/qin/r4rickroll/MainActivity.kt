package com.qin.r4rickroll

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    // รายชื่อ permission ที่ต้องขอ ขึ้นอยู่กับเวอร์ชัน Android
    private val requiredPermissions: Array<String>
        get() = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                startScanService()
            } else {
                Toast.makeText(
                    this,
                    "ต้องอนุญาตสิทธิ์ Bluetooth/แจ้งเตือนก่อน ถึงจะสแกนหา R4 ได้",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        findViewById<Button>(R.id.startButton).setOnClickListener { requestPermissionsThenStart() }
        findViewById<Button>(R.id.stopButton).setOnClickListener { stopScanService() }
    }

    private fun requestPermissionsThenStart() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startScanService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startScanService() {
        val intent = Intent(this, BleScanService::class.java)
        ContextCompat.startForegroundService(this, intent)
        statusText.text = "สถานะ: กำลังสแกนหา R4 อยู่เบื้องหลัง..."
    }

    private fun stopScanService() {
        stopService(Intent(this, BleScanService::class.java))
        statusText.text = "สถานะ: หยุดแล้ว"
    }
}
