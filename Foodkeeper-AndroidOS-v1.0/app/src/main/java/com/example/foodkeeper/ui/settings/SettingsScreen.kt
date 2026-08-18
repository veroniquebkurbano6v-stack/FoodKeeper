package com.example.foodkeeper.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.foodkeeper.data.repository.InventoryRepository
import com.example.foodkeeper.ui.theme.BgColor
import com.example.foodkeeper.ui.theme.CardColor
import com.example.foodkeeper.ui.theme.DangerColor
import com.example.foodkeeper.ui.theme.PrimaryColor
import com.example.foodkeeper.ui.theme.SectionColor
import com.example.foodkeeper.ui.theme.Text2Color
import com.example.foodkeeper.ui.theme.Text3Color
import com.example.foodkeeper.ui.theme.TextColor

@Composable
fun SettingsScreen(refreshSignal: Int) {
    val context = LocalContext.current
    val repo = remember { InventoryRepository.getInstance(context) }
    var storageInfo by remember { mutableStateOf(repo.getStorageInfo()) }
    var logs by remember { mutableStateOf(repo.getConsumeLogs()) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var expandedLog by remember { mutableStateOf(-1) }  // 展开第几条日志，-1=不展开

    // 监听 ON_RESUME 刷新
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                storageInfo = repo.getStorageInfo()
                logs = repo.getConsumeLogs()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(refreshSignal) {
        storageInfo = repo.getStorageInfo()
        logs = repo.getConsumeLogs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // 1. 标题栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("系统设置", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColor)
        }

        // 2. 本地数据存储区
        SectionTitle("💾 本地数据存储")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                InfoRow("存储位置", "设备本地沙箱")
                InfoRow("数据上传", "不上传任何服务器")
                InfoRow("当前食材数量", "${storageInfo.foodCount} 项")
                InfoRow("数据版本", "v${storageInfo.version} · 重置 ${storageInfo.resetCount} 次")
                InfoRow("烹饪记录", "${storageInfo.logCount} 条")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 数据管理区
        SectionTitle("🔧 数据管理")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DangerColor.copy(alpha = 0.1f))
                        .clickable { showResetDialog = true }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("重置为初始数据", color = DangerColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                resetMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, fontSize = 12.sp, color = Text3Color, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 完成烹饪记录区
        SectionTitle("📜 完成烹饪记录")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            if (logs.isEmpty()) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无烹饪记录", fontSize = 13.sp, color = Text3Color)
                }
            } else {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("共 ${logs.size} 条记录（仅保留最近20条）", fontSize = 12.sp, color = Text3Color)
                    Spacer(modifier = Modifier.height(10.dp))
                    logs.reversed().forEachIndexed { idx, log ->
                        val realIdx = logs.size - 1 - idx
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { expandedLog = if (expandedLog == realIdx) -1 else realIdx }
                                .padding(vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🍳 ${log.recipeName}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextColor, modifier = Modifier.weight(1f))
                                Text(log.timestamp, fontSize = 11.sp, color = Text3Color)
                            }
                            Text(
                                "扣减 ${log.deducted} 项 · 不足 ${log.insufficient} 项 · 跳过 ${log.skipped} 项",
                                fontSize = 11.sp,
                                color = Text2Color
                            )
                            if (expandedLog == realIdx) {
                                Spacer(modifier = Modifier.height(8.dp))
                                log.details.forEach { detail ->
                                    val actionText = when (detail.action.name) {
                                        "DEDUCTED" -> "已扣减"
                                        "DEDUCTED_CONVERTED" -> "已换算扣减"
                                        "INSUFFICIENT" -> "库存不足"
                                        "SKIPPED_NO_AMOUNT" -> "用量无法量化"
                                        "SKIPPED_NOT_INVENTORY" -> "库存无匹配"
                                        "SKIPPED_UNIT_MISMATCH" -> "单位无法换算"
                                        else -> detail.action.name
                                    }
                                    val actionColor = when (detail.action.name) {
                                        "DEDUCTED" -> PrimaryColor
                                        "DEDUCTED_CONVERTED" -> PrimaryColor
                                        "INSUFFICIENT" -> DangerColor
                                        "SKIPPED_NO_AMOUNT", "SKIPPED_NOT_INVENTORY" -> Text3Color
                                        "SKIPPED_UNIT_MISMATCH" -> DangerColor
                                        else -> Text3Color
                                    }
                                    Text(
                                        "  • ${detail.name} ${detail.recipeAmount} → $actionText",
                                        fontSize = 11.sp,
                                        color = actionColor
                                    )
                                    if (detail.conversionNote.isNotEmpty()) {
                                        Text("    ${detail.conversionNote}", fontSize = 10.sp, color = Text3Color)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. 关于应用区
        SectionTitle("ℹ️ 关于应用")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                InfoRow("应用名称", "食库管家")
                InfoRow("版本", "v1.0.0")
                InfoRow("技术栈", "Android Kotlin + Jetpack Compose")
                InfoRow("数据规模", "440 条食材知识 · 349 道菜谱")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. 说明卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SectionColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("食库管家 - 让食材管理更简单", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "本应用由鸿蒙 HarmonyOS ArkTS 项目无损转换为 Android 项目，数据完全存储在设备本地，不上传任何服务器。" +
                        "基于 440 条食材知识库（matchFood 五级匹配算法）与 349 道菜谱引擎（两阶段推荐算法），" +
                        "为你智能管理食材与推荐菜谱。",
                    fontSize = 12.sp,
                    color = Text2Color,
                    lineHeight = 18.sp
                )
            }
        }
    }

    // 重置确认对话框
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("确认重置") },
            text = { Text("将清除所有库存数据并恢复为 15 条示例食材，此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    val ok = repo.resetToSeed()
                    showResetDialog = false
                    if (ok) {
                        storageInfo = repo.getStorageInfo()
                        logs = repo.getConsumeLogs()
                        resetMessage = "✓ 已重置为初始数据（15 条食材）"
                        Toast.makeText(context, "已重置为初始数据", Toast.LENGTH_SHORT).show()
                    } else {
                        resetMessage = "✗ 重置失败，请稍后重试"
                        Toast.makeText(context, "重置失败", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("确认重置", color = DangerColor) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = TextColor,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Text2Color)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = TextColor)
    }
}
