# Notification Event Closure Report

Updated: 2026-06-28

| Source | Target Resolution | Result |
| --- | --- | --- |
| Admin notice | all/role/student/teacher/admin/grade/major/class/offering | Target preview and zero-recipient guard |
| Status-change batch review | application owner | Student notification |
| Registration batch review | application owner | Student notification |
| Grade update | grade owner | Student notification |
| Exam create/update/delete | selected students in offering | Existing event notification retained |

Regression coverage: `NotificationTargetingClosureTests`, `BatchReviewClosureTests`, `SystemInteroperabilityIntegrationTests`.
