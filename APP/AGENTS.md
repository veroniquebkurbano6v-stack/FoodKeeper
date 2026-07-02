# AGENTS.md - 食库管家开发规范

> 本文件为所有 AI 代码生成的强制约束，所有代码输出必须严格遵守以下规范。

---

## 一、技术栈约束

### 1.1 后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 核心语言（必须使用 LTS 版本） |
| Spring Boot | 3.2.x | 应用框架 |
| MyBatis-Plus | 3.5.x | ORM 框架（增强版 MyBatis） |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存/分布式锁/Session |
| Maven | 3.9.x | 项目构建管理 |

### 1.2 前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x | 前端框架（Composition API） |
| TypeScript | 5.x | 类型安全语言 |
| Element Plus | 2.x | UI 组件库 |
| Vite | 5.x | 构建工具 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP 客户端 |

### 1.3 强制约束
- **禁止**使用低于指定版本的依赖
- **禁止**引入未在本规范中列出的框架（需经评审批准）
- **禁止**混用 Spring Boot 2.x 与 3.x 的 API（如 `javax.*` → `jakarta.*`）
- **必须**使用 Lombok 简化 POJO（`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`）

---

## 二、代码分层规则

### 2.1 分层架构图
```
┌─────────────────────────────────────────────────┐
│                   Controller                    │  ← 接口层：接收请求、参数校验、返回响应
├─────────────────────────────────────────────────┤
│                    Service                      │  ← 业务层：业务逻辑、事务控制、领域模型
├─────────────────────────────────────────────────┤
│                     Mapper                      │  ← 数据层：SQL 执行、CRUD 操作
├─────────────────────────────────────────────────┤
│                  Database                       │  ← MySQL 8.0
└─────────────────────────────────────────────────┘
```

### 2.2 各层职责与禁止项

#### Controller 层
| 职责（必须） | 禁止项（严禁） |
|------------|---------------|
| 接收 HTTP 请求 | 编写业务逻辑 |
| 调用 Service 方法 | 直接调用 Mapper |
| 参数校验（`@Validated`） | 操作数据库连接 |
| 构建返回体 `Result<T>` | 捕获异常后吞掉 |
| 接口文档注解 `@ApiOperation` | 返回 `void` 或裸对象 |

**示例**：
```java
@RestController
@RequestMapping("/api/food")
@Validated
public class FoodController {
    
    @Autowired
    private FoodService foodService;
    
    @PostMapping("/add")
    @ApiOperation("添加食材")
    public Result<Long> addFood(@RequestBody @Validated FoodAddDTO dto) {
        Long foodId = foodService.addFood(dto);
        return Result.success(foodId);
    }
}
```

#### Service 层
| 职责（必须） | 禁止项（严禁） |
|------------|---------------|
| 实现业务逻辑 | 定义 HTTP 相关注解 |
| 事务控制 `@Transactional` | 直接拼接 SQL 字符串 |
| 调用 Mapper 或其他 Service | 返回 `Result` 包装体 |
| DTO → DO 转换 | 操作 Request/Response 对象 |
| 异常抛出（业务异常） | 硬编码错误信息 |

**示例**：
```java
@Service
public class FoodServiceImpl implements FoodService {
    
    @Autowired
    private FoodMapper foodMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFood(FoodAddDTO dto) {
        // DTO 转 DO
        FoodDO food = FoodConvertor.toDO(dto);
        
        // 校验分类是否存在
        CategoryDO category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        
        // 保存食材
        foodMapper.insert(food);
        return food.getId();
    }
}
```

#### Mapper 层
| 职责（必须） | 禁止项（严禁） |
|------------|---------------|
| 继承 `BaseMapper<T>` | 编写业务判断逻辑 |
| 自定义 SQL 方法（`@Select` 等） | 返回 DTO/VO 对象 |
| 分页查询 `IPage<T>` | 使用 `*` 查询所有字段 |
| 批量操作方法 | 拼接动态 SQL 不用 `#{}` |

**示例**：
```java
@Mapper
public interface FoodMapper extends BaseMapper<FoodDO> {
    
    @Select("SELECT id, name, category_id, quantity, unit FROM food WHERE category_id = #{categoryId} AND deleted = 0")
    List<FoodDO> selectByCategoryId(@Param("categoryId") Long categoryId);
    
    IPage<FoodDO> selectExpiringPage(IPage<FoodDO> page, @Param("days") Integer days);
}
```

