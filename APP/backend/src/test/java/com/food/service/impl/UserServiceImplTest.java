package com.food.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.common.exception.BusinessException;
import com.food.common.result.ErrorCode;
import com.food.common.util.UserContext;
import com.food.dto.UserAddDTO;
import com.food.dto.UserPageDTO;
import com.food.dto.UserResetPasswordDTO;
import com.food.dto.UserUpdateDTO;
import com.food.entity.UserDO;
import com.food.mapper.UserMapper;
import com.food.vo.UserDetailVO;
import com.food.vo.UserListVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务实现类单元测试
 * <p>
 * 测试覆盖 UserServiceImpl 中所有方法的正常流程、异常流程和边界条件
 * 使用 Mockito 模拟数据库层操作，不依赖真实数据库
 *
 * @author FoodInventoryManager
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private static final Long SUPER_ADMIN_ID = 1L;
    private static final Long NORMAL_ADMIN_ID = 2L;
    private static final Long NORMAL_USER_ID = 3L;

    /**
     * 每个测试前设置当前用户为超级管理员
     */
    @BeforeEach
    void setUp() {
        UserContext.setUserId(SUPER_ADMIN_ID);
        UserContext.setRoleCodes(Set.of("SUPER_ADMIN"));
        UserContext.setPermissions(Set.of("*:*:*"));
    }

    /**
     * 每个测试后清理 ThreadLocal，防止内存泄漏和测试间干扰
     */
    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ==================== pageUserList 测试 ====================

    @Nested
    @DisplayName("分页查询用户列表")
    class PageUserListTests {

        @Test
        @DisplayName("正常流程：分页查询成功，返回用户列表")
        void pageUserList_success() {
            // 准备数据
            UserPageDTO dto = new UserPageDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setKeyword(null);
            dto.setStatus(null);

            UserDO user1 = createUserDO(NORMAL_ADMIN_ID, "admin", "管理员");
            UserDO user2 = createUserDO(NORMAL_USER_ID, "user", "普通用户");

            Page<UserDO> resultPage = new Page<>(1, 10, 2);
            resultPage.setRecords(Arrays.asList(user1, user2));

            when(userMapper.selectUserPage(any(Page.class), isNull(), isNull()))
                    .thenReturn(resultPage);

            // 执行测试
            Page<UserListVO> result = userService.pageUserList(dto);

            // 验证结果
            assertNotNull(result);
            assertEquals(1, result.getCurrent());
            assertEquals(10, result.getSize());
            assertEquals(2, result.getTotal());
            assertEquals(2, result.getRecords().size());
            assertEquals("admin", result.getRecords().get(0).getUsername());
            assertEquals("user", result.getRecords().get(1).getUsername());

            // 验证手机号脱敏
            assertTrue(result.getRecords().get(0).getPhone().contains("****"));

            verify(userMapper, times(1)).selectUserPage(any(Page.class), isNull(), isNull());
        }

        @Test
        @DisplayName("正常流程：带关键词搜索")
        void pageUserList_withKeyword() {
            UserPageDTO dto = new UserPageDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setKeyword("admin");
            dto.setStatus(null);

            UserDO user = createUserDO(NORMAL_ADMIN_ID, "admin", "管理员");
            Page<UserDO> resultPage = new Page<>(1, 10, 1);
            resultPage.setRecords(Collections.singletonList(user));

            when(userMapper.selectUserPage(any(Page.class), eq("admin"), isNull()))
                    .thenReturn(resultPage);

            Page<UserListVO> result = userService.pageUserList(dto);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals("admin", result.getRecords().get(0).getUsername());

            verify(userMapper, times(1)).selectUserPage(any(Page.class), eq("admin"), isNull());
        }

        @Test
        @DisplayName("正常流程：按状态筛选")
        void pageUserList_withStatus() {
            UserPageDTO dto = new UserPageDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setKeyword(null);
            dto.setStatus(1);

            UserDO user = createUserDO(NORMAL_USER_ID, "user", "普通用户");
            Page<UserDO> resultPage = new Page<>(1, 10, 1);
            resultPage.setRecords(Collections.singletonList(user));

            when(userMapper.selectUserPage(any(Page.class), isNull(), eq(1)))
                    .thenReturn(resultPage);

            Page<UserListVO> result = userService.pageUserList(dto);

            assertNotNull(result);
            assertEquals(1, result.getTotal());

            verify(userMapper, times(1)).selectUserPage(any(Page.class), isNull(), eq(1));
        }

        @Test
        @DisplayName("边界条件：空列表")
        void pageUserList_emptyList() {
            UserPageDTO dto = new UserPageDTO();
            dto.setPageNum(1);
            dto.setPageSize(10);

            Page<UserDO> resultPage = new Page<>(1, 10, 0);
            resultPage.setRecords(Collections.emptyList());

            when(userMapper.selectUserPage(any(Page.class), isNull(), isNull()))
                    .thenReturn(resultPage);

            Page<UserListVO> result = userService.pageUserList(dto);

            assertNotNull(result);
            assertEquals(0, result.getTotal());
            assertTrue(result.getRecords().isEmpty());

            verify(userMapper, times(1)).selectUserPage(any(Page.class), isNull(), isNull());
        }

        @Test
        @DisplayName("边界条件：pageSize 超过最大值时被截断为100")
        void pageUserList_pageSizeExceedsMax() {
            UserPageDTO dto = new UserPageDTO();
            dto.setPageNum(1);
            dto.setPageSize(200);

            Page<UserDO> resultPage = new Page<>(1, 100, 50);
            resultPage.setRecords(Collections.emptyList());

            when(userMapper.selectUserPage(any(Page.class), isNull(), isNull()))
                    .thenReturn(resultPage);

            userService.pageUserList(dto);

            // 验证传入的 pageSize 被限制为 100
            verify(userMapper, times(1)).selectUserPage(
                    argThat(page -> page.getSize() == 100),
                    isNull(), isNull());
        }

        @Test
        @DisplayName("边界条件：pageNum 为 null 时默认第1页")
        void pageUserList_pageNumNull() {
            UserPageDTO dto = new UserPageDTO();
            dto.setPageNum(null);
            dto.setPageSize(10);

            Page<UserDO> resultPage = new Page<>(1, 10, 0);
            resultPage.setRecords(Collections.emptyList());

            when(userMapper.selectUserPage(any(Page.class), isNull(), isNull()))
                    .thenReturn(resultPage);

            userService.pageUserList(dto);

            verify(userMapper, times(1)).selectUserPage(
                    argThat(page -> page.getCurrent() == 1),
                    isNull(), isNull());
        }

        @Test
        @DisplayName("边界条件：pageSize 为 null 时默认10条")
        void pageUserList_pageSizeNull() {
            UserPageDTO dto = new UserPageDTO();
            dto.setPageNum(1);
            dto.setPageSize(null);

            Page<UserDO> resultPage = new Page<>(1, 10, 0);
            resultPage.setRecords(Collections.emptyList());

            when(userMapper.selectUserPage(any(Page.class), isNull(), isNull()))
                    .thenReturn(resultPage);

            userService.pageUserList(dto);

            verify(userMapper, times(1)).selectUserPage(
                    argThat(page -> page.getSize() == 10),
                    isNull(), isNull());
        }
    }

    // ==================== getUserDetail 测试 ====================

    @Nested
    @DisplayName("获取用户详情")
    class GetUserDetailTests {

        @Test
        @DisplayName("正常流程：查询成功")
        void getUserDetail_success() {
            UserDO user = createUserDO(NORMAL_USER_ID, "testuser", "测试用户");
            user.setLastLoginIp("192.168.1.100");
            user.setEmail("testuser@example.com");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(user);

            UserDetailVO result = userService.getUserDetail(NORMAL_USER_ID);

            assertNotNull(result);
            assertEquals(NORMAL_USER_ID, result.getId());
            assertEquals("testuser", result.getUsername());
            assertEquals("测试用户", result.getNickname());

            // 验证敏感字段脱敏
            assertTrue(result.getPhone().contains("****"));
            assertTrue(result.getEmail().contains("***"));
            assertTrue(result.getLastLoginIp().contains("*"));

            verify(userMapper, times(1)).selectById(NORMAL_USER_ID);
        }

        @Test
        @DisplayName("异常流程：用户不存在，抛出业务异常")
        void getUserDetail_userNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.getUserDetail(999L));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
            assertEquals(ErrorCode.USER_NOT_FOUND.getMessage(), exception.getMessage());

            verify(userMapper, times(1)).selectById(999L);
        }

        @Test
        @DisplayName("边界条件：ID 为 null，抛出参数异常")
        void getUserDetail_idNull() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.getUserDetail(null));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("不合法"));

            verify(userMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("边界条件：ID 为负数，抛出参数异常")
        void getUserDetail_idNegative() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.getUserDetail(-1L));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());

            verify(userMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("边界条件：ID 为 0，抛出参数异常")
        void getUserDetail_idZero() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.getUserDetail(0L));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());

            verify(userMapper, never()).selectById(anyLong());
        }
    }

    // ==================== addUser 测试 ====================

    @Nested
    @DisplayName("添加用户")
    class AddUserTests {

        @Test
        @DisplayName("正常流程：添加用户成功")
        void addUser_success() {
            UserAddDTO dto = new UserAddDTO();
            dto.setUsername("newuser");
            dto.setPassword("Test123456");
            dto.setNickname("新用户");
            dto.setPhone("13912345678");
            dto.setEmail("newuser@example.com");

            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.selectByPhone("13912345678")).thenReturn(null);
            when(passwordEncoder.encode("Test123456")).thenReturn("encodedPassword");
            when(userMapper.insert(any(UserDO.class))).thenAnswer(invocation -> {
                UserDO user = invocation.getArgument(0);
                user.setId(10L);
                return 1;
            });

            Long userId = userService.addUser(dto);

            assertNotNull(userId);
            assertEquals(10L, userId);

            verify(userMapper, times(1)).selectByUsername("newuser");
            verify(userMapper, times(1)).selectByPhone("13912345678");
            verify(passwordEncoder, times(1)).encode("Test123456");
            verify(userMapper, times(1)).insert(any(UserDO.class));
        }

        @Test
        @DisplayName("正常流程：不填昵称时使用用户名作为昵称")
        void addUser_nicknameDefaultToUsername() {
            UserAddDTO dto = new UserAddDTO();
            dto.setUsername("newuser");
            dto.setPassword("Test123456");
            dto.setNickname(null);

            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(passwordEncoder.encode("Test123456")).thenReturn("encodedPassword");
            when(userMapper.insert(any(UserDO.class))).thenAnswer(invocation -> {
                UserDO user = invocation.getArgument(0);
                user.setId(10L);
                return 1;
            });

            Long userId = userService.addUser(dto);

            assertNotNull(userId);

            verify(userMapper).insert(argThat(user ->
                    "newuser".equals(user.getNickname())
            ));
        }

        @Test
        @DisplayName("正常流程：不填性别时默认为0（未知）")
        void addUser_genderDefaultZero() {
            UserAddDTO dto = new UserAddDTO();
            dto.setUsername("newuser");
            dto.setPassword("Test123456");
            dto.setGender(null);

            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(passwordEncoder.encode("Test123456")).thenReturn("encodedPassword");
            when(userMapper.insert(any(UserDO.class))).thenAnswer(invocation -> {
                UserDO user = invocation.getArgument(0);
                user.setId(10L);
                return 1;
            });

            userService.addUser(dto);

            verify(userMapper).insert(argThat(user ->
                    user.getGender() == 0
            ));
        }

        @Test
        @DisplayName("正常流程：手机号为空时不校验手机号唯一性")
        void addUser_phoneNullSkipPhoneCheck() {
            UserAddDTO dto = new UserAddDTO();
            dto.setUsername("newuser");
            dto.setPassword("Test123456");
            dto.setPhone(null);

            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(passwordEncoder.encode("Test123456")).thenReturn("encodedPassword");
            when(userMapper.insert(any(UserDO.class))).thenAnswer(invocation -> {
                UserDO user = invocation.getArgument(0);
                user.setId(10L);
                return 1;
            });

            userService.addUser(dto);

            verify(userMapper, never()).selectByPhone(any());
        }

        @Test
        @DisplayName("异常流程：用户名已存在，抛出异常")
        void addUser_usernameExists() {
            UserAddDTO dto = new UserAddDTO();
            dto.setUsername("existing");
            dto.setPassword("Test123456");

            UserDO existingUser = createUserDO(5L, "existing", "已存在用户");
            when(userMapper.selectByUsername("existing")).thenReturn(existingUser);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.addUser(dto));

            assertEquals(ErrorCode.USERNAME_EXISTS.getCode(), exception.getCode());

            verify(userMapper, times(1)).selectByUsername("existing");
            verify(userMapper, never()).insert(any());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("异常流程：手机号已存在，抛出异常")
        void addUser_phoneExists() {
            UserAddDTO dto = new UserAddDTO();
            dto.setUsername("newuser");
            dto.setPassword("Test123456");
            dto.setPhone("13900000000");

            UserDO existingUser = createUserDO(5L, "other", "其他用户");
            existingUser.setPhone("13900000000");

            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(userMapper.selectByPhone("13900000000")).thenReturn(existingUser);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.addUser(dto));

            assertEquals(ErrorCode.PHONE_EXISTS.getCode(), exception.getCode());

            verify(userMapper, times(1)).selectByUsername("newuser");
            verify(userMapper, times(1)).selectByPhone("13900000000");
            verify(userMapper, never()).insert(any());
        }

        @Test
        @DisplayName("异常流程：数据库插入失败，抛出操作失败异常")
        void addUser_insertFailed() {
            UserAddDTO dto = new UserAddDTO();
            dto.setUsername("newuser");
            dto.setPassword("Test123456");

            when(userMapper.selectByUsername("newuser")).thenReturn(null);
            when(passwordEncoder.encode("Test123456")).thenReturn("encodedPassword");
            when(userMapper.insert(any(UserDO.class))).thenReturn(0);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.addUser(dto));

            assertEquals(ErrorCode.OPERATION_FAILED.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("失败"));

            verify(userMapper, times(1)).insert(any(UserDO.class));
        }
    }

    // ==================== updateUser 测试 ====================

    @Nested
    @DisplayName("更新用户信息")
    class UpdateUserTests {

        @Test
        @DisplayName("正常流程：更新用户信息成功")
        void updateUser_success() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setId(NORMAL_USER_ID);
            dto.setNickname("新昵称");
            dto.setEmail("newemail@example.com");

            UserDO existingUser = createUserDO(NORMAL_USER_ID, "testuser", "旧昵称");
            existingUser.setPhone("13800000000");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(existingUser);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

            Boolean result = userService.updateUser(dto);

            assertTrue(result);

            verify(userMapper, times(1)).selectById(NORMAL_USER_ID);
            verify(userMapper, times(1)).updateById(any(UserDO.class));
        }

        @Test
        @DisplayName("正常流程：更新手机号（手机号变更时校验唯一性）")
        void updateUser_phoneChanged_checkUnique() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setId(NORMAL_USER_ID);
            dto.setPhone("13911112222");

            UserDO existingUser = createUserDO(NORMAL_USER_ID, "testuser", "用户");
            existingUser.setPhone("13800000000");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(existingUser);
            when(userMapper.selectByPhone("13911112222")).thenReturn(null);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

            Boolean result = userService.updateUser(dto);

            assertTrue(result);

            verify(userMapper, times(1)).selectByPhone("13911112222");
            verify(userMapper).updateById(argThat(user ->
                    "13911112222".equals(user.getPhone())
            ));
        }

        @Test
        @DisplayName("正常流程：手机号未变更时不校验唯一性")
        void updateUser_phoneNotChanged_skipCheck() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setId(NORMAL_USER_ID);
            dto.setPhone("13800000000");

            UserDO existingUser = createUserDO(NORMAL_USER_ID, "testuser", "用户");
            existingUser.setPhone("13800000000");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(existingUser);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

            userService.updateUser(dto);

            verify(userMapper, never()).selectByPhone(any());
        }

        @Test
        @DisplayName("异常流程：用户不存在，抛出异常")
        void updateUser_userNotFound() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setId(999L);
            dto.setNickname("新昵称");

            when(userMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUser(dto));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());

            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("异常流程：ID 为 null，抛出参数异常")
        void updateUser_idNull() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setId(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUser(dto));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());

            verify(userMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("异常流程：新手机号已被其他用户使用")
        void updateUser_phoneExistsForOtherUser() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setId(NORMAL_USER_ID);
            dto.setPhone("13900000000");

            UserDO currentUser = createUserDO(NORMAL_USER_ID, "user1", "用户1");
            currentUser.setPhone("13800000000");

            UserDO otherUser = createUserDO(NORMAL_ADMIN_ID, "user2", "用户2");
            otherUser.setPhone("13900000000");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(currentUser);
            when(userMapper.selectByPhone("13900000000")).thenReturn(otherUser);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUser(dto));

            assertEquals(ErrorCode.PHONE_EXISTS.getCode(), exception.getCode());

            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("异常流程：普通管理员尝试修改超级管理员，抛出越权异常")
        void updateUser_normalAdminModifySuperAdmin_forbidden() {
            // 切换为普通管理员
            UserContext.clear();
            UserContext.setUserId(NORMAL_ADMIN_ID);
            UserContext.setRoleCodes(Set.of("ADMIN"));
            UserContext.setPermissions(Set.of("system:user:edit"));

            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setId(SUPER_ADMIN_ID);
            dto.setNickname("修改超级管理员");

            UserDO superAdmin = createUserDO(SUPER_ADMIN_ID, "admin", "超级管理员");

            when(userMapper.selectById(SUPER_ADMIN_ID)).thenReturn(superAdmin);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUser(dto));

            assertEquals(ErrorCode.FORBIDDEN.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("无权限"));

            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("边界条件：更新影响行数为0，返回false")
        void updateUser_updateRowsZero() {
            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setId(NORMAL_USER_ID);
            dto.setNickname("新昵称");

            UserDO existingUser = createUserDO(NORMAL_USER_ID, "testuser", "旧昵称");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(existingUser);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(0);

            Boolean result = userService.updateUser(dto);

            assertFalse(result);

            verify(userMapper, times(1)).updateById(any(UserDO.class));
        }
    }

    // ==================== deleteUser 测试 ====================

    @Nested
    @DisplayName("删除用户")
    class DeleteUserTests {

        @Test
        @DisplayName("正常流程：删除普通用户成功")
        void deleteUser_success() {
            UserDO user = createUserDO(NORMAL_USER_ID, "testuser", "测试用户");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(user);
            when(userMapper.deleteById(NORMAL_USER_ID)).thenReturn(1);

            Boolean result = userService.deleteUser(NORMAL_USER_ID);

            assertTrue(result);

            verify(userMapper, times(1)).selectById(NORMAL_USER_ID);
            verify(userMapper, times(1)).deleteById(NORMAL_USER_ID);
        }

        @Test
        @DisplayName("异常流程：删除超级管理员，抛出异常")
        void deleteUser_cannotDeleteSuperAdmin() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(SUPER_ADMIN_ID));

            assertEquals(ErrorCode.CANNOT_DELETE_ADMIN.getCode(), exception.getCode());

            verify(userMapper, never()).selectById(anyLong());
            verify(userMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("异常流程：用户不存在，抛出异常")
        void deleteUser_userNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(999L));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());

            verify(userMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("边界条件：ID 为 null，抛出参数异常")
        void deleteUser_idNull() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(null));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());

            verify(userMapper, never()).selectById(anyLong());
            verify(userMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("边界条件：ID 为负数，抛出参数异常")
        void deleteUser_idNegative() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(-1L));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("边界条件：删除影响行数为0，返回false")
        void deleteUser_deleteRowsZero() {
            UserDO user = createUserDO(NORMAL_USER_ID, "testuser", "测试用户");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(user);
            when(userMapper.deleteById(NORMAL_USER_ID)).thenReturn(0);

            Boolean result = userService.deleteUser(NORMAL_USER_ID);

            assertFalse(result);

            verify(userMapper, times(1)).deleteById(NORMAL_USER_ID);
        }
    }

    // ==================== updateUserStatus 测试 ====================

    @Nested
    @DisplayName("更新用户状态")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("正常流程：禁用普通用户成功")
        void updateUserStatus_disableSuccess() {
            UserDO user = createUserDO(NORMAL_USER_ID, "testuser", "测试用户");
            user.setStatus(1);

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(user);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

            Boolean result = userService.updateUserStatus(NORMAL_USER_ID, 0);

            assertTrue(result);

            verify(userMapper).updateById(argThat(u -> u.getStatus() == 0));
        }

        @Test
        @DisplayName("正常流程：启用普通用户成功")
        void updateUserStatus_enableSuccess() {
            UserDO user = createUserDO(NORMAL_USER_ID, "testuser", "测试用户");
            user.setStatus(0);

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(user);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

            Boolean result = userService.updateUserStatus(NORMAL_USER_ID, 1);

            assertTrue(result);

            verify(userMapper).updateById(argThat(u -> u.getStatus() == 1));
        }

        @Test
        @DisplayName("异常流程：禁用超级管理员，抛出异常")
        void updateUserStatus_cannotDisableSuperAdmin() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUserStatus(SUPER_ADMIN_ID, 0));

            assertEquals(ErrorCode.CANNOT_DISABLE_ADMIN.getCode(), exception.getCode());

            verify(userMapper, never()).selectById(anyLong());
            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("正常流程：启用超级管理员（允许）")
        void updateUserStatus_enableSuperAdmin_allowed() {
            UserDO user = createUserDO(SUPER_ADMIN_ID, "admin", "超级管理员");
            user.setStatus(0);

            when(userMapper.selectById(SUPER_ADMIN_ID)).thenReturn(user);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(1);

            Boolean result = userService.updateUserStatus(SUPER_ADMIN_ID, 1);

            assertTrue(result);

            verify(userMapper, times(1)).updateById(any(UserDO.class));
        }

        @Test
        @DisplayName("异常流程：用户不存在，抛出异常")
        void updateUserStatus_userNotFound() {
            when(userMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUserStatus(999L, 1));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());

            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("边界条件：状态值非法（2），抛出参数异常")
        void updateUserStatus_invalidStatus() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUserStatus(NORMAL_USER_ID, 2));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());

            verify(userMapper, never()).selectById(anyLong());
            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("边界条件：状态值为 null，抛出参数异常")
        void updateUserStatus_statusNull() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUserStatus(NORMAL_USER_ID, null));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("边界条件：状态未变化时直接返回成功，不执行更新")
        void updateUserStatus_statusNotChanged_skipUpdate() {
            UserDO user = createUserDO(NORMAL_USER_ID, "testuser", "测试用户");
            user.setStatus(1);

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(user);

            Boolean result = userService.updateUserStatus(NORMAL_USER_ID, 1);

            assertTrue(result);

            // 不调用 updateById
            verify(userMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("边界条件：ID 为 null，抛出参数异常")
        void updateUserStatus_idNull() {
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.updateUserStatus(null, 1));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("边界条件：更新影响行数为0，返回false")
        void updateUserStatus_updateRowsZero() {
            UserDO user = createUserDO(NORMAL_USER_ID, "testuser", "测试用户");
            user.setStatus(1);

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(user);
            when(userMapper.updateById(any(UserDO.class))).thenReturn(0);

            Boolean result = userService.updateUserStatus(NORMAL_USER_ID, 0);

            assertFalse(result);
        }
    }

    // ==================== resetPassword 测试 ====================

    @Nested
    @DisplayName("重置用户密码")
    class ResetPasswordTests {

        @Test
        @DisplayName("正常流程：重置密码成功")
        void resetPassword_success() {
            UserResetPasswordDTO dto = new UserResetPasswordDTO();
            dto.setNewPassword("NewPass123");

            UserDO user = createUserDO(NORMAL_USER_ID, "testuser", "测试用户");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(user);
            when(passwordEncoder.encode("NewPass123")).thenReturn("encodedNewPassword");
            when(userMapper.updatePassword(eq(NORMAL_USER_ID), anyString())).thenReturn(1);

            Boolean result = userService.resetPassword(NORMAL_USER_ID, dto);

            assertTrue(result);

            verify(passwordEncoder, times(1)).encode("NewPass123");
            verify(userMapper, times(1)).updatePassword(eq(NORMAL_USER_ID), eq("encodedNewPassword"));
        }

        @Test
        @DisplayName("异常流程：用户不存在，抛出异常")
        void resetPassword_userNotFound() {
            UserResetPasswordDTO dto = new UserResetPasswordDTO();
            dto.setNewPassword("NewPass123");

            when(userMapper.selectById(999L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.resetPassword(999L, dto));

            assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());

            verify(userMapper, never()).updatePassword(anyLong(), anyString());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("异常流程：ID 为 null，抛出参数异常")
        void resetPassword_idNull() {
            UserResetPasswordDTO dto = new UserResetPasswordDTO();
            dto.setNewPassword("NewPass123");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.resetPassword(null, dto));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());

            verify(userMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("异常流程：新密码为空，抛出参数异常")
        void resetPassword_newPasswordEmpty() {
            UserResetPasswordDTO dto = new UserResetPasswordDTO();
            dto.setNewPassword("");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.resetPassword(NORMAL_USER_ID, dto));

            assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());

            verify(userMapper, never()).selectById(anyLong());
            verify(passwordEncoder, never()).encode(any());
        }

        @Test
        @DisplayName("异常流程：普通管理员重置超级管理员密码，抛出越权异常")
        void resetPassword_normalAdminResetSuperAdmin_forbidden() {
            // 切换为普通管理员
            UserContext.clear();
            UserContext.setUserId(NORMAL_ADMIN_ID);
            UserContext.setRoleCodes(Set.of("ADMIN"));
            UserContext.setPermissions(Set.of("system:user:resetPwd"));

            UserResetPasswordDTO dto = new UserResetPasswordDTO();
            dto.setNewPassword("NewPass123");

            BusinessException exception = assertThrows(BusinessException.class,
                    () -> userService.resetPassword(SUPER_ADMIN_ID, dto));

            assertEquals(ErrorCode.FORBIDDEN.getCode(), exception.getCode());
            assertTrue(exception.getMessage().contains("无权限"));

            verify(userMapper, never()).selectById(anyLong());
            verify(userMapper, never()).updatePassword(anyLong(), anyString());
        }

        @Test
        @DisplayName("边界条件：更新影响行数为0，返回false")
        void resetPassword_updateRowsZero() {
            UserResetPasswordDTO dto = new UserResetPasswordDTO();
            dto.setNewPassword("NewPass123");

            UserDO user = createUserDO(NORMAL_USER_ID, "testuser", "测试用户");

            when(userMapper.selectById(NORMAL_USER_ID)).thenReturn(user);
            when(passwordEncoder.encode("NewPass123")).thenReturn("encodedNewPassword");
            when(userMapper.updatePassword(eq(NORMAL_USER_ID), anyString())).thenReturn(0);

            Boolean result = userService.resetPassword(NORMAL_USER_ID, dto);

            assertFalse(result);

            verify(userMapper, times(1)).updatePassword(eq(NORMAL_USER_ID), anyString());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试用用户DO对象
     *
     * @param id       用户ID
     * @param username 用户名
     * @param nickname 昵称
     * @return 用户DO
     */
    private UserDO createUserDO(Long id, String username, String nickname) {
        UserDO user = new UserDO();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPassword("encodedPassword");
        user.setPhone("13812345678");
        user.setEmail(username + "@example.com");
        user.setGender(1);
        user.setStatus(1);
        user.setAvatar(null);
        user.setRemark("测试用户");
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp("192.168.1.100");
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        return user;
    }
}
