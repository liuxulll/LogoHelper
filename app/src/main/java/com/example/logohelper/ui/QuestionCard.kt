package com.example.logohelper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logohelper.ParsedQuestion

/**
 * 单题卡片：题目加粗、推荐答案高亮，
 * 下方提供「查看其他可用答案」折叠区（最多 5 条备选）。
 */
@Composable
fun QuestionCard(
    label: String,
    question: ParsedQuestion,
    showKeywords: Boolean
) {
    var showAlternatives by remember(question.answerText) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                "$label  ${question.questionText}",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Column(Modifier.padding(top = 4.dp)) {
                Text(
                    question.answerText,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
            if (showKeywords && !question.keywords.isNullOrBlank()) {
                Column(Modifier.padding(top = 3.dp)) {
                    Text(
                        "关键词：${question.keywords}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (question.alternatives.isNotEmpty()) {
                TextButton(
                    onClick = { showAlternatives = !showAlternatives },
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        if (showAlternatives) "收起其他可用答案"
                        else "查看其他可用答案（${question.alternatives.size}）",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (showAlternatives) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        question.alternatives.forEachIndexed { n, alt ->
                            Text(
                                "${n + 1}. $alt",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
