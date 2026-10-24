package com.example.logohelper

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.example.logohelper.ui.LogoHelperTheme
import com.example.logohelper.ui.MainScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 主界面状态快照（传给 Compose） */
data class MainUiState(
    val storageGranted: Boolean,
    val storageTitle: String,
    val scanning: Boolean,
    val homeworks: List<ParsedHomework>,
    val selectedPath: String?,
    val notice: String?,
    val showKeywords: Boolean,
    val onToggleKeywords: () -> Unit
) {
    val selectedHomework: ParsedHomework?
        get() = homeworks.firstOrNull { it.folderPath == selectedPath }
}

class MainActivity : ComponentActivity() {

    private var storageGranted by mutableStateOf(false)
    private var scanning by mutableStateOf(false)
    private var homeworks by mutableStateOf<List<ParsedHomework>>(emptyList())
    private var selectedPath by mutableStateOf<String?>(null)
    private var notice by mutableStateOf<String?>(null)
    private var showKeywords by mutableStateOf(false)

    // 运行时权限申请（Android 8-10 存储）
    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LogoHelperTheme {
                // 从系统设置页返回（onResume）时刷新权限状态
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                MainScreen(
                    state = MainUiState(
                        storageGranted = storageGranted,
                        storageTitle = PermissionHelper.storageCardTitle(),
                        scanning = scanning,
                        homeworks = homeworks,
                        selectedPath = selectedPath,
                        notice = notice,
                        showKeywords = showKeywords,
                        onToggleKeywords = { showKeywords = !showKeywords }
                    ),
                    onSelectHomework = { idx ->
                        homeworks.getOrNull(idx)?.let { selectedPath = it.folderPath }
                    },
                    onRefresh = ::refreshPermissionsAndScan,
                    onGrantStorage = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            PermissionHelper.requestStorageAccess(this)
                        } else {
                            storagePermissionLauncher.launch(
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        }
                    }
                )
            }
        }
    }

    private fun refreshPermissions() {
        storageGranted = PermissionHelper.hasStorageAccess(this)
        if (storageGranted && homeworks.isEmpty()) {
            scan()
        }
    }

    private fun refreshPermissionsAndScan() {
        refreshPermissions()
        scan()
    }

    private fun scan() {
        if (!storageGranted) {
            notice = "请先授予${PermissionHelper.storageCardTitle()}，才能读取 E听说缓存目录。"
            return
        }
        scanning = true
        notice = null
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { FileRepository.scan() }
            scanning = false
            when (result) {
                is FileRepository.ScanResult.Success -> {
                    homeworks = result.homeworks
                    // 默认选中最新（第一个）
                    if (selectedPath == null ||
                        homeworks.none { it.folderPath == selectedPath }
                    ) {
                        selectedPath = homeworks.first().folderPath
                    }
                }
                FileRepository.ScanResult.DirMissing -> {
                    homeworks = emptyList(); selectedPath = null
                    notice = "未找到 E听说缓存目录，请先在 E听说 App 中下载至少一份作业。"
                }
                FileRepository.ScanResult.Empty -> {
                    homeworks = emptyList(); selectedPath = null
                    notice = null // 走空状态文案
                }
                is FileRepository.ScanResult.Error -> {
                    homeworks = emptyList(); selectedPath = null
                    notice = result.message
                }
            }
        }
    }
}
