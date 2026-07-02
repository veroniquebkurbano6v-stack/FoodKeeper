"""
公共请求封装模块

提供统一的HTTP请求封装，包含：
- 鉴权Token自动获取与刷新
- 统一请求头管理
- 响应处理与断言工具
"""

import time
import threading
import requests
from typing import Optional, Dict, Any, Union
from config.config import (
    BASE_URL, API_PREFIX, REQUEST_TIMEOUT,
    TOKEN_REFRESH_THRESHOLD, ADMIN_ACCOUNT,
    SUCCESS_CODE, UNAUTHORIZED_CODE, TOKEN_EXPIRED_CODE
)


class AuthManager:
    """
    鉴权管理器
    
    负责 Token 的获取、存储、自动刷新
    """
    
    _instance = None
    _lock = threading.Lock()
    
    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        if not hasattr(self, '_initialized'):
            self._token: Optional[str] = None
            self._token_expire_time: float = 0
            self._last_refresh_time: float = 0
            self._initialized = True
    
    def get_token(self) -> str:
        """
        获取有效Token，自动刷新过期Token
        
        Returns:
            str: 有效Token
        """
        # 开发模式：直接返回测试Token
        # 生产模式：需要实现真实的登录获取Token逻辑
        if self._token is None or self._need_refresh():
            self._refresh_token()
        return self._token
    
    def _need_refresh(self) -> bool:
        """
        判断是否需要刷新Token
        
        Returns:
            bool: 是否需要刷新
        """
        current_time = time.time()
        remaining_time = self._token_expire_time - current_time
        return remaining_time < TOKEN_REFRESH_THRESHOLD
    
    def _refresh_token(self) -> None:
        """
        刷新Token（从登录接口获取）
        
        实际项目中应调用登录接口获取真实Token
        开发模式下使用固定测试Token
        """
        # 开发模式：使用固定Token（后端开发模式允许任意Token）
        self._token = "test-token-for-api-test"
        self._token_expire_time = time.time() + 3600  # 1小时有效期
        self._last_refresh_time = time.time()
    
    def login_and_get_token(self, username: str, password: str) -> str:
        """
        通过登录接口获取Token
        
        Args:
            username: 用户名
            password: 密码
            
        Returns:
            str: Token
            
        Raises:
            Exception: 登录失败时抛出异常
        """
        login_url = f"{BASE_URL}{API_PREFIX['auth']}/login"
        payload = {
            "username": username,
            "password": password
        }
        
        response = requests.post(
            login_url,
            json=payload,
            timeout=REQUEST_TIMEOUT
        )
        
        if response.status_code == 200:
            result = response.json()
            if result.get("code") == SUCCESS_CODE:
                # 从响应中提取Token（根据实际接口结构调整）
                data = result.get("data", {})
                self._token = data.get("token", "")
                self._token_expire_time = time.time() + data.get("expireTime", 3600)
                self._last_refresh_time = time.time()
                return self._token
        
        raise Exception(f"登录失败: {response.text}")
    
    def clear_token(self) -> None:
        """
        清除Token（用于测试无Token场景）
        """
        self._token = None
        self._token_expire_time = 0
    
    def set_token(self, token: str, expire_seconds: int = 3600) -> None:
        """
        手动设置Token（用于测试特定Token场景）
        
        Args:
            token: Token字符串
            expire_seconds: 有效期（秒）
        """
        self._token = token
        self._token_expire_time = time.time() + expire_seconds


# 全局鉴权管理器实例
auth_manager = AuthManager()


