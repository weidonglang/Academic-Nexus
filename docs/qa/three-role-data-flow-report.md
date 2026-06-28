# Three-Role Data Flow Report

Updated: 2026-06-28

| Business Flow | Student | Teacher | Admin | Verification |
| --- | --- | --- | --- | --- |
| Batch user import | Imported student can log in and has class data | Imported teacher gets TEACHER role | Admin preview/commit and export available | PASS |
| Batch offering import | Imported offering visible in selection page | Teacher account must exist and can be bound | Admin imports course/offering and gets task report | PASS |
| Batch review | Application status and notification update | Teacher awareness cache evicted | Partial success, skip reason, task, audit | PASS |
| Targeted notice | Only target receives notification | Teacher/role targets supported | Preview count, zero guard, targeted audit | PASS |
| Teacher readonly awareness | Student forbidden from teacher API | Homeroom/course teacher sees summaries only | Admin review remains the only write path | PASS |
