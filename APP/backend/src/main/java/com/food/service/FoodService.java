package com.food.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.dto.FoodAddDTO;
import com.food.dto.FoodUpdateDTO;
import com.food.dto.QuickAddDTO;
import com.food.vo.FoodDetailVO;
import com.food.vo.FoodListVO;

import java.util.List;

/**
 * 食材服务接口
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
public interface FoodService {

    /**
     * 添加食材
     *
     * @param dto 食材添加请求
     * @return 食材ID
     */
    Long addFood(FoodAddDTO dto);

    /**
     * 快速添加食材（从常用食品）
     *
     * @param dto 快速添加请求
     * @return 食材ID
     */
    Long quickAddFood(QuickAddDTO dto);

    /**
     * 更新食材
     *
     * @param dto 食材更新请求
     * @return 是否成功
     */
    Boolean updateFood(FoodUpdateDTO dto);

    /**
     * 删除食材（逻辑删除）
     *
     * @param id 食材ID
     * @return 是否成功
     */
    Boolean deleteFood(Long id);

    /**
     * 获取食材详情
     *
     * @param id 食材ID
     * @return 食材详情
     */
    FoodDetailVO getFoodDetail(Long id);

    /**
     * 分页查询食材列表
     *
     * @param categoryId 分类ID（可选）
     * @param keyword    搜索关键词（可选）
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @return 食材列表分页数据
     */
    Page<FoodListVO> pageFoodList(Long categoryId, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 获取临期食材列表
     *
     * @param days 剩余天数阈值
     * @return 临期食材列表
     */
    List<FoodListVO> listExpiringFood(Integer days);

    /**
     * 出库操作（烹饪消耗）
     *
     * @param id       食材ID
     * @param quantity 出库数量
     * @return 是否成功
     */
    Boolean outboundFood(Long id, Integer quantity);

}