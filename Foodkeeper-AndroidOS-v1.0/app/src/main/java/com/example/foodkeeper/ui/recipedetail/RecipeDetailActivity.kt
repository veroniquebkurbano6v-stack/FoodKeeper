package com.example.foodkeeper.ui.recipedetail

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodkeeper.data.model.ConsumeResult
import com.example.foodkeeper.data.model.DisplayRecipe
import com.example.foodkeeper.data.repository.InventoryRepository
import com.example.foodkeeper.domain.RecipeHolder
import com.example.foodkeeper.ui.theme.BgColor
import com.example.foodkeeper.ui.theme.CardColor
import com.example.foodkeeper.ui.theme.DangerColor
import com.example.foodkeeper.ui.theme.DangerBgColor
import com.example.foodkeeper.ui.theme.ExpiredBgColor
import com.example.foodkeeper.ui.theme.ExpiredColor
import com.example.foodkeeper.ui.theme.PrimaryBgColor
import com.example.foodkeeper.ui.theme.PrimaryColor
import com.example.foodkeeper.ui.theme.RecipeCatColors
import com.example.foodkeeper.ui.theme.RecipeCatLabels
import com.example.foodkeeper.ui.theme.SectionColor
import com.example.foodkeeper.ui.theme.Text2Color
import com.example.foodkeeper.ui.theme.Text3Color
import com.example.foodkeeper.ui.theme.TextColor
import com.example.foodkeeper.ui.theme.WarnBgColor
import com.example.foodkeeper.ui.theme.WarnColor

class RecipeDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#7C9473")
        setContent {
            RecipeDetailScreen { finish() }
        }
    }
}

