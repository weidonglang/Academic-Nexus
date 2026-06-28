# Batch Import Closure Report

Updated: 2026-06-28

Batch imports are implemented as two-stage CSV flows: preview validates and writes no business rows; commit imports valid rows, skips invalid rows, records batch task summaries, and writes audit logs.

| Import | Preview | Commit | Tests |
| --- | --- | --- | --- |
| Users | `/api/admin/users/import-preview` | `/api/admin/users/import-commit` | `BatchUserImportClosureTests` |
| Courses | `/api/admin/courses/import-preview` | `/api/admin/courses/import-commit` | `BatchCourseOfferingImportClosureTests` |
| Offerings | `/api/admin/course-offerings/import-preview` | `/api/admin/course-offerings/import-commit` | `BatchCourseOfferingImportClosureTests` |

Current implementation uses pasted/uploaded CSV content from the admin UI. Excel parsing is intentionally not added in this pass; CSV is the reproducible import format for v1.4.1 final closure.
