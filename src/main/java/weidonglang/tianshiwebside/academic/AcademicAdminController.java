package weidonglang.tianshiwebside.academic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import weidonglang.tianshiwebside.audit.AuditLogService;
import weidonglang.tianshiwebside.academic.mapper.AcademicAdminMapper;
import weidonglang.tianshiwebside.academic.mapper.AcademicAdminMapper.ExamAdminRow;
import weidonglang.tianshiwebside.academic.mapper.AcademicAdminMapper.GradeAdminRow;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.api.PageResponse;
import weidonglang.tianshiwebside.common.api.Pagination;
import weidonglang.tianshiwebside.common.cache.QueryCacheService;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.notice.NotificationService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/academic")
public class AcademicAdminController {
    private final AcademicAdminMapper mapper;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final QueryCacheService queryCacheService;

    public AcademicAdminController(
            AcademicAdminMapper mapper,
            AuditLogService auditLogService,
            NotificationService notificationService,
            QueryCacheService queryCacheService
    ) {
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.queryCacheService = queryCacheService;
    }

    @GetMapping("/grades")
    @PreAuthorize("hasAuthority('GRADE_READ')")
    /**
     * 功能：查询成绩管理列表。
     * 说明：管理端按学期和关键字查询学生成绩，返回学生、课程、分数、绩点、
     * 考试类型、成绩状态和锁定状态，供成绩后台表格展示。
     */
    public ApiResponse<PageResponse<GradeAdminRow>> grades(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String normalizedTerm = normalize(term);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : "%" + keyword.trim() + "%";
        int safePage = Pagination.safePage(page);
        int safeSize = Pagination.safeSize(size);
        return ApiResponse.success(new PageResponse<>(
                mapper.findGrades(normalizedTerm, normalizedKeyword, safeSize, Pagination.offset(safePage, safeSize)),
                safePage,
                safeSize,
                mapper.countGrades(normalizedTerm, normalizedKeyword)
        ));
    }

    @PostMapping("/grades")
    @PreAuthorize("hasAuthority('GRADE_WRITE')")
    /**
     * 功能：后台新增成绩记录。
     * 说明：管理员录入学生学号、课程、学期、分数和成绩状态，后端校验学生和课程存在后写入成绩表。
     */
    public ApiResponse<Void> createGrade(Authentication authentication, @Valid @RequestBody GradeRequest request) {
        Long studentId = mapper.findStudentIdByStudentNo(request.studentNo().trim());
        if (studentId == null || mapper.countCourseById(request.courseId()) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学生或课程不存在");
        }
        mapper.insertGrade(toGradeCommand(null, studentId, request));
        auditLogService.record(authentication.getName(), "CREATE_GRADE", "GRADE", request.studentNo(), request.examType(), null);
        evictAcademicCaches();
        return ApiResponse.success();
    }

    @PutMapping("/grades/{gradeId}")
    @PreAuthorize("hasAuthority('GRADE_WRITE')")
    /**
     * 功能：修改成绩记录。
     * 说明：成绩未锁定时允许管理员更新分数、绩点和状态；已锁定成绩禁止继续修改，
     * 体现成绩发布后的数据保护。
     */
    public ApiResponse<Void> updateGrade(Authentication authentication, @PathVariable Long gradeId, @Valid @RequestBody GradeRequest request) {
        if (mapper.countLockedGrade(gradeId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "成绩已锁定，不能修改");
        }
        Long studentId = mapper.findStudentIdByStudentNo(request.studentNo().trim());
        if (studentId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学生不存在");
        }
        mapper.updateGrade(toGradeCommand(gradeId, studentId, request));
        auditLogService.record(authentication.getName(), "UPDATE_GRADE", "GRADE", gradeId,
                "score=" + request.score() + ", status=" + request.gradeStatus(), null);
        evictAcademicCaches();
        return ApiResponse.success();
    }

