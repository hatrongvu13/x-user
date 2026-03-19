package com.htv.xuser.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.htv.xuser.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * ApiResponse<T> — Response wrapper chuẩn cho toàn MSA
 *
 * Cấu trúc thành công:
 * <pre>
 * {
 *   "success": true,
 *   "code": 200,
 *   "message": "Tạo người dùng thành công",
 *   "data": { ... },
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 * </pre>
 *
 * Cấu trúc lỗi nghiệp vụ:
 * <pre>
 * {
 *   "success": false,
 *   "code": 4001,
 *   "message": "Không tìm thấy người dùng",
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 * </pre>
 *
 * Cấu trúc lỗi validation:
 * <pre>
 * {
 *   "success": false,
 *   "code": 1000,
 *   "message": "Dữ liệu không hợp lệ",
 *   "errors": { "email": "Email không đúng định dạng", "password": "..." },
 *   "timestamp": "2024-01-01T00:00:00Z"
 * }
 * </pre>
 *
 * Factory methods:
 * <pre>
 *   ApiResponse.ok(data)
 *   ApiResponse.ok(data, message)
 *   ApiResponse.created(data, message)
 *   ApiResponse.noContent(message)
 *   ApiResponse.error(errorCode, message)
 *   ApiResponse.validationError(message, fieldErrors)
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final int     code;       // HTTP status code (success) hoặc ErrorCode.code (failure)
    private final String  message;
    private final T       data;
    private final Object  errors;     // Map<String,String> cho validation errors
    private final Instant timestamp;

    private ApiResponse(boolean success, int code, String message, T data, Object errors) {
        this.success   = success;
        this.code      = code;
        this.message   = message;
        this.data      = data;
        this.errors    = errors;
        this.timestamp = Instant.now();
    }

    // =========================================================================
    // SUCCESS FACTORIES
    // =========================================================================

    /** 200 OK với data */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, null, data, null);
    }

    /** 200 OK với data + message */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, 200, message, data, null);
    }

    /** 201 Created với data + message */
    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, 201, message, data, null);
    }

    /** 200 OK không có data — dùng cho DELETE, logout, ... */
    public static <T> ApiResponse<T> noContent(String message) {
        return new ApiResponse<>(true, 200, message, null, null);
    }

    // =========================================================================
    // ERROR FACTORIES
    // =========================================================================

    /** Lỗi nghiệp vụ với ErrorCode — message đã được resolve i18n ở tầng handler */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, errorCode.getCode(), message, null, null);
    }

    /** Lỗi validation — kèm field errors */
    public static <T> ApiResponse<T> validationError(String message, Object fieldErrors) {
        return new ApiResponse<>(false, ErrorCode.VALIDATION_FAILED.getCode(), message, null, fieldErrors);
    }

    /** Lỗi generic với code tự chọn */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message, null, null);
    }
}
