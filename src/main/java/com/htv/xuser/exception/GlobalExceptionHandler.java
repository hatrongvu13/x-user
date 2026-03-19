package com.htv.xuser.exception;

import com.htv.xuser.model.response.ApiResponse;
import com.htv.xuser.services.msg.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * GlobalExceptionHandler — Xử lý tất cả exception, trả về ApiResponse chuẩn
 *
 * Thứ tự ưu tiên xử lý:
 *  1. AppException        — lỗi nghiệp vụ có ErrorCode
 *  2. Validation          — @Valid thất bại
 *  3. Spring Security     — 401 / 403
 *  4. Spring MVC          — 404, 405, bad request
 *  5. RuntimeException    — lỗi không mong đợi
 *  6. Exception           — fallback cuối cùng
 */

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService msg;

    // =========================================================================
    // 1. AppException — lỗi nghiệp vụ có ErrorCode
    // =========================================================================

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        String message = msg.get(ex.getMessageKey(), ex.getArgs());
        log.debug("[AppException] code={} message={}", ex.getCode(), message);
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), message));
    }

    // =========================================================================
    // 2. Validation — @Valid thất bại
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        e -> resolveValidationMessage(e),
                        (first, second) -> first  // giữ lỗi đầu tiên nếu trùng field
                ));

        String message = msg.get(ErrorCode.VALIDATION_FAILED.getMessageKey());
        log.debug("[Validation] fields={}", fieldErrors.keySet());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.validationError(message, fieldErrors));
    }

    // =========================================================================
    // 3. Spring Security
    // =========================================================================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        log.debug("[Auth] Unauthenticated: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.TOKEN_INVALID,
                        msg.get(ErrorCode.TOKEN_INVALID.getMessageKey())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.debug("[Auth] Forbidden: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.ACCESS_DENIED,
                        msg.get(ErrorCode.ACCESS_DENIED.getMessageKey())));
    }

    // =========================================================================
    // 4. Spring MVC
    // =========================================================================

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, msg.get("error.endpoint.not.found")));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(405, msg.get("error.method.not.allowed")));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST,
                        msg.get(ErrorCode.INVALID_REQUEST.getMessageKey())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.MISSING_REQUIRED_FIELD,
                        msg.get(ErrorCode.MISSING_REQUIRED_FIELD.getMessageKey(), ex.getParameterName())));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_FIELD_FORMAT,
                        msg.get(ErrorCode.INVALID_FIELD_FORMAT.getMessageKey(), ex.getName())));
    }

    // =========================================================================
    // 5. RuntimeException
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[IllegalArgument] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.INVALID_REQUEST,
                        msg.get(ErrorCode.INVALID_REQUEST.getMessageKey())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.error("[IllegalState] {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR,
                        msg.get(ErrorCode.INTERNAL_ERROR.getMessageKey())));
    }

    // =========================================================================
    // 6. Fallback
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("[UNHANDLED] {}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR,
                        msg.get(ErrorCode.INTERNAL_ERROR.getMessageKey())));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String resolveValidationMessage(FieldError error) {
        String raw = error.getDefaultMessage();
        if (raw == null) return error.getField() + " is invalid";

        // Message dạng "{validation.required}" → bỏ { }
        String key = raw.replace("{", "").replace("}", "");
        try {
            String fieldLabel = msg.getOrDefault("field." + error.getField(), error.getField());
            return msg.get(key, fieldLabel);
        } catch (Exception e) {
            return raw;
        }
    }
}
