# 天津天狮学院教学综合信息服务平台复刻项目

> 本项目是课程设计/毕业设计场景下的教学综合信息服务平台复刻项目，仅用于学习、演示和课程答辩，不代表天津天狮学院官方系统，也不接入真实教务数据。

## 项目简介

本项目围绕高校教务管理系统的核心流程进行复刻，目标不是堆叠复杂框架，而是使用课程中容易讲清楚、维护得住的技术完成一个完整的前后端分离系统。

系统覆盖学生端、教师端、教务管理端、Redis 抢课高并发、压测报告、数据库只读浏览、Redis 状态监控、代码分析演示页等模块。项目强调“普通学生能理解、能讲清楚、能维护”，适合课程答辩、项目演示和基础工程实践学习。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Spring Boot 4、Spring MVC、Spring Security |
| 持久层 | MyBatis、JPA、JdbcTemplate |
| 数据库 | MySQL、Flyway |
| 缓存 | Redis |
| 前端 | Vue 3、Vite、Element Plus、Pinia、Vue Router |
| 图表 | ECharts |
| 压测 | Node.js、Python Tkinter |
| 演示页 | HTML、CSS、JavaScript |

## 核心功能

### 学生端

- 登录认证与个人首页
- 首页公告、通知、未读状态
- 个人课表、班级课表、空闲教室查询
- 学生个人信息、学籍信息、联系方式
- 学籍异动申请、微专业报名、重修报名
- 校内/校外课程学分节点替代申请
- 成绩加分申请、分流专业确认、专业方向确认
- 自主选课、抢课、已选课程查询
- 成绩查询、考试安排查询、论文成绩查看
- 教学评价、教学信息反馈
- 学业预警、毕业审核结果核查
- 教学执行计划、选课名单、学生学业情况查询

### 教师端

- 教师登录与教师工作台
- 任课教学班查看
- 成绩录入、成绩修改
- 考试安排维护
- 教学评价结果查看

### 管理端

- 教务管理菜单
- 用户管理、角色权限管理、菜单权限管理
- 课程与教学班维护
- 成绩管理、成绩发布、成绩锁定
- 补考、重修、缓考状态维护
- 考试安排、考场、座位、监考信息维护
- 通知公告管理、首页公告、选课通知、考试通知、审核通知
- 学籍异动审核
- 教学评价统计
- 操作日志与审计
- 文件导入导出基础功能
- 数据库只读浏览与可视化分析
- Redis 状态监控与缓存展示
- 压测历史报告列表

## Redis 设计

项目中的 Redis 不是只做“概念展示”，而是围绕抢课高并发、幂等控制和运行监控设计了实际 key。

| 负责人 | Redis 模块 | Redis Key | 说明 |
| --- | --- | --- | --- |
| 魏语石 | 抢课库存与并发扣减 | `selection:offering:{offeringId}:remaining` | 缓存教学班剩余名额，抢课时先扣 Redis 库存，降低数据库压力并防止超卖 |
| 郭凤圣 | 请求幂等与重复提交控制 | `selection:request:{requestId}` | 防止同一次抢课请求重复提交，避免重复写入和重复扣减 |
| 敖东磊 | 短锁与运行状态检测 | `selection:grab:lock:{offeringId}:{username}` | 限制同一学生短时间内重复抢同一门课，并负责 Redis 连通性检测、缓存清理说明 |

Redis 作为可选加速层：

- Redis 正常：使用 Redis 做库存缓存、幂等请求、短锁。
- Redis 不可用：自动退回数据库兜底，系统功能不崩溃。
- Redis 连续失败：使用熔断策略减少反复等待。

## 数据库与可视化

项目提供数据库只读浏览模块，适合答辩时展示数据库设计：

- 数据库连接信息展示
- 表结构、字段、主键、外键、索引查看
- 数据分页预览
- CSV 导出
- ER 关系图
- 表数据量柱状图
- 字段类型分布饼图
- 数据库操作历史趋势
- SQL/数据库操作成功率图
- 查询结果一键生成图表

为了安全，数据库管理模块默认只读，不提供任意 SQL 执行、建表、删表、修改数据等高风险能力。

## 压测能力

项目提供万人抢课压测脚本和 Python 可视化面板。

核心脚本：

```bash
scripts/course-grab-load-test.js
scripts/course_grab_panel.py
```

常用压测参数：

