package weidonglang.tianshiwebside.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import weidonglang.tianshiwebside.common.api.ApiResponse;
import weidonglang.tianshiwebside.common.error.BusinessException;
import weidonglang.tianshiwebside.common.error.ErrorCode;
import weidonglang.tianshiwebside.user.mapper.UserAccountMapper;

import java.util.List;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final UserAccountMapper userAccountMapper;

    public MeController(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @GetMapping
    public ApiResponse<CurrentUserResponse> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        UserAccountMapper.UserAccountRow user = userAccountMapper.findByUsername(authentication.getName());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return ApiResponse.success(new CurrentUserResponse(
                user.id(),
                user.username(),
                user.displayName(),
                userAccountMapper.findRoleCodesByUserId(user.id())
        ));
    }

    public record CurrentUserResponse(
            Long id,
            String username,
            String displayName,
            List<String> roles
    ) {
    }
}
