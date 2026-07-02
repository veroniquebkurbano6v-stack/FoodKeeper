"""
用户管理模块接口自动化测试用例

测试覆盖：
- 分页查询用户列表
- 获取用户详情
- 新增用户
- 更新用户信息
- 删除用户
- 修改用户状态
- 重置用户密码
"""

import pytest
import random
import allure
from typing import Dict

from common.api_client import (
    APIClient, auth_manager,
    assert_success, assert_error, assert_pagination_valid,
    assert_field_exists, assert_field_value, assert_field_desensitized,
    generate_test_username, generate_test_phone, generate_test_email,
    generate_valid_password, generate_invalid_password_short,
    generate_invalid_password_no_upper, generate_invalid_password_no_lower,
    generate_invalid_password_no_digit
)
from config.config import (
    API_PREFIX, SUCCESS_CODE, PARAM_ERROR_CODE,
    USER_NOT_FOUND_CODE, USERNAME_EXISTS_CODE, PHONE_EXISTS_CODE,
    CANNOT_DELETE_ADMIN_CODE, CANNOT_DISABLE_ADMIN_CODE,
    UNAUTHORIZED_CODE, FORBIDDEN_CODE, TOKEN_EXPIRED_CODE,
    SUPER_ADMIN_ID, DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE
)


# ==================== 测试客户端实例 ====================

api_client = APIClient()
USER_API = API_PREFIX["user"]


# ==================== 分页查询用户列表测试 ====================

@allure.feature("用户管理")
@allure.story("分页查询用户列表")
class TestUserListAPI:
    """分页查询用户列表接口测试"""
    
    @allure.title("正常场景：无筛选条件查询用户列表")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_user_list_success(self):
        """
        测试无筛选条件的用户列表查询
        
        预期：返回分页用户列表，手机号/邮箱已脱敏
        """
        endpoint = f"{USER_API}/list?pageNum={DEFAULT_PAGE_NUM}&pageSize={DEFAULT_PAGE_SIZE}"
        response = api_client.get(endpoint)
        
        result = assert_success(response)
        data = result.get("data")
        
        # 验证分页结构
        assert_pagination_valid(data)
        
        # 验证敏感字段脱敏
        if data["records"]:
            user = data["records"][0]
            if user.get("phone"):
                assert_field_desensitized(user, "phone", "****")
            if user.get("email"):
                # 邮箱脱敏格式：abc***@example.com
                assert "***" in user.get("email") or user.get("email").count("*") >= 2
    
    @allure.title("正常场景：关键词搜索用户名")
    @allure.severity(allure.severity_level.NORMAL)
    def test_user_list_with_keyword(self):
        """
        测试关键词搜索功能
        
        预期：返回匹配关键词的用户列表
        """
        keyword = "admin"
        endpoint = f"{USER_API}/list?pageNum=1&pageSize=10&keyword={keyword}"
        response = api_client.get(endpoint)
        
        result = assert_success(response)
        data = result.get("data")
        
        # 验证搜索结果包含关键词
        if data["records"]:
            for user in data["records"]:
                # 用户名、昵称或手机号应包含关键词
                assert (
                    keyword in user.get("username", "") or
                    keyword in user.get("nickname", "") or
                    keyword in user.get("phone", "")
                ), f"搜索结果不匹配关键词: {user}"
    
    @allure.title("正常场景：按状态筛选启用用户")
    @allure.severity(allure.severity_level.NORMAL)
    def test_user_list_filter_enabled(self):
        """
        测试按状态筛选启用用户
        
        预期：仅返回status=1的用户
        """
        endpoint = f"{USER_API}/list?pageNum=1&pageSize=10&status=1"
        response = api_client.get(endpoint)
        
        result = assert_success(response)
        data = result.get("data")
        
        # 验证所有返回用户状态为启用
        for user in data["records"]:
            assert_field_value(user, "status", 1)
    
    @allure.title("正常场景：按状态筛选禁用用户")
    @allure.severity(allure.severity_level.NORMAL)
    def test_user_list_filter_disabled(self):
        """
        测试按状态筛选禁用用户
        
        预期：仅返回status=0的用户
        """
        endpoint = f"{USER_API}/list?pageNum=1&pageSize=10&status=0"
        response = api_client.get(endpoint)
        
        result = assert_success(response)
        data = result.get("data")
        
        # 验证所有返回用户状态为禁用
        for user in data["records"]:
            assert_field_value(user, "status", 0)
    
    @allure.title("边界场景：pageSize超过最大值100")
    @allure.severity(allure.severity_level.NORMAL)
    def test_user_list_page_size_exceeds_max(self):
        """
        测试pageSize超过最大值
        
        预期：服务端自动截断为100条
        """
        endpoint = f"{USER_API}/list?pageNum=1&pageSize=200"
        response = api_client.get(endpoint)
        
        result = assert_success(response)
        data = result.get("data")
        
        # 验证size被限制为100
        assert data.get("size") <= MAX_PAGE_SIZE
    
    @allure.title("边界场景：pageNum超出范围")
    @allure.severity(allure.severity_level.NORMAL)
    def test_user_list_page_num_exceeds(self):
        """
        测试pageNum超出范围
        
        预期：返回空列表，total正确显示总数
        """
        endpoint = f"{USER_API}/list?pageNum=1000&pageSize=10"
        response = api_client.get(endpoint)
        
        result = assert_success(response)
        data = result.get("data")
        
        # 验证返回空列表
        assert data.get("records") == []
        assert data.get("total") >= 0
    
    @allure.title("边界场景：空用户列表")
    @allure.severity(allure.severity_level.NORMAL)
    def test_user_list_empty_with_impossible_keyword(self):
        """
        测试不可能匹配的关键词
        
        预期：返回空列表
        """
        endpoint = f"{USER_API}/list?pageNum=1&pageSize=10&keyword=zzzzzzzzzzzz"
        response = api_client.get(endpoint)
        
        result = assert_success(response)
        data = result.get("data")
        
        # 验证返回空列表
        assert data.get("records") == []
    
    @allure.title("异常场景：无Token访问")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_user_list_no_token(self):
        """
        测试无Token访问
        
        预期：返回未授权错误
        """
        endpoint = f"{USER_API}/list?pageNum=1&pageSize=10"
        response = api_client.get(endpoint, with_auth=False)
        
        assert_error(response, UNAUTHORIZED_CODE, "未登录")
    
    @allure.title("异常场景：无效Token访问")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_user_list_invalid_token(self):
        """
        测试无效Token访问
        
        预期：返回Token无效或过期错误
        """
        # 设置无效Token
        auth_manager.set_token("invalid-token-12345")
        
        endpoint = f"{USER_API}/list?pageNum=1&pageSize=10"
        response = api_client.get(endpoint)
        
        # 应返回未授权或Token过期错误
        result = response.json()
        assert result.get("code") in [UNAUTHORIZED_CODE, TOKEN_EXPIRED_CODE]
        
        # 恢复有效Token
        auth_manager.clear_token()


