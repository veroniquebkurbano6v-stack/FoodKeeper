@file:OptIn(ExperimentalLayoutApi::class)

package com.example.foodkeeper.ui.recipe

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.foodkeeper.data.model.DisplayRecipe
import com.example.foodkeeper.data.model.RecipeGenOptions
import com.example.foodkeeper.data.repository.InventoryRepository
import com.example.foodkeeper.domain.RecipeEngine
import com.example.foodkeeper.domain.RecipeHolder
import com.example.foodkeeper.domain.daysUntilExpiry
import com.example.foodkeeper.domain.getExpiryStatus
import com.example.foodkeeper.ui.recipedetail.RecipeDetailActivity
import com.example.foodkeeper.ui.theme.BgColor
import com.example.foodkeeper.ui.theme.CardColor
import com.example.foodkeeper.ui.theme.DangerColor
import com.example.foodkeeper.ui.theme.PrimaryColor
import com.example.foodkeeper.ui.theme.RecipeCatColors
import com.example.foodkeeper.ui.theme.RecipeCatLabels
import com.example.foodkeeper.ui.theme.Text2Color
import com.example.foodkeeper.ui.theme.Text3Color
import com.example.foodkeeper.ui.theme.TextColor
import com.example.foodkeeper.ui.theme.WarnColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeScreen(
    refreshSignal: Int,
    favoriteSet: Set<String>,
    completedSet: Set<String>,
    onFavoriteChange: (Set<String>) -> Unit,
    onCompletedChange: (Set<String>) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { InventoryRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var recipes by remember { mutableStateOf<List<DisplayRecipe>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf(-1) }  // -1=全部, 0/1/2=分类, 3=我的菜谱
    var lastRecipeNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastVersion by remember { mutableIntStateOf(0) }
    var lastResetCount by remember { mutableIntStateOf(0) }
    var firstResume by remember { mutableStateOf(true) }
    // 食材统计的 foods（放到 remember，ON_RESUME 时刷新）
    var foods by remember { mutableStateOf(repo.getFoods()) }

    // 自动生成菜谱（用协程，主线程安全）
    fun autoGenerate() {
        if (loading) return
        loading = true
        val currentFoods = repo.getFoods()
        val options = RecipeGenOptions(
            excludeNames = lastRecipeNames,
            favoriteNames = favoriteSet.toList()
        )
        Log.d("RecipeScreen", "autoGenerate 开始, 食材数=${currentFoods.size}, 菜谱库=${RecipeEngine.size()}")
        scope.launch {
            try {
                delay(1200)
                val result = RecipeEngine.generateRecipes(currentFoods, options)
                Log.d("RecipeScreen", "autoGenerate 完成, 生成菜谱数=${result.size}")
                recipes = result
                lastRecipeNames = result.map { it.name }
                foods = repo.getFoods()
            } catch (e: Exception) {
                Log.e("RecipeScreen", "autoGenerate 失败", e)
            } finally {
                loading = false
            }
        }
    }

    // 监听 ON_RESUME 事件
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                foods = repo.getFoods()
                val currentVersion = repo.getVersion()
                val currentResetCount = repo.getResetCount()
                // 检测完成烹饪标记
                RecipeHolder.getLastCompleted()?.let { name ->
                    onCompletedChange(completedSet + name)
                    RecipeHolder.clearLastCompleted()
                }
                // 检测重置事件：清空所有本地状态
                if (currentResetCount != lastResetCount && !firstResume) {
                    onFavoriteChange(emptySet())
                    onCompletedChange(emptySet())
                    lastRecipeNames = emptyList()
                    recipes = emptyList()
                    lastVersion = currentVersion
                    lastResetCount = currentResetCount
                    autoGenerate()
                }
                // 检测库存版本变更：自动重新生成（首次进入不触发，由 LaunchedEffect 处理）
                else if (currentVersion != lastVersion && !firstResume) {
                    lastVersion = currentVersion
                    autoGenerate()
                } else {
                    lastVersion = currentVersion
                    lastResetCount = currentResetCount
                }
                firstResume = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 首次进入自动生成
    LaunchedEffect(Unit) {
        if (recipes.isEmpty() && !loading) {
            lastVersion = repo.getVersion()
            lastResetCount = repo.getResetCount()
            autoGenerate()
        }
    }

    // 食材统计
    val validFoods = foods.filter { it.name.isNotEmpty() && it.quantity > 0 }
    val availableCount = validFoods.size
    val expiringCount = validFoods.count {
        val days = daysUntilExpiry(it.expiryDate)
        getExpiryStatus(days) in listOf("yellow", "red")
    }
    val expiredCount = validFoods.count { daysUntilExpiry(it.expiryDate) < 0 }

    // 按分类过滤
    val filteredRecipes = when (activeCategory) {
        -1 -> recipes
        3 -> recipes.filter { it.name in favoriteSet }  // 我的菜谱
        else -> recipes.filter { it.category == activeCategory }
    }

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
            Text("AI 菜谱推荐", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColor)
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PrimaryColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("v3", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 2. 食材统计条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChip("可用食材", availableCount.toString(), PrimaryColor)
            StatChip("临期预警", expiringCount.toString(), WarnColor)
            StatChip("已过期", expiredCount.toString(), DangerColor)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. 重新生成按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PrimaryColor)
                .clickable(enabled = !loading) { autoGenerate() },
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI 正在匹配食材...", color = Color.White, fontSize = 13.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Refresh, contentDescription = "重新生成", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("重新生成食谱", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. 分类导航
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecipeCategoryChip("全部", activeCategory == -1) { activeCategory = -1 }
            RecipeCategoryChip("快手懒人餐", activeCategory == 0) { activeCategory = 0 }
            RecipeCategoryChip("减脂轻食餐", activeCategory == 2) { activeCategory = 2 }
            RecipeCategoryChip("家常正餐", activeCategory == 1) { activeCategory = 1 }
            RecipeCategoryChip("我的菜谱", activeCategory == 3) { activeCategory = 3 }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. 菜谱列表
        if (loading && recipes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("正在为你智能匹配菜谱...", fontSize = 13.sp, color = Text3Color)
                }
            }
        } else if (filteredRecipes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(top = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍳", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("暂无菜谱", fontSize = 14.sp, color = Text3Color)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRecipes, key = { it.name }) { recipe ->
                    RecipeCard(
                        recipe = recipe,
                        isFavorite = recipe.name in favoriteSet,
                        isCompleted = recipe.name in completedSet,
                        onFavoriteToggle = {
                            onFavoriteChange(
                                if (recipe.name in favoriteSet) favoriteSet - recipe.name
                                else favoriteSet + recipe.name
                            )
                        },
                        onClick = {
                            RecipeHolder.set(recipe)
                            val intent = Intent(context, RecipeDetailActivity::class.java)
                            intent.putExtra("recipe_name", recipe.name)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatChip(label: String, value: String, color: Color) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = Text3Color)
        }
    }
}

