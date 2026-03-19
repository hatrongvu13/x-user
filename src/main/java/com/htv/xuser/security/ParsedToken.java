package com.htv.xuser.security;

import com.htv.xuser.model.dto.TokenType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ParsedToken — value object chứa claims đã được validate từ JWT.
 *
 * Thay thế raw {@code JWTClaimsSet} của Nimbus — type-safe, dễ dùng trong service.
 *
 * Cách dùng:
 * <pre>
 *   ParsedToken pt = tokenProvider.validateAccess(rawToken);
 *   UUID userId = pt.getUserId();
 *   boolean isAdmin = pt.getRoles().contains("ROLE_ADMIN");
 * </pre>
 */
@Getter
@Builder
public class ParsedToken {
    private final String     jti;
    private final UUID userId;
    private final String     email;
    private final String     username;
    private final TokenType type;
    private final Instant    issuedAt;
    private final Instant expiresAt;

    /** Chỉ có trong ACCESS token */
    private final List<String> roles;

    /** Chỉ có trong ACCESS token */
    private final List<String> perms;

    /** Chỉ có trong MFA_PENDING token */
    private final String mfaType;
}