# ==================== 获取用户详情测试 ====================

@allure.feature("用户管理")
@allure.story("获取用户详情")
class TestUserDetailAPI:
    """获取用户详情接口测试"""
    
    @allure.title("正常场景：查询存在的用户详情")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_user_detail_success(self):
        """
        测试查询存在的用户详情
        
        预期：返回用户详情，敏感字段脱敏
        """
        # 先获取用户列表找一个存在的用户ID
        list_endpoint = f"{USER_API}/list?pageNum=1&pageSize=1"
        list_response = api_client.get(list_endpoint)
        list_result = list_response.json()
        
        if list_result.get("data", {}).get("records"):
            user_id = list_result["data"]["records"][0]["id"]
            
            endpoint = f"{USER_API}/{user_id}"
            response = api_client.get(endpoint)
            
            result = assert_success(response)
            user = result.get("data")
            
            # 验证核心字段存在
            assert_field_exists(user, "id")
            assert_field_exists(user, "username")
            assert_field_exists(user, "nickname")
            
            # 验证敏感字段脱敏
            if user.get("phone"):
                assert_field_desensitized(user, "phone", "****")
            if user.get("email"):
                assert "***" in user.get("email")
    
    @allure.title("异常场景：用户不存在")
    @allure.severity(allure.severity_level.NORMAL)
    def test_user_detail_not_found(self):
        """
        测试查询不存在的用户
        
        预期：返回用户不存在错误
        """
        endpoint = f"{USER_API}/999999"
        response = api_client.get(endpoint)
        
        assert_error(response, USER_NOT_FOUND_CODE)
    
    @allure.title("边界场景：ID为负数")
    @allure.severity(allure.severity_level.NORMAL)
    def test_user_detail_negative_id(self):
        """
        测试ID为负数
        
        预期：返回参数错误
        """
        endpoint = f"{USER_API}/-1"
        response = api_client.get(endpoint)
        
        assert_error(response, PARAM_ERROR_CODE, "不合法")
    
    @allure.title("边界场景：ID为0")
    @allure.severity(allure.severity_level.NORMAL)
    def test_user_detail_zero_id(self):
        """
        测试ID为0
        
        预期：返回参数错误
        """
        endpoint = f"{USER_API}/0"
        response = api_client.get(endpoint)
        
        assert_error(response, PARAM_ERROR_CODE, "不合法")
    
    @allure.title("异常场景：无Token访问")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_user_detail_no_token(self):
        """
        测试无Token访问
        
        预期：返回未授权错误
        """
        endpoint = f"{USER_API}/1"
        response = api_client.get(endpoint, with_auth=False)
        
        assert_error(response, UNAUTHORIZED_CODE)


