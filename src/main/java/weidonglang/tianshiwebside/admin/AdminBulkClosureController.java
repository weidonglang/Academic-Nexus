package weidonglang.tianshiwebside.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import weidonglang.tianshiwebside.audit.AuditLogService;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.cache.QueryCacheService;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.common.trace.TraceIdHolder;
import weidonglang.tianshiwebside.notice.NotificationService;
import weidonglang.tianshiwebside.schedule.ScheduleParser;
import weidonglang.tianshiwebside.student.ApplicationStatus;

import java.security.Principal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/admin")
public class AdminBulkClosureController {
    private static final String DEFAULT_PASSWORD = "Academic@123";
    private static final Set<String> REVIEWABLE = Set.of("SUBMITTED", "UNDER_REVIEW");
    private static final Pattern TERM_PATTERN = Pattern.compile("\\d{4}-\\d{4}-[12]");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^$|^[0-9+\\- ]{6,30}$");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final QueryCacheService queryCacheService;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;
    private final ScheduleParser scheduleParser;

    public AdminBulkClosureController(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            QueryCacheService queryCacheService,
            NotificationService notificationService,
            StringRedisTemplate redisTemplate,
            ScheduleParser scheduleParser
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.queryCacheService = queryCacheService;
        this.notificationService = notificationService;
        this.redisTemplate = redisTemplate;
        this.scheduleParser = scheduleParser;
    }

    @GetMapping(value = "/users/import-template", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public ResponseEntity<String> userTemplate() {
        return csvResponse("user-import-template.csv",
                "\uFEFFusername,displayName,roleCodes,password,studentNo,college,major,grade,className,phone,email\n"
                        + "stu240001,演示学生,STUDENT,,240001,信息学院,软件工程,2024,软件2401,13800000001,stu240001@example.com\n"
                        + "teacher.demo,演示教师,TEACHER,,T0001,,,,,13800000002,teacher@example.com\n");
    }

    @PostMapping("/users/import-preview")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public ApiResponse<ImportPreview> previewUsers(Authentication authentication, @Valid @RequestBody CsvImportRequest request) {
        ImportPreview preview = previewUserRows(parseCsv(request.content()));
        auditLogService.record(authentication.getName(), "BATCH_USER_IMPORT_PREVIEW", "USER", null,
                "total=" + preview.totalRows() + ", valid=" + preview.validRows(), TraceIdHolder.get());
        return ApiResponse.success(preview);
    }

    @PostMapping("/users/import-commit")
    @Transactional
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public ApiResponse<ImportCommitResult> commitUsers(Authentication authentication, @Valid @RequestBody CsvImportRequest request) {
        List<Map<String, String>> rows = parseCsv(request.content());
        ImportPreview preview = previewUserRows(rows);
        List<ImportIssue> issues = new ArrayList<>(preview.errors());
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            if (hasIssue(issues, rowNumber)) {
                continue;
            }
            Map<String, String> row = rows.get(i);
            String username = clean(row.get("username"));
            String displayName = clean(row.get("displayName"));
            String roleCodes = clean(row.get("roleCodes"));
            try {
                Long userId = insertUser(username, displayName, clean(row.get("password")));
                for (String roleCode : splitRoles(roleCodes)) {
                    jdbcTemplate.update("""
                                    insert into sys_user_role (user_id, role_id)
                                    select ?, id from sys_role where code = ?
                                    """, userId, roleCode);
                    auditLogService.record(authentication.getName(), "ASSIGN_ROLE_BY_IMPORT", "USER", userId, roleCode, TraceIdHolder.get());
                }
                if (splitRoles(roleCodes).contains("STUDENT")) {
                    jdbcTemplate.update("""
                                    insert into student
                                      (user_id, student_no, college, major, class_name, grade, status, phone, email)
                                    values (?, ?, ?, ?, ?, ?, '在籍', ?, ?)
                                    """,
                            userId, clean(row.get("studentNo")), clean(row.get("college")), clean(row.get("major")),
                            clean(row.get("className")), clean(row.get("grade")), clean(row.get("phone")), clean(row.get("email")));
                    auditLogService.record(authentication.getName(), "CREATE_STUDENT_PROFILE_BY_IMPORT", "STUDENT",
                            clean(row.get("studentNo")), clean(row.get("className")), TraceIdHolder.get());
                }
                auditLogService.record(authentication.getName(), "CREATE_USER_BY_IMPORT", "USER", userId, username, TraceIdHolder.get());
                success++;
            } catch (DuplicateKeyException ex) {
                issues.add(new ImportIssue(rowNumber, "username", "账号或学生档案已存在", "确认是否重复导入，重复账号默认跳过"));
            }
        }
        Long taskId = insertBatchTask(authentication.getName(), "USER_IMPORT", success, issues.size(), issueSummary(issues));
        auditLogService.record(authentication.getName(), "BATCH_USER_IMPORT_COMMIT", "USER", taskId,
                "success=" + success + ", failure=" + issues.size(), TraceIdHolder.get());
        evictUserCaches();
        return ApiResponse.success(new ImportCommitResult(taskId, success, issues.size(), issues));
    }

