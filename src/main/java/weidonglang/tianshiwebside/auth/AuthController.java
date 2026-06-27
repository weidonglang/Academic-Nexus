package weidonglang.tianshiwebside.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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

    @PostMapping("/refresh")
    /**
     * 功能：使用 refresh token 换取新的登录会话。
     * 说明：旧 refresh token 会立即失效，前端收到 401 后最多尝试刷新一次，
     * 避免多个失效请求反复刷新造成风暴。
     */
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    /**
     * 功能：提供退出登录接口。
     * 说明：前端退出时调用本接口后会清空本地 token、用户信息和菜单状态，
     * 防止下一个登录用户看到上一个用户的页面状态。
     */
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request
    ) {
        authService.logout(resolveBearerToken(authorization), request == null ? null : request.refreshToken());
        return ApiResponse.success();
    }

    private String resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isBlank() ? null : token;
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }
}
