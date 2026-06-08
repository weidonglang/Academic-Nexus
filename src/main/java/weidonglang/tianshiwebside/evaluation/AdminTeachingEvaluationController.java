package weidonglang.tianshiwebside.evaluation;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.cache.QueryCacheService;
import weidonglang.tianshiwebside.evaluation.mapper.EvaluationRecordRow;
import weidonglang.tianshiwebside.evaluation.mapper.EvaluationSummaryRow;
import weidonglang.tianshiwebside.evaluation.mapper.TeachingEvaluationMapper;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/admin/evaluations")
public class AdminTeachingEvaluationController {
    private final TeachingEvaluationMapper evaluationMapper;
    private final QueryCacheService queryCacheService;

    public AdminTeachingEvaluationController(TeachingEvaluationMapper evaluationMapper, QueryCacheService queryCacheService) {
        this.evaluationMapper = evaluationMapper;
        this.queryCacheService = queryCacheService;
    }

    @GetMapping("/summaries")
    public ApiResponse<List<EvaluationSummaryRow>> summaries(@RequestParam(required = false) String term) {
        String normalizedTerm = normalizeTerm(term);
        return ApiResponse.success(queryCacheService.get(
                "query:evaluations:admin:summaries:" + (normalizedTerm == null ? "all" : normalizedTerm),
                Duration.ofSeconds(20),
                new TypeReference<List<EvaluationSummaryRow>>() {
                },
                () -> evaluationMapper.findSummaries(normalizedTerm)
        ));
    }

    @GetMapping("/records")
    public ApiResponse<List<EvaluationRecordRow>> records(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Long offeringId
    ) {
        String normalizedTerm = normalizeTerm(term);
        return ApiResponse.success(queryCacheService.get(
                "query:evaluations:admin:records:" + (normalizedTerm == null ? "all" : normalizedTerm) + ":" + (offeringId == null ? "all" : offeringId),
                Duration.ofSeconds(20),
                new TypeReference<List<EvaluationRecordRow>>() {
                },
                () -> evaluationMapper.findRecords(normalizedTerm, offeringId)
        ));
    }

    @GetMapping(value = "/export-csv", produces = "text/csv;charset=UTF-8")
    public String exportCsv(@RequestParam(required = false) String term) {
        StringBuilder builder = new StringBuilder("courseCode,courseName,teacherName,term,selectedCount,submittedCount,averageOverallScore\n");
        for (EvaluationSummaryRow row : evaluationMapper.findSummaries(normalizeTerm(term))) {
            builder.append(row.courseCode()).append(',')
                    .append(row.courseName()).append(',')
                    .append(row.teacherName()).append(',')
                    .append(row.term()).append(',')
                    .append(row.selectedCount()).append(',')
                    .append(row.submittedCount()).append(',')
                    .append(row.averageOverallScore() == null ? "" : row.averageOverallScore()).append('\n');
        }
        return builder.toString();
    }

    private String normalizeTerm(String term) {
        return term == null || term.isBlank() ? null : term.trim();
    }
}
