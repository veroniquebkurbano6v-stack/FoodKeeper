package com.example.foodkeeper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodkeeper.data.model.FoodItem
import com.example.foodkeeper.domain.getFoodCardInfo
import com.example.foodkeeper.ui.theme.CardColor
import com.example.foodkeeper.ui.theme.Text2Color
import com.example.foodkeeper.ui.theme.Text3Color

/**
 * 食材卡片组件（对应原 ArkTS FoodCard.ets）
 *
 * 布局：Row > [3px分类色条 | Column(状态徽章 + 名称 + 数量·分类)]
 */
@Composable
fun FoodCard(
    food: FoodItem,
    onTap: (Long) -> Unit
) {
    val info = getFoodCardInfo(food)
    val statusColor = parseColor(info.statusColor)
    val statusBg = parseColor(info.statusBg)
    val catDot = parseColor(info.catDot)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap(food.id) },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.height(76.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧3px分类色条
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(48.dp)
                    .background(catDot)
            )
            Spacer(modifier = Modifier.width(12.dp))
            // 内容区
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // 顶部状态徽章
                Box(
                    modifier = Modifier
                        .background(statusBg, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = info.daysText,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // 食材名称
                Text(
                    text = food.name,
                    color = Color(0xFF3D352E),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                // 底部：数量·单位·分类
                Text(
                    text = "${formatQuantity(food.quantity)}${food.unit} · ${info.catLabel}",
                    color = Text2Color,
                    fontSize = 12.sp
                )
            }
        }
    }
}

fun formatQuantity(q: Double): String {
    return if (q == q.toInt().toDouble()) q.toInt().toString() else q.toString()
}

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Text3Color
    }
}
