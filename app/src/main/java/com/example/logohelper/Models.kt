package com.example.logohelper

import java.io.Serializable

/**
 * 单道题目解析结果。
 * type: "role"（对话/听力选择题）或 "picture"（思维导图转述题）
 */
data class ParsedQuestion(
    val index: Int,
    val type: String,
    val questionText: String,
    val answerText: String,
    val keywords: String? = null
) : Serializable

/**
 * 一次作业（resource/ 下一个文件夹）的解析结果。
 */
data class ParsedHomework(
    val folderName: String,
    val folderPath: String,
    val lastModified: Long,
    val stid: String?,
    val questions: List<ParsedQuestion>
) : Serializable
