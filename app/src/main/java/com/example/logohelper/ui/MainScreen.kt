package com.example.logohelper.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onShowOverlay: () -> Unit
) {
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

            if (state.scanning) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                HomeworkSection(state, onSelectHomework)
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onShowOverlay,
                    enabled = state.selectedHomework != null && state.overlayGranted,
                    modifier = Modifier.weight(1f)
                ) { Text("显示悬浮窗") }
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                    Text("刷新列表")
                }
            }
        }
    }
}

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
                    color = Color(0xFF2E7D32)
                )
                Spacer(Modifier.width(4.dp))
                Text("已授权", fontSize = 12.sp, color = Color(0xFF2E7D32))
            } else {
                Text("未授权", fontSize = 12.sp, color = Color(0xFFC62828))
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

    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("作业列表") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("解析预览") })
        }
        Spacer(Modifier.height(8.dp))
        if (tab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
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
                Text("请先在「作业列表」中选择一份作业。", fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 8.dp)
                ) {
                    if (selected.questions.isEmpty()) {
                        Text("该作业未解析出题目。", fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp))
                    }
                    selected.questions.forEach { q ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Q${q.index + 1}  ${q.questionText}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    q.answerText,
                                    color = MaterialTheme.colorScheme.primary,
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
    val time = DateFormat.format("yyyy-MM-dd HH:mm", Date(homework.lastModified)).toString()
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
                Text(time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text("${homework.questions.size} 题", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary)
                if (selected) {
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text("当前作业", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
            homework.stid?.let {
                Text("stid: $it", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
