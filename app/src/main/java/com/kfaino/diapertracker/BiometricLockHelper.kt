package com.kfaino.diapertracker

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * 生物识别指纹与面容隐私锁管理工具 (Biometric Lock Helper)
 */
object BiometricLockHelper {

    // 运行时会话状态 (避免在前台操作时频繁弹窗)
    var isUnlockedThisSession = false

    /** 检查设备是否支持生物识别或设备凭据锁屏 */
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /** 调起系统原生生物识别认证弹窗 */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "🔐 身份验证",
        subtitle: String = "请验证指纹或面容以访问资产管家",
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isUnlockedThisSession = true
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError?.invoke(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        biometricPrompt.authenticate(promptInfo)
    }
}
