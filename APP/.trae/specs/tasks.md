# 食库管家 —— 开发任务分解与优先级规划

## 任务总览

本项目按模块分层开发，遵循"基础先行、核心优先、扩展跟进"的原则。所有任务按依赖关系排序，高优先级任务优先完成。

---

## 第一阶段：基础架构搭建（P0）

### [ ] Task 1.1：项目初始化与技术栈配置
- **优先级**：P0
- **Depends On**：None
- **Description**：
  - 使用 UniApp 脚手架初始化项目
  - 配置 TypeScript 支持
  - 安装并配置 uView UI 组件库
  - 配置项目基础目录结构
- **Acceptance Criteria Addressed**：NF1, NF4, NF6
- **Test Requirements**：
  - `programmatic` TR-1.1.1：项目可正常构建，无编译错误
  - `programmatic` TR-1.1.2：H5 端可正常启动开发服务器
  - `human-judgement` TR-1.1.3：目录结构清晰，符合 UniApp 最佳实践

### [ ] Task 1.2：本地存储封装与数据模型定义
- **优先级**：P0
- **Depends On**：Task 1.1
- **Description**：
  - 封装 LocalStorage 操作工具类
  - 封装 IndexedDB 操作工具类（用于图片存储）
  - 定义食材库存、出库流水、用户偏好、系统配置等数据模型接口
  - 实现数据序列化/反序列化工具
- **Acceptance Criteria Addressed**：F7.2, NF3, NF7
- **Test Requirements**：
  - `programmatic` TR-1.2.1：数据可正常写入和读取 LocalStorage
  - `programmatic` TR-1.2.2：图片数据可正常存储和读取 IndexedDB
  - `programmatic` TR-1.2.3：数据模型类型检查通过

### [ ] Task 1.3：状态管理与全局数据结构设计
- **优先级**：P0
- **Depends On**：Task 1.2
- **Description**：
  - 配置 Pinia/Vuex 状态管理
  - 定义全局 store：库存数据、流水日志、系统配置
  - 实现数据变更订阅机制
  - 定义响应式数据更新策略
- **Acceptance Criteria Addressed**：F7.2, NF1, NF3
- **Test Requirements**：
  - `programmatic` TR-1.3.1：状态变更可触发组件重新渲染
  - `programmatic` TR-1.3.2：数据更新后自动持久化到本地存储
  - `human-judgement` TR-1.3.3：状态管理逻辑清晰，易于维护

### [ ] Task 1.4：系统配置与初始化逻辑
- **优先级**：P0
- **Depends On**：Task 1.2, Task 1.3
- **Description**：
  - 实现应用启动时的数据加载逻辑
  - 检测本地存储数据完整性
  - 实现数据损坏时的恢复模式
  - 初始化内置食材默认保质期数据库
- **Acceptance Criteria Addressed**：F1.2, F7.2, F7-E2
- **Test Requirements**：
  - `programmatic` TR-1.4.1：首次启动时自动初始化默认配置
  - `programmatic` TR-1.4.2：数据损坏时进入恢复模式
  - `programmatic` TR-1.4.3：内置保质期数据库可正常查询

---

## 第二阶段：核心功能开发（P0）

### [ ] Task 2.1：食材台账管理模块 - 入库功能
- **优先级**：P0
- **Depends On**：Task 1.4
- **Description**：
  - 实现入库表单页面：食材名称、分类、数量、单位、采购日期、保质期、存放分区
  - 实现必填字段校验（F1-E1）
  - 实现重复食材入库确认逻辑（F1-E2）
  - 实现保质期输入非法校验（F1-E3）
  - 实现默认保质期自动填充（F1.2）
- **Acceptance Criteria Addressed**：F1.1, F1.2, F1-E1, F1-E2, F1-E3
- **Test Requirements**：
  - `programmatic` TR-2.1.1：必填字段为空时阻止提交并标红
  - `programmatic` TR-2.1.2：重复食材入库时弹出合并/新建确认框
  - `programmatic` TR-2.1.3：采购日期晚于当前日期时拦截提交
  - `programmatic` TR-2.1.4：选择食材分类+名称后自动填充保质期