class APIClient:
    """
    API请求客户端
    
    提供统一的HTTP请求封装，自动处理鉴权和错误响应
    """
    
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.session = requests.Session()
        self._setup_session()
    
    def _setup_session(self) -> None:
        """
        配置Session默认参数
        """
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json"
        })
    
    def _build_headers(self, with_auth: bool = True, custom_headers: Optional[Dict] = None) -> Dict:
        """
        构建请求头
        
        Args:
            with_auth: 是否携带鉴权Token
            custom_headers: 自定义请求头
            
        Returns:
            Dict: 完整请求头
        """
        headers = {}
        
        if with_auth:
            token = auth_manager.get_token()
            headers["Authorization"] = token
        
        if custom_headers:
            headers.update(custom_headers)
        
        return headers
    
    def request(
        self,
        method: str,
        endpoint: str,
        with_auth: bool = True,
        **kwargs
    ) -> requests.Response:
        """
        发送HTTP请求
        
        Args:
            method: 请求方法 GET/POST/PUT/DELETE
            endpoint: API路径
            with_auth: 是否携带鉴权Token
            **kwargs: 其他requests参数
            
        Returns:
            Response: HTTP响应对象
        """
        url = f"{self.base_url}{endpoint}"
        headers = self._build_headers(with_auth, kwargs.pop("headers", None))
        
        # 设置超时
        if "timeout" not in kwargs:
            kwargs["timeout"] = REQUEST_TIMEOUT
        
        response = self.session.request(
            method=method,
            url=url,
            headers=headers,
            **kwargs
        )
        
        return response
    
    def get(self, endpoint: str, with_auth: bool = True, **kwargs) -> requests.Response:
        """
        发送GET请求
        
        Args:
            endpoint: API路径
            with_auth: 是否携带鉴权Token
            **kwargs: 其他参数
            
        Returns:
            Response: HTTP响应对象
        """
        return self.request("GET", endpoint, with_auth, **kwargs)
    
    def post(self, endpoint: str, with_auth: bool = True, **kwargs) -> requests.Response:
        """
        发送POST请求
        
        Args:
            endpoint: API路径
            with_auth: 是否携带鉴权Token
            **kwargs: 其他参数
            
        Returns:
            Response: HTTP响应对象
        """
        return self.request("POST", endpoint, with_auth, **kwargs)
    
    def put(self, endpoint: str, with_auth: bool = True, **kwargs) -> requests.Response:
        """
        发送PUT请求
        
        Args:
            endpoint: API路径
            with_auth: 是否携带鉴权Token
            **kwargs: 其他参数
            
        Returns:
            Response: HTTP响应对象
        """
        return self.request("PUT", endpoint, with_auth, **kwargs)
    
    def delete(self, endpoint: str, with_auth: bool = True, **kwargs) -> requests.Response:
        """
        发送DELETE请求
        
        Args:
            endpoint: API路径
            with_auth: 是否携带鉴权Token
            **kwargs: 其他参数
            
        Returns:
            Response: HTTP响应对象
        """
        return self.request("DELETE", endpoint, with_auth, **kwargs)


# ==================== 断言工具函数 ====================

def assert_success(response: requests.Response, message: str = "操作成功") -> Dict:
    """
    断言响应成功
    
    Args:
        response: HTTP响应对象
        message: 期望的成功消息
        
    Returns:
        Dict: 响应数据
        
    Raises:
        AssertionError: 断言失败时抛出
    """
    assert response.status_code == 200, f"HTTP状态码错误: {response.status_code}"
    
    result = response.json()
    assert result.get("code") == SUCCESS_CODE, f"业务码错误: {result.get('code')}, 消息: {result.get('message')}"
    
    if message:
        assert message in result.get("message", ""), f"消息不匹配: {result.get('message')}"
    
    return result


def assert_error(response: requests.Response, error_code: int, message_contains: Optional[str] = None) -> Dict:
    """
    断言响应错误（预期错误场景）
    
    Args:
        response: HTTP响应对象
        error_code: 期望的错误码
        message_contains: 期望包含的消息内容
        
    Returns:
        Dict: 响应数据
        
    Raises:
        AssertionError: 断言失败时抛出
    """
    result = response.json()
    assert result.get("code") == error_code, f"错误码不匹配: 期望 {error_code}, 实际 {result.get('code')}"
    
    if message_contains:
        actual_message = result.get("message", "")
        assert message_contains in actual_message, f"消息不包含'{message_contains}': {actual_message}"
    
    return result


