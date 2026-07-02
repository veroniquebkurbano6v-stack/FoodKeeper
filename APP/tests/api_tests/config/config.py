"""
测试环境配置文件

包含测试环境的基础URL、测试账号、超时配置等
"""

# ==================== 环境配置 ====================

# 测试环境基础URL
BASE_URL = "http://localhost:8080"

# API路径前缀
API_PREFIX = {
    "user": "/system/user",
    "auth": "/api/auth",
    "food": "/api/food",
    "category": "/api/category"
}

# ==================== 测试账号配置 ====================

# 管理员账号（用于需要管理员权限的接口）
ADMIN_ACCOUNT = {
    "username": "admin",
    "password": "Admin123456"
}

# 普通用户账号（用于权限不足场景测试）
NORMAL_ACCOUNT = {
    "username": "testuser",
    "password": "Test123456"
}

# 超级管理员ID（用于越权测试）
SUPER_ADMIN_ID = 1

# ==================== 请求配置 ====================

# 请求超时时间（秒）
REQUEST_TIMEOUT = 30

# Token刷新阈值（剩余有效时间少于该值时自动刷新，秒）
TOKEN_REFRESH_THRESHOLD = 300

# ==================== 断言配置 ====================

# 成功响应码
SUCCESS_CODE = 200

# 参数错误码
PARAM_ERROR_CODE = 1001

# 用户不存在错误码
USER_NOT_FOUND_CODE = 2100

# 用户名已存在错误码
USERNAME_EXISTS_CODE = 2101

# 手机号已存在错误码
PHONE_EXISTS_CODE = 2102

# 不能删除超级管理员错误码
CANNOT_DELETE_ADMIN_CODE = 2106

# 不能禁用超级管理员错误码
CANNOT_DISABLE_ADMIN_CODE = 2105

# 未授权错误码
UNAUTHORIZED_CODE = 3000

# 无权限错误码
FORBIDDEN_CODE = 3001

# Token过期错误码
TOKEN_EXPIRED_CODE = 3002

# ==================== 测试数据配置 ====================

# 测试用户前缀（避免与真实数据冲突）
TEST_USER_PREFIX = "test_auto_"

# 测试手机号前缀
TEST_PHONE_PREFIX = "1390000"

# 默认分页参数
DEFAULT_PAGE_NUM = 1
DEFAULT_PAGE_SIZE = 10
MAX_PAGE_SIZE = 100