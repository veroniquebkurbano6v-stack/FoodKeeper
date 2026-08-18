package com.example.foodkeeper.ui.inventory

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.foodkeeper.data.model.CATEGORIES
import com.example.foodkeeper.data.model.FoodItem
import com.example.foodkeeper.data.model.getCategoryColor
import com.example.foodkeeper.data.model.getCategoryLabel
import com.example.foodkeeper.data.repository.InventoryRepository
import com.example.foodkeeper.domain.daysUntilExpiry
import com.example.foodkeeper.domain.getExpiryStatus
import com.example.foodkeeper.domain.getStatusPriority
import com.example.foodkeeper.ui.addfood.AddFoodActivity
import com.example.foodkeeper.ui.components.FoodCard
import com.example.foodkeeper.ui.components.parseColor
import com.example.foodkeeper.ui.theme.BgColor
import com.example.foodkeeper.ui.theme.CardColor
import com.example.foodkeeper.ui.theme.PrimaryColor
import com.example.foodkeeper.ui.theme.PrimaryBgColor
import com.example.foodkeeper.ui.theme.Text2Color
import com.example.foodkeeper.ui.theme.Text3Color
import com.example.foodkeeper.ui.theme.TextColor

@Composable
fun InventoryScreen(refreshSignal: Int) {
    val context = LocalContext.current
    val repo = remember { InventoryRepository.getInstance(context) }
    var foods by remember { mutableStateOf(repo.getFoods()) }

    // 监听页面 ON_RESUME 事件，回到页面时刷新数据（从 AddFoodActivity 返回后立即生效）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                foods = repo.getFoods()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 也响应外部 refreshSignal
    LaunchedEffect(refreshSignal) {
        foods = repo.getFoods()
    }

    var keyword by remember { mutableStateOf("") }
    var activeCategory by remember { mutableStateOf(0) }  // 0=全部, 1-8=分类
    var sortByExpiry by remember { mutableStateOf(true) }  // true=按保质期, false=按入库时间

    // 过滤排序逻辑（与原 ArkTS recomputeDerived 一致）
    val validFoods = foods.filter { it.name.isNotEmpty() && it.quantity > 0 }
    val categoryCounts = (1..8).associateWith { catId ->
        validFoods.count { it.categoryId == catId }
    }
    val filtered = validFoods
        .filter { keyword.isBlank() || it.name.contains(keyword.trim()) }
        .filter { activeCategory == 0 || it.categoryId == activeCategory }
        .sortedWith(
            if (sortByExpiry) {
                // 按保质期：先按状态优先级（expired<red<yellow<green），同状态按剩余天数升序
                compareBy(
                    { getStatusPriority(getExpiryStatus(daysUntilExpiry(it.expiryDate))) },
                    { daysUntilExpiry(it.expiryDate) }
                )
            } else {
                // 按入库时间：createdAt 降序
                compareByDescending { it.createdAt }
            }
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // 1. 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("库存清单", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColor)
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(PrimaryColor)
                    .clickable {
                        val intent = Intent(context, AddFoodActivity::class.java)
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, contentDescription = "新增", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新增", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // 2. 搜索栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(CardColor)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = "搜索", tint = Text3Color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 14.sp, color = TextColor),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (keyword.isEmpty()) {
                            Text("搜索食材名称", fontSize = 14.sp, color = Text3Color)
                        }
                        innerTextField()
                    }
                )
                if (keyword.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { keyword = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("×", color = Text3Color, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. 分类标签栏（横向滚动）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                label = "全部",
                count = validFoods.size,
                selected = activeCategory == 0,
                color = PrimaryColor,
                bg = PrimaryBgColor,
                onClick = { activeCategory = 0 }
            )
            CATEGORIES.forEach { cat ->
                CategoryChip(
                    label = cat.label,
                    count = categoryCounts[cat.id] ?: 0,
                    selected = activeCategory == cat.id,
                    color = parseColor(cat.color),
                    bg = parseColor(cat.bg),
                    onClick = { activeCategory = cat.id }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. 排序栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("共 ${filtered.size} 项", fontSize = 12.sp, color = Text3Color)
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardColor)
                    .clickable { sortByExpiry = !sortByExpiry }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    if (sortByExpiry) "⏰ 按保质期排序" else "📦 按入库时间排序",
                    fontSize = 12.sp,
                    color = Text2Color
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. 食材网格或空状态
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📦", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("暂无食材", fontSize = 14.sp, color = Text3Color)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("点击右上角「新增」开始管理", fontSize = 12.sp, color = Text3Color)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
            ) {
                items(filtered, key = { it.id }) { food ->
                    FoodCard(food = food) { id ->
                        val intent = Intent(context, AddFoodActivity::class.java)
                        intent.putExtra("food_id", id)
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    count: Int,
    selected: Boolean,
    color: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) color else CardColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                fontSize = 12.sp,
                color = if (selected) Color.White else Text2Color,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                count.toString(),
                fontSize = 10.sp,
                color = if (selected) Color.White.copy(alpha = 0.8f) else Text3Color
            )
        }
    }
}
