package weidonglang.tianshiwebside.auth;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.auth.dto.LoginRequest;
import weidonglang.tianshiwebside.auth.dto.LoginResponse;
import weidonglang.tianshiwebside.common.api.ApiResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    /**
     * 功能：实现用户登录认证入口。
     * 说明：前端登录页提交账号和密码到本接口，后端调用 AuthService 校验账号状态和密码，
     * 成功后返回 token、用户信息和角色信息，供前端保存登录状态并加载对应菜单。
     */
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    /**
     * 功能：提供退出登录接口。
     * 说明：前端退出时调用本接口后会清空本地 token、用户信息和菜单状态，
     * 防止下一个登录用户看到上一个用户的页面状态。
     */
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }
}