@Composable
private fun RecipeCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) PrimaryColor else CardColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (selected) Color.White else Text2Color,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun RecipeCard(
    recipe: DisplayRecipe,
    isFavorite: Boolean,
    isCompleted: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit
) {
    val catColorPair = RecipeCatColors[recipe.category] ?: (PrimaryColor to CardColor)
    val catLabel = RecipeCatLabels[recipe.category] ?: "菜谱"
    val matchRate = if (recipe.totalCount > 0) recipe.haveCount * 100 / recipe.totalCount else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 标题行
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    recipe.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    modifier = Modifier.weight(1f)
                )
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("已完成", color = Color.White, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                // 匹配率徽章
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (matchRate == 100) PrimaryColor else catColorPair.first.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "已备 ${recipe.haveCount}/${recipe.totalCount}",
                        color = if (matchRate == 100) Color.White else catColorPair.first,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // 收藏按钮
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .clickable { onFavoriteToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "收藏",
                        tint = if (isFavorite) Color(0xFFD4A55B) else Text3Color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 标签行：分类 + 时间 + 热量 + 难度 + 临期/需买标记
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Tag(text = catLabel, color = catColorPair.first, bg = catColorPair.second)
                Tag(text = "⏱ ${recipe.time}", color = Text2Color, bg = CardColor)
                Tag(text = "🔥 ${recipe.calories}", color = Text2Color, bg = CardColor)
                Tag(text = "📊 ${recipe.difficulty}", color = Text2Color, bg = CardColor)
                if (recipe.expiringCount > 0) {
                    Tag(text = "⚠️ 临期${recipe.expiringCount}", color = WarnColor, bg = WarnColor.copy(alpha = 0.15f))
                }
                if (recipe.needBuyNames.isNotEmpty()) {
                    Tag(text = "需买${recipe.needBuyNames.size}", color = DangerColor, bg = DangerColor.copy(alpha = 0.15f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 食材标签预览
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                recipe.ingredients.forEach { ing ->
                    val ingColor = if (ing.haveIt) {
                        if (ing.isExpiring) WarnColor else PrimaryColor
                    } else {
                        Text3Color
                    }
                    val ingText = if (ing.haveIt) {
                        "${ing.name} ${ing.quantity} · 剩${ing.days}天"
                    } else {
                        "${ing.name} ${ing.quantity} · 需购买"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ingColor.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(ingText, fontSize = 11.sp, color = ingColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 底部：步骤数
            Text(
                "共 ${recipe.steps} 步 · 点击查看详情",
                fontSize = 11.sp,
                color = Text3Color
            )
        }
    }
}

@Composable
private fun Tag(text: String, color: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, color = color)
    }
}
