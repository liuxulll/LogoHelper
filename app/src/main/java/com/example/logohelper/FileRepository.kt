package com.example.logohelper

import android.os.Environment
import java.io.File

/**
 * E听说缓存目录扫描：找出 resource/ 下包含 content.json 的作业文件夹，
 * 按 lastModified 降序排序并逐个解析。
 */
object FileRepository {

    /** 目标 resource 目录 */
    fun resourceDir(): File {
        val external = Environment.getExternalStorageDirectory() // /storage/emulated/0
        return File(
            external,
            "Android/data/com.ets100.secondary/files/Download/ETS_secondary/resource"
        )
    }

    sealed class ScanResult {
        /** 扫描成功 */
        data class Success(val homeworks: List<ParsedHomework>) : ScanResult()
        /** 目录不存在 */
        object DirMissing : ScanResult()
        /** 目录存在但无含 content.json 的子文件夹 */
        object Empty : ScanResult()
        /** 权限不足或 IO 错误 */
        data class Error(val message: String) : ScanResult()
    }

    /**
     * 必须在 IO 线程调用（Dispatchers.IO）。
     */
    fun scan(): ScanResult {
        val dir = resourceDir()
        if (!dir.exists() || !dir.isDirectory) {
            return ScanResult.DirMissing
        }
        val subDirs = dir.listFiles { f -> f.isDirectory }?.toList() ?: emptyList()
        val withContent = subDirs.filter { File(it, "content.json").isFile }
        if (withContent.isEmpty()) {
            return ScanResult.Empty
        }
        // 按修改时间降序，最新的在最前
        val sorted = withContent.sortedByDescending { it.lastModified() }

        val homeworks = mutableListOf<ParsedHomework>()
        for (folder in sorted) {
            try {
                val content = File(folder, "content.json")
                val parsed = EtsParser.parse(
                    contentJson = content,
                    folderName = folder.name,
                    folderPath = folder.absolutePath,
                    lastModified = folder.lastModified()
                )
                homeworks.add(parsed)
            } catch (e: Exception) {
                // 单个文件损坏不影响整体，跳过即可
            }
        }
        if (homeworks.isEmpty()) {
            return ScanResult.Error("缓存文件均无法解析，请确认 E听说作业已下载完成。")
        }
        return ScanResult.Success(homeworks)
    }
}
