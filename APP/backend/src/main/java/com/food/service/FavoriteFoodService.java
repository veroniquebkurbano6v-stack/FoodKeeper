package com.food.service;

import com.food.dto.FavoriteFoodAddDTO;
import com.food.vo.FavoriteFoodListVO;

import java.util.List;

/**
 * 常用食品服务接口
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
public interface FavoriteFoodService {

    /**
     * 添加常用食品
     *
     * @param dto 常用食品添加请求
     * @return 常用食品ID
     */
    Long addFavoriteFood(FavoriteFoodAddDTO dto);

    /**
     * 删除常用食品
     *
     * @param id 常用食品ID
     * @return 是否成功
     */
    Boolean deleteFavoriteFood(Long id);

    /**
     * 获取用户的常用食品列表
     *
     * @param userId 用户ID
     * @return 常用食品列表
     */
    List<FavoriteFoodListVO> listUserFavoriteFoods(Long userId);

    /**
     * 更新常用食品使用次数
     *
     * @param id 常用食品ID
     * @return 是否成功
     */
    Boolean updateUseCount(Long id);

    /**
     * 根据用户ID和食材名称查询常用食品
     *
     * @param userId 用户ID
     * @param name   食材名称
     * @return 常用食品信息
     */
    FavoriteFoodListVO getFavoriteFoodByName(Long userId, String name);

}