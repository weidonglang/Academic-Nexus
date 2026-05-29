# 贡献指南

感谢关注本项目。本项目以课程设计、毕业设计和教学演示为主要目标，代码贡献应优先保持清晰、可讲解、易维护。

## 开发原则

- 优先使用项目已有技术栈，不随意引入复杂框架。
- 后端接口保持统一返回结构 `ApiResponse`。
- 数据库结构变更通过 Flyway migration 完成。
- 管理端高风险功能默认谨慎处理，尤其是数据库写操作和任意 SQL 执行。
- Redis 相关逻辑需要保留数据库兜底能力。
- 前端页面优先复用 Element Plus、Pinia、Vue Router 和已有 API 封装。

## 本地开发流程

1. Fork 或克隆仓库。
2. 安装 JDK 17、Node.js、MySQL。
3. 创建本地数据库 `tianshiwebside`。
4. 按 README 启动后端和前端。
5. 修改代码后执行：

```powershell
.\mvnw.cmd -q -DskipTests compile
cd frontend
npm run build
```

## 提交建议

提交信息建议使用清晰的中文或英文动词开头，例如：

```text
feat: 增加 Redis 状态监控页面
fix: 修复公告发布全体用户时的批量写入问题
docs: 补充数据库设计说明
```

## 不建议提交的内容

- `target/`
- `frontend/node_modules/`
- `frontend/dist/`
- `reports/`
- `uploads/`
- `release/`
- `.env`
- 日志文件
- 本地数据库密码
- 真实学生、教师、身份证号等敏感信息

## 安全说明

如果发现安全问题，请不要直接公开贴出可利用细节。可以先在 Issue 中描述影响范围，或通过项目维护者指定方式沟通。