#### DO (Data Object)
| 职责（必须） | 禁止项（严禁） |
|------------|---------------|
| 映射数据库表结构 | 包含业务计算字段 |
| 使用 `@TableName`, `@TableId` | 添加 Swagger 注解 |
| 字段与数据库列一一对应 | 使用复杂类型（如 `List`） |
| 实现序列化接口 | 包含前端展示逻辑 |

**示例**：
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("food")
public class FoodDO {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private Long categoryId;
    private Integer quantity;
    private String unit;
    private LocalDate purchaseDate;
    private LocalDate expiryDate;
    private String storageZone;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}
```

#### DTO (Data Transfer Object)
| 职责（必须） | 禁止项（严禁） |
|------------|---------------|
| 接收前端请求参数 | 包含数据库无关字段 |
| 参数校验注解 `@NotNull` 等 | 直接传递给 Mapper |
| Swagger 文档注解 | 包含敏感数据未脱敏 |
| 字段命名与前端一致 | 使用 DO 作为父类 |

**示例**：
```java
@Data
@ApiModel("食材添加请求")
public class FoodAddDTO {
    
    @NotBlank(message = "食材名称不能为空")
    @Length(max = 50, message = "食材名称最长50字符")
    @ApiModelProperty("食材名称")
    private String name;
    
    @NotNull(message = "分类ID不能为空")
    @ApiModelProperty("分类ID")
    private Long categoryId;
    
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    @ApiModelProperty("数量")
    private Integer quantity;
    
    @NotBlank(message = "单位不能为空")
    @ApiModelProperty("单位")
    private String unit;
    
    @NotNull(message = "采购日期不能为空")
    @ApiModelProperty("采购日期")
    private LocalDate purchaseDate;
    
    @NotNull(message = "保质期天数不能为空")
    @Min(value = 1, message = "保质期至少1天")
    @ApiModelProperty("保质期天数")
    private Integer expiryDays;
}
```

#### VO (View Object)
| 职责（必须） | 禁止项（严禁） |
|------------|---------------|
| 返回给前端的展示对象 | 包含敏感数据（密码等） |
| 敏感字段脱敏处理 | 直接从 DO 复制全部字段 |
| 计算字段（如剩余天数） | 包含数据库字段命名 |
| Swagger 文档注解 | 用于接收请求参数 |

**示例**：
```java
@Data
@ApiModel("食材详情响应")
public class FoodDetailVO {
    
    @ApiModelProperty("食材ID")
    private Long id;
    
    @ApiModelProperty("食材名称")
    private String name;
    
    @ApiModelProperty("分类名称")
    private String categoryName;
    
    @ApiModelProperty("数量")
    private Integer quantity;
    
    @ApiModelProperty("单位")
    private String unit;
    
    @ApiModelProperty("采购日期")
    private String purchaseDate;
    
    @ApiModelProperty("保质期截止")
    private String expiryDate;
    
    @ApiModelProperty("剩余天数")
    private Integer remainingDays;
    
    @ApiModelProperty("状态：GREEN/YELLOW/RED/EXPIRED")
    private String expiryStatus;
    
    @ApiModelProperty("存放分区")
    private String storageZone;
    
    @ApiModelProperty("总价")
    private String totalPrice;
}
```

### 2.3 类型转换规范
```java
// DTO → DO：使用 Convertor 工具类
@Component
public class FoodConvertor {
    
    public static FoodDO toDO(FoodAddDTO dto) {
        LocalDate expiryDate = dto.getPurchaseDate().plusDays(dto.getExpiryDays());
        return FoodDO.builder()
            .name(dto.getName())
            .categoryId(dto.getCategoryId())
            .quantity(dto.getQuantity())
            .unit(dto.getUnit())
            .purchaseDate(dto.getPurchaseDate())
            .expiryDate(expiryDate)
            .build();
    }
    
    public static FoodDetailVO toVO(FoodDO food, String categoryName) {
        int remainingDays = calculateRemainingDays(food.getExpiryDate());
        return FoodDetailVO.builder()
            .id(food.getId())
            .name(food.getName())
            .categoryName(categoryName)
            .quantity(food.getQuantity())
            .unit(food.getUnit())
            .purchaseDate(food.getPurchaseDate().toString())
            .expiryDate(food.getExpiryDate().toString())
            .remainingDays(remainingDays)
            .expiryStatus(getExpiryStatus(remainingDays))
            .build();
    }
}
```

---

## 三、安全约束

### 3.1 SQL 安全
| 约束 | 说明 |
|------|------|
| **强制参数化查询** | 所有 SQL 参数必须使用 `#{param}`，禁止 `${param}` |
| **禁止拼接 SQL** | 动态条件必须用 MyBatis-Plus `QueryWrapper` 或 XML `<if>` |
| **禁止 `SELECT *`** | 必须明确指定查询字段 |
| **批量操作上限** | 单次批量插入/更新不超过 500 条 |