    @GetMapping(value = "/users/export-csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<String> exportUsers(@RequestParam(required = false) String keyword) {
        String like = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                        select u.username, u.display_name, u.status,
                               group_concat(r.code order by r.code separator '|') as roles,
                               s.student_no, s.college, s.major, s.grade, s.class_name, s.phone, s.email
                        from sys_user u
                        left join sys_user_role ur on ur.user_id = u.id
                        left join sys_role r on r.id = ur.role_id
                        left join student s on s.user_id = u.id
                        where (? is null or u.username like ? or u.display_name like ?)
                        group by u.id, s.id
                        order by u.id desc
                        """, like, like, like);
        StringBuilder csv = new StringBuilder("\uFEFFusername,displayName,status,roleCodes,studentNo,college,major,grade,className,phone,email\n");
        rows.forEach(row -> csv.append(csv(row.get("username"))).append(',')
                .append(csv(row.get("display_name"))).append(',')
                .append(csv(row.get("status"))).append(',')
                .append(csv(row.get("roles"))).append(',')
                .append(csv(row.get("student_no"))).append(',')
                .append(csv(row.get("college"))).append(',')
                .append(csv(row.get("major"))).append(',')
                .append(csv(row.get("grade"))).append(',')
                .append(csv(row.get("class_name"))).append(',')
                .append(csv(row.get("phone"))).append(',')
                .append(csv(row.get("email"))).append('\n'));
        return csvResponse("users-export.csv", csv.toString());
    }

    @GetMapping(value = "/courses/import-template", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('COURSE_WRITE')")
    public ResponseEntity<String> courseTemplate() {
        return csvResponse("course-import-template.csv",
                "\uFEFFcourseCode,courseName,credit,category,courseType,department,description\n"
                        + "CS2401,软件工程实践,3,专业课,必修,信息学院,面向项目闭环演示\n");
    }

    @PostMapping("/courses/import-preview")
    @PreAuthorize("hasAuthority('COURSE_WRITE')")
    public ApiResponse<ImportPreview> previewCourses(Authentication authentication, @Valid @RequestBody CsvImportRequest request) {
        ImportPreview preview = previewCourseRows(parseCsv(request.content()));
        auditLogService.record(authentication.getName(), "BATCH_COURSE_IMPORT_PREVIEW", "COURSE", null,
                "total=" + preview.totalRows() + ", valid=" + preview.validRows(), TraceIdHolder.get());
        return ApiResponse.success(preview);
    }

    @PostMapping("/courses/import-commit")
    @Transactional
    @PreAuthorize("hasAuthority('COURSE_WRITE')")
    public ApiResponse<ImportCommitResult> commitCourses(Authentication authentication, @Valid @RequestBody CsvImportRequest request) {
        List<Map<String, String>> rows = parseCsv(request.content());
        ImportPreview preview = previewCourseRows(rows);
        List<ImportIssue> issues = new ArrayList<>(preview.errors());
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            if (hasIssue(issues, rowNumber)) {
                continue;
            }
            Map<String, String> row = rows.get(i);
            if (count("select count(*) from course where code = ?", clean(row.get("courseCode"))) > 0) {
                continue;
            }
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                                insert into course (code, name, credit, category)
                                values (?, ?, ?, ?)
                                """, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, clean(row.get("courseCode")));
                ps.setString(2, clean(row.get("courseName")));
                ps.setInt(3, Integer.parseInt(clean(row.get("credit"))));
                ps.setString(4, clean(row.get("category")));
                return ps;
            }, keyHolder);
            auditLogService.record(authentication.getName(), "CREATE_COURSE_BY_IMPORT", "COURSE",
                    keyHolder.getKey() == null ? null : keyHolder.getKey().longValue(), clean(row.get("courseCode")), TraceIdHolder.get());
            success++;
        }
        Long taskId = insertBatchTask(authentication.getName(), "COURSE_IMPORT", success, issues.size(), issueSummary(issues));
        auditLogService.record(authentication.getName(), "BATCH_COURSE_IMPORT_COMMIT", "COURSE", taskId,
                "success=" + success + ", failure=" + issues.size(), TraceIdHolder.get());
        evictCourseCaches();
        return ApiResponse.success(new ImportCommitResult(taskId, success, issues.size(), issues));
    }

    @GetMapping(value = "/course-offerings/import-template", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('COURSE_WRITE')")
    public ResponseEntity<String> offeringTemplate() {
        return csvResponse("course-offering-import-template.csv",
                "\uFEFFcourseCode,term,teacherName,capacity,scheduleText,classroom,selectionStartAt,selectionEndAt,status\n"
                        + "CS2401,2026-2027-1,演示教师,60,周一 1-2节,A101,2026-08-20T00:00:00Z,2026-09-30T23:59:59Z,OPEN\n");
    }

    @PostMapping("/course-offerings/import-preview")
    @PreAuthorize("hasAuthority('COURSE_WRITE')")
    public ApiResponse<ImportPreview> previewOfferings(Authentication authentication, @Valid @RequestBody CsvImportRequest request) {
        ImportPreview preview = previewOfferingRows(parseCsv(request.content()));
        auditLogService.record(authentication.getName(), "BATCH_OFFERING_IMPORT_PREVIEW", "COURSE_OFFERING", null,
                "total=" + preview.totalRows() + ", valid=" + preview.validRows(), TraceIdHolder.get());
        return ApiResponse.success(preview);
    }

    @PostMapping("/course-offerings/import-commit")
    @Transactional
    @PreAuthorize("hasAuthority('COURSE_WRITE')")
    public ApiResponse<ImportCommitResult> commitOfferings(Authentication authentication, @Valid @RequestBody CsvImportRequest request) {
        List<Map<String, String>> rows = parseCsv(request.content());
        ImportPreview preview = previewOfferingRows(rows);
        List<ImportIssue> issues = new ArrayList<>(preview.errors());
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            if (hasIssue(issues, rowNumber)) {
                continue;
            }
            Map<String, String> row = rows.get(i);
            Long courseId = queryLong("select id from course where code = ?", clean(row.get("courseCode")));
            int duplicate = count("""
                            select count(*)
                            from course_offering
                            where course_id = ? and term = ? and teacher_name = ? and schedule_text = ?
                            """, courseId, clean(row.get("term")), clean(row.get("teacherName")), clean(row.get("scheduleText")));
            if (duplicate > 0) {
                continue;
            }
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement("""
                                insert into course_offering
                                  (course_id, teacher_name, term, capacity, schedule_text, classroom, selection_start_at, selection_end_at)
                                values (?, ?, ?, ?, ?, ?, ?, ?)
                                """, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, courseId);
                ps.setString(2, clean(row.get("teacherName")));
                ps.setString(3, clean(row.get("term")));
                ps.setInt(4, Integer.parseInt(clean(row.get("capacity"))));
                ps.setString(5, clean(row.get("scheduleText")));
                ps.setString(6, clean(row.get("classroom")));
                ps.setObject(7, Instant.parse(clean(row.get("selectionStartAt"))));
                ps.setObject(8, Instant.parse(clean(row.get("selectionEndAt"))));
                return ps;
            }, keyHolder);
            Long offeringId = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
            prewarmStock(offeringId, Integer.parseInt(clean(row.get("capacity"))));
            auditLogService.record(authentication.getName(), "CREATE_OFFERING_BY_IMPORT", "COURSE_OFFERING",
                    offeringId, clean(row.get("courseCode")) + "/" + clean(row.get("term")), TraceIdHolder.get());
            success++;
        }
        Long taskId = insertBatchTask(authentication.getName(), "COURSE_OFFERING_IMPORT", success, issues.size(), issueSummary(issues));
        auditLogService.record(authentication.getName(), "BATCH_OFFERING_IMPORT_COMMIT", "COURSE_OFFERING", taskId,
                "success=" + success + ", failure=" + issues.size(), TraceIdHolder.get());
        evictCourseCaches();
        return ApiResponse.success(new ImportCommitResult(taskId, success, issues.size(), issues));
    }

    @PostMapping("/status-changes/batch-review")
    @Transactional
    @PreAuthorize("hasAuthority('STATUS_REVIEW')")
    public ApiResponse<BatchReviewResult> batchReviewStatusChanges(Authentication authentication, @Valid @RequestBody BatchReviewRequest request) {
        return ApiResponse.success(batchReview(authentication, request, "student_status_change_application", "STATUS_CHANGE_BATCH_REVIEW",
                "STATUS_CHANGE", "BATCH_REVIEW_STATUS_CHANGE"));
    }

    @PostMapping("/registration-applications/batch-review")
    @Transactional
    @PreAuthorize("hasAuthority('STATUS_REVIEW')")
    public ApiResponse<BatchReviewResult> batchReviewRegistrations(Authentication authentication, @Valid @RequestBody BatchReviewRequest request) {
        return ApiResponse.success(batchReview(authentication, request, "student_registration_application", "REGISTRATION_APPLICATION_BATCH_REVIEW",
                "REGISTRATION_APPLICATION", "BATCH_REVIEW_REGISTRATION"));
    }

    private BatchReviewResult batchReview(Authentication authentication, BatchReviewRequest request, String table,
                                          String taskType, String targetType, String batchAction) {
        if ("REJECT".equals(request.decision()) && clean(request.comment()).isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "批量驳回必须填写统一原因");
        }
        ApplicationStatus target = "APPROVE".equals(request.decision()) ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED;
        List<BatchReviewItem> items = new ArrayList<>();
        int success = 0;
        for (Long id : request.ids()) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                            select a.id, a.status, a.student_id, s.status as student_status, s.user_id
                            from %s a
                            join student s on s.id = a.student_id
                            where a.id = ?
                            """.formatted(table), id);
            if (rows.isEmpty()) {
                items.add(new BatchReviewItem(id, false, "申请不存在"));
                continue;
            }
            Map<String, Object> row = rows.get(0);
            String status = Objects.toString(row.get("status"), "");
            if (!REVIEWABLE.contains(status)) {
                items.add(new BatchReviewItem(id, false, "该申请已审核，不能重复处理"));
                continue;
            }
            jdbcTemplate.update("""
                            update %s
                            set status = ?, reviewed_at = ?, review_comment = ?
                            where id = ?
                            """.formatted(table), target.name(), Instant.now(), clean(request.comment()), id);
            if ("student_status_change_application".equals(table) && target == ApplicationStatus.APPROVED) {
                applyStatusChange(id);
            }
            Long studentId = ((Number) row.get("student_id")).longValue();
            notificationService.notifyStudent(studentId,
                    targetType.equals("STATUS_CHANGE") ? "学籍异动审核结果" : "报名申请审核结果",
                    "你的申请已" + (target == ApplicationStatus.APPROVED ? "通过" : "驳回") + "：" + clean(request.comment()),
                    "APPLICATION", targetType, id);
            auditLogService.record(authentication.getName(),
                    target == ApplicationStatus.APPROVED ? "APPROVE_" + targetType : "REJECT_" + targetType,
                    targetType, id, clean(request.comment()), TraceIdHolder.get());
            items.add(new BatchReviewItem(id, true, ""));
            success++;
        }
        int failure = items.size() - success;
        Long taskId = insertBatchTask(authentication.getName(), taskType, success, failure, items.toString());
        auditLogService.record(authentication.getName(), batchAction, targetType, taskId,
                "decision=" + request.decision() + ", success=" + success + ", failure=" + failure, TraceIdHolder.get());
        evictApplicationCaches();
        return new BatchReviewResult(taskId, success, failure, items);
    }

    @PostMapping("/notices/target-preview")
    @PreAuthorize("hasAuthority('NOTICE_WRITE')")
    public ApiResponse<NoticeTargetPreview> noticeTargetPreview(@Valid @RequestBody NoticeTargetRequest request) {
        List<UserTargetRow> users = resolveNoticeTargets(request);
        return ApiResponse.success(new NoticeTargetPreview(summaryOf(request), users.size(), users.stream().limit(5).toList()));
    }

    private List<UserTargetRow> resolveNoticeTargets(NoticeTargetRequest request) {
        String type = clean(request.targetType()).isBlank()
                ? (clean(request.roleCode()).isBlank() ? "ALL" : "ROLE")
                : clean(request.targetType()).toUpperCase(Locale.ROOT);
        String value = clean(request.targetValue());
        return switch (type) {
            case "ALL" -> queryUsers("select id, username, display_name from sys_user where status = 'ACTIVE'");
            case "ROLE", "STUDENT", "TEACHER", "ADMIN" -> queryUsers("""
                    select u.id, u.username, u.display_name
                    from sys_user u
                    join sys_user_role ur on ur.user_id = u.id
                    join sys_role r on r.id = ur.role_id
                    where u.status = 'ACTIVE' and r.code = ?
                    """, type.equals("ROLE") ? requireValue(request.roleCode(), "角色目标不能为空") : type);
            case "GRADE" -> queryUsers("""
                    select u.id, u.username, u.display_name
                    from student s join sys_user u on u.id = s.user_id
                    where u.status = 'ACTIVE' and s.grade = ?
                    """, value);
            case "MAJOR" -> queryUsers("""
                    select u.id, u.username, u.display_name
                    from student s join sys_user u on u.id = s.user_id
                    where u.status = 'ACTIVE' and s.major = ?
                    """, value);
            case "CLASS" -> queryUsers("""
                    select u.id, u.username, u.display_name
                    from student s join sys_user u on u.id = s.user_id
                    where u.status = 'ACTIVE' and s.class_name = ?
                    """, value);
            case "OFFERING" -> queryUsers("""
                    select distinct u.id, u.username, u.display_name
                    from course_selection cs
                    join student s on s.id = cs.student_id
                    join sys_user u on u.id = s.user_id
                    where u.status = 'ACTIVE' and cs.offering_id = ?
                    """, parseLongTarget(value, "教学班 ID 必须是数字"));
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的通知目标范围: " + type);
        };
    }

    private void applyStatusChange(Long applicationId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                        select a.type, s.id as student_id, s.status as student_status
                        from student_status_change_application a
                        join student s on s.id = a.student_id
                        where a.id = ?
                        """, applicationId);
        String next = switch (Objects.toString(row.get("type"), "")) {
            case "SUSPEND" -> "休学";
            case "RESUME" -> "在籍";
            default -> Objects.toString(row.get("student_status"), "在籍");
        };
        jdbcTemplate.update("update student set status = ? where id = ?", next, row.get("student_id"));
    }

    private ImportPreview previewUserRows(List<Map<String, String>> rows) {
        List<ImportIssue> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            require(row, "username", rowNumber, errors);
            require(row, "displayName", rowNumber, errors);
            require(row, "roleCodes", rowNumber, errors);
            String username = clean(row.get("username"));
            if (!seen.add(username)) {
                errors.add(new ImportIssue(rowNumber, "username", "导入文件中账号重复", "保留一行或改成不同账号"));
            }
            if (count("select count(*) from sys_user where username = ?", username) > 0) {
                errors.add(new ImportIssue(rowNumber, "username", "账号已存在", "改账号或跳过重复导入"));
            }
            List<String> roles = splitRoles(clean(row.get("roleCodes")));
            if (roles.isEmpty()) {
                errors.add(new ImportIssue(rowNumber, "roleCodes", "角色不能为空", "填写 STUDENT/TEACHER/ADMIN"));
            }
            for (String role : roles) {
                if (count("select count(*) from sys_role where code = ?", role) == 0) {
                    errors.add(new ImportIssue(rowNumber, "roleCodes", "角色不存在: " + role, "使用系统内已有角色编码"));
                }
            }
            if (roles.contains("STUDENT")) {
                for (String column : List.of("studentNo", "college", "major", "grade", "className")) {
                    require(row, column, rowNumber, errors);
                }
                if (count("select count(*) from academic_class where class_name = ?", clean(row.get("className"))) == 0) {
                    errors.add(new ImportIssue(rowNumber, "className", "班级不存在", "先在班级管理中创建班级"));
                }
                if (count("select count(*) from student where student_no = ?", clean(row.get("studentNo"))) > 0) {
                    errors.add(new ImportIssue(rowNumber, "studentNo", "学号已存在", "确认是否重复导入"));
                }
            }
            validateContact(row, rowNumber, errors);
        }
        return preview(rows.size(), errors);
    }

    private ImportPreview previewCourseRows(List<Map<String, String>> rows) {
        List<ImportIssue> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            require(row, "courseCode", rowNumber, errors);
            require(row, "courseName", rowNumber, errors);
            require(row, "credit", rowNumber, errors);
            require(row, "category", rowNumber, errors);
            try {
                if (Integer.parseInt(clean(row.get("credit"))) <= 0) {
                    errors.add(new ImportIssue(rowNumber, "credit", "学分必须大于 0", "填写正整数"));
                }
            } catch (RuntimeException ex) {
                errors.add(new ImportIssue(rowNumber, "credit", "学分格式错误", "填写正整数"));
            }
        }
        return preview(rows.size(), errors);
    }

    private ImportPreview previewOfferingRows(List<Map<String, String>> rows) {
        List<ImportIssue> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNumber = i + 2;
            Map<String, String> row = rows.get(i);
            for (String column : List.of("courseCode", "term", "teacherName", "capacity", "scheduleText", "classroom", "selectionStartAt", "selectionEndAt")) {
                require(row, column, rowNumber, errors);
            }
            if (count("select count(*) from course where code = ?", clean(row.get("courseCode"))) == 0) {
                errors.add(new ImportIssue(rowNumber, "courseCode", "课程不存在", "先导入或创建课程"));
            }
            if (!TERM_PATTERN.matcher(clean(row.get("term"))).matches()) {
                errors.add(new ImportIssue(rowNumber, "term", "学期格式错误", "示例：2026-2027-1"));
            }
            if (count("""
                            select count(*)
                            from sys_user u
                            join sys_user_role ur on ur.user_id = u.id
                            join sys_role r on r.id = ur.role_id
                            where r.code = 'TEACHER' and u.display_name = ? and u.status = 'ACTIVE'
                            """, clean(row.get("teacherName"))) == 0) {
                errors.add(new ImportIssue(rowNumber, "teacherName", "教师不存在或未启用", "先创建教师账号并分配 TEACHER 角色"));
            }
            try {
                if (Integer.parseInt(clean(row.get("capacity"))) <= 0) {
                    errors.add(new ImportIssue(rowNumber, "capacity", "容量必须大于 0", "填写正整数"));
                }
            } catch (RuntimeException ex) {
                errors.add(new ImportIssue(rowNumber, "capacity", "容量格式错误", "填写正整数"));
            }
            if (!scheduleParser.parse(clean(row.get("scheduleText"))).valid()) {
                errors.add(new ImportIssue(rowNumber, "scheduleText", "上课时间格式不可解析", "示例：周一 1-2节"));
            }
            try {
                Instant start = Instant.parse(clean(row.get("selectionStartAt")));
                Instant end = Instant.parse(clean(row.get("selectionEndAt")));
                if (!end.isAfter(start)) {
                    errors.add(new ImportIssue(rowNumber, "selectionEndAt", "选课结束时间必须晚于开始时间", "调整选课窗口"));
                }
            } catch (RuntimeException ex) {
                errors.add(new ImportIssue(rowNumber, "selectionStartAt", "时间格式错误", "使用 ISO-8601，如 2026-08-20T00:00:00Z"));
            }
        }
        return preview(rows.size(), errors);
    }

    private ImportPreview preview(int total, List<ImportIssue> errors) {
        long duplicate = errors.stream().filter(error -> error.reason().contains("重复") || error.reason().contains("已存在")).count();
        long missing = errors.stream().filter(error -> error.reason().contains("不能为空")).count();
        long format = errors.stream().filter(error -> error.reason().contains("格式")).count();
        Set<Integer> badRows = new HashSet<>();
        errors.forEach(error -> badRows.add(error.rowNumber()));
        return new ImportPreview(total, total - badRows.size(), badRows.size(), duplicate, missing, format, errors);
    }

    private List<Map<String, String>> parseCsv(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String[] lines = content.replace("\uFEFF", "").split("\\R");
        if (lines.length == 0) {
            return List.of();
        }
        List<String> headers = splitCsvLine(lines[0]);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            List<String> values = splitCsvLine(lines[i]);
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                row.put(headers.get(c).trim(), c < values.size() ? values.get(c).trim() : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private List<String> splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private void require(Map<String, String> row, String column, int rowNumber, List<ImportIssue> errors) {
        if (clean(row.get(column)).isBlank()) {
            errors.add(new ImportIssue(rowNumber, column, "字段不能为空", "补充 " + column));
        }
    }

    private void validateContact(Map<String, String> row, int rowNumber, List<ImportIssue> errors) {
        String email = clean(row.get("email"));
        if (!email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add(new ImportIssue(rowNumber, "email", "邮箱格式错误", "示例：name@example.com"));
        }
        String phone = clean(row.get("phone"));
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            errors.add(new ImportIssue(rowNumber, "phone", "手机号格式错误", "仅使用数字、空格、+ 或 -"));
        }
    }

    private Long insertUser(String username, String displayName, String password) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            insert into sys_user (username, password_hash, display_name, status)
                            values (?, ?, ?, 'ACTIVE')
                            """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, passwordEncoder.encode(password.isBlank() ? DEFAULT_PASSWORD : password));
            ps.setString(3, displayName);
            return ps;
        }, keyHolder);
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    private Long insertBatchTask(String operator, String taskType, int success, int failure, String detail) {
        Instant now = Instant.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            insert into batch_task
                              (task_type, operator, status, success_count, failure_count, failure_detail, report_path, started_at, ended_at)
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, taskType);
            ps.setString(2, operator);
            ps.setString(3, failure > 0 && success > 0 ? "PARTIAL_SUCCESS" : failure > 0 ? "FAILED" : "SUCCESS");
            ps.setInt(4, success);
            ps.setInt(5, failure);
            ps.setString(6, truncate(detail, 1900));
            ps.setString(7, "api:/api/admin/batch-tasks/{id}/report.csv");
            ps.setObject(8, now);
            ps.setObject(9, now);
            return ps;
        }, keyHolder);
        return keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
    }

    private void prewarmStock(Long offeringId, int capacity) {
        if (offeringId == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set("selection:offering:" + offeringId + ":remaining", String.valueOf(capacity));
            auditLogService.record("system", "PREWARM_STOCK_AFTER_IMPORT", "COURSE_OFFERING", offeringId,
                    "remaining=" + capacity, TraceIdHolder.get());
        } catch (RuntimeException ignored) {
            auditLogService.record("system", "PREWARM_STOCK_AFTER_IMPORT_DEGRADED", "COURSE_OFFERING", offeringId,
                    "Redis unavailable; DB remains authoritative", TraceIdHolder.get(), true, null);
        }
    }

    private List<String> splitRoles(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[|;，,]"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> item.toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<UserTargetRow> queryUsers(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> new UserTargetRow(
                rs.getLong("id"), rs.getString("username"), rs.getString("display_name")), args);
    }

    private Long queryLong(String sql, Object... args) {
        List<Long> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String requireValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private Long parseLongTarget(String value, String message) {
        try {
            return Long.parseLong(requireValue(value, message));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private int count(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0 : count.intValue();
    }

    private boolean hasIssue(List<ImportIssue> issues, int rowNumber) {
        return issues.stream().anyMatch(issue -> issue.rowNumber() == rowNumber);
    }

    private String issueSummary(List<ImportIssue> issues) {
        if (issues.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ImportIssue issue : issues) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append("row ").append(issue.rowNumber()).append(' ')
                    .append(issue.column()).append(": ").append(issue.reason());
        }
        return truncate(builder.toString(), 1900);
    }

    private String summaryOf(NoticeTargetRequest request) {
        String type = clean(request.targetType()).isBlank()
                ? (clean(request.roleCode()).isBlank() ? "ALL" : "ROLE")
                : clean(request.targetType()).toUpperCase(Locale.ROOT);
        return type + (clean(request.targetValue()).isBlank() ? "" : "=" + clean(request.targetValue()));
    }

    private ResponseEntity<String> csvResponse(String filename, String body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }

    private String csv(Object value) {
        return "\"" + (value == null ? "" : value.toString().replace("\"", "\"\"")) + "\"";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private void evictUserCaches() {
        queryCacheService.evictByPrefix("query:menus:");
        queryCacheService.evictByPrefix("query:dashboard:");
        queryCacheService.evictByPrefix("query:teacher:");
        queryCacheService.evictByPrefix("query:student:");
    }

    private void evictCourseCaches() {
        queryCacheService.evictByPrefix("query:admin:courses:");
        queryCacheService.evictByPrefix("query:admin:course-offerings:");
        queryCacheService.evictByPrefix("query:course-selection:");
        queryCacheService.evictByPrefix("query:teacher:");
        queryCacheService.evictByPrefix("query:schedule:");
        queryCacheService.evictByPrefix("query:information:");
        queryCacheService.evictByPrefix("query:dashboard:");
    }

    private void evictApplicationCaches() {
        queryCacheService.evictByPrefix("query:student:status-changes:");
        queryCacheService.evictByPrefix("query:admin:status-changes:");
        queryCacheService.evictByPrefix("query:student:registration-applications:");
        queryCacheService.evictByPrefix("query:admin:registration-applications:");
        queryCacheService.evictByPrefix("query:teacher:application-awareness:");
        queryCacheService.evictByPrefix("query:dashboard:");
        queryCacheService.evictByPrefix("query:notifications:");
    }

    public record CsvImportRequest(@NotBlank String content) {
    }

    public record ImportPreview(long totalRows, long validRows, long errorRows, long duplicateAccounts,
                                long missingFields, long formatErrors, List<ImportIssue> errors) {
    }

    public record ImportIssue(int rowNumber, String column, String reason, String suggestion) {
    }

    public record ImportCommitResult(Long taskId, int successCount, int failureCount, List<ImportIssue> items) {
    }

    public record BatchReviewRequest(@NotNull List<Long> ids, @NotBlank String decision, @Size(max = 500) String comment) {
    }

    public record BatchReviewResult(Long taskId, int successCount, int failureCount, List<BatchReviewItem> items) {
    }

    public record BatchReviewItem(Long id, boolean success, String reason) {
    }

    public record NoticeTargetRequest(String targetType, String targetValue, String roleCode) {
    }

    public record NoticeTargetPreview(String summary, int receiverCount, List<UserTargetRow> samples) {
    }

    public record UserTargetRow(Long id, String username, String displayName) {
    }
}
