# Academic-Nexus 演示检查清单

## 启动前

- [ ] Java 17 可用。
- [ ] MySQL 数据库 `tianshiwebside` 已创建。
- [ ] Redis `localhost:6379` 可连接。
- [ ] `DB_USERNAME`、`DB_PASSWORD` 已按本机设置。
- [ ] `.\start-redis.ps1` 检查通过。

## 构建验证

- [ ] `npm --prefix frontend ci`
- [ ] `npm --prefix frontend run build`
- [ ] `.\mvnw.cmd test`
- [ ] `.\mvnw.cmd -f ai-service/pom.xml test`
- [ ] `.\scripts\build-release.ps1 -Version 1.0 -SkipTests`

## 管理员页面

- [ ] `/admin/system-health`
- [ ] `/admin/audit-logs`
- [ ] `/admin/permission-matrix`
- [ ] `/admin/course-selection-consistency`
- [ ] `/admin/data-dictionary`
- [ ] `/admin/sensitive-words`
- [ ] `/admin/database-browser`
- [ ] `/admin/ai-sql`
- [ ] `/admin/load-test-reports`

## 教师页面

- [ ] `/teacher/offerings`
- [ ] `/teacher/grades`
- [ ] `/teacher/exams`
- [ ] `/teacher/evaluations`
- [ ] AI 助手和 AI 调用日志能显示降级或真实模型状态。

## 学生页面

- [ ] `/course/selection`
- [ ] `/schedule/personal`
- [ ] `/grade/query`
- [ ] `/exam/query`
- [ ] `/student/status-change`
- [ ] `/ai/academic-profile`

## 权限和安全

- [ ] 学生访问 `/admin/system-health` 跳 403。
- [ ] 教师访问 `/admin/audit-logs` 跳 403。
- [ ] 未登录访问业务页跳登录。
- [ ] 不存在路由跳 404。
- [ ] 发布包含 `示例敏感词A` 的通知被拦截。
- [ ] 文件上传非法扩展名被拒绝。
- [ ] 高风险操作能在审计中心看到。

## 交付包

- [ ] `release/Academic-Nexus-1.0.zip` 存在。
- [ ] zip 内包含 `academic-nexus-web.jar`。
- [ ] zip 内包含 `academic-nexus-ai-service.jar`。
- [ ] zip 内包含 `.env.example`。
- [ ] zip 内包含 `start-release.ps1` 和 `start-release.bat`。
