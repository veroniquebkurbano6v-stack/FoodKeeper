package com.example.foodkeeper.ui.home

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodkeeper.data.model.FoodItem
import com.example.foodkeeper.data.model.getCategoryColor
import com.example.foodkeeper.data.model.getCategoryLabel
import com.example.foodkeeper.data.repository.InventoryRepository
import com.example.foodkeeper.domain.daysUntilExpiry
import com.example.foodkeeper.domain.getExpiryStatus
import com.example.foodkeeper.domain.getFoodCardInfo
import com.example.foodkeeper.domain.getStatusColor
import com.example.foodkeeper.ui.addfood.AddFoodActivity
import com.example.foodkeeper.ui.components.parseColor
import com.example.foodkeeper.ui.theme.BgColor
import com.example.foodkeeper.ui.theme.CardColor
import com.example.foodkeeper.ui.theme.PrimaryColor
import com.example.foodkeeper.ui.theme.Text2Color
import com.example.foodkeeper.ui.theme.Text3Color
import com.example.foodkeeper.ui.theme.TextColor

@Composable
fun HomeScreen(
    refreshSignal: Int,
    onTabChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { InventoryRepository.getInstance(context) }
    var foods by remember { mutableStateOf(repo.getFoods()) }

    // 刷新信号变化时重新加载
    LaunchedEffect(refreshSignal) {
        foods = repo.getFoods()
    }

    // 统计数据
    val validFoods = foods.filter { it.name.isNotEmpty() && it.quantity > 0 }
    val categoryCount = validFoods.map { it.categoryId }.distinct().size
    val expiringCount = validFoods.count {
        val days = daysUntilExpiry(it.expiryDate)
        val status = getExpiryStatus(days)
        status == "yellow" || status == "red"
    }
    val expiredCount = validFoods.count {
        daysUntilExpiry(it.expiryDate) < 0
    }

    // 临期提醒列表（最多5条，按过期天数升序）
    val expiringList = validFoods.map { it to daysUntilExpiry(it.expiryDate) }
        .filter { it.second <= 3 }
        .sortedBy { it.second }
        .take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // 1. 顶部标题栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "食库管家",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextColor
            )
        }

        // 2. 渐变头部卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF7C9473), Color(0xFF6E8CA8)),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🥬", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "让食材管理更简单",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "本地存储 · 隐私安全 · 临期提醒",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 3列统计网格
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "食材品类",
                value = categoryCount.toString(),
                unit = "类"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "临期预警",
                value = expiringCount.toString(),
                unit = "项",
                valueColor = Color(0xFFD4A55B)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "已过期",
                value = expiredCount.toString(),
                unit = "项",
                valueColor = Color(0xFFC75D4D)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 新增食材入库按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(PrimaryColor)
                .clickable {
                    val intent = Intent(context, AddFoodActivity::class.java)
                    context.startActivity(intent)
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = "新增", tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("新增食材入库", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. 临期提醒区域
        if (expiringList.isNotEmpty()) {
            SectionTitle("⚠️ 临期提醒")
            Spacer(modifier = Modifier.height(8.dp))
            expiringList.forEach { (food, days) ->
                ExpiringItemRow(food, days) {
                    val intent = Intent(context, AddFoodActivity::class.java)
                    intent.putExtra("food_id", food.id)
                    context.startActivity(intent)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 6. 8宫格快捷操作
        SectionTitle("🚀 快捷操作")
        Spacer(modifier = Modifier.height(10.dp))
        val actions = listOf(
            QuickAction("库存管理", Icons.Filled.ShoppingCart) { onTabChange(1) },
            QuickAction("AI菜谱", Icons.Filled.Receipt) { onTabChange(2) },
            QuickAction("数据报表", Icons.Filled.Analytics) { onTabChange(1) },
            QuickAction("出库日志", Icons.Filled.History) { onTabChange(3) },
            QuickAction("临期汇总", Icons.Filled.Warning) { onTabChange(1) },
            QuickAction("口味偏好", Icons.Filled.Favorite) { onTabChange(2) },
            QuickAction("系统设置", Icons.Filled.Settings) { onTabChange(3) },
            QuickAction("新增食材", Icons.Filled.Add) {
                val intent = Intent(context, AddFoodActivity::class.java)
                context.startActivity(intent)
            }
        )
        // 4列2行
        actions.chunked(4).forEach { rowActions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowActions.forEach { action ->
                    QuickActionItem(modifier = Modifier.weight(1f), action = action)
                }
                // 补齐空位
                repeat(4 - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 7. 说明卡片
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("食库管家 v1.0.0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextColor)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "数据完全存储在设备本地，不上传任何服务器。基于食材知识库（440条）与菜谱引擎（349道），为你智能管理食材。",
                    fontSize = 12.sp,
                    color = Text2Color,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    valueColor: Color = PrimaryColor
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, color = Text2Color)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = valueColor)
                Spacer(modifier = Modifier.width(2.dp))
                Text(unit, fontSize = 12.sp, color = Text3Color, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = TextColor,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

private data class QuickAction(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val action: () -> Unit
)

@Composable
private fun QuickActionItem(modifier: Modifier = Modifier, action: QuickAction) {
    Column(
        modifier = modifier
            .clickable { action.action() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(action.icon, contentDescription = action.name, tint = PrimaryColor, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(action.name, fontSize = 11.sp, color = Text2Color)
    }
}

@Composable
private fun ExpiringItemRow(food: FoodItem, days: Int, onClick: () -> Unit) {
    val info = getFoodCardInfo(food)
    val statusColor = parseColor(info.statusColor)
    val statusBg = parseColor(info.statusBg)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类色点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parseColor(getCategoryColor(food.categoryId)))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextColor)
                Text(
                    "${formatQty(food.quantity)}${food.unit} · ${getCategoryLabel(food.categoryId)}",
                    fontSize = 12.sp,
                    color = Text3Color
                )
            }
            Box(
                modifier = Modifier
                    .background(statusBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(info.daysText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun formatQty(q: Double): String =
    if (q == q.toInt().toDouble()) q.toInt().toString() else q.toString()