### [ ] Task 2.2：食材台账管理模块 - 库存清单展示
- **优先级**：P0
- **Depends On**：Task 2.1
- **Description**：
  - 实现库存清单页面，以列表/卡片形式展示食材
  - 实现按分类筛选功能
  - 实现按存放分区筛选功能
  - 实现按名称搜索功能
  - 实现按保质期剩余天数排序功能
- **Acceptance Criteria Addressed**：F1.4
- **Test Requirements**：
  - `programmatic` TR-2.2.1：筛选功能可正确过滤食材列表
  - `programmatic` TR-2.2.2：搜索功能可匹配食材名称
  - `programmatic` TR-2.2.3：排序功能可按剩余保质期天数升序/降序
  - `human-judgement` TR-2.2.4：列表展示清晰，信息完整

### [ ] Task 2.3：食材台账管理模块 - 首页统计与库存编辑
- **优先级**：P0/P1
- **Depends On**：Task 2.2
- **Description**：
  - 实现首页库存总量统计（F1.6）：总品类数、总件数、临期预警数
  - 实现食材信息编辑功能（F1.5）：修改存量、保质期、存放分区
  - 实现库存数据异常标记（F1-E5）
- **Acceptance Criteria Addressed**：F1.5, F1.6, F1-E5
- **Test Requirements**：
  - `programmatic` TR-2.3.1：首页统计数据实时更新
  - `programmatic` TR-2.3.2：编辑食材信息后库存表实时刷新
  - `programmatic` TR-2.3.3：存量为负数时自动标记异常状态

### [ ] Task 2.4：出库消耗管理模块 - 烹饪消耗出库
- **优先级**：P0
- **Depends On**：Task 2.2
- **Description**：
  - 实现批量勾选食材功能
  - 实现消耗数量输入与校验（F2-E1）
  - 实现库存不足拦截（F2.5）
  - 实现批量出库原子性操作（F2-E3）
  - 实现出库流水日志记录（F2.4）
- **Acceptance Criteria Addressed**：F2.1, F2.4, F2.5, F2-E1, F2-E2, F2-E3
- **Test Requirements**：
  - `programmatic` TR-2.4.1：消耗数量大于库存时拦截并提示
  - `programmatic` TR-2.4.2：未选择食材时提交按钮置灰
  - `programmatic` TR-2.4.3：批量出库时任一食材库存不足则整单回滚
  - `programmatic` TR-2.4.4：出库后自动生成流水记录

### [ ] Task 2.5：出库消耗管理模块 - 报废出库与清零
- **优先级**：P0/P1
- **Depends On**：Task 2.4
- **Description**：
  - 实现变质报废出库功能（F2.2）：选择报废原因（过期变质/储存不当/其他）
  - 实现存量清零出库功能（F2.3）：一键归零，二次确认
  - 实现出库误操作撤销功能（F2-E4）：10秒内可撤销
  - 实现报废食材已为零提示（F2-E5）
- **Acceptance Criteria Addressed**：F2.2, F2.3, F2-E4, F2-E5
- **Test Requirements**：
  - `programmatic` TR-2.5.1：报废出库后库存归零，流水记录生成
  - `programmatic` TR-2.5.2：存量清零需二次确认
  - `programmatic` TR-2.5.3：出库后10秒内显示撤销按钮，点击可回滚
  - `programmatic` TR-2.5.4：库存为0时报废操作提示无需报废

### [ ] Task 2.6：临期预警模块 - 保质期核算与三色标签
- **优先级**：P0
- **Depends On**：Task 2.2
- **Description**：
  - 实现保质期自动核算逻辑（F3.1）：剩余天数 = 截止日期 - 当前日期
  - 实现三色分级标签（F3.2）：绿色(>7天)、黄色(3~7天)、红色(≤2天)
  - 实现已过期食材标记（F3.5）：剩余<0置顶标红
  - 实现系统日期异常检测（F3-E1）
- **Acceptance Criteria Addressed**：F3.1, F3.2, F3.5, F3-E1, F3-E2
- **Test Requirements**：
  - `programmatic` TR-2.6.1：保质期核算准确，误差为0
  - `programmatic` TR-2.6.2：三色标签判定准确率100%
  - `programmatic` TR-2.6.3：已过期食材自动置顶标红
  - `programmatic` TR-2.6.4：系统日期异常时以最后巡检日期为基准