```powershell
$env:LOAD_USERS="10000"
$env:REQUESTS="10000"
$env:CONCURRENCY="500"
$env:LOGIN_CONCURRENCY="200"
$env:ACCOUNT_BATCH_SIZE="500"
$env:OFFERING_IDS="1,2,3,4"
$env:SMART_MODE="random"
node .\scripts\course-grab-load-test.js
```

压测报告会自动生成到：

```text
reports/
```

报告包括：

- 压测配置
- Redis 是否连通
- 请求总数
- 成功数、满员数、失败数
- 吞吐量
- 平均响应时间
- P50、P95、P99 延迟
- 阶段耗时
- 课程随机分布
- 失败样例
- 自动清理记录

## 运行环境

建议环境：

- JDK 17
- Node.js 20+
- MySQL 8
- Redis 7，可选
- Maven Wrapper，项目已内置
- Windows PowerShell，项目脚本以 Windows 为主

## 快速启动

### 1. 准备数据库

本地创建 MySQL 数据库：

```sql
create database if not exists tianshiwebside
  default character set utf8mb4
  collate utf8mb4_unicode_ci;
```

默认配置读取环境变量：

```properties
DB_URL
DB_USERNAME
DB_PASSWORD
REDIS_HOST
REDIS_PORT
```

也可以复制 `.env.example` 作为本地配置参考。

### 2. 启动 Redis，可选

如果本机已有 Redis：

```powershell
Test-NetConnection localhost -Port 6379
```

如果没有 Redis，系统会使用数据库兜底。项目也提供脚本：

```powershell
.\start-redis.ps1
```

### 3. 启动项目

推荐直接使用：

```powershell
.\start-project.ps1
```

默认地址：

```text
后端：http://localhost:8080
前端：http://localhost:5173
```

如果手动启动：

```powershell
$env:SPRING_PROFILES_ACTIVE="demo"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的MySQL密码"
.\mvnw.cmd spring-boot:run
```

前端：

```powershell
cd frontend
npm install
npm run dev
```

## 常用页面

| 页面 | 地址 |
| --- | --- |
| 前端系统 | `http://localhost:5173` |
| 后端接口根路径 | `http://localhost:8080` |
| Redis 状态监控 | `http://localhost:5173/admin/redis-monitor` |
| 压测报告列表 | `http://localhost:5173/admin/load-test-reports` |
| 数据库只读浏览 | `http://localhost:5173/admin/database-browser` |
| 静态答辩分工页 | `team-division-demo.html` |
| 代码分析演示页 | `preview.html` |
| 数据库设计讲解页 | `database-design.html` |

## 演示账号

项目内置的是演示数据，不包含真实用户信息。默认演示账号密码为本地演示用途，请不要用于生产环境。

```text
管理员：admin001 / 123456
教师：teacher001 / 123456
学生：student001 / 123456
开发者：23111141 / 123456
```

如果发布到公开仓库，建议在 README 中只保留最少量演示账号，并在部署后立即修改默认密码。

## 项目结构

```text
.
├─ src/main/java/weidonglang/tianshiwebside
│  ├─ auth              登录认证
│  ├─ security          Spring Security 与 Token 过滤
│  ├─ course            课程、教学班、选课、Redis 抢课
│  ├─ student           学生信息与学籍
│  ├─ teacher           教师端业务
│  ├─ admin             管理端、系统监控、数据库浏览
│  ├─ notice            通知公告
│  ├─ audit             操作日志
│  ├─ file              文件管理
│  └─ config            初始化数据与配置
├─ src/main/resources
│  └─ db/migration      Flyway 数据库迁移
├─ frontend
│  ├─ src/api           前端接口封装
│  ├─ src/views         Vue 页面
│  ├─ src/stores        Pinia 状态
│  └─ src/components    通用组件与图表组件
├─ scripts              压测脚本与 Python 面板
├─ reports              压测报告输出目录，本地生成，不建议提交
├─ docs                 文档
└─ release              打包发布目录，本地生成，不建议提交
```

## 构建

前端构建：

```powershell
cd frontend
npm run build
```

后端构建：

```powershell
.\mvnw.cmd clean package -DskipTests
```

## 注意事项

- 本项目是教学演示项目，不建议直接用于生产环境。
- 仓库中不要提交真实数据库密码、真实个人信息、日志文件、上传文件和压测报告。
- Redis 是可选组件，未启动 Redis 时系统会降级到数据库兜底。
- 数据库只读浏览模块默认不提供危险写操作。
- 公开发布前请检查 `.env`、日志、截图、报告中是否包含敏感信息。

## 许可证

本项目采用 MIT License，详见 [LICENSE](LICENSE)。

Copyright (c) 2004-2026 魏语石