@Composable
fun RecipeDetailScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { InventoryRepository.getInstance(context) }
    val recipe = remember { RecipeHolder.get() }
    var consumeResult by remember { mutableStateOf<ConsumeResult?>(null) }
    var cooking by remember { mutableStateOf(false) }

    if (recipe == null) {
        // 没有数据，直接关闭
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("菜谱数据加载失败", color = Text3Color)
        }
        return
    }

    val catColorPair = RecipeCatColors[recipe.category] ?: (PrimaryColor to PrimaryBgColor)
    val catLabel = RecipeCatLabels[recipe.category] ?: "菜谱"
    val matchRate = if (recipe.totalCount > 0) recipe.haveCount * 100 / recipe.totalCount else 0

    fun onCompleteCooking() {
        if (cooking) return
        cooking = true
        val result = repo.consumeIngredients(recipe.ingredients, recipe.name)
        consumeResult = result
        if (result.ok) {
            RecipeHolder.setLastCompleted(recipe.name)
            Toast.makeText(
                context,
                "已完成烹饪，扣减 ${result.deducted} 项库存（${result.insufficient} 项不足）（${result.skipped} 项跳过）",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(context, "扣减失败：无可扣减的食材", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // 1. 导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.clickable { onClose() }) {
                Text("←", fontSize = 22.sp, color = PrimaryColor)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("食谱详情", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextColor, modifier = Modifier.weight(1f))
        }

        // 2. 固定操作栏（完成烹饪按钮，置顶始终可见）
        if (consumeResult == null) {
            Button(
                onClick = { onCompleteCooking() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("✓ 完成烹饪（自动扣减库存）", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // 3. 内容区
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 食谱标题区
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(recipe.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextColor)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailTag(catLabel, catColorPair.first, catColorPair.second)
                        DetailTag("⏱ ${recipe.time}", Text2Color, SectionColor)
                        DetailTag("🔥 ${recipe.calories}", Text2Color, SectionColor)
                        DetailTag("📊 ${recipe.difficulty}", Text2Color, SectionColor)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailTag("已备 ${recipe.haveCount}/${recipe.totalCount}", if (matchRate == 100) Color.White else PrimaryColor, if (matchRate == 100) PrimaryColor else PrimaryBgColor)
                        if (recipe.expiringCount > 0) {
                            DetailTag("⚠️ 临期 ${recipe.expiringCount}", WarnColor, WarnBgColor)
                        }
                        if (recipe.needBuyNames.isNotEmpty()) {
                            DetailTag("需买 ${recipe.needBuyNames.size}", DangerColor, DangerBgColor)
                        }
                    }
                }
            }

            // 4. 食材清单区
            SectionTitle("🥘 食材清单（${recipe.ingredients.size} 项）")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardColor)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recipe.ingredients.forEach { ing ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ing.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextColor, modifier = Modifier.weight(1f))
                            Text(ing.quantity, fontSize = 13.sp, color = Text2Color)
                            Spacer(modifier = Modifier.width(8.dp))
                            // 状态标签
                            when {
                                !ing.haveIt -> DetailTag("需购买", Text3Color, SectionColor)
                                ing.isExpiring && ing.days < 0 -> DetailTag("已过期${Math.abs(ing.days)}天", ExpiredColor, ExpiredBgColor)
                                ing.isExpiring -> DetailTag("剩${ing.days}天", WarnColor, WarnBgColor)
                                else -> DetailTag("剩${ing.days}天", PrimaryColor, PrimaryBgColor)
                            }
                        }
                    }
                }
            }

            // 5. 烹饪步骤区
            SectionTitle("👨‍🍳 烹饪步骤（${recipe.stepList.size} 步）")
            recipe.stepList.forEachIndexed { idx, step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${idx + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(step.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextColor, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        // 火候与用时标签
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (step.heat.isNotEmpty()) DetailTag("🔥 ${step.heat}", WarnColor, WarnBgColor)
                            if (step.duration.isNotEmpty()) DetailTag("⏱ ${step.duration}", Text2Color, SectionColor)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(step.detail, fontSize = 13.sp, color = TextColor, lineHeight = 20.sp)
                        if (step.tip.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = PrimaryBgColor)
                            ) {
                                Row(modifier = Modifier.padding(10.dp)) {
                                    Text("💡", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(step.tip, fontSize = 12.sp, color = PrimaryColor, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 6. 库存扣减明细区（完成烹饪后展示）
            consumeResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                SectionTitle("📦 库存扣减明细")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "扣减 ${result.deducted} 项 · 不足 ${result.insufficient} 项 · 跳过 ${result.skipped} 项",
                            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextColor
                        )
                        result.details.forEach { detail ->
                            val actionInfo = when (detail.action.name) {
                                "DEDUCTED" -> ActionInfo("已扣减", PrimaryColor, PrimaryBgColor)
                                "DEDUCTED_CONVERTED" -> ActionInfo("已换算扣减", PrimaryColor, PrimaryBgColor)
                                "INSUFFICIENT" -> ActionInfo("库存不足", WarnColor, WarnBgColor)
                                "SKIPPED_NO_AMOUNT" -> ActionInfo("用量无法量化", Text3Color, SectionColor)
                                "SKIPPED_NOT_INVENTORY" -> ActionInfo("库存无匹配", Text3Color, SectionColor)
                                "SKIPPED_UNIT_MISMATCH" -> ActionInfo("单位无法换算", DangerColor, DangerBgColor)
                                else -> ActionInfo(detail.action.name, Text3Color, SectionColor)
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(detail.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextColor, modifier = Modifier.weight(1f))
                                    DetailTag(actionInfo.text, actionInfo.color, actionInfo.bg)
                                }
                                Text(
                                    "菜谱用量：${detail.recipeAmount}  ·  库存：${formatQty(detail.beforeQty)}${detail.inventoryUnit} → ${formatQty(detail.afterQty)}${detail.inventoryUnit}",
                                    fontSize = 11.sp, color = Text2Color
                                )
                                if (detail.conversionNote.isNotEmpty()) {
                                    Text("换算依据：${detail.conversionNote}", fontSize = 11.sp, color = Text3Color)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class ActionInfo(val text: String, val color: Color, val bg: Color)

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextColor)
}

@Composable
private fun DetailTag(text: String, color: Color, bg: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, color = color)
    }
}

private fun formatQty(q: Double): String =
    if (q == q.toInt().toDouble()) q.toInt().toString() else q.toString()