### [ ] Task 2.7：临期预警模块 - 推送提醒与汇总页
- **优先级**：P0
- **Depends On**：Task 2.6
- **Description**：
  - 实现临期推送提醒（F3.3）：黄色/红色食材进入系统时生成提醒卡片
  - 实现红色等级弹窗级高优先级提醒
  - 实现提醒频率控制（F3-E3）：同一食材每日最多弹窗1次
  - 实现临期食材汇总页（F3.4）：独立页面汇总黄+红色食材
- **Acceptance Criteria Addressed**：F3.3, F3.4, F3-E3
- **Test Requirements**：
  - `programmatic` TR-2.7.1：黄色/红色食材入库时生成首页提醒卡片
  - `programmatic` TR-2.7.2：红色食材触发弹窗级提醒
  - `programmatic` TR-2.7.3：同一食材红色提醒每日最多1次
  - `programmatic` TR-2.7.4：临期汇总页按剩余天数升序排列

### [ ] Task 2.8：系统管理模块 - 定时巡检与数据持久化
- **优先级**：P0
- **Depends On**：Task 2.6
- **Description**：
  - 实现每日08:00定时巡检逻辑（F7.1）：自动遍历食材保质期，更新预警等级
  - 实现数据本地持久化（F7.2）：LocalStorage/IndexedDB
  - 实现每日自动备份库存快照（NF3）
  - 实现浏览器存储空间不足处理（F7-E1）
- **Acceptance Criteria Addressed**：F7.1, F7.2, NF3, F7-E1
- **Test Requirements**：
  - `programmatic` TR-2.8.1：每日08:00自动执行保质期巡检
  - `programmatic` TR-2.8.2：刷新页面后数据不丢失
  - `programmatic` TR-2.8.3：每日自动生成库存备份快照
  - `programmatic` TR-2.8.4：LocalStorage超出配额时提示用户导出备份

---

## 第三阶段：AI 功能开发（P0/P1）

### [ ] Task 3.1：AI 菜谱推荐模块 - 库存读取与筛选
- **优先级**：P0
- **Depends On**：Task 2.2, Task 2.6
- **Description**：
  - 实现实时库存读取（F4.1）：自动读取当前在库食材清单
  - 实现可用食材筛选（F4.2）：剔除库存为0或已过期食材
  - 实现红色食材优先标记（F4.6）
- **Acceptance Criteria Addressed**：F4.1, F4.2, F4.6
- **Test Requirements**：
  - `programmatic` TR-3.1.1：可正确读取当前库存清单
  - `programmatic` TR-3.1.2：库存为0或已过期食材被正确剔除
  - `programmatic` TR-3.1.3：红色食材被正确标记为优先推荐

### [ ] Task 3.2：AI 菜谱推荐模块 - 接口调用与三档位生成
- **优先级**：P0
- **Depends On**：Task 3.1
- **Description**：
  - 实现 AI 接口调用封装
  - 实现三档位菜谱生成（F4.3）：快手懒人餐、减脂轻食餐、家常正餐
  - 实现 AI 生成超时处理（F4-E2）：15秒超时提示
  - 实现空库存/极少库存处理（F4-E1）
- **Acceptance Criteria Addressed**：F4.3, F4-E1, F4-E2, F4-E4
- **Test Requirements**：
  - `programmatic` TR-3.2.1：有效食材<3种时返回库存不足提示
  - `programmatic` TR-3.2.2：AI请求超过15秒显示超时提示
  - `human-judgement` TR-3.2.3：可生成三类不同档位的菜谱
  - `human-judgement` TR-3.2.4：无用户偏好时按通用口味生成

### [ ] Task 3.3：AI 菜谱推荐模块 - 缺料标注与详情展示
- **优先级**：P1
- **Depends On**：Task 3.2
- **Description**：
  - 实现缺料清单标注（F4.4）：标注所需缺失辅料
  - 实现菜谱详情展示（F4.5）：菜名、时长、难度、食材清单、热量、步骤
  - 实现菜谱与库存不匹配校验（F4-E3）
- **Acceptance Criteria Addressed**：F4.4, F4.5, F4-E3
- **Test Requirements**：
  - `human-judgement` TR-3.3.1：菜谱中缺失食材被正确标注
  - `human-judgement` TR-3.3.2：菜谱详情信息完整展示
  - `programmatic` TR-3.3.3：非在库食材必须在缺失辅料中标出

