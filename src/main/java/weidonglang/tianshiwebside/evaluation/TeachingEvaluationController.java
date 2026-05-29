package weidonglang.tianshiwebside.evaluation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.evaluation.mapper.EvaluationTaskRow;
import weidonglang.tianshiwebside.evaluation.mapper.TeachingEvaluationMapper;

import java.time.Instant;
import java.util.List;

/**
 * 学生教学评价接口。
 *
 * 学生只能评价自己已经选过的教学班，系统会校验选课记录和是否重复评价。
 * 这一模块体现了“学生选课数据”和“教学评价数据”之间的业务关联。
 */
@RestController
@RequestMapping("/api/evaluations")
public class TeachingEvaluationController {
    private final TeachingEvaluationMapper evaluationMapper;

    public TeachingEvaluationController(TeachingEvaluationMapper evaluationMapper) {
        this.evaluationMapper = evaluationMapper;
    }

    /**
     * 查询当前学生待评价课程。
     *
     * 前端教学评价页面先展示任务列表，学生选择课程后再提交评价分数和文字意见。
     */
    @GetMapping("/tasks")
    public ApiResponse<List<EvaluationTaskRow>> tasks(Authentication authentication) {
        return ApiResponse.success(evaluationMapper.findTasksByUsername(authenticatedUsername(authentication)));
    }

    /**
     * 提交某个教学班的评价。
     *
     * 后端会检查：学生是否存在、是否选过该课、是否已经评价过，避免伪造请求或重复提交。
     */
    @PostMapping("/tasks/{offeringId}")
    public ApiResponse<Void> submit(
            Authentication authentication,
            @PathVariable Long offeringId,
            @Valid @RequestBody SubmitEvaluationRequest request
    ) {
        String username = authenticatedUsername(authentication);
        Long studentId = evaluationMapper.findStudentIdByUsername(username);
        if (studentId == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学生档案不存在");
        }
        if (evaluationMapper.countSelection(studentId, offeringId) == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能评价已选课程");
        }
        if (evaluationMapper.countEvaluation(studentId, offeringId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该课程已提交评价");
        }
        evaluationMapper.insertEvaluation(new TeachingEvaluationMapper.InsertEvaluationCommand(
                studentId,
                offeringId,
                request.teachingScore(),
                request.contentScore(),
                request.interactionScore(),
                request.overallScore(),
                request.comment() == null ? null : request.comment().trim(),
                Instant.now()
        ));
        return ApiResponse.success();
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }

    public record SubmitEvaluationRequest(
            @NotNull @Min(1) @Max(5) Integer teachingScore,
            @NotNull @Min(1) @Max(5) Integer contentScore,
            @NotNull @Min(1) @Max(5) Integer interactionScore,
            @NotNull @Min(1) @Max(5) Integer overallScore,
            @Size(max = 500) String comment
    ) {
    }
}
