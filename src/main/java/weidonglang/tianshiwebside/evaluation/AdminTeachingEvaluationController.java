package weidonglang.tianshiwebside.evaluation;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.evaluation.mapper.EvaluationRecordRow;
import weidonglang.tianshiwebside.evaluation.mapper.EvaluationSummaryRow;
import weidonglang.tianshiwebside.evaluation.mapper.TeachingEvaluationMapper;

import java.util.List;

@RestController
@RequestMapping("/api/admin/evaluations")
public class AdminTeachingEvaluationController {
    private final TeachingEvaluationMapper evaluationMapper;

    public AdminTeachingEvaluationController(TeachingEvaluationMapper evaluationMapper) {
        this.evaluationMapper = evaluationMapper;
    }

    @GetMapping("/summaries")
    public ApiResponse<List<EvaluationSummaryRow>> summaries(@RequestParam(required = false) String term) {
        return ApiResponse.success(evaluationMapper.findSummaries(normalizeTerm(term)));
    }

    @GetMapping("/records")
    public ApiResponse<List<EvaluationRecordRow>> records(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Long offeringId
    ) {
        return ApiResponse.success(evaluationMapper.findRecords(normalizeTerm(term), offeringId));
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
