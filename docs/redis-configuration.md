# Redis 配置与集群扩展说明

本项目默认使用单机 Redis，同时已经预留 Redis Cluster 配置。

## 一、默认单机 Redis

默认配置文件：

- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-demo.properties`

核心配置：

```properties
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.database=${REDIS_DATABASE:0}
spring.data.redis.timeout=2s
spring.data.redis.repositories.enabled=false
```

默认连接：

```text
localhost:6379
```

本地开发或普通演示时，只需要启动一个 Redis 实例即可。

## 二、Redis Cluster 配置

新增配置文件：

```text
src/main/resources/application-redis-cluster.properties
```

内容：

```properties
spring.data.redis.cluster.nodes=${REDIS_CLUSTER_NODES:localhost:7000,localhost:7001,localhost:7002}
spring.data.redis.cluster.max-redirects=${REDIS_CLUSTER_MAX_REDIRECTS:3}
spring.data.redis.password=${REDIS_PASSWORD:}
spring.data.redis.timeout=${REDIS_TIMEOUT:2s}
spring.data.redis.repositories.enabled=false
```

启用方式：

```text
--spring.profiles.active=demo,redis-cluster
```

如果使用 IDEA：

```text
Active profiles: demo,redis-cluster
```

如果使用命令行：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo,redis-cluster"
```

如果 Redis 集群节点不是本机，需要设置环境变量：

```powershell
$env:REDIS_CLUSTER_NODES="192.168.1.10:7000,192.168.1.10:7001,192.168.1.10:7002"
$env:REDIS_CLUSTER_MAX_REDIRECTS="3"
$env:REDIS_PASSWORD=""
```

## 三、项目中 Redis 的实际使用点

### 1. 登录失败次数限制

代码：

```text
src/main/java/weidonglang/tianshiwebside/auth/AuthService.java
```

Redis Key：

```text
auth:failures:{username}
```

用途：

- 密码错误时计数。
- 超过阈值后临时锁定。
- 设置 TTL，过期后自动恢复。

### 2. 登录 token 存储

代码：

```text
src/main/java/weidonglang/tianshiwebside/auth/AuthTokenStore.java
```

Redis Key：

```text
auth:access:{token}
auth:refresh:{token}
```

用途：

- 保存 token 到用户名的映射。
- 根据 token 找到当前登录用户。
- 设置 access token 和 refresh token 过期时间。

### 3. 抢课库存、短锁、幂等

代码：

```text
src/main/java/weidonglang/tianshiwebside/course/grab/LocalCourseGrabService.java
```

Redis Key：

```text
selection:offering:{offeringId}:remaining
selection:grab:lock:{offeringId}:{username}
selection:request:{requestId}
```

用途：

- `remaining`：缓存课程剩余容量。
- `lock`：同一学生同一课程短期处理锁，防止连续点击。
- `request`：请求幂等，防止重复提交。

### 4. 后台维护后清理缓存

代码：

```text
src/main/java/weidonglang/tianshiwebside/course/AdminCourseController.java
```

Redis Key：

```text
selection:offering:{offeringId}:remaining
```

用途：

- 管理员修改教学班容量后，删除旧库存缓存。
- 下一次抢课时重新按数据库已选人数计算剩余容量。

## 四、单机和集群的区别

默认单机 Redis：

- 配置简单。
- 适合本地开发、课程设计、演示部署。
- 使用 `spring.data.redis.host` 和 `spring.data.redis.port`。

Redis Cluster：

- 多节点分片存储。
- 可承载更高访问量。
- 适合真实高并发抢课场景。
- 使用 `spring.data.redis.cluster.nodes`。

当前项目的业务代码使用的是 Spring 提供的 `StringRedisTemplate`，不直接关心底层是单机还是集群。切换到集群时，只需要切换 Spring Boot Redis 连接配置，业务代码基本不用修改。

## 五、答辩表述建议

可以这样说明：

> 项目默认使用单机 Redis，满足本地开发和教学演示需求。Redis 主要用于登录 token、登录失败次数限制、选课抢课库存、短锁和请求幂等。为了预留扩展能力，我们新增了 `redis-cluster` profile，可以通过 `spring.data.redis.cluster.nodes` 切换到 Redis Cluster。由于业务代码统一使用 `StringRedisTemplate`，所以单机和集群的切换主要发生在配置层，业务层无需大规模改动。
