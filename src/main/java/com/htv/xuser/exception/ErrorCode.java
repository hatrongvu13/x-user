package com.htv.xuser.exception;


import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * ErrorCode — Mã lỗi chuẩn hoá dùng chung trong toàn MSA
 *
 * Quy ước:
 *   Prefix 4 chữ số đầu = nhóm:
 *     1000-1999  Validation / Bad request
 *     2000-2999  Authentication / Token
 *     3000-3999  Authorization / Permission
 *     4000-4999  Resource (User, Role, Permission)
 *     5000-5999  Business rule
 *     9000-9999  System / Internal
 *
 *   i18n key = "error." + code.name().toLowerCase().replace('_', '.')
 *   VD: ErrorCode.USER_NOT_FOUND → key "error.user.not.found"
 *
 * Cách dùng:
 *   throw AppException.of(ErrorCode.USER_NOT_FOUND);
 *   throw AppException.of(ErrorCode.TOKEN_EXPIRED, "access_token");
 */

@Getter
public enum ErrorCode {

    // =========================================================================
    // 1000 — VALIDATION
    // =========================================================================
    VALIDATION_FAILED       (1000, HttpStatus.UNPROCESSABLE_ENTITY, "error.validation.failed"),
    INVALID_REQUEST         (1001, HttpStatus.BAD_REQUEST,          "error.invalid.request"),
    INVALID_FIELD_FORMAT    (1002, HttpStatus.BAD_REQUEST,          "error.invalid.field.format"),
    MISSING_REQUIRED_FIELD  (1003, HttpStatus.BAD_REQUEST,          "error.missing.required.field"),
    PASSWORD_MISMATCH       (1004, HttpStatus.BAD_REQUEST,          "error.password.mismatch"),
    PASSWORD_TOO_WEAK       (1005, HttpStatus.BAD_REQUEST,          "error.password.too.weak"),
    PASSWORD_SAME_AS_OLD    (1006, HttpStatus.BAD_REQUEST,          "error.password.same.as.old"),
    PASSWORD_INCORRECT      (1007, HttpStatus.BAD_REQUEST,          "error.password.incorrect"),

    // =========================================================================
    // 2000 — AUTHENTICATION
    // =========================================================================
    INVALID_CREDENTIALS     (2000, HttpStatus.UNAUTHORIZED,         "error.invalid.credentials"),
    TOKEN_INVALID           (2001, HttpStatus.UNAUTHORIZED,         "error.token.invalid"),
    TOKEN_EXPIRED           (2002, HttpStatus.UNAUTHORIZED,         "error.token.expired"),
    TOKEN_REVOKED           (2003, HttpStatus.UNAUTHORIZED,         "error.token.revoked"),
    TOKEN_TYPE_MISMATCH     (2004, HttpStatus.UNAUTHORIZED,         "error.token.type.mismatch"),
    MFA_CODE_INVALID        (2010, HttpStatus.UNAUTHORIZED,         "error.mfa.code.invalid"),
    MFA_CODE_EXPIRED        (2011, HttpStatus.UNAUTHORIZED,         "error.mfa.code.expired"),
    MFA_SESSION_EXPIRED     (2012, HttpStatus.UNAUTHORIZED,         "error.mfa.session.expired"),
    MFA_ATTEMPTS_EXCEEDED   (2013, HttpStatus.TOO_MANY_REQUESTS,    "error.mfa.attempts.exceeded"),
    EMAIL_NOT_VERIFIED      (2020, HttpStatus.UNAUTHORIZED,         "error.email.not.verified"),
    EMAIL_VERIFY_TOKEN_INVALID(2021, HttpStatus.BAD_REQUEST,        "error.email.verify.token.invalid"),
    EMAIL_ALREADY_VERIFIED  (2022, HttpStatus.BAD_REQUEST,          "error.email.already.verified"),
    RESET_TOKEN_INVALID     (2030, HttpStatus.BAD_REQUEST,          "error.reset.token.invalid"),
    RESET_TOKEN_USED        (2031, HttpStatus.BAD_REQUEST,          "error.reset.token.used"),

    // =========================================================================
    // 3000 — AUTHORIZATION
    // =========================================================================
    ACCESS_DENIED           (3000, HttpStatus.FORBIDDEN,            "error.access.denied"),
    INSUFFICIENT_PERMISSION (3001, HttpStatus.FORBIDDEN,            "error.insufficient.permission"),
    RESOURCE_FORBIDDEN      (3002, HttpStatus.FORBIDDEN,            "error.resource.forbidden"),

    // =========================================================================
    // 4000 — RESOURCE
    // =========================================================================
    USER_NOT_FOUND          (4001, HttpStatus.NOT_FOUND,            "error.user.not.found"),
    USER_EMAIL_EXISTS       (4002, HttpStatus.CONFLICT,             "error.user.email.exists"),
    USER_USERNAME_EXISTS    (4003, HttpStatus.CONFLICT,             "error.user.username.exists"),
    USER_ACCOUNT_LOCKED     (4004, HttpStatus.FORBIDDEN,            "error.user.account.locked"),
    USER_ACCOUNT_INACTIVE   (4005, HttpStatus.FORBIDDEN,            "error.user.account.inactive"),
    USER_ACCOUNT_SUSPENDED  (4006, HttpStatus.FORBIDDEN,            "error.user.account.suspended"),
    USER_ACCOUNT_BANNED     (4007, HttpStatus.FORBIDDEN,            "error.user.account.banned"),
    ROLE_NOT_FOUND          (4011, HttpStatus.NOT_FOUND,            "error.role.not.found"),
    ROLE_NAME_EXISTS        (4012, HttpStatus.CONFLICT,             "error.role.name.exists"),
    ROLE_ALREADY_ASSIGNED   (4013, HttpStatus.CONFLICT,             "error.role.already.assigned"),
    ROLE_NOT_ASSIGNED       (4014, HttpStatus.BAD_REQUEST,          "error.role.not.assigned"),
    PERMISSION_NOT_FOUND    (4021, HttpStatus.NOT_FOUND,            "error.permission.not.found"),
    PERMISSION_NAME_EXISTS  (4022, HttpStatus.CONFLICT,             "error.permission.name.exists"),

    // =========================================================================
    // 5000 — BUSINESS RULE
    // =========================================================================
    SYSTEM_ROLE_IMMUTABLE   (5001, HttpStatus.FORBIDDEN,            "error.system.role.immutable"),
    MFA_ALREADY_ENABLED     (5010, HttpStatus.CONFLICT,             "error.mfa.already.enabled"),
    MFA_NOT_ENABLED         (5011, HttpStatus.BAD_REQUEST,          "error.mfa.not.enabled"),
    EMAIL_RESEND_TOO_SOON   (5020, HttpStatus.TOO_MANY_REQUESTS,    "error.email.resend.too.soon"),

    // =========================================================================
    // 9000 — SYSTEM
    // =========================================================================
    INTERNAL_ERROR          (9000, HttpStatus.INTERNAL_SERVER_ERROR,"error.internal"),
    SERVICE_UNAVAILABLE     (9001, HttpStatus.SERVICE_UNAVAILABLE,  "error.service.unavailable"),
    ;

    private final int        code;
    private final HttpStatus httpStatus;
    private final String     messageKey;

    ErrorCode(int code, HttpStatus httpStatus, String messageKey) {
        this.code       = code;
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }
}
