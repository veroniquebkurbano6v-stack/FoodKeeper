package com.example.foodkeeper

import android.app.Application
import android.util.Log
import com.example.foodkeeper.data.repository.InventoryRepository
import com.example.foodkeeper.domain.FoodKnowledgeBase
import com.example.foodkeeper.domain.RecipeEngine
import com.example.foodkeeper.domain.UnitConverter

/**
 * Application 入口（对应原 ArkTS EntryAbility 的 onCreate）
 *
 * 负责在应用启动时初始化：
 *  1. 数据加载器（FoodKnowledgeBase / RecipeEngine / UnitConverter 从 assets 读取 .ets 数据）
 *  2. 库存存储（InventoryRepository 从 SharedPreferences 加载，首次写入种子数据）
 *
 * 数据完全本地化，不依赖任何网络请求
 */
class FoodkeeperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this

        // 初始化数据加载器（从 assets 读取 .ets 源码并解析）
        FoodKnowledgeBase.init(this)
        UnitConverter.init(this)
        RecipeEngine.init(this)

        Log.d("FoodkeeperApp", "数据初始化完成: 食材知识库=${FoodKnowledgeBase.getAll().size}条, 菜谱库=${RecipeEngine.size()}条")

        // 初始化库存存储（从 SharedPreferences 加载，首次启动写入种子数据）
        InventoryRepository.getInstance(this).init()
    }

    companion object {
        @Volatile
        lateinit var appContext: Application
            private set
    }
}
