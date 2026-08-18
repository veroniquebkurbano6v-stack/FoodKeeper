package com.example.foodkeeper.ui.addfood

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodkeeper.data.model.AddFoodInput
import com.example.foodkeeper.data.model.CATEGORIES
import com.example.foodkeeper.data.model.STORAGE_ZONES
import com.example.foodkeeper.data.model.UNITS
import com.example.foodkeeper.data.model.UpdateFoodPatch
import com.example.foodkeeper.data.model.formatDate
import com.example.foodkeeper.data.repository.InventoryRepository
import com.example.foodkeeper.domain.FoodKnowledgeBase
import com.example.foodkeeper.domain.UnitConverter
import com.example.foodkeeper.ui.theme.BgColor
import com.example.foodkeeper.ui.theme.CardColor
import com.example.foodkeeper.ui.theme.DangerColor
import com.example.foodkeeper.ui.theme.PrimaryBgColor
import com.example.foodkeeper.ui.theme.PrimaryColor
import com.example.foodkeeper.ui.theme.Text2Color
import com.example.foodkeeper.ui.theme.Text3Color
import com.example.foodkeeper.ui.theme.TextColor
import java.util.Calendar

class AddFoodActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#7C9473")
        val editId = intent.getLongExtra("food_id", -1L)
        setContent {
            AddFoodScreen(editId = if (editId > 0) editId else null) { finish() }
        }
    }
}

