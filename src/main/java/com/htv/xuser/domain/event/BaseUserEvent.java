package com.htv.xuser.domain.event;

import java.time.Instant;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// BASE EVENT — tất cả events kế thừa
// ─────────────────────────────────────────────────────────────────────────────
public abstract class BaseUserEvent {
    // Kafka message key = userId (đảm bảo ordering cho cùng 1 user)
    public final String eventId = UUID.randomUUID().toString();
    public final String eventType;     // tên event — xem từng class bên dưới
    public final String userId;        // UUID của user liên quan
    public final Instant occurredAt;
    public final String source = "x-user-service";

    protected BaseUserEvent(String eventType, String userId) {
        this.eventType = eventType;
        this.userId = userId;
        this.occurredAt = Instant.now();
    }
}