    @GetMapping("/grades/export")
    @PreAuthorize("hasAuthority('GRADE_READ')")
    public ApiResponse<List<GradeAdminRow>> exportGrades(@RequestParam(required = false) String term) {
        return ApiResponse.success(mapper.findGradesForExport(normalize(term), null));
    }

    @GetMapping(value = "/grades/export-csv", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAuthority('GRADE_READ')")
    public String exportGradesCsv(@RequestParam(required = false) String term) {
        StringBuilder builder = new StringBuilder("studentNo,courseId,term,score,gradePoint,examType,gradeStatus,locked\n");
        for (GradeAdminRow row : mapper.findGradesForExport(normalize(term), null)) {
            builder.append(row.studentNo()).append(',')
                    .append(row.courseId()).append(',')
                    .append(row.term()).append(',')
                    .append(row.score()).append(',')
                    .append(row.gradePoint()).append(',')
                    .append(row.examType()).append(',')
                    .append(row.gradeStatus()).append(',')
                    .append(row.locked()).append('\n');
        }
        return builder.toString();
    }

    @PostMapping("/grades/import-csv")
    @PreAuthorize("hasAuthority('GRADE_WRITE')")
    public ApiResponse<Void> importGradesCsv(Authentication authentication, @RequestParam("file") MultipartFile file) throws java.io.IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        for (String line : content.split("\\R")) {
            if (line.isBlank() || line.startsWith("studentNo,")) {
                continue;
            }
            String[] parts = line.split(",", -1);
            if (parts.length < 8) {
                continue;
            }
            createGrade(authentication, new GradeRequest(parts[0].trim(), Long.parseLong(parts[1].trim()), parts[2].trim(),
                    Integer.parseInt(parts[3].trim()), new BigDecimal(parts[4].trim()), parts[5].trim(), parts[6].trim(),
                    Boolean.parseBoolean(parts[7].trim())));
        }
        return ApiResponse.success();
    }

    @PostMapping("/grades/import")
    @PreAuthorize("hasAuthority('GRADE_WRITE')")
    /**
     * 功能：批量导入成绩。
     * 说明：管理端提交多条成绩数据，后端逐条复用新增成绩逻辑保存，
     * 并记录导入数量，作为文件导入导出功能的基础结构。
     */
    public ApiResponse<Void> importGrades(Authentication authentication, @Valid @RequestBody ImportGradesRequest request) {
        for (GradeRequest grade : request.grades()) {
            createGrade(authentication, grade);
        }
        auditLogService.record(authentication.getName(), "IMPORT_GRADES", "GRADE", null, "count=" + request.grades().size(), null);
        return ApiResponse.success();
    }

    @GetMapping("/exams")
    /**
     * 功能：查询考试安排列表。
     * 说明：管理端按学期查看教学班对应的考试时间、考场、座位、考试类型和监考教师。
     */
    public ApiResponse<PageResponse<ExamAdminRow>> exams(
            @RequestParam(required = false) String term,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String normalizedTerm = normalize(term);
        int safePage = Pagination.safePage(page);
        int safeSize = Pagination.safeSize(size);
        return ApiResponse.success(new PageResponse<>(
                mapper.findExams(normalizedTerm, safeSize, Pagination.offset(safePage, safeSize)),
                safePage,
                safeSize,
                mapper.countExams(normalizedTerm)
        ));
    }

    @PostMapping("/exams")
    @PreAuthorize("hasAuthority('EXAM_WRITE')")
    /**
     * 功能：新增考试安排。
     * 说明：管理员为教学班安排考试时间、地点、座位和监考信息，
     * 后端保存后向该教学班已选学生发送考试通知。
     */
    public ApiResponse<Void> createExam(Authentication authentication, @Valid @RequestBody ExamRequest request) {
        ensureOffering(request.offeringId());
        mapper.insertExam(toExamCommand(null, request));
        notificationService.notifyUsers(mapper.findSelectedUserIdsByOfferingId(request.offeringId()),
                "考试安排通知", "你有新的考试安排：" + request.examTime() + " " + request.room(),
                "EXAM", "EXAM", request.offeringId());
        auditLogService.record(authentication.getName(), "CREATE_EXAM", "EXAM", request.offeringId(), request.room(), null);
        evictAcademicCaches();
        return ApiResponse.success();
    }

