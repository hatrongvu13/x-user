package com.htv.xuser.model.dto;

/**
 * TokenType — phân loại JWT, ghi vào claim "typ"
 *
 * <pre>
 * ACCESS      → gọi API thông thường      TTL: 15 phút
 * REFRESH     → xin access token mới     TTL: 7 ngày  (rotation)
 * MFA_PENDING → bước 2 MFA sau login     TTL: 5 phút  (single-use)
 * RESET       → reset password qua email TTL: 1 giờ   (single-use)
 * VERIFY      → xác thực email           TTL: 24 giờ  (single-use)
 * </pre>
 */
public enum TokenType {
    ACCESS,
    REFRESH,
    MFA_PENDING,
    RESET,
    VERIFY
}

