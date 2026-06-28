# Academic-Nexus v1.4 批量操作指南

本指南覆盖 v1.4.0-final-polish 中的批量维护、任务中心、数据归档清理和数据库只读模板能力。

## 批量任务中心

入口：

```text
/admin/batch-tasks
```

记录字段：

```text
任务类型、操作人、开始时间、结束时间、状态、成功数、失败数、失败明细、报告路径
```

状态：

```text
PENDING / RUNNING / PARTIAL_SUCCESS / SUCCESS / FAILED
```

当前已纳入任务中心的操作包括用户/课程/教学班导入、批量通知、批量审核、归档与清理记录。后续新增批量能力时应写入 `batch_task`，并在审计日志中写总审计和明细审计。

## 批量用户导入

入口：

```text
/admin/users
```

接口：

```text
GET  /api/admin/users/import-template
POST /api/admin/users/import-preview
POST /api/admin/users/import-commit
GET  /api/admin/users/export-csv
```

当前 v1.4.1-final-closure 使用 CSV 作为可复刻导入格式。预检阶段只校验，不写业务表；正式导入只导入通过预检的行，错误行进入结果明细。学生行会写入 `sys_user`、`sys_user_role` 和 `student`，并校验班级是否存在。默认密码不会写入报告，入库密码使用 BCrypt。

## 批量课程与教学班导入

入口：

```text
/admin/course-offerings
```

接口：

```text
GET  /api/admin/courses/import-template
POST /api/admin/courses/import-preview
POST /api/admin/courses/import-commit
GET  /api/admin/course-offerings/import-template
POST /api/admin/course-offerings/import-preview
POST /api/admin/course-offerings/import-commit
```

教学班导入校验课程、任课教师、学期、容量、上课时间、教室和选课窗口。导入后写批量任务、审计日志，并在 Redis 可用时预热 `selection:offering:{offeringId}:remaining`。

## 批量审核

入口：

```text
/admin/status-changes
/admin/registration-applications
```

接口：

```text
POST /api/admin/status-changes/batch-review
POST /api/admin/registration-applications/batch-review
```

支持批量通过、批量驳回、统一原因、跳过已审核记录、部分成功明细、学生通知、缓存清理、批量任务和审计日志。驳回必须填写原因。

## 批量通知目标预览

入口：

```text
/admin/notices
```

接口：

```text
POST /api/admin/notices/target-preview
POST /api/admin/notices
```

支持全部、角色、学生、教师、管理员、年级、专业、班级和教学班目标。发布前先预览预计接收人数，接收人数为 0 时禁止发布。

## 数据归档清理

入口：

```text
/admin/data-archive
```

支持对象：

```text
历史选课记录、成绩记录、考试安排、通知记录、AI 调用日志、审计日志
```

安全边界：

- dry-run 只统计影响记录数，不改变数据库。
- 当前学期禁止清理。
- 演示环境的正式清理仅记录任务和审计，不做物理删除。
- 所有操作写入 `data_archive_record` 和 `operation_audit_log`。
- 可导出归档记录 CSV。

## 数据库只读模板

入口：

```text
/admin/database-browser
```

模板：

- 查询某课程的所有选课学生
- 查询某学生的成绩与绩点
- 查询某教学班的考试安排
- 查询某班级学生名单
- 查询某学期选课人数统计
- 查询 AI 调用失败记录

模板只执行后端白名单 SELECT，不开放任意写入 SQL。结果沿用数据库浏览器的敏感字段脱敏和 CSV 导出逻辑，操作会写入审计日志。

## 不在 v1.4 范围内

以下能力明确不实现：

- 智能备课与教学资源库
- 课程资源文件夹、视频资源库、课程克隆、资源推送
- 数据库备份、定时备份、恢复、备份下载
