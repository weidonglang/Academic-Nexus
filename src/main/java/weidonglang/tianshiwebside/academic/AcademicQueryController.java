package weidonglang.tianshiwebside.academic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;

import java.security.Principal;
import java.util.List;

/**
 * 学生端教务查询接口。
 *
 * 这里主要服务“成绩查询、考试安排查询、空闲教室查询”等只读功能。
 * 这些查询使用 MyBatis Mapper 直接读取数据库，适合展示课程设计中 SSM/MyBatis 的应用。
 */
@RestController
public class AcademicQueryController {
    private final AcademicQueryMapper academicQueryMapper;

    public AcademicQueryController(AcademicQueryMapper academicQueryMapper) {
        this.academicQueryMapper = academicQueryMapper;
    }

    /**
     * 查询当前学生的成绩列表。
     *
     * 前端成绩查询页面进入时调用，后端通过登录账号定位学生，避免学生传入别人学号查询数据。
     */
    @GetMapping("/api/grades/me")
    public ApiResponse<List<GradeRecordRow>> grades(Principal principal) {
        return ApiResponse.success(academicQueryMapper.findGradesByUsername(principal.getName()));
    }

    /**
     * 查询当前学生的考试安排。
     *
     * 返回考试时间、地点、座位号、考试类型等信息，用于学生端考试安排页面。
     */
    @GetMapping("/api/exams/me")
    public ApiResponse<List<ExamScheduleRow>> exams(Principal principal) {
        return ApiResponse.success(academicQueryMapper.findExamsByUsername(principal.getName()));
    }

    /**
     * 查询空闲教室。
     *
     * 支持按校区、教学楼、节次筛选，前端可以用表单条件快速找到可用教室。
     */
    @GetMapping("/api/classrooms/free")
    public ApiResponse<List<FreeClassroomRow>> freeClassrooms(
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String slot
    ) {
        return ApiResponse.success(academicQueryMapper.findFreeClassrooms(campus, building, slot));
    }
}