**正确示例**：
```java
// MyBatis-Plus QueryWrapper
LambdaQueryWrapper<FoodDO> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(FoodDO::getCategoryId, categoryId)
       .ge(FoodDO::getQuantity, 10)
       .orderByAsc(FoodDO::getExpiryDate);

// XML 方式
@Select("<script>SELECT id, name FROM food WHERE category_id = #{categoryId} " +
        "<if test='name != null'> AND name LIKE CONCAT('%', #{name}, '%')</if></script>")
List<FoodDO> selectByCondition(@Param("categoryId") Long categoryId, @Param("name") String name);
```

**错误示例**：
```java
// ❌ 禁止：SQL 拼接
String sql = "SELECT * FROM food WHERE name = '" + name + "'";

// ❌ 禁止：使用 ${} 可能导致注入
@Select("SELECT * FROM food WHERE name = '${name}'")
```

### 3.2 接口鉴权
| 约束 | 说明 |
|------|------|
| **必须鉴权** | 所有业务接口必须校验 Token |
| **Token 存储** | 使用 Redis 存储 Session，设置过期时间 |
| **敏感操作** | 删除/重置操作必须二次校验密码 |
| **接口幂等** | 使用 Token 或 Redis 分布式锁防重复提交 |

**示例**：
```java
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            throw new UnauthorizedException("未登录");
        }
        
        // Redis 校验 Token
        String userId = redisTemplate.opsForValue().get("token:" + token);
        if (userId == null) {
            throw new UnauthorizedException("Token 已过期");
        }
        
        // 存入 ThreadLocal
        UserContext.setUserId(Long.parseLong(userId));
        return true;
    }
}
```

### 3.3 密钥管理
| 约束 | 说明 |
|------|------|
| **禁止硬编码** | 密钥、密码、Token 禁止写入代码 |
| **配置文件加密** | 使用 Jasypt 加密 `application.yml` |
| **环境变量** | 生产环境密钥通过环境变量注入 |
| **密钥轮换** | 敏感密钥定期更换（建议 90 天） |

**正确示例**：
```yaml
# application.yml
spring:
  datasource:
    password: ENC(加密后的密码)
    
jasypt:
  encryptor:
    password: ${JASYPT_PASSWORD:默认开发密钥}
```

**错误示例**：
```java
// ❌ 禁止：硬编码密钥
private static final String API_KEY = "sk-xxx123456";
private static final String DB_PASSWORD = "root123";
```

### 3.4 输入校验
| 约束 | 说明 |
|------|------|
| **必须校验** | 所有外部输入必须校验类型、长度、格式 |
| **白名单优先** | 使用白名单校验，禁止黑名单绕过 |
| **正则规范** | 正则表达式预编译，禁止运行时动态拼接 |
| **文件上传** | 校验文件类型、大小、内容（禁止执行权限） |

**示例**：
```java
@Data
public class UserRegisterDTO {
    
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名格式错误")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Length(min = 8, max = 32, message = "密码长度8-32位")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).*$", message = "密码必须包含大小写字母和数字")
    private String password;
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;
}
```

### 3.5 敏感字段脱敏
| 字段类型 | 脱敏规则 |
|---------|---------|
| 手机号 | 显示前3后4，中间 `****` → `138****1234` |
| 身份证 | 显示前6后4，中间 `********` → `320********1234` |
| 银行卡 | 显示后4位 → `**** **** **** 1234` |
| 密码 | 完全隐藏 → `******` |
| 邮箱 | 显示前3字符和域名 → `abc***@example.com` |

**示例**：
```java
public class DesensitizeUtil {
    
    public static String phone(String phone) {
        if (StringUtils.isBlank(phone) || phone.length() < 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    public static String idCard(String idCard) {
        if (StringUtils.isBlank(idCard) || idCard.length() < 18) return idCard;
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }
}
```

---

## 四、命名规范