### [ ] Task 3.4：智能采购规划模块 - 饮食偏好录入
- **优先级**：P1
- **Depends On**：Task 1.4
- **Description**：
  - 实现饮食偏好录入页面（F5.1）：忌口、偏好菜系、辣度
  - 实现偏好修改功能
  - 实现偏好数据持久化
- **Acceptance Criteria Addressed**：F5.1, F5-E1
- **Test Requirements**：
  - `programmatic` TR-3.4.1：偏好数据可正常保存和读取
  - `programmatic` TR-3.4.2：未录入口味偏好时不阻断功能使用
  - `human-judgement` TR-3.4.3：偏好录入界面友好，操作便捷

### [ ] Task 3.5：智能采购规划模块 - 一周膳食规划
- **优先级**：P1
- **Depends On**：Task 3.4, Task 3.1
- **Description**：
  - 实现一周膳食规划生成（F5.2）：AI结合库存+偏好生成7天食谱
  - 实现采购清单去重（F5.3）：排除已充足库存食材
  - 实现库存变动提醒（F5-E3）
- **Acceptance Criteria Addressed**：F5.2, F5.3, F5-E3
- **Test Requirements**：
  - `human-judgement` TR-3.5.1：可生成7天每日三餐食谱
  - `programmatic` TR-3.5.2：采购清单自动排除已充足库存食材
  - `programmatic` TR-3.5.3：库存变动后提示重新生成规划

---

## 第四阶段：数据报表与扩展功能（P1/P2）

### [ ] Task 4.1：数据统计报表模块 - 月度采购支出统计
- **优先级**：P1
- **Depends On**：Task 2.1
- **Description**：
  - 实现月度采购支出统计（F6.1）：汇总当月入库食材采购金额
  - 实现金额缺失处理（F6-E1）
- **Acceptance Criteria Addressed**：F6.1, F6-E1
- **Test Requirements**：
  - `programmatic` TR-4.1.1：月度支出 = 各入库金额之和，误差为0
  - `programmatic` TR-4.1.2：未录入金额的条目不纳入统计
  - `human-judgement` TR-4.1.3：报表中标注"部分数据缺失"

### [ ] Task 4.2：数据统计报表模块 - 食材浪费分析
- **优先级**：P1
- **Depends On**：Task 2.5
- **Description**：
  - 实现食材浪费分析（F6.2）：统计当月报废数量、成本、占比
  - 实现饼图/柱状图展示（使用uCharts）
  - 实现空数据处理（F6-E2）
- **Acceptance Criteria Addressed**：F6.2, F6-E2
- **Test Requirements**：
  - `programmatic` TR-4.2.1：浪费金额 = 各报废出库金额之和，误差为0
  - `human-judgement` TR-4.2.2：图表展示清晰，数据准确
  - `human-judgement` TR-4.2.3：空数据时展示友好提示

### [ ] Task 4.3：数据统计报表模块 - 高频菜式与趋势对比
- **优先级**：P2
- **Depends On**：Task 4.2
- **Description**：
  - 实现高频菜式排行（F6.3）：当月高频使用食材组合Top5
  - 实现浪费趋势对比（F6.4）：近3个月浪费趋势折线图
- **Acceptance Criteria Addressed**：F6.3, F6.4
- **Test Requirements**：
  - `human-judgement` TR-4.3.1：高频菜式排行数据准确
  - `human-judgement` TR-4.3.2：趋势折线图展示清晰

### [ ] Task 4.4：智能采购规划模块 - 商超比价参考
- **优先级**：P2
- **Depends On**：Task 3.5
- **Description**：
  - 实现商超比价参考（F5.4）：展示主流商超市场参考均价
  - 实现性价比建议标注
  - 实现比价数据缺失处理（F5-E2）
- **Acceptance Criteria Addressed**：F5.4, F5-E2
- **Test Requirements**：
  - `human-judgement` TR-4.4.1：商超价格数据展示清晰
  - `programmatic` TR-4.4.2：无比价数据时显示"暂无数据"

