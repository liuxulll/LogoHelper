package com.example.logohelper

import org.json.JSONArray
import java.io.File

/**
 * content.json 解析器。
 *
 * 支持两种 structure_type：
 * - collector.role    ：对话/听力选择题
 * - collector.picture ：思维导图转述题（归入模仿朗读，排在最后）
 *
 * 答案挑选优先级：
 * 1. 题干括号选项强制匹配（最高）：std 中命中题干选项的那条直接选中。
 * 2. 语法结构梯队（次高）：主语陈述句 > To 不定式；剔除动词原形祈使句与极短碎片。
 * 3. 同梯队内取最短作为展示答案。
 * 4. 未被选中的候选保留最多 5 条，供界面「查看其他可用答案」折叠展开。
 *
 * 解析后按 听选信息 → 回答问题 → 模仿朗读 智能排序。
 */
object EtsParser {

    const val CATEGORY_LISTEN = "听选信息"
    const val CATEGORY_ANSWER = "回答问题"
    const val CATEGORY_READ = "模仿朗读"

    /** 排序权重：听选信息 → 回答问题 → 模仿朗读，未知类别排最后 */
    fun categoryOrder(category: String): Int = when (category) {
        CATEGORY_LISTEN -> 0
        CATEGORY_ANSWER -> 1
        CATEGORY_READ -> 2
        else -> 3
    }

    /** 低于该长度的答案视为无效碎片（单个词/字母），直接剔除 */
    private const val MIN_ANSWER_LENGTH = 4

    /** 主语开头词：代词主语 + 常见人名，判定为第一梯队陈述句 */
    private val SUBJECT_STARTERS = listOf(
        "I", "We", "You", "He", "She", "It", "They",
        "There", "This", "That", "These", "Those", "One",
        "Tina", "Mike", "Mary", "Peter", "Tom", "Anna", "Lucy", "Lily",
        "Bob", "John", "Jim", "Kate", "David", "Sally", "Grace", "Eric",
        "Frank", "Cindy", "Helen", "Paul", "Bill", "Amy", "Sara", "Jane",
        "Mr", "Mrs", "Ms", "Dr"
    )

    /** 第二梯队：以 To 开头的动词不定式短语 */
    private val TO_INF_INITIVE = Regex("""(?i)^to\s+\w""")

    /** 动词原形开头（祈使句），需绝对过滤 */
    private val VERB_FIRST_WORDS = setOf(
        "make", "take", "give", "get", "buy", "sell", "bring", "send",
        "show", "tell", "ask", "answer", "read", "write", "listen", "speak",
        "say", "talk", "play", "run", "swim", "jump", "walk", "go", "come",
        "eat", "drink", "cook", "wash", "clean", "open", "close", "turn",
        "throw", "catch", "hit", "kick", "pull", "push", "carry", "move",
        "put", "set", "cut", "draw", "sing", "dance", "study", "learn",
        "teach", "help", "want", "need", "like", "love", "hate", "try",
        "use", "watch", "visit", "call", "meet", "find", "lose", "leave",
        "start", "finish", "begin", "stop", "wait", "work", "do", "have",
        "has", "let", "keep", "enjoy", "practice", "remember", "forget",
        "think", "know", "believe", "hope", "wish", "choose", "change",
        "share", "show", "describe", "explain", "introduce", "retell"
    )

    /** 清理 </br>、<br>、<br/>、<p>、</p> 等 HTML 标签 */
    private fun stripTags(text: String): String {
        return text
            .replace(Regex("""(?i)<br\s*/?>"""), "\n")
            .replace(Regex("""(?i)</br>"""), "")
            .replace(Regex("""(?i)<p\s*/?>"""), "")
            .replace(Regex("""(?i)</p>"""), "")
            .trim()
    }

    /**
     * 题目显示清理：
     *  1. <br> 系列换成换行、去残留标签、去首尾空白；
     *  2. 括号内的 A./B./C. 选项拆成独立行。
     */
    private fun cleanQuestionText(raw: String): String {
        var text = stripTags(raw)
        text = text.replace(Regex("""(?<=[\s(\[（])([A-D])\.\s*""")) { m ->
            "\n" + m.groupValues[1] + ". "
        }
        text = text.lines().map { it.trim() }
            .filterIndexed { i, line ->
                !(line.isEmpty() && i > 0 && text.lines()[i - 1].isBlank())
            }.joinToString("\n")
        return text.trim()
    }

    /** 根据题目文本判断小节：含 A. B. 等两个以上选项标记 → 听选信息，否则 → 回答问题 */
    private fun detectCategory(cleanedAsk: String): String {
        val letters = Regex("""\b([A-D])[.]""").findAll(cleanedAsk)
            .map { it.groupValues[1] }.distinct().toList()
        return if (letters.size >= 2) CATEGORY_LISTEN else CATEGORY_ANSWER
    }

    /** 归一化用于比较：转小写、只保留字母数字与空格 */
    private fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    /**
     * 从原始 ask 文本中提取括号内的选项列表。
     * 取最后一个括号段，按 " / " 或 "/" 切分，并去掉 A./B./C. 前缀。
     */
    private fun extractOptions(rawAsk: String): List<String> {
        val stripped = stripTags(rawAsk)
        var lastClose = -1
        var lastOpen = -1
        for (i in stripped.length - 1 downTo 0) {
            val c = stripped[i]
            if (lastClose < 0 && (c == ')' || c == ')'.toString()[0])) {
                lastClose = i
            }
            if (lastClose >= 0 && (c == '(' || c == '(')) {
                lastOpen = i
                break
            }
        }
        if (lastOpen < 0 || lastClose <= lastOpen) return emptyList()
        val inner = stripped.substring(lastOpen + 1, lastClose)
        if (inner.isBlank()) return emptyList()
        val parts = if (inner.contains("/")) inner.split("/") else listOf(inner)
        return parts.map { part ->
            part.trim().removePrefix("A.").removePrefix("B.")
                .removePrefix("C.").removePrefix("D.")
                .trim()
        }.filter { it.isNotBlank() && it.length >= 2 }
    }

