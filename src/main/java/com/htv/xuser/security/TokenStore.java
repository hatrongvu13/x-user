package com.htv.xuser.security;

import com.htv.xuser.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TokenStore — quản lý vòng đời token (blacklist + valid set)
 *
 * ── Hiện tại: In-Memory (ConcurrentHashMap) ────────────────────────────────
 * Dùng cho dev/test, không persistence qua restart.
 * TTL được implement bằng cách lưu Instant expiry và check khi đọc.
 *
 * ── Khi deploy production với Redis ────────────────────────────────────────
 * Thay class này bằng implementation dùng StringRedisTemplate.
 * API không thay đổi — service layer không cần sửa.
 *
 * Redis key schema (để sẵn documentation):
 * <pre>
 *   jwt:bl:{jti}              → "1"  TTL = token remaining life  (blacklist)
 *   jwt:rt:{userId}:{jti}     → "1"  TTL = refresh TTL           (valid refresh tokens)
 *   jwt:mfa:{userId}:{jti}    → "1"  TTL = mfa TTL               (single-use)
 *   jwt:rst:{userId}:{jti}    → "1"  TTL = reset TTL             (single-use)
 *   jwt:vfy:{userId}:{jti}    → "1"  TTL = verify TTL            (single-use)
 * </pre>
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenStore {
    private final JwtProperties props;

    // ConcurrentHashMap<key, expiresAt>
    // Key format giống Redis để dễ migrate sau này
    private final Map<String, Instant> store = new ConcurrentHashMap<>();

    private static final String BL  = "jwt:bl:";
    private static final String RT  = "jwt:rt:";
    private static final String MFA = "jwt:mfa:";
    private static final String RST = "jwt:rst:";
    private static final String VFY = "jwt:vfy:";

    // =========================================================================
    // BLACKLIST — access/refresh token bị revoke
    // =========================================================================

    /**
     * Blacklist token theo JTI.
     * TTL = thời gian còn lại của token để tiết kiệm memory.
     */
    public void blacklist(String jti, Instant expiresAt) {
        if (expiresAt.isAfter(Instant.now())) {
            store.put(BL + jti, expiresAt);
            log.debug("Blacklisted jti={} until={}", jti, expiresAt);
        }
        evictExpired(); // opportunistic cleanup
    }

    public boolean isBlacklisted(String jti) {
        return isValid(BL + jti);
    }

    // =========================================================================
    // REFRESH TOKEN
    // =========================================================================

    public void saveRefresh(UUID userId, String jti) {
        String key = RT + userId + ":" + jti;
        Instant exp = Instant.now().plusSeconds(props.getRefreshTtlSeconds());
        store.put(key, exp);
        log.debug("Saved refresh token jti={} userId={}", jti, userId);
    }

    public boolean isRefreshValid(UUID userId, String jti) {
        return isValid(RT + userId + ":" + jti);
    }

    public void revokeRefresh(UUID userId, String jti) {
        store.remove(RT + userId + ":" + jti);
        log.debug("Revoked refresh token jti={} userId={}", jti, userId);
    }

    /**
     * Thu hồi tất cả refresh token của user (logout all devices).
     */
    public void revokeAllRefresh(UUID userId) {
        String prefix = RT + userId + ":";
        Set<String> keys = store.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .collect(Collectors.toSet());
        keys.forEach(store::remove);
        log.info("Revoked {} refresh tokens for userId={}", keys.size(), userId);
    }

    // =========================================================================
    // MFA PENDING — single-use
    // =========================================================================

    public void saveMfa(UUID userId, String jti) {
        store.put(MFA + userId + ":" + jti,
                Instant.now().plusSeconds(props.getMfaPendingTtlSeconds()));
    }

    public boolean isMfaValid(UUID userId, String jti) {
        return isValid(MFA + userId + ":" + jti);
    }

    /** Single-use: xoá ngay sau khi dùng */
    public void consumeMfa(UUID userId, String jti) {
        store.remove(MFA + userId + ":" + jti);
    }

    // =========================================================================
    // RESET TOKEN — single-use
    // =========================================================================

    public void saveReset(UUID userId, String jti) {
        store.put(RST + userId + ":" + jti,
                Instant.now().plusSeconds(props.getPasswordResetTtlSeconds()));
    }

    public boolean isResetValid(UUID userId, String jti) {
        return isValid(RST + userId + ":" + jti);
    }

    public void consumeReset(UUID userId, String jti) {
        store.remove(RST + userId + ":" + jti);
    }

    // =========================================================================
    // VERIFY TOKEN — single-use
    // =========================================================================

    public void saveVerify(UUID userId, String jti) {
        store.put(VFY + userId + ":" + jti,
                Instant.now().plusSeconds(props.getEmailVerifyTtlSeconds()));
    }

    public boolean isVerifyValid(UUID userId, String jti) {
        return isValid(VFY + userId + ":" + jti);
    }

    public void consumeVerify(UUID userId, String jti) {
        store.remove(VFY + userId + ":" + jti);
    }

    // =========================================================================
    // PRIVATE
    // =========================================================================

    /** Kiểm tra key tồn tại và chưa expire */
    private boolean isValid(String key) {
        Instant exp = store.get(key);
        if (exp == null) return false;
        if (exp.isBefore(Instant.now())) {
            store.remove(key); // lazy eviction
            return false;
        }
        return true;
    }

    /**
     * Evict tất cả entry đã hết hạn.
     * Gọi opportunistically để tránh memory leak trên in-memory store.
     * Redis tự xử lý TTL nên không cần logic này khi migrate.
     */
    private void evictExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
