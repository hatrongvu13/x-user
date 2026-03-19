package com.htv.xuser.filter;

import com.htv.xuser.exception.AppException;
import com.htv.xuser.security.JwtTokenProvider;
import com.htv.xuser.security.ParsedToken;
import com.htv.xuser.security.TokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JwtAuthFilter — đọc Bearer token từ Authorization header,
 * validate bằng {@link JwtTokenProvider} (Nimbus) và set SecurityContext.
 *
 * Không throw exception ra ngoài — lỗi token chỉ log debug,
 * để Spring Security trả 401 qua AuthenticationEntryPoint.
 *
 * Principal được set = email (dùng SecurityContextHolder.getContext()
 *   .getAuthentication().getName() để lấy email trong service).
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        String raw = extractBearer(request);

        if (raw != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                ParsedToken pt = tokenProvider.validateAccess(raw);

                // Kiểm tra blacklist theo JTI
                if (tokenStore.isBlacklisted(pt.getJti())) {
                    log.debug("Rejected blacklisted token jti={}", pt.getJti());
                } else {
                    setAuthentication(pt, request);
                }

            } catch (AppException e) {
                // Không propagate — Spring Security sẽ trả 401 nếu endpoint cần auth
                log.debug("JWT rejected [code={}]: {}", e.getCode(), e.getMessage());
            } catch (Exception e) {
                log.debug("JWT unexpected error: {}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    private void setAuthentication(ParsedToken pt, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // Roles — VD: ROLE_ADMIN
        if (pt.getRoles() != null) {
            pt.getRoles().forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
        }

        // Permissions — VD: USER:READ
        if (pt.getPerms() != null) {
            pt.getPerms().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }

        // Principal = email — dùng trong service qua Authentication.getName()
        var auth = new UsernamePasswordAuthenticationToken(
                pt.getEmail() != null ? pt.getEmail() : pt.getUserId().toString(),
                null,
                authorities
        );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.debug("Authenticated userId={} email={} roles={} perms={}",
                pt.getUserId(), pt.getEmail(),
                pt.getRoles() != null ? pt.getRoles().size() : 0,
                pt.getPerms() != null ? pt.getPerms().size() : 0);
    }

    private String extractBearer(HttpServletRequest req) {
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        return StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)
                ? header.substring(BEARER_PREFIX.length()).strip()
                : null;
    }
}
