package com.example.logohelper

/**
 * 单道题目解析结果。
 * category 取值："听选信息" / "回答问题" / "模仿朗读"
 */
data class ParsedQuestion(
    val index: Int,
    val type: String,        // "role" 或 "picture"
    val category: String,    // 小节名
    val questionText: String,
    val answerText: String,
    val keywords: String? = null,
    val alternatives: List<String> = emptyList()  // 其他可用备选答案（最多 5 条）
)

/**
 * 一次作业（resource/ 下一个文件夹）的解析结果。
 * questions 已按 听选信息 → 回答问题 → 模仿朗读 智能排序。
 */
data class ParsedHomework(
    val folderName: String,
    val folderPath: String,
    val lastModified: Long,
    val stid: String?,
    val questions: List<ParsedQuestion>
) {

    /** 各小节题数摘要，如 "听选信息6题 · 回答问题4题 · 模仿朗读1题" */
    val categoriesSummary: String
        get() = if (questions.isEmpty()) "未解析出题目"
        else questions.groupingBy { it.category }.eachCount()
            .toList().sortedBy { EtsParser.categoryOrder(it.first) }
            .joinToString(" · ") { "${it.first}${it.second}题" }
}
