package com.food.controller;

import com.food.common.result.Result;
import com.food.common.util.UserContext;
import com.food.dto.FavoriteFoodAddDTO;
import com.food.service.FavoriteFoodService;
import com.food.vo.FavoriteFoodListVO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 常用食品控制器
 * <p>
 * 提供常用食品管理相关接口
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Validated
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
@Tag(name = "常用食品管理", description = "常用食品增删查接口")
public class FavoriteFoodController {

    private final FavoriteFoodService favoriteFoodService;

    /**
     * 添加常用食品
     */
    @PostMapping("/add")
    @Operation(summary = "添加常用食品", description = "将食材添加为常用食品")
    public Result<Long> addFavoriteFood(@RequestBody @Valid FavoriteFoodAddDTO dto) {
        Long favoriteFoodId = favoriteFoodService.addFavoriteFood(dto);
        return Result.success(favoriteFoodId);
    }

    /**
     * 删除常用食品
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除常用食品", description = "逻辑删除常用食品")
    public Result<Boolean> deleteFavoriteFood(
            @Parameter(description = "常用食品ID") @PathVariable Long id) {
        Boolean result = favoriteFoodService.deleteFavoriteFood(id);
        return Result.success(result);
    }

    /**
     * 获取用户的常用食品列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取常用食品列表", description = "查询用户的常用食品列表")
    public Result<List<FavoriteFoodListVO>> listFavoriteFoods() {
        Long userId = UserContext.getUserId();
        List<FavoriteFoodListVO> list = favoriteFoodService.listUserFavoriteFoods(userId);
        return Result.success(list);
    }

    /**
     * 根据食材名称查询常用食品
     */
    @GetMapping("/search/{name}")
    @Operation(summary = "根据名称查询常用食品", description = "根据食材名称查询是否为常用食品")
    public Result<FavoriteFoodListVO> getFavoriteFoodByName(
            @Parameter(description = "食材名称") @PathVariable String name) {
        Long userId = UserContext.getUserId();
        FavoriteFoodListVO favoriteFood = favoriteFoodService.getFavoriteFoodByName(userId, name);
        return Result.success(favoriteFood);
    }

}