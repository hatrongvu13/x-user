package com.htv.xuser.security;

import com.htv.xuser.config.JwtProperties;
import com.htv.xuser.exception.AppException;
import com.htv.xuser.exception.ErrorCode;
import com.htv.xuser.exception.JwtTokenException;
import com.htv.xuser.model.dto.TokenType;
import com.htv.xuser.model.entity.RoleEntity;
import com.htv.xuser.model.entity.UserEntity;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JwtTokenProvider — tạo và validate JWT token với Nimbus JOSE+JWT 10.7
 *
 * Lý do chuyển từ JJWT sang Nimbus:
 *   - Nimbus là thư viện JWT chuẩn công nghiệp, được dùng bởi Spring Security OAuth2
 *   - Tránh các CVE bảo mật mới trong JJWT (algorithm confusion, claim injection)
 *   - API tường minh hơn, kiểm soát header/payload rõ ràng
 *   - Không còn dependency ngoài (Nimbus đã có qua spring-security-oauth2-resource-server)
 *
 * Thuật toán ký: HS512 (HMAC-SHA512)
 * Key requirement: min 512-bit (64 bytes)
 *
 * ── Cấu trúc ACCESS token ──────────────────────────────────────────────────
 * Header: { "alg": "HS512", "typ": "JWT" }
 * Payload:
 * {
 *   "jti":      "<uuid>",
 *   "iss":      "x-user-service",
 *   "sub":      "<userId>",
 *   "iat":      <epochSeconds>,
 *   "exp":      <epochSeconds>,
 *   "tok_typ":  "ACCESS",
 *   "email":    "user@example.com",
 *   "username": "johndoe",
 *   "roles":    ["ROLE_ADMIN", "ROLE_USER"],
 *   "perms":    ["USER:READ", "USER:WRITE", "ROLE:READ"]
 * }
 *
 * ── Cấu trúc REFRESH token ─────────────────────────────────────────────────
 * { "jti", "iss", "sub", "iat", "exp", "tok_typ": "REFRESH" }
 *
 * ── Cấu trúc MFA_PENDING token ─────────────────────────────────────────────
 * { "jti", "sub", "iat", "exp", "tok_typ": "MFA_PENDING", "email", "mfa_type" }
 *
 * ── Cấu trúc RESET / VERIFY token ──────────────────────────────────────────
 * { "jti", "sub", "iat", "exp", "tok_typ": "RESET|VERIFY", "email" }
 *
 * NOTE: Dùng claim name "tok_typ" thay vì "typ" để tránh xung đột với
 *       JOSE header "typ" field của Nimbus.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    // Claim name cho token type — tránh xung đột với JOSE header "typ"
    private static final String CLAIM_TOKEN_TYPE = "tok_typ";
    private static final String CLAIM_EMAIL      = "email";
    private static final String CLAIM_USERNAME   = "username";
    private static final String CLAIM_ROLES      = "roles";
    private static final String CLAIM_PERMS      = "perms";
    private static final String CLAIM_MFA_TYPE   = "mfa_type";

    private final JwtProperties props;

    private MACSigner   signer;
    private MACVerifier verifier;

    @PostConstruct
    void init() throws JOSEException {
        byte[] secretBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        // Nimbus tự validate key length — HS512 cần min 512 bits (64 bytes)
        this.signer   = new MACSigner(secretBytes);
        this.verifier = new MACVerifier(secretBytes);
        log.info("JwtTokenProvider initialised  algorithm=HS512  issuer={}",
                props.getIssuer());
    }

    // =========================================================================
    // GENERATE — public API
    // =========================================================================

    /**
     * Tạo Access token đầy đủ claims: roles + perms.
     */
    public String generateAccessToken(UserEntity user) {
        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toList());

        List<String> perms = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName())
                .distinct()
                .collect(Collectors.toList());

        JWTClaimsSet claims = baseBuilder(user.getId(), props.getAccessTtlSeconds(), TokenType.ACCESS)
                .claim(CLAIM_EMAIL,    user.getEmail())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLES,    roles)
                .claim(CLAIM_PERMS,    perms)
                .build();

        return sign(claims);
    }

    /**
     * Tạo Refresh token — không chứa roles/perms.
     */
    public String generateRefreshToken(UserEntity user) {
        JWTClaimsSet claims = baseBuilder(user.getId(), props.getRefreshTtlSeconds(), TokenType.REFRESH)
                .build();
        return sign(claims);
    }

    /**
     * Tạo MFA Pending token — dùng sau login đúng password, trước khi xác nhận MFA.
     */
    public String generateMfaPendingToken(UserEntity user) {
        JWTClaimsSet claims = baseBuilder(user.getId(), props.getMfaPendingTtlSeconds(), TokenType.MFA_PENDING)
                .claim(CLAIM_EMAIL,    user.getEmail())
                .claim(CLAIM_MFA_TYPE, user.getMfaType() != null ? user.getMfaType().name() : null)
                .build();
        return sign(claims);
    }

    /**
     * Tạo Password Reset token — gửi trong link email.
     */
    public String generateResetToken(UserEntity user) {
        JWTClaimsSet claims = baseBuilder(user.getId(), props.getPasswordResetTtlSeconds(), TokenType.RESET)
                .claim(CLAIM_EMAIL, user.getEmail())
                .build();
        return sign(claims);
    }

    /**
     * Tạo Email Verify token — gửi trong link email xác thực.
     */
    public String generateVerifyToken(UserEntity user) {
        JWTClaimsSet claims = baseBuilder(user.getId(), props.getEmailVerifyTtlSeconds(), TokenType.VERIFY)
                .claim(CLAIM_EMAIL, user.getEmail())
                .build();
        return sign(claims);
    }

    // =========================================================================
    // VALIDATE — trả về ParsedToken đã type-safe
    // =========================================================================

    /**
     * Validate token và trả về {@link ParsedToken}.
     * Throw {@link AppException} với ErrorCode tương ứng nếu thất bại.
     */
    public ParsedToken validate(String rawToken, TokenType expectedType) {
        JWTClaimsSet claims = parseAndVerify(rawToken);

        // Kiểm tra token type
        String actualType = getClaim(claims, CLAIM_TOKEN_TYPE, String.class);
        if (!expectedType.name().equals(actualType)) {
            log.debug("Token type mismatch: expected={} actual={}", expectedType, actualType);
            throw AppException.of(ErrorCode.TOKEN_TYPE_MISMATCH);
        }

        // Kiểm tra expiry (Nimbus đã verify trong parseAndVerify, nhưng explicit check rõ ràng hơn)
        if (claims.getExpirationTime() == null ||
                claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
            throw AppException.of(ErrorCode.TOKEN_EXPIRED);
        }

        return toParsedToken(claims, expectedType);
    }

    /** Shorthand validate theo từng loại */
    public ParsedToken validateAccess(String token)     { return validate(token, TokenType.ACCESS);      }
    public ParsedToken validateRefresh(String token)    { return validate(token, TokenType.REFRESH);     }
    public ParsedToken validateMfaPending(String token) { return validate(token, TokenType.MFA_PENDING); }
    public ParsedToken validateReset(String token)      { return validate(token, TokenType.RESET);       }
    public ParsedToken validateVerify(String token)     { return validate(token, TokenType.VERIFY);      }

    // =========================================================================
    // EXTRACT — không throw khi expired, dùng cho blacklist/cleanup
    // =========================================================================

    /**
     * Lấy JTI mà không validate signature hay expiry.
     * An toàn vì chỉ dùng để lưu vào blacklist sau khi token đã verified trước đó.
     */
    public String extractJti(String rawToken) {
        try {
            return SignedJWT.parse(rawToken).getJWTClaimsSet().getJWTID();
        } catch (ParseException e) {
            log.warn("Cannot extract JTI from token: {}", e.getMessage());
            throw AppException.of(ErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * Lấy expiration kể cả khi token đã hết hạn — dùng để tính TTL cho blacklist.
     */
    public Instant extractExpiration(String rawToken) {
        try {
            Date exp = SignedJWT.parse(rawToken).getJWTClaimsSet().getExpirationTime();
            return exp != null ? exp.toInstant() : Instant.now();
        } catch (ParseException e) {
            log.warn("Cannot extract expiration: {}", e.getMessage());
            return Instant.now();
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Builder chứa claims chung cho mọi token type.
     */
    private JWTClaimsSet.Builder baseBuilder(UUID userId, long ttlSeconds, TokenType type) {
        Instant now       = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        return new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())       // jti — unique token ID
                .issuer(props.getIssuer())                 // iss
                .subject(userId.toString())                // sub = userId
                .issueTime(Date.from(now))                 // iat
                .expirationTime(Date.from(expiresAt))      // exp
                .claim(CLAIM_TOKEN_TYPE, type.name());     // tok_typ
    }

    /**
     * Ký token bằng HS512.
     */
    private String sign(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claims
            );
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            log.error("Failed to sign JWT", e);
            throw AppException.of(ErrorCode.INTERNAL_ERROR).withDetail("JWT signing failed");
        }
    }

    /**
     * Parse và verify signature + expiry.
     * Throw AppException nếu sai signature hoặc hết hạn.
     */
    private JWTClaimsSet parseAndVerify(String rawToken) {
        try {
            SignedJWT jwt = SignedJWT.parse(rawToken);

            // Verify signature
            if (!jwt.verify(verifier)) {
                log.debug("JWT signature verification failed");
                throw AppException.of(ErrorCode.TOKEN_INVALID);
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            // Verify expiry
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.toInstant().isBefore(Instant.now())) {
                throw AppException.of(ErrorCode.TOKEN_EXPIRED);
            }

            // Verify issuer (nếu token có iss claim)
            String iss = claims.getIssuer();
            if (iss != null && !props.getIssuer().equals(iss)) {
                log.debug("JWT issuer mismatch: expected={} actual={}", props.getIssuer(), iss);
                throw AppException.of(ErrorCode.TOKEN_INVALID);
            }

            return claims;

        } catch (ParseException e) {
            log.debug("JWT parse error: {}", e.getMessage());
            throw AppException.of(ErrorCode.TOKEN_INVALID);
        } catch (JOSEException e) {
            log.debug("JWT verify error: {}", e.getMessage());
            throw AppException.of(ErrorCode.TOKEN_INVALID);
        } catch (AppException e) {
            throw e; // re-throw không wrap
        }
    }

    /**
     * Convert {@link JWTClaimsSet} → {@link ParsedToken} type-safe.
     */
    @SuppressWarnings("unchecked")
    private ParsedToken toParsedToken(JWTClaimsSet claims, TokenType type) {
        try {
            return ParsedToken.builder()
                    .jti(claims.getJWTID())
                    .userId(UUID.fromString(claims.getSubject()))
                    .email(getClaim(claims, CLAIM_EMAIL,    String.class))
                    .username(getClaim(claims, CLAIM_USERNAME, String.class))
                    .type(type)
                    .issuedAt(claims.getIssueTime() != null
                            ? claims.getIssueTime().toInstant() : null)
                    .expiresAt(claims.getExpirationTime().toInstant())
                    .roles(getListClaim(claims, CLAIM_ROLES))
                    .perms(getListClaim(claims, CLAIM_PERMS))
                    .mfaType(getClaim(claims, CLAIM_MFA_TYPE, String.class))
                    .build();
        } catch (Exception e) {
            log.error("Failed to map JWT claims to ParsedToken", e);
            throw AppException.of(ErrorCode.TOKEN_INVALID);
        }
    }

    private <T> T getClaim(JWTClaimsSet claims, String name, Class<T> type) {
        try {
            Object val = claims.getClaim(name);
            if (val == null) return null;
            return type.cast(val);
        } catch (ClassCastException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getListClaim(JWTClaimsSet claims, String name) {
        try {
            Object val = claims.getClaim(name);
            if (val instanceof List<?> list) {
                return list.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}

