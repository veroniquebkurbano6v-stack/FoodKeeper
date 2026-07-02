package com.food.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.common.exception.BusinessException;
import com.food.common.result.ErrorCode;
import com.food.dto.FoodAddDTO;
import com.food.dto.FoodUpdateDTO;
import com.food.dto.QuickAddDTO;
import com.food.entity.CategoryDO;
import com.food.entity.FavoriteFoodDO;
import com.food.entity.FoodDO;
import com.food.mapper.CategoryMapper;
import com.food.mapper.FavoriteFoodMapper;
import com.food.mapper.FoodMapper;
import com.food.service.FavoriteFoodService;
import com.food.service.FoodService;
import com.food.vo.FoodDetailVO;
import com.food.vo.FoodListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 食材服务实现类
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private final FoodMapper foodMapper;
    private final CategoryMapper categoryMapper;
    private final FavoriteFoodMapper favoriteFoodMapper;
    private final FavoriteFoodService favoriteFoodService;

    /**
     * 添加食材
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFood(FoodAddDTO dto) {
        log.debug("开始添加食材，参数: {}", dto);

        // 校验分类是否存在
        CategoryDO category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 计算保质期截止日期
        LocalDate expiryDate = dto.getPurchaseDate().plusDays(dto.getExpiryDays());

        // 构建 DO 对象
        FoodDO food = new FoodDO();
        food.setName(dto.getName());
        food.setCategoryId(dto.getCategoryId());
        food.setQuantity(dto.getQuantity());
        food.setUnit(dto.getUnit());
        food.setPurchaseDate(dto.getPurchaseDate());
        food.setExpiryDate(expiryDate);
        food.setStorageZone(dto.getStorageZone());
        food.setPrice(dto.getPrice());
        food.setTotalPrice(dto.getTotalPrice());
        food.setStatus(0);

        // 保存食材
        foodMapper.insert(food);

        log.info("食材添加成功，ID={}, 名称={}", food.getId(), food.getName());
        return food.getId();
    }

    /**
     * 快速添加食材（从常用食品）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long quickAddFood(QuickAddDTO dto) {
        log.debug("开始快速添加食材，参数: {}", dto);

        // 查询常用食品
        FavoriteFoodDO favoriteFood = favoriteFoodMapper.selectById(dto.getFavoriteFoodId());
        if (favoriteFood == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }

        // 校验分类是否存在
        CategoryDO category = categoryMapper.selectById(favoriteFood.getCategoryId());
        if (category == null) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        // 确定采购日期（默认使用当前日期）
        LocalDate purchaseDate = dto.getPurchaseDate() != null
                ? dto.getPurchaseDate()
                : LocalDate.now();

        // 确定保质期天数（默认使用常用食品的默认值）
        Integer expiryDays = dto.getExpiryDays() != null
                ? dto.getExpiryDays()
                : favoriteFood.getDefaultExpiryDays();

        // 如果没有默认保质期天数，使用7天作为默认值
        if (expiryDays == null) {
            expiryDays = 7;
        }

        // 计算保质期截止日期
        LocalDate expiryDate = purchaseDate.plusDays(expiryDays);

        // 确定单价（默认使用常用食品的默认值）
        java.math.BigDecimal price = dto.getPrice() != null
                ? dto.getPrice()
                : favoriteFood.getDefaultPrice();

        // 构建 DO 对象
        FoodDO food = new FoodDO();
        food.setName(favoriteFood.getName());
        food.setCategoryId(favoriteFood.getCategoryId());
        food.setQuantity(dto.getQuantity());
        food.setUnit(favoriteFood.getUnit());
        food.setPurchaseDate(purchaseDate);
        food.setExpiryDate(expiryDate);
        food.setStorageZone(favoriteFood.getStorageZone());
        food.setPrice(price);
        food.setTotalPrice(dto.getTotalPrice());
        food.setStatus(0);

        // 保存食材
        foodMapper.insert(food);

        // 更新常用食品的使用次数
        favoriteFoodService.updateUseCount(dto.getFavoriteFoodId());

        log.info("食材快速添加成功，ID={}, 名称={}", food.getId(), food.getName());
        return food.getId();
    }

    /**
     * 更新食材
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateFood(FoodUpdateDTO dto) {
        log.debug("开始更新食材，参数: {}", dto);

        // 查询食材是否存在
        FoodDO food = foodMapper.selectById(dto.getId());
        if (food == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }

        // 更新字段
        if (dto.getName() != null) {
            food.setName(dto.getName());
        }
        if (dto.getQuantity() != null) {
            food.setQuantity(dto.getQuantity());
        }
        if (dto.getExpiryDate() != null) {
            food.setExpiryDate(dto.getExpiryDate());
        }
        if (dto.getStorageZone() != null) {
            food.setStorageZone(dto.getStorageZone());
        }

        // 执行更新
        int rows = foodMapper.updateById(food);

        log.info("食材更新成功，ID={}", food.getId());
        return rows > 0;
    }

    /**
     * 删除食材（逻辑删除）
     */
    @Override
    public Boolean deleteFood(Long id) {
        log.debug("开始删除食材，ID={}", id);

        // 查询食材是否存在
        FoodDO food = foodMapper.selectById(id);
        if (food == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }

        // 执行逻辑删除
        int rows = foodMapper.deleteById(id);

        log.info("食材删除成功，ID={}", id);
        return rows > 0;
    }

    /**
     * 获取食材详情
     */
    @Override
    public FoodDetailVO getFoodDetail(Long id) {
        // 查询食材
        FoodDO food = foodMapper.selectById(id);
        if (food == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }

        // 查询分类名称
        CategoryDO category = categoryMapper.selectById(food.getCategoryId());
        String categoryName = category != null ? category.getName() : "";

        // 计算剩余天数
        int remainingDays = calculateRemainingDays(food.getExpiryDate());

        // 构建 VO
        FoodDetailVO vo = new FoodDetailVO();
        vo.setId(food.getId());
        vo.setName(food.getName());
        vo.setCategoryName(categoryName);
        vo.setQuantity(food.getQuantity());
        vo.setUnit(food.getUnit());
        vo.setPurchaseDate(food.getPurchaseDate().toString());
        vo.setExpiryDate(food.getExpiryDate().toString());
        vo.setRemainingDays(remainingDays);
        vo.setExpiryStatus(getExpiryStatus(remainingDays));
        vo.setStorageZone(food.getStorageZone());
        vo.setTotalPrice(food.getTotalPrice() != null ? food.getTotalPrice().toString() : "0");
        return vo;
    }

    /**
     * 分页查询食材列表
     */
    @Override
    public Page<FoodListVO> pageFoodList(Long categoryId, String keyword, Integer pageNum, Integer pageSize) {
        // 构建查询条件
        LambdaQueryWrapper<FoodDO> wrapper = new LambdaQueryWrapper<>();
        
        // 分类筛选
        if (categoryId != null) {
            wrapper.eq(FoodDO::getCategoryId, categoryId);
        }
        
        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(FoodDO::getName, keyword);
        }
        
        // 按保质期排序（临期优先）
        wrapper.orderByAsc(FoodDO::getExpiryDate);

        // 分页查询
        Page<FoodDO> page = new Page<>(pageNum, pageSize);
        Page<FoodDO> result = foodMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<FoodListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<FoodListVO> voList = result.getRecords().stream()
                .map(this::convertToListVO)
                .toList();
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 获取临期食材列表
     */
    @Override
    public List<FoodListVO> listExpiringFood(Integer days) {
        LocalDate threshold = LocalDate.now().plusDays(days);

        LambdaQueryWrapper<FoodDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(FoodDO::getExpiryDate, threshold)
               .gt(FoodDO::getQuantity, 0)
               .orderByAsc(FoodDO::getExpiryDate);

        List<FoodDO> foods = foodMapper.selectList(wrapper);
        return foods.stream().map(this::convertToListVO).toList();
    }

    /**
     * 出库操作（烹饪消耗）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean outboundFood(Long id, Integer quantity) {
        FoodDO food = foodMapper.selectById(id);
        if (food == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }

        // 校验库存数量
        if (food.getQuantity() < quantity) {
            throw new BusinessException(ErrorCode.QUANTITY_NOT_ENOUGH);
        }

        // 更新库存
        food.setQuantity(food.getQuantity() - quantity);
        int rows = foodMapper.updateById(food);

        log.info("食材出库成功，ID={}, 数量={}", id, quantity);
        return rows > 0;
    }

    /**
     * 计算剩余天数
     */
    private int calculateRemainingDays(LocalDate expiryDate) {
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    /**
     * 获取保质期状态
     */
    private String getExpiryStatus(int remainingDays) {
        if (remainingDays < 0) {
            return "EXPIRED";
        } else if (remainingDays <= 2) {
            return "RED";
        } else if (remainingDays <= 7) {
            return "YELLOW";
        } else {
            return "GREEN";
        }
    }

    /**
     * DO 转 ListVO
     */
    private FoodListVO convertToListVO(FoodDO food) {
        int remainingDays = calculateRemainingDays(food.getExpiryDate());
        FoodListVO vo = new FoodListVO();
        vo.setId(food.getId());
        vo.setName(food.getName());
        vo.setCategoryId(food.getCategoryId());
        vo.setQuantity(food.getQuantity());
        vo.setUnit(food.getUnit());
        vo.setExpiryDate(food.getExpiryDate().toString());
        vo.setRemainingDays(remainingDays);
        vo.setExpiryStatus(getExpiryStatus(remainingDays));
        vo.setStorageZone(food.getStorageZone());
        return vo;
    }

}