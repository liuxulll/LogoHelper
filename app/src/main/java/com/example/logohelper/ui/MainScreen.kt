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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
    onGrantStorage: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "logo助手",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Text("刷新", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(46.dp)
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 分屏/横屏：宽明显大于高 → 左右两栏；否则上下单栏
            val sideBySide = maxWidth > maxHeight

            Column(Modifier.fillMaxSize()) {
                // 紧凑提示条（默认一行，可展开）
                var tipExpanded by remember { mutableStateOf(false) }
                CompactTipBar(expanded = tipExpanded, onToggle = { tipExpanded = !it })

                if (!state.storageGranted) {
                    PermissionBar(title = state.storageTitle, onGrant = onGrantStorage)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = if (sideBySide) 8.dp else 10.dp)
                ) {
                if (sideBySide) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            SectionBody(
                                state = state,
                                onSelectHomework = onSelectHomework,
                                forceTab = 0,
                                showTabRow = false
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .weight(1.4f)
                                .fillMaxHeight()
                        ) {
                            SectionBody(
                                state = state,
                                onSelectHomework = onSelectHomework,
                                forceTab = 1,
                                showTabRow = false
                            )
                        }
                    }
                } else {
                    SectionBody(
                        state = state,
                        onSelectHomework = onSelectHomework,
                        forceTab = null,
                        showTabRow = true
                    )
                }
            }

            // 底部操作条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 10.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val hw = state.selectedHomework
                        if (hw == null) {
                            Toast.makeText(context, "请先选择一份作业", Toast.LENGTH_SHORT).show()
                        } else if (hw.questions.isEmpty()) {
                            Toast.makeText(context, "该作业没有可复制的题目", Toast.LENGTH_SHORT).show()
                        } else {
                            copyToClipboard(context, hw)
                        }
                    },
                    enabled = state.selectedHomework != null,
                    modifier = Modifier.weight(1f)
                ) { Text("复制题目答案", fontSize = 13.sp) }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(0.55f)
                ) { Text("刷新", fontSize = 13.sp) }
            }
            }
        }
    }
}

/** 一行提示条：默认收起，点击展开原因与步骤 */
@Composable
private fun CompactTipBar(expanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⚠ 先用 E听说清缓存，再扫描",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (expanded) "▴" else "▾",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                Text(
                    "E听说会把历史作业残留在 resource/ 目录，未清缓存时“当前作业”可能不准。\n" +
                            "清缓存：E听说 → 我的/设置 → 清除缓存；或系统设置 → 应用 → E听说 → 存储 → 清除缓存。\n" +
                            "清完后回 E听说打开目标作业，等下载完成再回来点「刷新」。",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionBar(title: String, onGrant: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "未授予$title",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = onGrant) { Text("去授权", fontSize = 12.sp) }
        }
    }
}

/** 列表 + 预览主体：单栏时带 Tab，双栏时由外部指定只渲染一侧 */
@Composable
private fun SectionBody(
    state: MainUiState,
    onSelectHomework: (Int) -> Unit,
    forceTab: Int?,
    showTabRow: Boolean
) {
    var tab by remember { mutableIntStateOf(0) }
    val effectiveTab = forceTab ?: tab

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 2.dp)
    ) {
        if (showTabRow) {
            TabRow(
                selectedTabIndex = effectiveTab,
                containerColor = Color.Transparent,
                modifier = Modifier.height(40.dp)
            ) {
                Tab(selected = effectiveTab == 0, onClick = { tab = 0 },
                    text = { Text("作业列表", fontSize = 13.sp) })
                Tab(selected = effectiveTab == 1, onClick = { tab = 1 },
                    text = { Text("答案预览", fontSize = 13.sp) })
            }
        }

        val notice = state.notice
        if (state.scanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (notice != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    notice,
                    modifier = Modifier.padding(10.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            )
            return
        }

        val homeworks = state.homeworks
        if (homeworks.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "未找到作业缓存。请先在 E听说中打开目标作业并下载完成；若仍找不到，请清除 E听说缓存后重试。",
                    modifier = Modifier.padding(10.dp),
                    fontSize = 12.sp
                )
            }
            return
        }

        if (effectiveTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
                return
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                if (selected.questions.isEmpty()) {
                    Text(
                        "该作业未解析出题目。",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                var lastCategory = ""
                selected.questions.forEachIndexed { i, q ->
                    if (q.category != lastCategory) {
                        lastCategory = q.category
                        Text(
                            "「$lastCategory」",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                        )
                    }
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
                                "Q${i + 1}  ${q.questionText}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                q.answerText,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            if (state.showKeywords && !q.keywords.isNullOrBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "关键词：${q.keywords}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.showKeywords,
                        onCheckedChange = { state.onToggleKeywords() },
                        modifier = Modifier.height(32.dp)
                    )
                    Text("显示关键词", fontSize = 11.sp)
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
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                homework.folderName,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(homework.lastModified),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${homework.questions.size} 题",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (selected) {
                    Spacer(Modifier.width(8.dp))
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
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )
            }
        }
    }
}

/** 复制当前作业的全部题目（含小节）与答案 */
private fun copyToClipboard(context: Context, hw: ParsedHomework) {
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