### [ ] Task 4.5：系统管理模块 - 数据导出与重置
- **优先级**：P2
- **Depends On**：Task 2.2, Task 2.4
- **Description**：
  - 实现数据导出功能（F7.3）：支持CSV/JSON格式
  - 实现数据重置功能（F7.4）：二次确认+输入确认文字
- **Acceptance Criteria Addressed**：F7.3, F7.4, F7-E3
- **Test Requirements**：
  - `programmatic` TR-4.5.1：可导出库存表为CSV/JSON格式
  - `programmatic` TR-4.5.2：可导出流水日志为CSV/JSON格式
  - `programmatic` TR-4.5.3：重置数据需输入确认文字+二次确认

---

## 第五阶段：优化与测试（P0）

### [ ] Task 5.1：移动端适配与UI优化
- **优先级**：P0
- **Depends On**：所有页面开发完成
- **Description**：
  - 适配375px~430px宽度范围（iPhone SE ~ iPhone 14 Pro Max）
  - 确保核心按钮触控区域≥44×44px
  - 确保字体大小≥14px
  - 优化操作流程，确保核心任务操作路径≤3步
- **Acceptance Criteria Addressed**：NF2, NF4
- **Test Requirements**：
  - `human-judgement` TR-5.1.1：375px和430px宽度下无横向滚动条
  - `human-judgement` TR-5.1.2：核心按钮可单手指触控
  - `human-judgement` TR-5.1.3：字体清晰可读，≥14px

### [ ] Task 5.2：性能优化与离线可用性
- **优先级**：P0
- **Depends On**：所有功能开发完成
- **Description**：
  - 优化首屏加载时间≤2秒
  - 优化库存100条以内操作响应≤300ms
  - 确保核心功能（入库/出库/查询/预警）完全离线可用
  - AI功能离线时显示联网提示
- **Acceptance Criteria Addressed**：NF1, NF8
- **Test Requirements**：
  - `programmatic` TR-5.2.1：首屏加载时间≤2秒
  - `programmatic` TR-5.2.2：库存100条时操作响应≤300ms
  - `programmatic` TR-5.2.3：断开网络后核心功能正常运行

### [ ] Task 5.3：内置演示数据与最终测试
- **优先级**：P0
- **Depends On**：所有功能开发完成
- **Description**：
  - 内置15条以上演示数据，覆盖全部食材分类与三色预警状态
  - 执行完整功能测试：入库→库存查看→临期预警→AI菜谱→出库→报表
  - 修复发现的bug
  - 确保双击HTML文件即可独立运行
- **Acceptance Criteria Addressed**：所有核心功能验收标准
- **Test Requirements**：
  - `programmatic` TR-5.3.1：内置演示数据覆盖全部分类和预警状态
  - `human-judgement` TR-5.3.2：完整演示流程顺畅无卡顿
  - `programmatic` TR-5.3.3：双击HTML文件可直接打开全部功能

---

## 任务优先级汇总

| 优先级 | 任务数 | 任务列表 |
|:---:|:---:|---|
| P0 | 12 | Task 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.4, 2.6, 2.7, 2.8, 3.1, 3.2, 5.1, 5.2, 5.3 |
| P1 | 7 | Task 2.3, 2.5, 3.3, 3.4, 3.5, 4.1, 4.2 |
| P2 | 3 | Task 4.3, 4.4, 4.5 |

## 依赖关系图

```
Task 1.1 ──► Task 1.2 ──► Task 1.3 ──► Task 1.4
                         │              │
                         │              ▼
                         │         Task 2.1 ──► Task 2.2 ──► Task 2.3
                         │                       │
                         │                       ▼
                         │                  Task 2.4 ──► Task 2.5
                         │                       │
                         │                       ▼
                         │                  Task 2.6 ──► Task 2.7
                         │                       │
                         │                       ▼
                         │                  Task 2.8
                         │
                         ▼
                    Task 3.1 ──► Task 3.2 ──► Task 3.3
                         │
                         ▼
                    Task 3.4 ──► Task 3.5
                                   │
                                   ▼
                              Task 4.4
                          
Task 2.1 ──► Task 4.1
Task 2.5 ──► Task 4.2 ──► Task 4.3
Task 2.2 ──► Task 4.5

所有任务 ──► Task 5.1 ──► Task 5.2 ──► Task 5.3
```