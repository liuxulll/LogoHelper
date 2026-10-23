package com.example.logohelper

import java.io.File

/**
 * content.json 解析器。
 * 支持两种 structure_type：
 *  - collector.role    ：对话/听力选择题
 *  - collector.picture ：思维导图转述题
 */
object EtsParser {

    /** 清理 </br>、<br>、<br/>、<p>、</p> 等 HTML 标签 */
    private fun stripTags(text: String): String {
        return text
            .replace("(?i)<br\\s*/?>".toRegex(), "\n")
            .replace("(?i)</br>".toRegex(), "")
            .replace("(?i)<p\\s*/?>".toRegex(), "")
            .replace("(?i)</p>".toRegex(), "")
            .trim()
    }

    /**
     * 题目显示清理：
     *  1. <br> 系列换成换行、去残留标签、去首尾空白；
     *  2. 括号内的 A./B./C. 选项拆成独立行。
     */
    private fun cleanQuestionText(raw: String): String {
        var text = stripTags(raw)
        // 在 A. / B. / C. / D. 标记前插入换行（仅处理最后一个括号段中的选项）
        text = text.replace(Regex("""(?<=[\s(\[（])([A-D])\.\s*""")) { m ->
            "\n" + m.groupValues[1] + ". "
        }
        // 合并多余空行并去首尾空白
        text = text.lines().map { it.trim() }.filterIndexed { i, line ->
            !(line.isEmpty() && i > 0 && text.lines()[i - 1].isBlank())
        }.joinToString("\n")
        return text.trim()
    }

    /** std 数组取“去标签后长度最短”的一项；并列取第一个 */
    private fun shortestStd(json: org.json.JSONArray): String {
        var best = ""
        var bestLen = Int.MAX_VALUE
        for (i in 0 until json.length()) {
            val obj = json.optJSONObject(i) ?: continue
            val value = stripTags(obj.optString("value", ""))
            if (value.length < bestLen) {
                bestLen = value.length
                best = value
            }
        }
        return best
    }

    /**
     * 解析一个 content.json 文件，返回该作业的题目列表。
     * 解析失败时抛出异常，由调用方逐文件 try-catch。
     */
    fun parse(
        contentJson: File,
        folderName: String,
        folderPath: String,
        lastModified: Long
    ): ParsedHomework {
        val root = org.json.JSONObject(contentJson.readText(Charsets.UTF_8))
        val structureType = root.optString("structure_type", "")
        val info = root.optJSONObject("info")
            ?: throw IllegalStateException("missing info")

        val questions = mutableListOf<ParsedQuestion>()
        var stid: String? = null

        when (structureType) {
            "collector.role" -> {
                stid = info.optString("stid", "").ifEmpty { null }
                val questionArr = info.optJSONArray("question") ?: org.json.JSONArray()
                for (i in 0 until questionArr.length()) {
                    val q = questionArr.optJSONObject(i) ?: continue
                    val ask = cleanQuestionText(q.optString("ask", ""))
                    val stdArr = q.optJSONArray("std") ?: org.json.JSONArray()
                    val answer = shortestStd(stdArr)
                    val keywords = q.optString("keywords", "").ifEmpty { null }
                    questions.add(
                        ParsedQuestion(
                            index = i,
                            type = "role",
                            questionText = ask,
                            answerText = answer,
                            keywords = keywords
                        )
                    )
                }
            }

            "collector.picture" -> {
                val topic = stripTags(info.optString("topic", ""))
                val stdArr = info.optJSONArray("std") ?: org.json.JSONArray()
                // 思维导图转述题：答案取 std[0].value（最短的第一版完整段落）
                val answer = if (stdArr.length() > 0) {
                    stripTags(stdArr.optJSONObject(0)?.optString("value", "") ?: "")
                } else ""
                val keypoint = info.optString("keypoint", "").ifEmpty { null }
                if (topic.isNotEmpty() || answer.isNotEmpty()) {
                    questions.add(
                        ParsedQuestion(
                            index = 0,
                            type = "picture",
                            questionText = "（思维导图转述题）\n$topic",
                            answerText = answer,
                            keywords = keypoint
                        )
                    )
                }
            }

            else -> {
                // 未知类型：留空列表，UI 会显示“未解析出题目”
            }
        }

        return ParsedHomework(
            folderName = folderName,
            folderPath = folderPath,
            lastModified = lastModified,
            stid = stid,
            questions = questions
        )
    }
}
