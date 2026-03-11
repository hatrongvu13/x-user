package com.htv.xuser.domain.shared;

import java.time.Instant;

// ─────────────────────────────────────────────────────────────────────────────
// SHARED MODEL 6: UserEventPayload
// Dùng bởi: Kafka consumers trong các service khác
// Nhận từ: Kafka topics xsystem.user.*
// ─────────────────────────────────────────────────────────────────────────────
public record UserEventPayload(
        String eventId,
        String eventType,
        String userId,
        String email,
        Object data,           // cast tùy eventType
        Instant occurredAt,
        String source
) {
}
