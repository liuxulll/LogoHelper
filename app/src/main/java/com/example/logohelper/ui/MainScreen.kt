package com.example.logohelper.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logohelper.MainUiState
import com.example.logohelper.ParsedHomework
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: MainUiState,
    onSelectHomework: (Int) -> Unit,
    onRefresh: () -> Unit,
    onGrantStorage: () -> Unit,
    onGrantOverlay: () -> Unit,
    onShowOverlay: (ParsedHomework) -> Unit
) {
    val context = LocalContext.current
    var pickerVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "logo助手",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            UsageTipCard()

            Spacer(Modifier.height(8.dp))

            PermissionCard(
                title = state.storageTitle,
                granted = state.storageGranted,
                onGrant = onGrantStorage
            )
            Spacer(Modifier.height(6.dp))
            PermissionCard(
                title = "悬浮窗权限",
                granted = state.overlayGranted,
                onGrant = onGrantOverlay
            )

            Spacer(Modifier.height(10.dp))

            // 中间区域被限制在剩余高度内，列表/预览在内部滚动，不会盖住底部按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (state.scanning) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    HomeworkSection(state, onSelectHomework)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pickerVisible = true },
                    enabled = state.homeworks.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("显示悬浮窗", fontSize = 13.sp) }
                FilledTonalButton(
                    onClick = {
                        state.selectedHomework?.let { hw ->
                            copyAllToClipboard(context, hw)
                        }
                    },
                    enabled = state.selectedHomework != null,
                    modifier = Modifier.weight(1f)
                ) { Text("复制题目答案", fontSize = 13.sp) }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(0.7f)
                ) { Text("刷新", fontSize = 13.sp) }
            }
        }
    }

    // 悬浮窗作业选择弹窗：默认选中日期最新的一份（列表首项）
    if (pickerVisible && state.homeworks.isNotEmpty()) {
        var picked by remember { mutableIntStateOf(0) }
        AlertDialog(
            onDismissRequest = { pickerVisible = false },
            title = { Text("选择要显示的作业") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    state.homeworks.forEachIndexed { i, hw ->
                        val sel = i == picked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (sel) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { picked = i }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (sel) "◉" else "○",
                                fontSize = 18.sp,
                                color = if (sel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    hw.folderName,
                                    fontSize = 13.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                                Text(
                                    formatTime(hw.lastModified) + " · ${hw.categoriesSummary}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    pickerVisible = false
                    state.homeworks.getOrNull(picked)?.let(onShowOverlay)
                }) { Text("显示") }
            },
            dismissButton = {
                TextButton(onClick = { pickerVisible = false }) { Text("取消") }
            }
        )
    }
}

/** 一键复制：所有题目（含小节）与答案，按智能排序顺序 */
private fun copyAllToClipboard(context: Context, hw: ParsedHomework) {
    val sb = StringBuilder()
    sb.append("【logo助手】").append(hw.folderName)
        .append("（").append(formatTime(hw.lastModified)).append("）\n")
    hw.questions.forEachIndexed { i, q ->
        sb.append("Q").append(i + 1)
            .append("（").append(q.category).append("）\n")
            .append(q.questionText).append("\n")
            .append("答：").append(q.answerText).append("\n")
            .append("----\n")
    }
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("logo助手答案", sb.toString()))
    Toast.makeText(context, "已复制 ${hw.questions.size} 道题目与答案", Toast.LENGTH_SHORT).show()
}

private fun formatTime(millis: Long): String =
    DateFormat.format("yyyy-MM-dd HH:mm", Date(millis)).toString()

/** 使用前提示卡片：不可关闭，默认展开，内嵌可折叠的清缓存步骤 */
@Composable
private fun UsageTipCard() {
    var stepsExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "使用建议：请先在 E听说 App 中清除缓存，再打开本工具扫描。",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "原因：E听说会把历史作业缓存残留在 resource/ 目录，未清除时可能扫描到旧作业，" +
                        "导致按修改时间判断的“当前作业”不准确。清除缓存后，目录里只保留你刚下载/刚打开的那份作业，识别最准。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { stepsExpanded = !stepsExpanded }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("清除缓存步骤", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(4.dp))
                Text(
                    if (stepsExpanded) "▴ 收起" else "▾ 展开",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (stepsExpanded) {
                Text(
                    "· 打开 E听说 → 我的/设置 → 清除缓存；\n" +
                            "· 或在系统设置 → 应用 → E听说 → 存储 → 清除缓存；\n" +
                            "· 然后回到 E听说重新打开目标作业，确保其下载完成，再切回本工具点「刷新」。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            if (granted) {
                Text(
                    "✓",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1FA97A)
                )
                Spacer(Modifier.width(4.dp))
                Text("已授权", fontSize = 12.sp, color = Color(0xFF1FA97A))
            } else {
                Text("未授权", fontSize = 12.sp, color = Color(0xFFD64545))
                Spacer(Modifier.weight(1f))
                FilledTonalButton(onClick = onGrant) { Text("去授权") }
            }
        }
    }
}

@Composable
private fun HomeworkSection(state: MainUiState, onSelectHomework: (Int) -> Unit) {
    val notice = state.notice
    if (notice != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                notice,
                modifier = Modifier.padding(12.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        return
    }
    val homeworks = state.homeworks
    if (homeworks.isEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                "未找到作业缓存。请先在 E听说中打开目标作业并下载完成；若仍找不到，请清除 E听说缓存后重试。",
                modifier = Modifier.padding(12.dp),
                fontSize = 13.sp
            )
        }
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = Color.Transparent
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("作业列表") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("解析预览") })
        }
        Spacer(Modifier.height(8.dp))
        if (tab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(homeworks) { hw ->
                    HomeworkItem(
                        homework = hw,
                        selected = hw.folderPath == state.selectedPath,
                        onClick = { onSelectHomework(homeworks.indexOf(hw)) }
                    )
                }
            }
        } else {
            val selected = state.selectedHomework
            if (selected == null) {
                Text(
                    "请先在「作业列表」中选择一份作业。",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selected.questions.isEmpty()) {
                        Text(
                            "该作业未解析出题目。",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    var lastCategory = ""
                    selected.questions.forEachIndexed { i, q ->
                        if (q.category != lastCategory) {
                            lastCategory = q.category
                            Text(
                                "「$lastCategory」",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Q${i + 1}  ${q.questionText}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    q.answerText,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                if (state.showKeywords && !q.keywords.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "关键词：${q.keywords}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = state.showKeywords,
                            onCheckedChange = { state.onToggleKeywords() }
                        )
                        Text("显示关键词", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeworkItem(
    homework: ParsedHomework,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                homework.folderName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(homework.lastModified),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "${homework.questions.size} 题",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (selected) {
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text("当前作业", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
            if (homework.questions.isNotEmpty()) {
                Text(
                    homework.categoriesSummary,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
