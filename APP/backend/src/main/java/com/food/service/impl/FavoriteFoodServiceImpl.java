package com.food.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.common.exception.BusinessException;
import com.food.common.result.ErrorCode;
import com.food.common.util.UserContext;
import com.food.dto.FavoriteFoodAddDTO;
import com.food.entity.CategoryDO;
import com.food.entity.FavoriteFoodDO;
import com.food.mapper.CategoryMapper;
import com.food.mapper.FavoriteFoodMapper;
import com.food.service.FavoriteFoodService;
import com.food.vo.FavoriteFoodListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 常用食品服务实现类
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteFoodServiceImpl implements FavoriteFoodService {

    private final FavoriteFoodMapper favoriteFoodMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 添加常用食品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFavoriteFood(FavoriteFoodAddDTO dto) {
        log.debug("开始添加常用食品，参数: {}", dto);

        // 获取当前用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 校验分类是否存在
        CategoryDO category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 查询是否已存在相同名称的常用食品
        LambdaQueryWrapper<FavoriteFoodDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteFoodDO::getUserId, userId)
               .eq(FavoriteFoodDO::getName, dto.getName());
        FavoriteFoodDO existingFood = favoriteFoodMapper.selectOne(wrapper);

        if (existingFood != null) {
            // 如果已存在,更新使用次数
            existingFood.setUseCount(existingFood.getUseCount() + 1);
            favoriteFoodMapper.updateById(existingFood);
            log.info("常用食品已存在，更新使用次数，ID={}", existingFood.getId());
            return existingFood.getId();
        }

        // 构建 DO 对象
        FavoriteFoodDO favoriteFood = FavoriteFoodDO.builder()
                .userId(userId)
                .name(dto.getName())
                .categoryId(dto.getCategoryId())
                .unit(dto.getUnit())
                .storageZone(dto.getStorageZone())
                .defaultExpiryDays(dto.getDefaultExpiryDays())
                .defaultPrice(dto.getDefaultPrice())
                .useCount(1)
                .build();

        // 保存常用食品
        favoriteFoodMapper.insert(favoriteFood);

        log.info("常用食品添加成功，ID={}, 名称={}", favoriteFood.getId(), favoriteFood.getName());
        return favoriteFood.getId();
    }

    /**
     * 删除常用食品
     */
    @Override
    public Boolean deleteFavoriteFood(Long id) {
        log.debug("开始删除常用食品，ID={}", id);

        // 获取当前用户ID
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 查询常用食品是否存在
        FavoriteFoodDO favoriteFood = favoriteFoodMapper.selectById(id);
        if (favoriteFood == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }

        // 校验是否为当前用户的常用食品
        if (!favoriteFood.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 执行逻辑删除
        int rows = favoriteFoodMapper.deleteById(id);

        log.info("常用食品删除成功，ID={}", id);
        return rows > 0;
    }

    /**
     * 获取用户的常用食品列表
     */
    @Override
    public List<FavoriteFoodListVO> listUserFavoriteFoods(Long userId) {
        log.debug("开始查询用户常用食品列表，用户ID={}", userId);

        // 构建查询条件
        LambdaQueryWrapper<FavoriteFoodDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteFoodDO::getUserId, userId)
               .orderByDesc(FavoriteFoodDO::getUseCount)
               .orderByDesc(FavoriteFoodDO::getCreatedAt);

        // 查询列表，最多返回10条
        wrapper.last("LIMIT 10");
        List<FavoriteFoodDO> foods = favoriteFoodMapper.selectList(wrapper);

        // 转换为 VO
        return foods.stream()
                .map(this::convertToListVO)
                .toList();
    }

    /**
     * 更新常用食品使用次数
     */
    @Override
    public Boolean updateUseCount(Long id) {
        log.debug("开始更新常用食品使用次数，ID={}", id);

        FavoriteFoodDO favoriteFood = favoriteFoodMapper.selectById(id);
        if (favoriteFood == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }

        // 增加使用次数
        favoriteFood.setUseCount(favoriteFood.getUseCount() + 1);
        int rows = favoriteFoodMapper.updateById(favoriteFood);

        log.info("常用食品使用次数更新成功，ID={}, 使用次数={}", id, favoriteFood.getUseCount());
        return rows > 0;
    }

    /**
     * 根据用户ID和食材名称查询常用食品
     */
    @Override
    public FavoriteFoodListVO getFavoriteFoodByName(Long userId, String name) {
        log.debug("开始查询常用食品，用户ID={}, 名称={}", userId, name);

        LambdaQueryWrapper<FavoriteFoodDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteFoodDO::getUserId, userId)
               .eq(FavoriteFoodDO::getName, name);

        FavoriteFoodDO favoriteFood = favoriteFoodMapper.selectOne(wrapper);
        if (favoriteFood == null) {
            return null;
        }

        return convertToListVO(favoriteFood);
    }

    /**
     * DO 转 ListVO
     */
    private FavoriteFoodListVO convertToListVO(FavoriteFoodDO favoriteFood) {
        // 查询分类名称
        CategoryDO category = categoryMapper.selectById(favoriteFood.getCategoryId());
        String categoryName = category != null ? category.getName() : "";

        // 获取存放分区名称
        String storageZoneName = getStorageZoneName(favoriteFood.getStorageZone());

        return FavoriteFoodListVO.builder()
                .id(favoriteFood.getId())
                .name(favoriteFood.getName())
                .categoryName(categoryName)
                .categoryId(favoriteFood.getCategoryId())
                .unit(favoriteFood.getUnit())
                .storageZone(favoriteFood.getStorageZone())
                .storageZoneName(storageZoneName)
                .defaultExpiryDays(favoriteFood.getDefaultExpiryDays())
                .defaultPrice(favoriteFood.getDefaultPrice() != null
                        ? favoriteFood.getDefaultPrice().toString() : "")
                .useCount(favoriteFood.getUseCount())
                .build();
    }

    /**
     * 获取存放分区名称
     */
    private String getStorageZoneName(String storageZone) {
        if (storageZone == null) {
            return "";
        }
        switch (storageZone) {
            case "refrigerator":
                return "冷藏室";
            case "freezer":
                return "冷冻层";
            case "room":
                return "常温";
            default:
                return storageZone;
        }
    }

}