@Composable
fun AddFoodScreen(editId: Long?, onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { InventoryRepository.getInstance(context) }

    // 表单状态
    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(1) }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("个") }
    var storageZone by remember { mutableStateOf("refrigerator") }
    var price by remember { mutableStateOf("") }
    var totalPrice by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(formatDate(java.util.Date())) }
    var expiryDays by remember { mutableStateOf(7) }

    // 用户手动修改标记
    var manualCategory by remember { mutableStateOf(false) }
    var manualUnit by remember { mutableStateOf(false) }
    var manualStorage by remember { mutableStateOf(false) }
    var manualExpiry by remember { mutableStateOf(false) }

    // 知识库匹配状态
    var matchStatus by remember { mutableStateOf<String?>(null) }  // null=未匹配过, ""=未命中, "匹配名"=命中
    var matchTip by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // 编辑模式：加载已有数据
    LaunchedEffect(editId) {
        if (editId != null) {
            val food = repo.getFoods().find { it.id == editId }
            if (food != null) {
                name = food.name
                categoryId = food.categoryId
                quantity = formatInput(food.quantity)
                unit = food.unit
                storageZone = food.storageZone
                price = formatInput(food.price)
                totalPrice = formatInput(food.totalPrice)
                purchaseDate = food.purchaseDate
                // 反推保质期天数
                val purchaseTime = parseDateMs(food.purchaseDate)
                val expiryTime = parseDateMs(food.expiryDate)
                expiryDays = ((expiryTime - purchaseTime) / (24 * 3600 * 1000)).toInt()
                // 编辑模式下视为全部手动修改，不再自动覆盖
                manualCategory = true
                manualUnit = true
                manualStorage = true
                manualExpiry = true
            }
        }
    }

    // 知识库自动填充逻辑（与原 ArkTS tryMatchKnowledge 一致）
    fun tryMatchKnowledge(input: String) {
        if (input.isBlank()) {
            matchStatus = null
            matchTip = null
            return
        }
        val matched = FoodKnowledgeBase.matchFood(input)
        if (matched != null) {
            matchStatus = matched.name
            matchTip = if (matched.tip.isNotEmpty()) matched.tip else "已匹配知识库条目"
            if (!manualCategory) categoryId = matched.categoryId
            if (!manualUnit) unit = matched.unit
            if (!manualStorage) storageZone = matched.storageZone
            if (!manualExpiry) expiryDays = matched.expiryDays
        } else {
            matchStatus = ""
            matchTip = null
            // 未命中时按 estimateExpiryDays 估算
            if (!manualExpiry) {
                expiryDays = repo.estimateDays(input.trim(), categoryId)
            }
        }
    }

    fun recalcTotal() {
        val q = quantity.toDoubleOrNull() ?: 0.0
        val p = price.toDoubleOrNull() ?: 0.0
        if (q > 0 && p > 0) {
            totalPrice = formatInput(q * p)
        }
    }

    fun save() {
        if (name.isBlank()) {
            Toast.makeText(context, "请输入食材名称", Toast.LENGTH_SHORT).show()
            return
        }
        val q = quantity.toDoubleOrNull() ?: 0.0
        if (q <= 0) {
            Toast.makeText(context, "请输入有效数量", Toast.LENGTH_SHORT).show()
            return
        }
        val p = price.toDoubleOrNull() ?: 0.0
        val tp = totalPrice.toDoubleOrNull() ?: 0.0

        if (editId != null) {
            // 重新计算 expiryDate
            val purchaseTime = parseDateMs(purchaseDate)
            val expiryTime = purchaseTime + expiryDays * 24L * 3600 * 1000
            val expiryDateStr = formatDate(java.util.Date(expiryTime))
            val patch = UpdateFoodPatch(
                name = name.trim(),
                categoryId = categoryId,
                quantity = q,
                unit = unit,
                purchaseDate = purchaseDate,
                expiryDate = expiryDateStr,
                storageZone = storageZone,
                price = p,
                totalPrice = tp
            )
            val ok = repo.updateFood(editId, patch)
            if (ok) {
                Toast.makeText(context, "已更新", Toast.LENGTH_SHORT).show()
                onClose()
            } else {
                Toast.makeText(context, "更新失败", Toast.LENGTH_SHORT).show()
            }
        } else {
            val input = AddFoodInput(
                name = name.trim(),
                categoryId = categoryId,
                quantity = q,
                unit = unit,
                storageZone = storageZone,
                price = p,
                totalPrice = tp,
                expiryDays = expiryDays,
                purchaseDate = purchaseDate
            )
            val id = repo.addFood(input)
            if (id > 0) {
                Toast.makeText(context, "已添加", Toast.LENGTH_SHORT).show()
                onClose()
            } else {
                Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // 顶部导航栏
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
            Text(
                if (editId != null) "编辑食材" else "新增食材",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextColor,
                modifier = Modifier.weight(1f)
            )
            if (editId != null) {
                Box(modifier = Modifier.clickable { showDeleteDialog = true }) {
                    Text("删除", color = DangerColor, fontSize = 14.sp)
                }
            }
        }

        // 表单内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 食材名称
            FormField("食材名称") {
                BasicTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        tryMatchKnowledge(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = TextColor),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (name.isEmpty()) Text("如：西红柿", fontSize = 14.sp, color = Text3Color)
                        inner()
                    }
                )
            }
            // 知识库匹配状态
            matchStatus?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.isNotEmpty()) PrimaryBgColor else CardColor
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (status.isNotEmpty()) {
                            Text("✓ 已匹配「$status」，下方字段已自动填充", fontSize = 12.sp, color = PrimaryColor)
                            matchTip?.let {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("💡 $it", fontSize = 11.sp, color = Text2Color)
                            }
                        } else {
                            Text("未匹配到知识库条目，按分类估算保质期", fontSize = 12.sp, color = Text3Color)
                        }
                    }
                }
            }

            // 分类
            FormField("分类") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORIES.forEach { cat ->
                        val selected = categoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) PrimaryColor else CardColor)
                                .clickable {
                                    categoryId = cat.id
                                    manualCategory = true
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                cat.label,
                                fontSize = 13.sp,
                                color = if (selected) Color.White else Text2Color
                            )
                        }
                    }
                }
            }

            // 数量 + 单位
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    FormField("数量") {
                        BasicTextField(
                            value = quantity,
                            onValueChange = {
                                quantity = it
                                recalcTotal()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            textStyle = TextStyle(fontSize = 14.sp, color = TextColor),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            decorationBox = { inner ->
                                if (quantity.isEmpty()) Text("如：3", fontSize = 14.sp, color = Text3Color)
                                inner()
                            }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    FormField("单位") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            UNITS.forEach { u ->
                                val selected = unit == u
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (selected) PrimaryColor else CardColor)
                                        .clickable {
                                            unit = u
                                            manualUnit = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(u, fontSize = 12.sp, color = if (selected) Color.White else Text2Color)
                                }
                            }
                        }
                    }
                }
            }

            // 存储分区
            FormField("存储分区") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    STORAGE_ZONES.forEach { zone ->
                        val selected = storageZone == zone.value
                        val icon = when (zone.value) {
                            "refrigerator" -> "❄️"
                            "freezer" -> "🧊"
                            else -> "🌡"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) PrimaryBgColor else CardColor)
                                .clickable {
                                    storageZone = zone.value
                                    manualStorage = true
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$icon ${zone.label}", fontSize = 13.sp, color = if (selected) PrimaryColor else Text2Color)
                        }
                    }
                }
            }

            // 单价 + 总价
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    FormField("单价(元)") {
                        BasicTextField(
                            value = price,
                            onValueChange = {
                                price = it
                                recalcTotal()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            textStyle = TextStyle(fontSize = 14.sp, color = TextColor),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            decorationBox = { inner ->
                                if (price.isEmpty()) Text("0.00", fontSize = 14.sp, color = Text3Color)
                                inner()
                            }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    FormField("总价(元)") {
                        BasicTextField(
                            value = totalPrice,
                            onValueChange = { totalPrice = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            textStyle = TextStyle(fontSize = 14.sp, color = TextColor),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            decorationBox = { inner ->
                                if (totalPrice.isEmpty()) Text("0.00", fontSize = 14.sp, color = Text3Color)
                                inner()
                            }
                        )
                    }
                }
            }

            // 购买日期
            FormField("购买日期") {
                val calendar = Calendar.getInstance()
                val parts = purchaseDate.split("-")
                if (parts.size == 3) {
                    try {
                        calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    } catch (_: NumberFormatException) {
                        // 日期格式非法，使用当前日期
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardColor)
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    purchaseDate = "%04d-%02d-%02d".format(y, m + 1, d)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(purchaseDate, fontSize = 14.sp, color = TextColor)
                }
            }

            // 保质期天数
            FormField("保质期天数（预计到期：${calcExpiryDate(purchaseDate, expiryDays)}）") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardColor)
                            .clickable {
                                if (expiryDays > 1) {
                                    expiryDays -= 1
                                    manualExpiry = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("−", fontSize = 18.sp, color = PrimaryColor)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardColor)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$expiryDays 天", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextColor)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardColor)
                            .clickable {
                                expiryDays += 1
                                manualExpiry = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 18.sp, color = PrimaryColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 保存按钮
            Button(
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(23.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text(if (editId != null) "保存修改" else "添加食材", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // 删除确认
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除「$name」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    val ok = repo.deleteFood(editId!!)
                    showDeleteDialog = false
                    if (ok) {
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                        onClose()
                    }
                }) { Text("删除", color = DangerColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun FormField(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, fontSize = 13.sp, color = Text2Color, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardColor)
        ) {
            content()
        }
    }
}

private fun formatInput(v: Double): String =
    if (v == v.toInt().toDouble()) v.toInt().toString() else v.toString()

private fun parseDateMs(dateStr: String): Long {
    val parts = dateStr.split("-")
    if (parts.size == 3) {
        return try {
            val cal = Calendar.getInstance()
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (_: NumberFormatException) {
            System.currentTimeMillis()
        }
    }
    return System.currentTimeMillis()
}

private fun calcExpiryDate(purchaseDate: String, days: Int): String {
    val cal = Calendar.getInstance()
    val parts = purchaseDate.split("-")
    if (parts.size == 3) {
        try {
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        } catch (_: NumberFormatException) {
            // 日期格式非法，使用当前日期
        }
    }
    cal.add(Calendar.DAY_OF_MONTH, days)
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}