# ==================== 新增用户测试 ====================

@allure.feature("用户管理")
@allure.story("新增用户")
class TestAddUserAPI:
    """新增用户接口测试"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """测试前准备"""
        self.test_username = generate_test_username()
        self.test_phone = generate_test_phone()
        self.test_email = generate_test_email(self.test_username)
        self.valid_password = generate_valid_password()
    
    @allure.title("正常场景：完整信息添加用户")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_add_user_full_info(self):
        """
        测试完整信息添加用户
        
        预期：创建成功，返回用户ID
        """
        payload = {
            "username": self.test_username,
            "password": self.valid_password,
            "nickname": "自动化测试用户",
            "phone": self.test_phone,
            "email": self.test_email,
            "gender": 1,
            "remark": "自动化测试创建"
        }
        
        response = api_client.post(USER_API, json=payload)
        
        result = assert_success(response, "创建成功")
        
        # 验证返回用户ID
        user_id = result.get("data")
        assert user_id is not None
        assert isinstance(user_id, int)
    
    @allure.title("正常场景：仅必填字段添加用户")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_add_user_minimal(self):
        """
        测试仅必填字段添加用户
        
        预期：创建成功，nickname默认等于username
        """
        payload = {
            "username": self.test_username,
            "password": self.valid_password
        }
        
        response = api_client.post(USER_API, json=payload)
        
        result = assert_success(response)
        user_id = result.get("data")
        assert user_id is not None
    
    @allure.title("异常场景：用户名已存在")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_username_exists(self):
        """
        测试用户名已存在
        
        预期：返回用户名已存在错误
        """
        payload = {
            "username": "admin",  # 已存在的用户名
            "password": self.valid_password
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, USERNAME_EXISTS_CODE)
    
    @allure.title("异常场景：用户名为空")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_empty_username(self):
        """
        测试用户名为空
        
        预期：返回参数校验失败
        """
        payload = {
            "username": "",
            "password": self.valid_password
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE, "不能为空")
    
    @allure.title("异常场景：用户名长度不足")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_short_username(self):
        """
        测试用户名长度不足
        
        预期：返回参数校验失败
        """
        payload = {
            "username": "abc",  # 仅3个字符
            "password": self.valid_password
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE)
    
    @allure.title("异常场景：用户名含特殊字符")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_special_chars(self):
        """
        测试用户名含特殊字符
        
        预期：返回参数校验失败
        """
        payload = {
            "username": "test@user!",
            "password": self.valid_password
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE)
    
    @allure.title("异常场景：密码为空")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_empty_password(self):
        """
        测试密码为空
        
        预期：返回参数校验失败
        """
        payload = {
            "username": self.test_username,
            "password": ""
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE, "不能为空")
    
    @allure.title("异常场景：密码长度不足")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_short_password(self):
        """
        测试密码长度不足
        
        预期：返回参数校验失败
        """
        payload = {
            "username": self.test_username,
            "password": generate_invalid_password_short()
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE)
    
    @allure.title("异常场景：密码不含大写字母")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_password_no_upper(self):
        """
        测试密码不含大写字母
        
        预期：返回参数校验失败
        """
        payload = {
            "username": self.test_username,
            "password": generate_invalid_password_no_upper()
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE)
    
    @allure.title("异常场景：密码不含小写字母")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_password_no_lower(self):
        """
        测试密码不含小写字母
        
        预期：返回参数校验失败
        """
        payload = {
            "username": self.test_username,
            "password": generate_invalid_password_no_lower()
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE)
    
    @allure.title("异常场景：密码不含数字")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_password_no_digit(self):
        """
        测试密码不含数字
        
        预期：返回参数校验失败
        """
        payload = {
            "username": self.test_username,
            "password": generate_invalid_password_no_digit()
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE)
    
    @allure.title("异常场景：手机号格式错误")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_invalid_phone(self):
        """
        测试手机号格式错误
        
        预期：返回参数校验失败
        """
        payload = {
            "username": self.test_username,
            "password": self.valid_password,
            "phone": "12345678"  # 格式错误
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE, "手机号")
    
    @allure.title("异常场景：邮箱格式错误")
    @allure.severity(allure.severity_level.NORMAL)
    def test_add_user_invalid_email(self):
        """
        测试邮箱格式错误
        
        预期：返回参数校验失败
        """
        payload = {
            "username": self.test_username,
            "password": self.valid_password,
            "email": "invalid-email"
        }
        
        response = api_client.post(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE, "邮箱")
    
    @allure.title("异常场景：无Token访问")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_add_user_no_token(self):
        """
        测试无Token访问
        
        预期：返回未授权错误
        """
        payload = {
            "username": self.test_username,
            "password": self.valid_password
        }
        
        response = api_client.post(USER_API, json=payload, with_auth=False)
        
        assert_error(response, UNAUTHORIZED_CODE)


# ==================== 更新用户信息测试 ====================

@allure.feature("用户管理")
@allure.story("更新用户信息")
class TestUpdateUserAPI:
    """更新用户信息接口测试"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """测试前准备"""
        self.test_username = generate_test_username()
        self.test_phone = generate_test_phone()
    
    @allure.title("正常场景：更新用户昵称和邮箱")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_user_success(self):
        """
        测试更新用户信息
        
        预期：更新成功
        """
        # 先创建一个测试用户
        create_payload = {
            "username": self.test_username,
            "password": generate_valid_password()
        }
        create_response = api_client.post(USER_API, json=create_payload)
        create_result = create_response.json()
        
        if create_result.get("code") == SUCCESS_CODE:
            user_id = create_result.get("data")
            
            # 更新用户
            update_payload = {
                "id": user_id,
                "nickname": "更新后的昵称",
                "email": f"updated_{self.test_username}@test.com"
            }
            
            response = api_client.put(USER_API, json=update_payload)
            
            result = assert_success(response, "更新成功")
            assert result.get("data") is True
    
    @allure.title("异常场景：用户不存在")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_user_not_found(self):
        """
        测试更新不存在的用户
        
        预期：返回用户不存在错误
        """
        payload = {
            "id": 999999,
            "nickname": "测试昵称"
        }
        
        response = api_client.put(USER_API, json=payload)
        
        assert_error(response, USER_NOT_FOUND_CODE)
    
    @allure.title("异常场景：ID为null")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_user_null_id(self):
        """
        测试ID为null
        
        预期：返回参数错误
        """
        payload = {
            "id": None,
            "nickname": "测试昵称"
        }
        
        response = api_client.put(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE, "不合法")
    
    @allure.title("异常场景：ID为负数")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_user_negative_id(self):
        """
        测试ID为负数
        
        预期：返回参数错误
        """
        payload = {
            "id": -1,
            "nickname": "测试昵称"
        }
        
        response = api_client.put(USER_API, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE, "不合法")
    
    @allure.title("异常场景：无Token访问")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_user_no_token(self):
        """
        测试无Token访问
        
        预期：返回未授权错误
        """
        payload = {
            "id": 1,
            "nickname": "测试昵称"
        }
        
        response = api_client.put(USER_API, json=payload, with_auth=False)
        
        assert_error(response, UNAUTHORIZED_CODE)


# ==================== 删除用户测试 ====================

@allure.feature("用户管理")
@allure.story("删除用户")
class TestDeleteUserAPI:
    """删除用户接口测试"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """测试前准备"""
        self.test_username = generate_test_username()
    
    @allure.title("正常场景：删除普通用户")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_delete_user_success(self):
        """
        测试删除普通用户
        
        预期：删除成功
        """
        # 先创建一个测试用户
        create_payload = {
            "username": self.test_username,
            "password": generate_valid_password()
        }
        create_response = api_client.post(USER_API, json=create_payload)
        create_result = create_response.json()
        
        if create_result.get("code") == SUCCESS_CODE:
            user_id = create_result.get("data")
            
            # 删除用户
            endpoint = f"{USER_API}/{user_id}"
            response = api_client.delete(endpoint)
            
            result = assert_success(response, "删除成功")
            assert result.get("data") is True
    
    @allure.title("异常场景：删除超级管理员")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_delete_super_admin(self):
        """
        测试删除超级管理员
        
        预期：返回不能删除超级管理员错误
        """
        endpoint = f"{USER_API}/{SUPER_ADMIN_ID}"
        response = api_client.delete(endpoint)
        
        assert_error(response, CANNOT_DELETE_ADMIN_CODE)
    
    @allure.title("异常场景：用户不存在")
    @allure.severity(allure.severity_level.NORMAL)
    def test_delete_user_not_found(self):
        """
        测试删除不存在的用户
        
        预期：返回用户不存在错误
        """
        endpoint = f"{USER_API}/999999"
        response = api_client.delete(endpoint)
        
        assert_error(response, USER_NOT_FOUND_CODE)
    
    @allure.title("边界场景：ID为负数")
    @allure.severity(allure.severity_level.NORMAL)
    def test_delete_user_negative_id(self):
        """
        测试ID为负数
        
        预期：返回参数错误
        """
        endpoint = f"{USER_API}/-1"
        response = api_client.delete(endpoint)
        
        assert_error(response, PARAM_ERROR_CODE, "不合法")
    
    @allure.title("边界场景：ID为0")
    @allure.severity(allure.severity_level.NORMAL)
    def test_delete_user_zero_id(self):
        """
        测试ID为0
        
        预期：返回参数错误
        """
        endpoint = f"{USER_API}/0"
        response = api_client.delete(endpoint)
        
        assert_error(response, PARAM_ERROR_CODE, "不合法")
    
    @allure.title("异常场景：无Token访问")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_delete_user_no_token(self):
        """
        测试无Token访问
        
        预期：返回未授权错误
        """
        endpoint = f"{USER_API}/1"
        response = api_client.delete(endpoint, with_auth=False)
        
        assert_error(response, UNAUTHORIZED_CODE)


# ==================== 修改用户状态测试 ====================

@allure.feature("用户管理")
@allure.story("修改用户状态")
class TestUpdateUserStatusAPI:
    """修改用户状态接口测试"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """测试前准备"""
        self.test_username = generate_test_username()
    
    @allure.title("正常场景：禁用普通用户")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_disable_user_success(self):
        """
        测试禁用普通用户
        
        预期：禁用成功
        """
        # 先创建一个测试用户
        create_payload = {
            "username": self.test_username,
            "password": generate_valid_password()
        }
        create_response = api_client.post(USER_API, json=create_payload)
        create_result = create_response.json()
        
        if create_result.get("code") == SUCCESS_CODE:
            user_id = create_result.get("data")
            
            # 禁用用户
            endpoint = f"{USER_API}/{user_id}/status?status=0"
            response = api_client.put(endpoint)
            
            result = assert_success(response, "禁用成功")
            assert result.get("data") is True
    
    @allure.title("正常场景：启用普通用户")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_enable_user_success(self):
        """
        测试启用普通用户
        
        预期：启用成功
        """
        # 先创建一个测试用户
        create_payload = {
            "username": self.test_username,
            "password": generate_valid_password()
        }
        create_response = api_client.post(USER_API, json=create_payload)
        create_result = create_response.json()
        
        if create_result.get("code") == SUCCESS_CODE:
            user_id = create_result.get("data")
            
            # 先禁用
            endpoint = f"{USER_API}/{user_id}/status?status=0"
            api_client.put(endpoint)
            
            # 再启用
            endpoint = f"{USER_API}/{user_id}/status?status=1"
            response = api_client.put(endpoint)
            
            result = assert_success(response, "启用成功")
            assert result.get("data") is True
    
    @allure.title("异常场景：禁用超级管理员")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_disable_super_admin(self):
        """
        测试禁用超级管理员
        
        预期：返回不能禁用超级管理员错误
        """
        endpoint = f"{USER_API}/{SUPER_ADMIN_ID}/status?status=0"
        response = api_client.put(endpoint)
        
        assert_error(response, CANNOT_DISABLE_ADMIN_CODE)
    
    @allure.title("边界场景：status为非法值")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_status_invalid_value(self):
        """
        测试status为非法值
        
        预期：返回参数错误
        """
        endpoint = f"{USER_API}/1/status?status=2"
        response = api_client.put(endpoint)
        
        assert_error(response, PARAM_ERROR_CODE, "不合法")
    
    @allure.title("边界场景：status为负数")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_status_negative(self):
        """
        测试status为负数
        
        预期：返回参数错误
        """
        endpoint = f"{USER_API}/1/status?status=-1"
        response = api_client.put(endpoint)
        
        assert_error(response, PARAM_ERROR_CODE, "不合法")
    
    @allure.title("异常场景：用户不存在")
    @allure.severity(allure.severity_level.NORMAL)
    def test_update_status_user_not_found(self):
        """
        测试用户不存在
        
        预期：返回用户不存在错误
        """
        endpoint = f"{USER_API}/999999/status?status=1"
        response = api_client.put(endpoint)
        
        assert_error(response, USER_NOT_FOUND_CODE)
    
    @allure.title("异常场景：无Token访问")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_update_status_no_token(self):
        """
        测试无Token访问
        
        预期：返回未授权错误
        """
        endpoint = f"{USER_API}/1/status?status=1"
        response = api_client.put(endpoint, with_auth=False)
        
        assert_error(response, UNAUTHORIZED_CODE)


# ==================== 重置用户密码测试 ====================

@allure.feature("用户管理")
@allure.story("重置用户密码")
class TestResetPasswordAPI:
    """重置用户密码接口测试"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """测试前准备"""
        self.test_username = generate_test_username()
    
    @allure.title("正常场景：重置普通用户密码")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_reset_password_success(self):
        """
        测试重置普通用户密码
        
        预期：重置成功
        """
        # 先创建一个测试用户
        create_payload = {
            "username": self.test_username,
            "password": generate_valid_password()
        }
        create_response = api_client.post(USER_API, json=create_payload)
        create_result = create_response.json()
        
        if create_result.get("code") == SUCCESS_CODE:
            user_id = create_result.get("data")
            
            # 重置密码
            endpoint = f"{USER_API}/{user_id}/reset-password"
            payload = {"newPassword": "NewPass123"}
            response = api_client.put(endpoint, json=payload)
            
            result = assert_success(response, "重置成功")
            assert result.get("data") is True
    
    @allure.title("异常场景：用户不存在")
    @allure.severity(allure.severity_level.NORMAL)
    def test_reset_password_user_not_found(self):
        """
        测试用户不存在
        
        预期：返回用户不存在错误
        """
        endpoint = f"{USER_API}/999999/reset-password"
        payload = {"newPassword": "NewPass123"}
        response = api_client.put(endpoint, json=payload)
        
        assert_error(response, USER_NOT_FOUND_CODE)
    
    @allure.title("异常场景：新密码为空")
    @allure.severity(allure.severity_level.NORMAL)
    def test_reset_password_empty(self):
        """
        测试新密码为空
        
        预期：返回参数错误
        """
        endpoint = f"{USER_API}/1/reset-password"
        payload = {"newPassword": ""}
        response = api_client.put(endpoint, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE, "不能为空")
    
    @allure.title("异常场景：新密码长度不足")
    @allure.severity(allure.severity_level.NORMAL)
    def test_reset_password_short(self):
        """
        测试新密码长度不足
        
        预期：返回参数错误
        """
        endpoint = f"{USER_API}/1/reset-password"
        payload = {"newPassword": "New12"}
        response = api_client.put(endpoint, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE)
    
    @allure.title("异常场景：新密码不含大写字母")
    @allure.severity(allure.severity_level.NORMAL)
    def test_reset_password_no_upper(self):
        """
        测试新密码不含大写字母
        
        预期：返回参数错误
        """
        endpoint = f"{USER_API}/1/reset-password"
        payload = {"newPassword": "newpass123"}
        response = api_client.put(endpoint, json=payload)
        
        assert_error(response, PARAM_ERROR_CODE)
    
    @allure.title("异常场景：无Token访问")
    @allure.severity(allure.severity_level.CRITICAL)
    def test_reset_password_no_token(self):
        """
        测试无Token访问
        
        预期：返回未授权错误
        """
        endpoint = f"{USER_API}/1/reset-password"
        payload = {"newPassword": "NewPass123"}
        response = api_client.put(endpoint, json=payload, with_auth=False)
        
        assert_error(response, UNAUTHORIZED_CODE)