    @PutMapping("/exams/{examId}")
    @PreAuthorize("hasAuthority('EXAM_WRITE')")
    /**
     * 功能：修改考试安排。
     * 说明：考试时间、地点或监考信息变化后，后端更新考试记录并通知相关学生，
     * 保证学生端考试安排查询能同步看到最新结果。
     */
    public ApiResponse<Void> updateExam(Authentication authentication, @PathVariable Long examId, @Valid @RequestBody ExamRequest request) {
        ensureOffering(request.offeringId());
        mapper.updateExam(toExamCommand(examId, request));
        notificationService.notifyUsers(mapper.findSelectedUserIdsByOfferingId(request.offeringId()),
                "考试安排变更", "考试安排已更新：" + request.examTime() + " " + request.room(),
                "EXAM", "EXAM", examId);
        auditLogService.record(authentication.getName(), "UPDATE_EXAM", "EXAM", examId, request.room(), null);
        evictAcademicCaches();
        return ApiResponse.success();
    }

    @DeleteMapping("/exams/{examId}")
    @PreAuthorize("hasAuthority('EXAM_WRITE')")
    public ApiResponse<Void> deleteExam(Authentication authentication, @PathVariable Long examId) {
        Long offeringId = mapper.findOfferingIdByExamId(examId);
        mapper.deleteExam(examId);
        if (offeringId != null) {
            notificationService.notifyUsers(mapper.findSelectedUserIdsByOfferingId(offeringId),
                    "考试安排取消", "一条考试安排已取消，请以最新考试安排为准。",
                    "EXAM", "EXAM", examId);
        }
        auditLogService.record(authentication.getName(), "DELETE_EXAM", "EXAM", examId, null, null);
        evictAcademicCaches();
        return ApiResponse.success();
    }

    private void evictAcademicCaches() {
        queryCacheService.evictByPrefix("query:grades:");
        queryCacheService.evictByPrefix("query:exams:");
        queryCacheService.evictByPrefix("query:teacher:");
        queryCacheService.evictByPrefix("query:dashboard:");
        queryCacheService.evictByPrefix("query:notifications:");
    }

    private AcademicAdminMapper.GradeCommand toGradeCommand(Long id, Long studentId, GradeRequest request) {
        return new AcademicAdminMapper.GradeCommand(id, studentId, request.courseId(), request.term().trim(), request.score(),
                request.gradePoint(), request.examType().trim(), request.gradeStatus().trim(), request.locked());
    }

    private AcademicAdminMapper.ExamCommand toExamCommand(Long id, ExamRequest request) {
        return new AcademicAdminMapper.ExamCommand(id, request.offeringId(), request.examTime(), request.room().trim(),
                request.seatNo().trim(), request.examType().trim(), request.status().trim(),
                request.invigilator() == null ? null : request.invigilator().trim());
    }

    private void ensureOffering(Long offeringId) {
        if (mapper.countOfferingById(offeringId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "教学班不存在");
        }
    }

    private String normalize(String term) {
        return term == null || term.isBlank() ? null : term.trim();
    }

    public record GradeRequest(@NotBlank String studentNo, @NotNull Long courseId, @NotBlank String term,
                               @NotNull @Min(0) @Max(100) Integer score,
                               @NotNull @DecimalMin("0.00") @DecimalMax("5.00") BigDecimal gradePoint,
                               @NotBlank String examType, @NotBlank String gradeStatus, @NotNull Boolean locked) {
    }

    public record ImportGradesRequest(@NotNull List<GradeRequest> grades) {
    }

    public record ExamRequest(@NotNull Long offeringId, @NotNull LocalDateTime examTime, @NotBlank String room,
                              @NotBlank String seatNo, @NotBlank String examType, @NotBlank String status,
                              String invigilator) {
    }
}
