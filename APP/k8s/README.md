# ============================================
# 食库管家 K8s 部署清单说明
# ============================================

## 文件结构
```
k8s/
├── namespace.yml            # 命名空间配置
├── configmap.yml            # 非敏感配置（数据库地址、端口等）
├── secret.yml               # 敏感配置（密码、密钥等）- base64编码
├── pvc.yml                  # 持久化存储声明（日志、数据）
├── deployment.yml           # 应用部署配置（核心）
├── service.yml              # ClusterIP 服务（内部访问）
├── service-nodeport.yml     # NodePort 服务（外部访问）
├── ingress.yml              # Ingress 配置（域名访问）
└── hpa.yml                  # 水平自动伸缩配置
```

## 部署顺序
```bash
# 1. 创建命名空间
kubectl apply -f namespace.yml

# 2. 创建配置和密钥
kubectl apply -f configmap.yml
kubectl apply -f secret.yml

# 3. 创建持久化存储
kubectl apply -f pvc.yml

# 4. 创建服务
kubectl apply -f service.yml
kubectl apply -f service-nodeport.yml

# 5. 创建部署
kubectl apply -f deployment.yml

# 6. 创建 Ingress（需要配置域名和TLS证书）
kubectl apply -f ingress.yml

# 7. 创建 HPA（可选）
kubectl apply -f hpa.yml
```

## 验证部署
```bash
# 查看命名空间
kubectl get namespaces

# 查看部署状态
kubectl -n food-inventory get deployments

# 查看 Pod 状态
kubectl -n food-inventory get pods

# 查看服务状态
kubectl -n food-inventory get services

# 查看 Ingress 状态
kubectl -n food-inventory get ingress

# 查看 HPA 状态
kubectl -n food-inventory get hpa

# 查看 Pod 日志
kubectl -n food-inventory logs -f <pod-name>

# 执行健康检查
curl http://localhost:30080/actuator/health
```

## 配置说明

### ConfigMap（非敏感配置）
- 数据库地址、端口、库名
- Redis 地址、端口、数据库编号
- JVM 参数
- 日志级别配置
- 连接池配置

### Secret（敏感配置）
- MySQL 用户名/密码
- Redis 密码
- Jasypt 加密密钥
- 所有值均为 base64 编码

### Deployment 核心配置
- **副本数**: 3（高可用）
- **资源限制**: CPU 200m-500m, Memory 256Mi-512Mi
- **滚动更新**: maxSurge=1, maxUnavailable=0（零停机）
- **探针配置**:
  - Startup Probe: 最多等待120秒启动
  - Liveness Probe: 每30秒检查存活状态
  - Readiness Probe: 每10秒检查就绪状态
- **优雅停机**: 30秒等待时间
- **安全上下文**: 非 root 用户运行

### HPA 自动伸缩
- 最小副本: 3
- 最大副本: 10
- CPU 阈值: 70%
- Memory 阈值: 80%
- 扩容策略: 最多50%或2个Pod/分钟
- 缩容策略: 最多30%或1个Pod/分钟，等待10分钟稳定

## 生产环境注意事项

### 1. 镜像仓库
- 需要配置私有镜像仓库
- 创建 imagePullSecret: `kubectl create secret docker-registry regcred --docker-server=registry.example.com --docker-username=user --docker-password=pass`

### 2. TLS 证书
- 需要配置 SSL 证书
- 创建 TLS Secret: `kubectl create secret tls food-inventory-tls --cert=fullchain.pem --key=privkey.pem`

### 3. 敏感信息管理
- 生产环境建议使用 HashiCorp Vault 或 AWS Secrets Manager
- 不要将 Secret 文件提交到版本控制
- 使用外部 Secrets 管理系统自动注入

### 4. 存储类
- 需要配置 NFS 或其他分布式存储类
- 根据实际环境修改 storageClassName

### 5. 监控告警
- 建议部署 Prometheus + Grafana
- 配置 CPU、内存、磁盘、网络告警
- 配置应用健康检查告警

### 6. 日志收集
- 建议部署 ELK 或 Loki
- 配置日志轮转和清理策略
- 配置日志级别管理

### 7. 备份策略
- 数据库定时备份
- 配置文件版本管理
- 定期数据恢复测试

## 故障排查

### 常见问题
1. **Pod 启动失败**: 检查镜像拉取、配置注入、存储挂载
2. **服务无法访问**: 检查 Service、Ingress、防火墙规则
3. **健康检查失败**: 检查应用端口、Actuator 配置
4. **数据库连接失败**: 检查网络策略、数据库认证

### 调试命令
```bash
# 查看 Pod 详细信息
kubectl -n food-inventory describe pod <pod-name>

# 进入 Pod 调试
kubectl -n food-inventory exec -it <pod-name> -- /bin/sh

# 查看事件
kubectl -n food-inventory get events --sort-by='.lastTimestamp'

# 端口转发测试
kubectl -n food-inventory port-forward <pod-name> 8080:8080
```