def assert_unauthorized(response: requests.Response) -> Dict:
    """
    断言未授权响应
    
    Args:
        response: HTTP响应对象
        
    Returns:
        Dict: 响应数据
    """
    return assert_error(response, UNAUTHORIZED_CODE, "未登录")


def assert_forbidden(response: requests.Response) -> Dict:
    """
    断言无权限响应
    
    Args:
        response: HTTP响应对象
        
    Returns:
        Dict: 响应数据
    """
    return assert_error(response, UNAUTHORIZED_CODE, "无操作权限")


def assert_field_exists(data: Dict, field: str) -> None:
    """
    断言字段存在
    
    Args:
        data: 数据字典
        field: 字段名
    """
    assert field in data, f"字段 '{field}' 不存在"


def assert_field_value(data: Dict, field: str, expected_value: Any) -> None:
    """
    断言字段值
    
    Args:
        data: 数据字典
        field: 字段名
        expected_value: 期望值
    """
    assert_field_exists(data, field)
    assert data[field] == expected_value, f"字段 '{field}' 值不匹配: 期望 {expected_value}, 实际 {data[field]}"


def assert_field_desensitized(data: Dict, field: str, pattern: str = "****") -> None:
    """
    断言字段已脱敏
    
    Args:
        data: 数据字典
        field: 字段名
        pattern: 脱敏标识（如****）
    """
    assert_field_exists(data, field)
    assert pattern in data[field], f"字段 '{field}' 未脱敏: {data[field]}"


def assert_pagination_valid(data: Dict) -> None:
    """
    断言分页数据有效
    
    Args:
        data: 分页数据
    """
    assert_field_exists(data, "records")
    assert_field_exists(data, "total")
    assert_field_exists(data, "current")
    assert_field_exists(data, "size")
    assert isinstance(data["records"], list), "records 应为列表"
    assert data["total"] >= 0, "total 应大于等于0"
    assert data["size"] > 0, "size 应大于0"


# ==================== 测试数据生成工具 ====================

import uuid
import random


def generate_test_username(prefix: str = "test_auto_") -> str:
    """
    生成测试用户名
    
    Args:
        prefix: 用户名前缀
        
    Returns:
        str: 测试用户名
    """
    return f"{prefix}{uuid.uuid4().hex[:8]}"


def generate_test_phone() -> str:
    """
    生成测试手机号
    
    Returns:
        str: 测试手机号
    """
    prefix = random.choice(["139", "138", "137", "136", "135", "134", "159", "158"])
    suffix = "".join([str(random.randint(0, 9)) for _ in range(8)])
    return f"{prefix}{suffix}"


def generate_test_email(username: str) -> str:
    """
    生成测试邮箱
    
    Args:
        username: 用户名
        
    Returns:
        str: 测试邮箱
    """
    return f"{username}@test.com"


def generate_valid_password() -> str:
    """
    生成符合规则的测试密码
    
    Returns:
        str: 测试密码（包含大小写字母和数字）
    """
    return "TestPass123"


def generate_invalid_password_short() -> str:
    """
    生成过短的密码
    
    Returns:
        str: 过短密码
    """
    return "Test12"


def generate_invalid_password_no_upper() -> str:
    """
    生成不含大写字母的密码
    
    Returns:
        str: 不含大写字母的密码
    """
    return "testpass123"


def generate_invalid_password_no_lower() -> str:
    """
    生成不含小写字母的密码
    
    Returns:
        str: 不含小写字母的密码
    """
    return "TESTPASS123"


def generate_invalid_password_no_digit() -> str:
    """
    生成不含数字的密码
    
    Returns:
        str: 不含数字的密码
    """
    return "TestPassword"