### 4.1 类名命名
| 类型 | 规范 | 示例 |
|------|------|------|
| Controller | `XxxController` | `FoodController`, `UserController` |
| Service 接口 | `XxxService` | `FoodService` |
| Service 实现 | `XxxServiceImpl` | `FoodServiceImpl` |
| Mapper | `XxxMapper` | `FoodMapper` |
| DO | `XxxDO` | `FoodDO`, `UserDO` |
| DTO | `XxxDTO` | `FoodAddDTO`, `FoodUpdateDTO` |
| VO | `XxxVO` | `FoodDetailVO`, `FoodListVO` |
| Convertor | `XxxConvertor` | `FoodConvertor` |
| Enum | `XxxEnum` | `ExpiryStatusEnum`, `StorageZoneEnum` |
| Exception | `XxxException` | `BusinessException`, `UnauthorizedException` |
| Util | `XxxUtil` | `DesensitizeUtil`, `DateUtil` |
| Constant | `XxxConstants` | `FoodConstants`, `SystemConstants` |

### 4.2 方法名命名
| 操作类型 | 命名规范 | 示例 |
|---------|---------|------|
| 单条查询 | `getById`, `getByXxx` | `getFoodById`, `getFoodByName` |
| 列表查询 | `listByXxx`, `listAll` | `listFoodByCategory`, `listAllCategories` |
| 分页查询 | `pageXxx` | `pageFoodList`, `pageExpiringFood` |
| 新增 | `addXxx`, `saveXxx` | `addFood`, `saveFood` |
| 更新 | `updateXxx`, `modifyXxx` | `updateFood`, `modifyFoodQuantity` |
| 删除 | `deleteById`, `removeXxx` | `deleteFoodById`, `removeExpiredFood` |
| 批量操作 | `batchXxx` | `batchAddFood`, `batchDeleteFood` |
| 校验 | `checkXxx`, `validateXxx` | `checkFoodExists`, `validateCategory` |
| 计算 | `calculateXxx` | `calculateRemainingDays` |
| 转换 | `toXxx`, `convertToXxx` | `toDO`, `toVO`, `convertToDetail` |

### 4.3 变量命名
| 类型 | 规范 | 示例 |
|------|------|------|
| 局部变量 | 小驼峰，语义清晰 | `foodId`, `categoryName`, `remainingDays` |
| 常量 | 全大写+下划线 | `MAX_PAGE_SIZE`, `DEFAULT_EXPIRY_DAYS` |
| 集合变量 | 复数形式或加后缀 | `foodList`, `categoryMap`, `foodIds` |
| Boolean 变量 | `isXxx`, `hasXxx`, `canXxx` | `isValid`, `hasExpired`, `canDelete` |
| 临时变量 | 避免无意义命名 | 禁止 `a`, `b`, `temp`, `data` |

### 4.4 数据库字段命名
| 类型 | 规范 | 示例 |
|------|------|------|
| 主键 | `id` | `id BIGINT PRIMARY KEY AUTO_INCREMENT` |
| 外键 | `xxx_id` | `category_id`, `user_id` |
| 时间字段 | `xxx_at` 或 `xxx_time` | `created_at`, `updated_at`, `purchase_date` |
| 状态字段 | `xxx_status` 或 `status` | `expiry_status`, `order_status` |
| 数量字段 | `xxx_count` 或 `quantity` | `food_count`, `view_count` |
| 金额字段 | `xxx_amount` 或 `price` | `total_amount`, `unit_price` |
| 布尔字段 | `is_xxx` | `is_deleted`, `is_active` |
| 删除标记 | `deleted` | `deleted TINYINT DEFAULT 0` |

---

## 五、异常与日志规范

### 5.1 统一返回体
```java
@Data
@ApiModel("统一响应体")
public class Result<T> {
    
    @ApiModelProperty("状态码：200成功，其他失败")
    private Integer code;
    
    @ApiModelProperty("提示信息")
    private String message;
    
    @ApiModelProperty("业务数据")
    private T data;
    
    @ApiModelProperty("时间戳")
    private Long timestamp;
    
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
            .code(200)
            .message("操作成功")
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
            .code(200)
            .message(message)
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    public static <T> Result<T> fail(Integer code, String message) {
        return Result.<T>builder()
            .code(code)
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return Result.<T>builder()
            .code(errorCode.getCode())
            .message(errorCode.getMessage())
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
```

