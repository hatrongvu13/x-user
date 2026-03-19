package com.htv.xuser.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * AppException — Exception nghiệp vụ duy nhất của toàn dự án
 *
 * Mọi lỗi nghiệp vụ đều throw AppException với ErrorCode tương ứng.
 * GlobalExceptionHandler bắt và map về ApiResponse chuẩn.
 *
 * Cách dùng:
 * <pre>
 *   // Đơn giản
 *   throw AppException.of(ErrorCode.USER_NOT_FOUND);
 *
 *   // Kèm i18n args
 *   throw AppException.of(ErrorCode.USER_ACCOUNT_LOCKED, "15 phút");
 *
 *   // Kèm detail (debug, không expose ra client)
 *   throw AppException.of(ErrorCode.INTERNAL_ERROR).withDetail("Redis timeout");
 * </pre>
 */

@Getter
public class AppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Object[]  args;        // i18n args cho message key
    private       String    detail;      // optional debug detail — không expose ra client

    private AppException(ErrorCode errorCode, Object... args) {
        super(errorCode.getMessageKey());
        this.errorCode = errorCode;
        this.args      = args;
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static AppException of(ErrorCode errorCode, Object... args) {
        return new AppException(errorCode, args);
    }

    // Shorthand thường dùng
    public static AppException userNotFound()            { return of(ErrorCode.USER_NOT_FOUND); }
    public static AppException roleNotFound()            { return of(ErrorCode.ROLE_NOT_FOUND); }
    public static AppException permissionNotFound()      { return of(ErrorCode.PERMISSION_NOT_FOUND); }
    public static AppException invalidCredentials()      { return of(ErrorCode.INVALID_CREDENTIALS); }
    public static AppException tokenInvalid()            { return of(ErrorCode.TOKEN_INVALID); }
    public static AppException tokenExpired()            { return of(ErrorCode.TOKEN_EXPIRED); }
    public static AppException accessDenied()            { return of(ErrorCode.ACCESS_DENIED); }

    // ── Builder-style detail ─────────────────────────────────────────────────

    public AppException withDetail(String detail) {
        this.detail = detail;
        return this;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    public int getCode() {
        return errorCode.getCode();
    }

    public String getMessageKey() {
        return errorCode.getMessageKey();
    }
}
