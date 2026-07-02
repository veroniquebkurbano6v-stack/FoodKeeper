package com.food.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.common.annotation.RequiresPermissions;
import com.food.common.annotation.RequiresRoles;
import com.food.common.result.Result;
import com.food.common.util.UserContext;
import com.food.dto.UserAddDTO;
import com.food.dto.UserPageDTO;
import com.food.dto.UserResetPasswordDTO;
import com.food.dto.UserUpdateDTO;
import com.food.service.UserService;
import com.food.vo.UserDetailVO;
import com.food.vo.UserListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 用户管理控制器
 * <p>
 * 提供用户增删改查、状态变更、密码重置等接口
 * 所有接口需要管理员权限（SUPER_ADMIN 或 ADMIN）
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户增删改查、状态管理、密码重置接口")
@RequiresRoles({"SUPER_ADMIN", "ADMIN"})
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     * <p>
     * 支持关键词搜索（用户名/昵称/手机号）和状态筛选
     * 权限标识：system:user:list
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param keyword  搜索关键词（可选）
     * @param status   用户状态（可选）
     * @return 分页用户列表
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表",
            description = "支持关键词搜索（用户名/昵称/手机号）和状态筛选")
    @RequiresPermissions("system:user:list")
    public Result<Page<UserListVO>> pageUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户状态：0禁用 1启用") @RequestParam(required = false) Integer status) {

        UserPageDTO dto = new UserPageDTO();
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        dto.setKeyword(keyword);
        dto.setStatus(status);

        Page<UserListVO> page = userService.pageUserList(dto);
        return Result.success(page);
    }

    /**
     * 获取用户详情
     * <p>
     * 根据用户ID查询用户详细信息（不含密码，敏感字段脱敏）
     * 权限标识：system:user:query
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取用户详情", description = "根据ID查询用户详细信息（敏感字段脱敏）")
    @RequiresPermissions("system:user:query")
    public Result<UserDetailVO> getUserDetail(
            @Parameter(description = "用户ID") @PathVariable @NotNull(message = "用户ID不能为空") Long id) {

        UserDetailVO detail = userService.getUserDetail(id);
        log.info("查询用户详情，操作人={}, 用户ID={}", UserContext.getUserId(), id);
        return Result.success(detail);
    }

    /**
     * 新增用户
     * <p>
     * 用户名唯一，密码使用 BCrypt 加密存储
     * 权限标识：system:user:add
     *
     * @param dto 用户添加请求
     * @return 用户ID
     */
    @PostMapping
    @Operation(summary = "新增用户", description = "创建新用户，用户名唯一")
    @RequiresPermissions("system:user:add")
    public Result<Long> addUser(@RequestBody @Valid UserAddDTO dto) {
        Long userId = userService.addUser(dto);
        return Result.success("用户创建成功", userId);
    }

    /**
     * 更新用户信息
     * <p>
     * 仅更新非空字段，手机号唯一（排除自身）
     * 权限标识：system:user:edit
     *
     * @param dto 用户更新请求
     * @return 是否成功
     */
    @PutMapping
    @Operation(summary = "更新用户信息", description = "修改用户基本信息")
    @RequiresPermissions("system:user:edit")
    public Result<Boolean> updateUser(@RequestBody @Valid UserUpdateDTO dto) {
        Boolean result = userService.updateUser(dto);
        return Result.success("用户信息更新成功", result);
    }

    /**
     * 删除用户（逻辑删除）
     * <p>
     * 禁止删除超级管理员（ID=1）
     * 权限标识：system:user:delete
     *
     * @param id 用户ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "逻辑删除用户，禁止删除超级管理员")
    @RequiresPermissions("system:user:delete")
    public Result<Boolean> deleteUser(
            @Parameter(description = "用户ID") @PathVariable @NotNull(message = "用户ID不能为空") Long id) {

        Boolean result = userService.deleteUser(id);
        return Result.success("用户删除成功", result);
    }

    /**
     * 修改用户状态（启用/禁用）
     * <p>
     * 禁止禁用超级管理员（ID=1）
     * 权限标识：system:user:edit
     *
     * @param id     用户ID
     * @param status 目标状态：0禁用 1启用
     * @return 是否成功
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "修改用户状态", description = "启用或禁用用户账号，禁止禁用超级管理员")
    @RequiresPermissions("system:user:edit")
    public Result<Boolean> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable @NotNull(message = "用户ID不能为空") Long id,
            @Parameter(description = "目标状态：0禁用 1启用") @RequestParam @NotNull(message = "状态不能为空") Integer status) {

        Boolean result = userService.updateUserStatus(id, status);
        String message = status == 1 ? "用户启用成功" : "用户禁用成功";
        return Result.success(message, result);
    }

    /**
     * 重置用户密码
     * <p>
     * 管理员重置用户密码，新密码使用 BCrypt 加密
     * 权限标识：system:user:resetPwd
     *
     * @param id  用户ID
     * @param dto 重置密码请求
     * @return 是否成功
     */
    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    @RequiresPermissions("system:user:resetPwd")
    public Result<Boolean> resetPassword(
            @Parameter(description = "用户ID") @PathVariable @NotNull(message = "用户ID不能为空") Long id,
            @RequestBody @Valid UserResetPasswordDTO dto) {

        Boolean result = userService.resetPassword(id, dto);
        return Result.success("密码重置成功", result);
    }

}
