package com.food.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.common.result.Result;
import com.food.dto.FoodAddDTO;
import com.food.dto.FoodUpdateDTO;
import com.food.dto.QuickAddDTO;
import com.food.service.FoodService;
import com.food.vo.FoodDetailVO;
import com.food.vo.FoodListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 食材控制器
 * <p>
 * 提供食材管理相关接口
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Validated
@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
@Tag(name = "食材管理", description = "食材增删改查接口")
public class FoodController {

    private final FoodService foodService;

    /**
     * 添加食材
     */
    @PostMapping("/add")
    @Operation(summary = "添加食材", description = "录入新食材到库存")
    public Result<Long> addFood(@RequestBody @Valid FoodAddDTO dto) {
        Long foodId = foodService.addFood(dto);
        return Result.success(foodId);
    }

    /**
     * 快速添加食材（从常用食品）
     */
    @PostMapping("/quick-add")
    @Operation(summary = "快速添加食材", description = "从常用食品快速添加到库存")
    public Result<Long> quickAddFood(@RequestBody @Valid QuickAddDTO dto) {
        Long foodId = foodService.quickAddFood(dto);
        return Result.success(foodId);
    }

    /**
     * 更新食材
     */
    @PutMapping("/update")
    @Operation(summary = "更新食材", description = "修改食材信息")
    public Result<Boolean> updateFood(@RequestBody @Valid FoodUpdateDTO dto) {
        Boolean result = foodService.updateFood(dto);
        return Result.success(result);
    }

    /**
     * 删除食材
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除食材", description = "逻辑删除食材")
    public Result<Boolean> deleteFood(
            @Parameter(description = "食材ID") @PathVariable Long id) {
        Boolean result = foodService.deleteFood(id);
        return Result.success(result);
    }

    /**
     * 获取食材详情
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取食材详情", description = "根据ID查询食材详情")
    public Result<FoodDetailVO> getFoodDetail(
            @Parameter(description = "食材ID") @PathVariable Long id) {
        FoodDetailVO detail = foodService.getFoodDetail(id);
        return Result.success(detail);
    }

    /**
     * 分页查询食材列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询食材列表", description = "支持分类筛选和关键词搜索")
    public Result<Page<FoodListVO>> pageFoodList(
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<FoodListVO> page = foodService.pageFoodList(categoryId, keyword, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取临期食材列表
     */
    @GetMapping("/expiring")
    @Operation(summary = "获取临期食材列表", description = "查询即将过期的食材")
    public Result<List<FoodListVO>> listExpiringFood(
            @Parameter(description = "剩余天数阈值") @RequestParam(defaultValue = "7") Integer days) {
        List<FoodListVO> list = foodService.listExpiringFood(days);
        return Result.success(list);
    }

    /**
     * 食材出库
     */
    @PostMapping("/outbound/{id}")
    @Operation(summary = "食材出库", description = "减少库存数量（烹饪消耗）")
    public Result<Boolean> outboundFood(
            @Parameter(description = "食材ID") @PathVariable Long id,
            @Parameter(description = "出库数量") @RequestParam Integer quantity) {
        Boolean result = foodService.outboundFood(id, quantity);
        return Result.success(result);
    }

}