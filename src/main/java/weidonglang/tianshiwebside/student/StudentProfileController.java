package weidonglang.tianshiwebside.student;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.student.mapper.StudentMapper;

/**
 * 学生个人信息接口。
 *
 * 学生端“查询个人信息”和“联系方式维护”页面调用这里。后端只允许读取或修改当前登录学生自己的资料，
 * 不允许前端传学号任意查询，体现了接口级权限和数据隔离。
 */
@RestController
@RequestMapping("/api/students")
public class StudentProfileController {
    private final StudentMapper studentMapper;

    public StudentProfileController(StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    /**
     * 查询当前学生个人档案。
     *
     * 返回学号、姓名、学院、专业、班级、年级、学籍状态和联系方式。
     */
    @GetMapping("/me/profile")
    public ApiResponse<StudentProfileResponse> myProfile(Authentication authentication) {
        return ApiResponse.success(toResponse(currentStudent(authentication)));
    }

    /**
     * 修改当前学生联系方式。
     *
     * 只开放电话、邮箱、地址这类可由学生维护的信息，学院、专业、班级等学籍信息仍由后台管理。
     */
    @PutMapping("/me/profile")
    public ApiResponse<StudentProfileResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateStudentProfileRequest request
    ) {
        String username = authenticatedUsername(authentication);
        int updated = studentMapper.updateContactByUsername(username, request.phone(), request.email(), request.address());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Student profile not found");
        }
        return ApiResponse.success(toResponse(currentStudent(username)));
    }

    private StudentMapper.StudentProfileRow currentStudent(Authentication authentication) {
        return currentStudent(authenticatedUsername(authentication));
    }

    private StudentMapper.StudentProfileRow currentStudent(String username) {
        StudentMapper.StudentProfileRow row = studentMapper.findProfileByUsername(username);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Student profile not found");
        }
        return row;
    }

    private String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }

    private StudentProfileResponse toResponse(StudentMapper.StudentProfileRow student) {
        return new StudentProfileResponse(
                student.studentNo(),
                student.name(),
                student.college(),
                student.major(),
                student.className(),
                student.grade(),
                student.status(),
                student.phone(),
                student.email(),
                student.address()
        );
    }

    public record StudentProfileResponse(
            String studentNo,
            String name,
            String college,
            String major,
            String className,
            String grade,
            String status,
            String phone,
            String email,
            String address
    ) {
    }

    public record UpdateStudentProfileRequest(
            @Size(max = 30) String phone,
            @Email @Size(max = 120) String email,
            @Size(max = 200) String address
    ) {
    }
}
