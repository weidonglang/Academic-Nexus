package weidonglang.tianshiwebside.ai;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.api.PageResponse;
import weidonglang.tianshiwebside.common.api.Pagination;
import weidonglang.tianshiwebside.common.trace.TraceIdHolder;

import org.springframework.jdbc.core.JdbcTemplate;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@RestController
public class AiController {
    private final AiAssistantService assistantService;
    private final AiChatService chatService;
    private final NaturalSqlService naturalSqlService;
    private final SqlSchemaService sqlSchemaService;
    private final AiRemoteClient remoteClient;
    private final AiCallLogService callLogService;
    private final AcademicProfileService academicProfileService;
    private final LoadTestAnalysisService loadTestAnalysisService;
    private final JdbcTemplate jdbcTemplate;
    private final AiModelRegistryService modelRegistryService;
    private final AiSearchService searchService;

    public AiController(
            AiAssistantService assistantService,
            AiChatService chatService,
            NaturalSqlService naturalSqlService,
            SqlSchemaService sqlSchemaService,
            AiRemoteClient remoteClient,
            AiCallLogService callLogService,
            AcademicProfileService academicProfileService,
            LoadTestAnalysisService loadTestAnalysisService,
            JdbcTemplate jdbcTemplate,
            AiModelRegistryService modelRegistryService,
            AiSearchService searchService
    ) {
        this.assistantService = assistantService;
        this.chatService = chatService;
        this.naturalSqlService = naturalSqlService;
        this.sqlSchemaService = sqlSchemaService;
        this.remoteClient = remoteClient;
        this.callLogService = callLogService;
        this.academicProfileService = academicProfileService;
        this.loadTestAnalysisService = loadTestAnalysisService;
        this.jdbcTemplate = jdbcTemplate;
        this.modelRegistryService = modelRegistryService;
        this.searchService = searchService;
    }

    @PostMapping("/api/ai/assistant/ask")
    public ApiResponse<AiAssistantResponse> ask(@Valid @RequestBody AiAssistantRequest request, Principal principal) {
        return ApiResponse.success(assistantService.ask(request.question(), principal));
    }

    @PostMapping("/api/ai/chat")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request, Principal principal) {
        return ApiResponse.success(chatService.chat(request.message(), principal));
    }

    @GetMapping("/api/ai/status")
    public ApiResponse<AiServiceStatusResponse> status() {
        AiServiceStatusResponse status = remoteClient.status();
        AiSearchDtos.SearchConfig searchConfig = searchService.config();
        return ApiResponse.success(new AiServiceStatusResponse(
                status.aiServiceOnline(),
                status.ollamaEnabled(),
                status.ollamaReachable(),
                status.chatModel(),
                status.sqlModel(),
                status.currentMode(),
                status.lastLatencyMs(),
                status.lastError(),
                status.serviceName(),
                status.discoveryEnabled(),
                status.baseUrl(),
                modelRegistryService.defaultModelName("CHAT", status.chatModel()),
                modelRegistryService.defaultModelName("RAG", status.chatModel()),
                modelRegistryService.defaultModelName("SQL", status.sqlModel()),
                searchConfig.enabled(),
                searchConfig.provider(),
                searchConfig.lastStatus(),
                status.checkedAt()
        ));
    }

    @PostMapping("/api/ai/feedback")
    public ApiResponse<Void> feedback(@Valid @RequestBody AiFeedbackRequest request, Principal principal) {
        jdbcTemplate.update("""
                        insert into ai_feedback (call_log_id, username, rating, comment, trace_id, created_at)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                request.callLogId(),
                principal == null ? "anonymous" : principal.getName(),
                request.rating(),
                request.comment(),
                TraceIdHolder.get(),
                Instant.now()
        );
        return ApiResponse.success();
    }

    @GetMapping("/api/ai/academic-profile")
    public ApiResponse<AcademicProfileResponse> academicProfile(Principal principal) {
        return ApiResponse.success(academicProfileService.currentProfile(principal));
    }

    @GetMapping("/api/admin/ai/sql/schema")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SqlSchemaService.TableSchema>> schema() {
        return ApiResponse.success(sqlSchemaService.allowedSchemas());
    }

    @PostMapping("/api/admin/ai/sql/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<NaturalSqlGenerateResponse> generateSql(
            @Valid @RequestBody NaturalSqlGenerateRequest request,
            Principal principal
    ) {
        return ApiResponse.success(naturalSqlService.generate(request.question(), principal));
    }

    @PostMapping("/api/admin/ai/sql/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<NaturalSqlExecuteResponse> executeSql(
            @Valid @RequestBody NaturalSqlExecuteRequest request,
            Principal principal
    ) {
        return ApiResponse.success(naturalSqlService.execute(request.sql(), principal));
    }

    @PostMapping("/api/admin/ai/load-test/analyze")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LoadTestAnalysisResponse> analyzeLoadTest(
            @Valid @RequestBody LoadTestAnalysisRequest request,
            Principal principal
    ) {
        return ApiResponse.success(loadTestAnalysisService.analyze(request.jsonName(), principal));
    }

    @GetMapping("/api/admin/ai/call-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AiCallLogRow>> callLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Pagination.safePage(page);
        int safeSize = Pagination.safeSize(size);
        return ApiResponse.success(new PageResponse<>(
                callLogService.logs(safeSize, Pagination.offset(safePage, safeSize)),
                safePage,
                safeSize,
                callLogService.countLogs()
        ));
    }

    public record AiFeedbackRequest(Long callLogId, String rating, String comment) {
    }
}