    /** 答案梯队：0=主语陈述句，1=To 不定式，2=其他可接受，3=祈使句（剔除） */
    private fun answerTier(cleaned: String): Int {
        if (cleaned.isBlank()) return 3
        if (cleaned.length < MIN_ANSWER_LENGTH) return 3
        val firstWord = cleaned.substringBefore(" ").lowercase().trim { it <= ' ' || it == '.' }
        if (VERB_FIRST_WORDS.contains(firstWord)) return 3
        val subjectWord = cleaned.substringBefore(" ").replace(".", "")
        if (SUBJECT_STARTERS.any { it.equals(subjectWord, ignoreCase = true) }) return 0
        if (TO_INF_INITIVE.containsMatchIn(cleaned)) return 1
        if (Regex("""(?i)^(it|this|that|there)\b""").containsMatchIn(cleaned)) return 0
        if (Regex("""(?i)^(he|she|they|we|i)\b""").containsMatchIn(cleaned)) return 0
        return 2
    }

    /**
     * 从 std 数组挑选答案。
     * 返回 Pair(选中答案, 备选列表)。无有效候选时返回第一梯队兜底结果。
     */
    private fun pickAnswer(rawAsk: String, stdArr: JSONArray): Pair<String, List<String>> {
        val cleaned = ArrayList<String>()
        for (i in 0 until stdArr.length()) {
            val value = stripTags(stdArr.optJSONObject(i)?.optString("value", "") ?: "")
            if (value.isNotBlank()) cleaned.add(value)
        }
        if (cleaned.isEmpty()) return "" to emptyList()

        val options = extractOptions(rawAsk).map { normalize(it) }.filter { it.isNotEmpty() }

        // 优先级 1：命中题干选项
        val matched = ArrayList<String>()
        if (options.isNotEmpty()) {
            for (c in cleaned) {
                val n = normalize(c)
                if (n.isEmpty()) continue
                val hit = options.any { opt ->
                    n == opt || n.contains(opt) || (opt.length >= 4 && opt.contains(n))
                }
                if (hit) matched.add(c)
            }
        }

        val pool: List<String>
        val tierSorted: List<String>
        if (matched.isNotEmpty()) {
            pool = matched
            // 命中题干选项的多条候选：按最短排（用户要求并列时最短兜底）
            tierSorted = pool.sortedBy { it.length }
        } else {
            pool = cleaned
            tierSorted = pool.sortedWith(
                compareBy({ answerTier(it) }, { it.length })
            )
        }

        val chosen = tierSorted.firstOrNull() ?: cleaned.first()
        val alternatives = tierSorted.filter { it != chosen }.take(5)
        return chosen to alternatives
    }

    /**
     * 解析一个 content.json 文件，返回该作业的题目列表（已智能排序）。
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
                val questionArr = info.optJSONArray("question") ?: JSONArray()
                for (i in 0 until questionArr.length()) {
                    val q = questionArr.optJSONObject(i) ?: continue
                    val rawAsk = q.optString("ask", "")
                    val ask = cleanQuestionText(rawAsk)
                    val stdArr = q.optJSONArray("std") ?: JSONArray()
                    val (answer, alternatives) = pickAnswer(rawAsk, stdArr)
                    val keywords = q.optString("keywords", "").ifEmpty { null }
                    questions.add(
                        ParsedQuestion(
                            index = i,
                            type = "role",
                            category = detectCategory(ask),
                            questionText = ask,
                            answerText = answer,
                            keywords = keywords,
                            alternatives = alternatives
                        )
                    )
                }
            }

            "collector.picture" -> {
                val topic = stripTags(info.optString("topic", ""))
                val stdArr = info.optJSONArray("std") ?: JSONArray()
                val answer = if (stdArr.length() > 0) {
                    stripTags(stdArr.optJSONObject(0)?.optString("value", "") ?: "")
                } else ""
                val alternatives = ArrayList<String>()
                for (i in 1 until stdArr.length()) {
                    if (alternatives.size >= 5) break
                    val value = stripTags(stdArr.optJSONObject(i)?.optString("value", "") ?: "")
                    if (value.isNotBlank() && value != answer) alternatives.add(value)
                }
                val keypoint = info.optString("keypoint", "").ifEmpty { null }
                if (topic.isNotEmpty() || answer.isNotEmpty()) {
                    questions.add(
                        ParsedQuestion(
                            index = 0,
                            type = "picture",
                            category = CATEGORY_READ,
                            questionText = "（思维导图转述题）\n$topic",
                            answerText = answer,
                            keywords = keypoint,
                            alternatives = alternatives
                        )
                    )
                }
            }

            else -> {
                // 未知类型：留空列表，UI 会显示"未解析出题目"
            }
        }

        val sorted = questions.sortedWith(
            compareBy({ categoryOrder(it.category) }, { it.index })
        )

        return ParsedHomework(
            folderName = folderName,
            folderPath = folderPath,
            lastModified = lastModified,
            stid = stid,
            questions = sorted
        )
    }
}
