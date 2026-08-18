package com.example.foodkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.example.foodkeeper.ui.home.HomeScreen
import com.example.foodkeeper.ui.inventory.InventoryScreen
import com.example.foodkeeper.ui.recipe.RecipeScreen
import com.example.foodkeeper.ui.settings.SettingsScreen
import com.example.foodkeeper.ui.theme.BgColor
import com.example.foodkeeper.ui.theme.FoodkeeperTheme
import com.example.foodkeeper.ui.theme.PrimaryColor
import com.example.foodkeeper.ui.theme.Text3Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置状态栏颜色
        window.statusBarColor = android.graphics.Color.parseColor("#7C9473")
        setContent {
            FoodkeeperTheme {
                MainScreen()
            }
        }
    }
}

private data class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen() {
    val tabs = remember {
        listOf(
            TabItem("首页", Icons.Filled.Home, Icons.Outlined.Home),
            TabItem("库存", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
            TabItem("AI菜谱", Icons.Filled.Receipt, Icons.Outlined.Receipt),
            TabItem("我的", Icons.Filled.Person, Icons.Outlined.Person)
        )
    }
    var currentTab by remember { mutableIntStateOf(0) }
    // 全局刷新信号：切回Tab时自增，触发子页面刷新
    var refreshSignal by remember { mutableIntStateOf(0) }
    // 菜谱收藏/已完成状态提升到 MainScreen 层级，避免切换 Tab 时丢失
    var favoriteSet by remember { mutableStateOf(emptySet<String>()) }
    var completedSet by remember { mutableStateOf(emptySet<String>()) }

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                modifier = Modifier.background(Color.White)
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = {
                            if (currentTab != index) {
                                currentTab = index
                                refreshSignal++
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == index) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (currentTab == index) PrimaryColor else Text3Color
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                color = if (currentTab == index) PrimaryColor else Text3Color
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgColor)
        ) {
            when (currentTab) {
                0 -> HomeScreen(refreshSignal = refreshSignal, onTabChange = { currentTab = it })
                1 -> InventoryScreen(refreshSignal = refreshSignal)
                2 -> RecipeScreen(
                    refreshSignal = refreshSignal,
                    favoriteSet = favoriteSet,
                    completedSet = completedSet,
                    onFavoriteChange = { favoriteSet = it },
                    onCompletedChange = { completedSet = it }
                )
                3 -> SettingsScreen(refreshSignal = refreshSignal)
            }
        }
    }
}
