package com.example.logohelper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

/**
 * 权限检查与跳转的统一封装。
 * 内部按 Android 版本分三档：
 *  - Android 8-9（26-28）：READ_EXTERNAL_STORAGE 运行时申请
 *  - Android 10（29）：READ_EXTERNAL_STORAGE + manifest 的 requestLegacyExternalStorage
 *  - Android 11+（30+）：MANAGE_EXTERNAL_STORAGE（所有文件访问）
 * 主界面只调用这里暴露的四个方法，不关心版本差异。
 */
object PermissionHelper {

    private const val REQ_STORAGE = 1001

    val STORAGE_PERMISSION_REQUEST_CODE = REQ_STORAGE

    /** 当前是否具备读取 E听说缓存目录的存储权限 */
    fun hasStorageAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+：所有文件访问权限
            Environment.isExternalStorageManager()
        } else {
            // Android 8-10：传统外置存储读取权限
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** 发起存储权限申请：低版本走运行时弹窗，高版本跳系统设置页 */
    fun requestStorageAccess(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = try {
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
            } catch (e: Exception) {
                // 个别 ROM 不支持按包名定位，退回总设置页
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            }
            try {
                activity.startActivity(intent)
            } catch (e: Exception) {
                activity.startActivity(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQ_STORAGE
            )
        }
    }

    /** 当前是否具备悬浮窗权限（Android 8+ 统一 canDrawOverlays） */
    fun hasOverlayAccess(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /** 跳转悬浮窗权限设置页 */
    fun requestOverlayAccess(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            )
        }
    }

    /** Android 13+ 才需要通知运行时权限；8-12 恒为 true */
    fun hasNotificationAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, "android.permission.POST_NOTIFICATIONS"
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** 存储权限卡片的标题文案，按版本区分 */
    fun storageCardTitle(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "所有文件访问权限"
        } else {
            "存储权限"
        }
    }
}
