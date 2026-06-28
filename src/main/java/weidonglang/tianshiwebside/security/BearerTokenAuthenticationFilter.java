package weidonglang.tianshiwebside.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import weidonglang.tianshiwebside.auth.AuthTokenStore;
import weidonglang.tianshiwebside.user.UserStatus;
import weidonglang.tianshiwebside.user.mapper.UserAccountMapper;

import java.io.IOException;
import java.util.Optional;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {
    private final AuthTokenStore tokenStore;
    private final UserAccountMapper userAccountMapper;

    public BearerTokenAuthenticationFilter(AuthTokenStore tokenStore, UserAccountMapper userAccountMapper) {
        this.tokenStore = tokenStore;
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            tokenStore.findAccessTokenOwner(token)
                    .flatMap(username -> Optional.ofNullable(userAccountMapper.findByUsername(username)))
                    .filter(user -> user.status() == UserStatus.ACTIVE)
                    .ifPresent(user -> {
                        var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
                        authorities.addAll(userAccountMapper.findRoleCodesByUserId(user.id()).stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                .toList());
                        authorities.addAll(userAccountMapper.findPermissionCodesByUserId(user.id()).stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList());
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user.username(),
                                token,
                                authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }
}
