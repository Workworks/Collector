package com.kfaino.diapertracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.CaptureManager
import com.kfaino.diapertracker.databinding.ActivityScannerBinding

/**
 * 智能扫码相机 Activity (基于 ZXing 离线解码)
 * 支持识别收纳箱二维码 collecter://room?house=...&room=... 以及通用商品条码
 */
class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding
    private lateinit var captureManager: CaptureManager
    private var isTorchOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        captureManager = CaptureManager(this, binding.barcodeScannerView)
        captureManager.initializeFromIntent(intent, savedInstanceState)
        captureManager.decode()

        binding.btnScannerBack.applyPressScaleAnimation(0.90f)
        binding.btnScannerBack.setOnClickListener { finish() }

        binding.btnScannerTorch.applyPressScaleAnimation(0.90f)
        binding.btnScannerTorch.setOnClickListener {
            isTorchOn = !isTorchOn
            if (isTorchOn) {
                binding.barcodeScannerView.setTorchOn()
            } else {
                binding.barcodeScannerView.setTorchOff()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        captureManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        captureManager.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        captureManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