### 5.2 错误码定义
```java
@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // 系统级错误 1000-1999
    SYSTEM_ERROR(1000, "系统异常"),
    PARAM_ERROR(1001, "参数校验失败"),
    NOT_FOUND(1002, "资源不存在"),
    
    // 业务级错误 2000-2999
    BUSINESS_ERROR(2000, "业务处理失败"),
    FOOD_NOT_FOUND(2001, "食材不存在"),
    CATEGORY_NOT_FOUND(2002, "分类不存在"),
    QUANTITY_NOT_ENOUGH(2003, "库存数量不足"),
    
    // 权限级错误 3000-3999
    UNAUTHORIZED(3000, "未登录或Token过期"),
    FORBIDDEN(3001, "无操作权限"),
    TOKEN_EXPIRED(3002, "Token已过期");
    
    private final Integer code;
    private final String message;
}
```

### 5.3 全局异常处理
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }
    
    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }
    
    /**
     * 权限异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("权限异常: {}", e.getMessage());
        return Result.fail(ErrorCode.UNAUTHORIZED.getCode(), e.getMessage());
    }
    
    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), "系统异常，请稍后重试");
    }
}
```

### 5.4 自定义业务异常
```java
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
```

### 5.5 日志规范
| 级别 | 使用场景 | 格式要求 |
|------|---------|---------|
| ERROR | 系统异常、第三方调用失败 | 必须包含异常堆栈 `log.error("xxx", e)` |
| WARN | 业务异常、潜在问题 | 必须说明原因和影响范围 |
| INFO | 关键业务节点、状态变更 | 简明扼要，避免过多日志 |
| DEBUG | 调试信息（生产关闭） | 仅开发环境使用 |

**日志格式示例**：
```java
// ✓ 正确：包含关键参数和异常堆栈
log.error("添加食材失败，用户ID={}, 食材名称={}", userId, foodName, e);

// ✓ 正确：关键业务节点
log.info("食材出库成功，用户ID={}, 食材ID={}, 数量={}", userId, foodId, quantity);

// ✗ 错误：日志信息不足
log.error("添加失败");

// ✗ 错误：丢失异常堆栈
log.error("添加食材失败: " + e.getMessage());
```

### 5.6 日志输出规范
```java
@Service
@Slf4j
public class FoodServiceImpl implements FoodService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFood(FoodAddDTO dto) {
        log.debug("开始添加食材，参数: {}", dto);
        
        try {
            FoodDO food = FoodConvertor.toDO(dto);
            foodMapper.insert(food);
            
            log.info("食材添加成功，ID={}, 名称={}", food.getId(), food.getName());
            return food.getId();
            
        } catch (Exception e) {
            log.error("食材添加失败，参数={}, 异常: ", dto, e);
            throw new BusinessException("食材添加失败");
        }
    }
}
```

---

## 六、前端规范补充

### 6.1 TypeScript 类型定义
```typescript
// types/food.ts
export interface FoodAddDTO {
  name: string;
  categoryId: number;
  quantity: number;
  unit: string;
  purchaseDate: string;
  expiryDays: number;
}

export interface FoodDetailVO {
  id: number;
  name: string;
  categoryName: string;
  quantity: number;
  unit: string;
  purchaseDate: string;
  expiryDate: string;
  remainingDays: number;
  expiryStatus: 'GREEN' | 'YELLOW' | 'RED' | 'EXPIRED';
}

export interface Result<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}
```

### 6.2 API 封装规范
```typescript
// api/food.ts
import request from '@/utils/request';
import type { FoodAddDTO, FoodDetailVO, Result } from '@/types';

export function addFood(data: FoodAddDTO): Promise<Result<number>> {
  return request.post('/api/food/add', data);
}

export function getFoodDetail(id: number): Promise<Result<FoodDetailVO>> {
  return request.get(`/api/food/detail/${id}`);
}

export function deleteFood(id: number): Promise<Result<void>> {
  return request.delete(`/api/food/delete/${id}`);
}
```

### 6.3 组件命名规范
| 类型 | 规范 | 示例 |
|------|------|------|
| 页面组件 | `XxxView.vue` | `FoodListView.vue`, `FoodDetailView.vue` |
| 业务组件 | `XxxComponent.vue` | `FoodCardComponent.vue`, `ExpiryTagComponent.vue` |
| 基础组件 | `BaseXxx.vue` | `BaseButton.vue`, `BaseTable.vue` |
| 组合式函数 | `useXxx.ts` | `useFoodList.ts`, `useExpiryStatus.ts` |

---

## 七、数据库设计规范

### 7.1 表设计规范
| 规范项 | 要求 |
|--------|------|
| 主键 | `BIGINT AUTO_INCREMENT`，禁止 UUID |
| 字段类型 | 禁止 `ENUM`，使用 `TINYINT` + 注释说明 |
| 字段注释 | 所有字段必须有注释 |
| 时间字段 | 使用 `DATETIME` 或 `TIMESTAMP` |
| 金额字段 | 使用 `DECIMAL(18, 2)`，禁止 `FLOAT/DOUBLE` |
| 软删除 | 添加 `deleted TINYINT DEFAULT 0` 字段 |
| 创建时间 | 添加 `created_at DATETIME` |
| 更新时间 | 添加 `updated_at DATETIME` |

### 7.2 索引规范
| 索引类型 | 规范 | 示例 |
|---------|------|------|
| 主键索引 | 默认创建 | `PRIMARY KEY (id)` |
| 外键索引 | 必须创建 | `INDEX idx_category_id (category_id)` |
| 组合索引 | 最左匹配原则 | `INDEX idx_category_expiry (category_id, expiry_date)` |
| 唯一索引 | 业务唯一约束 | `UNIQUE INDEX uk_name (name)` |

### 7.3 示例建表语句
```sql
CREATE TABLE `food` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '食材名称',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `quantity` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
  `unit` VARCHAR(20) NOT NULL COMMENT '单位',
  `purchase_date` DATE NOT NULL COMMENT '采购日期',
  `expiry_date` DATE NOT NULL COMMENT '保质期截止',
  `storage_zone` VARCHAR(20) NOT NULL COMMENT '存放分区',
  `price` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '单价',
  `total_price` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '总价',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0正常 1临期 2过期',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除 1已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_category_id` (`category_id`),
  INDEX `idx_expiry_date` (`expiry_date`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食材表';
```

---

## 八、Git 提交规范

### 8.1 提交信息格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

### 8.2 Type 类型
| Type | 说明 | 示例 |
|------|------|------|
| feat | 新功能 | `feat(food): 添加食材入库功能` |
| fix | 修复 Bug | `fix(food): 修复保质期计算错误` |
| refactor | 重构代码 | `refactor(service): 重构食材服务逻辑` |
| docs | 文档更新 | `docs(api): 更新接口文档` |
| style | 代码格式 | `style(controller): 调整代码格式` |
| test | 测试代码 | `test(food): 添加食材入库单元测试` |
| chore | 构建/配置 | `chore(maven): 更新依赖版本` |

### 8.3 示例提交
```
feat(food): 添加食材入库功能

- 实现 FoodController.addFood 接口
- 实现 FoodServiceImpl.addFood 业务逻辑
- 添加 FoodAddDTO 参数校验
- 添加 FoodMapper.insert 方法

Closes #123
```

---

## 九、禁止项清单

### 9.1 代码禁止项
| 禁止项 | 说明 |
|--------|------|
| `System.out.println` | 生产代码禁止使用 |
| `e.printStackTrace()` | 禁止打印堆栈，使用日志 |
| 硬编码密钥/密码 | 安全风险 |
| 硬编码 IP/端口 | 配置外置 |
| `SELECT *` | 必须指定字段 |
| SQL 字符串拼接 | 注入风险 |
| 使用 `${}` 拼接 SQL | 注入风险 |
| Controller 返回 `void` | 必须返回 `Result<T>` |
| Service 返回 `Result` | Service 返回业务对象 |
| 事务方法调用同类方法 | 事务失效 |
| 循环调用数据库 | 批量操作 |
| 无限制分页 | 必须限制 `pageSize <= 100` |

### 9.2 设计禁止项
| 禁止项 | 说明 |
|--------|------|
| 单表字段超过 30 个 | 拆分表 |
| 索引数量超过 5 个 | 优化设计 |
| 单次批量超过 500 条 | 分批处理 |
| 递归无深度限制 | 必须限制 |
| 接口响应超过 3 秒 | 优化性能 |

---

## 十、强制检查项

AI 生成代码前必须确认：

1. ✅ 技术栈版本是否符合规范
2. ✅ 分层职责是否正确（Controller/Service/Mapper）
3. ✅ DO/DTO/VO 是否分离
4. ✅ 是否使用参数化查询
5. ✅ 是否添加参数校验注解
6. ✅ 是否有敏感字段脱敏
7. ✅ 是否使用统一返回体 `Result<T>`
8. ✅ 是否有异常处理和日志
9. ✅ 命名是否符合规范
10. ✅ 是否遵守禁止项清单

---

> **重要声明**：本规范为强制约束，所有 AI 生成的代码必须严格遵守。如有特殊情况需偏离规范，必须在代码注释中明确说明原因并获得人工评审批准。