package com.htv.xuser.services.auth;

import com.htv.xuser.config.SecurityProperties;
import com.htv.xuser.exception.AppException;
import com.htv.xuser.exception.ErrorCode;
import com.htv.xuser.model.dto.AuthDto;
import com.htv.xuser.model.entity.PermissionEntity;
import com.htv.xuser.model.entity.RoleEntity;
import com.htv.xuser.model.entity.UserEntity;
import com.htv.xuser.model.repository.RoleRepository;
import com.htv.xuser.model.repository.UserRepository;
import com.htv.xuser.security.JwtTokenProvider;
import com.htv.xuser.security.ParsedToken;
import com.htv.xuser.security.TokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider tokenProvider;
    private final TokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProp;

    @Override
    @Transactional
    public AuthDto.RegisterResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByEmailAndDeletedFalse(req.getEmail())) {
            throw AppException.of(ErrorCode.USER_EMAIL_EXISTS);
        }
        if (userRepository.existsByUsernameAndDeletedFalse(req.getUsername())) {
            throw AppException.of(ErrorCode.USER_USERNAME_EXISTS);
        }

        RoleEntity defaultRole = roleRepository.findByNameAndDeletedFalse("ROLE_USER")
                .orElse(null);

        var user = UserEntity.builder()
                .email(req.getEmail())
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phoneNumber(req.getPhoneNumber())
                .status(UserEntity.UserStatus.PENDING)
                .emailVerified(false)
                .build();

        if (defaultRole != null) {
            user.addRole(defaultRole);
        }
        // Tạo email verify token
        String verifyToken = UUID.randomUUID().toString();
        user.setEmailVerifyToken(verifyToken);
        user.setEmailVerifyTokenExpiresAt(
                Instant.now().plusSeconds(86400)); // 24h

        user = userRepository.save(user);

        // TODO: gửi email xác thực (publish event)
        log.info("User registered: userId={} email={}", user.getId(), user.getEmail());

        return AuthDto.RegisterResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .emailVerified(false)
                .message("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.")
                .build();
    }

    @Override
    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        // Tìm user
        UserEntity user = userRepository.findByEmailWithRoles(req.getEmail())
                .orElseThrow(() -> AppException.of(ErrorCode.INVALID_CREDENTIALS));

        // Kiểm tra tài khoản bị khoá
        if (user.isLocked()) {
            throw AppException.of(ErrorCode.USER_ACCOUNT_LOCKED);
        }

        // Kiểm tra bị ban/suspend
        if (user.getStatus() == UserEntity.UserStatus.BANNED) {
            throw AppException.of(ErrorCode.USER_ACCOUNT_BANNED);
        }
        if (user.getStatus() == UserEntity.UserStatus.SUSPENDED) {
            throw AppException.of(ErrorCode.USER_ACCOUNT_SUSPENDED);
        }
        if (user.getStatus() == UserEntity.UserStatus.INACTIVE) {
            throw AppException.of(ErrorCode.USER_ACCOUNT_INACTIVE);
        }

        // Kiểm tra email chưa verify
        if (!user.isEmailVerified()) {
            throw AppException.of(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // Kiểm tra password
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw AppException.of(ErrorCode.INVALID_CREDENTIALS);
        }

        // Reset failed attempts khi login thành công
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // MFA bật → trả mfaPendingToken
        if (user.isMfaEnabled()) {
            String mfaPendingToken = tokenProvider.generateMfaPendingToken(user);
            tokenStore.saveMfa(user.getId(), tokenProvider.extractJti(mfaPendingToken));

            return AuthDto.LoginResponse.builder()
                    .mfaRequired(true)
                    .mfaType(user.getMfaType().name())
                    .mfaPendingToken(mfaPendingToken)
                    .user(toUserInfo(user))
                    .build();
        }

        return buildTokenResponse(user);
    }

    @Override
    @Transactional
    public AuthDto.RefreshTokenResponse refreshToken(AuthDto.RefreshTokenRequest req) {
        ParsedToken parsed;
        try {
            parsed = tokenProvider.validateRefresh(req.getRefreshToken());
        } catch (AppException e) {
            throw AppException.of(ErrorCode.TOKEN_INVALID);
        }

        UUID userId = parsed.getUserId();
        String jti = parsed.getJti();

        if (!tokenStore.isRefreshValid(userId, jti)) {
            throw AppException.of(ErrorCode.TOKEN_REVOKED);
        }

        // Rotation: revoke cũ
        tokenStore.revokeRefresh(userId, jti);

        UserEntity user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw AppException.of(ErrorCode.USER_ACCOUNT_INACTIVE);
        }

        // Tạo token pair mới
        String newAccess = tokenProvider.generateAccessToken(user);
        String newRefresh = tokenProvider.generateRefreshToken(user);
        tokenStore.saveRefresh(userId, tokenProvider.extractJti(newRefresh));

        return AuthDto.RefreshTokenResponse.builder()
                .accessToken(newAccess)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .accessTokenExpiresAt(tokenProvider.extractExpiration(newAccess))
                .refreshTokenExpiresAt(tokenProvider.extractExpiration(newRefresh))
                .build();
    }

    @Override
    @Transactional
    public void logout(AuthDto.LogoutRequest req, String rawAccessToken) {
        // Blacklist access token hiện tại
        if (rawAccessToken != null) {
            try {
                String jti = tokenProvider.extractJti(rawAccessToken);
                Instant expiry = tokenProvider.extractExpiration(rawAccessToken);
                tokenStore.blacklist(jti, expiry);
            } catch (Exception ignored) {
            }
        }

        // Revoke refresh token
        try {
            ParsedToken parsed = tokenProvider.validateRefresh(req.getRefreshToken());
            UUID userId = parsed.getUserId();

            if (req.isLogoutAllDevices()) {
                tokenStore.revokeAllRefresh(userId);
                log.info("Logged out all devices userId={}", userId);
            } else {
                tokenStore.revokeRefresh(userId, parsed.getJti());
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    @Transactional
    public void verifyEmail(AuthDto.VerifyEmailRequest req) {
        UserEntity user = userRepository.findByEmailVerifyTokenAndDeletedFalse(req.getToken())
                .orElseThrow(() -> AppException.of(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID));

        if (user.isEmailVerified()) {
            throw AppException.of(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        if (user.getEmailVerifyTokenExpiresAt() != null
                && Instant.now().isAfter(user.getEmailVerifyTokenExpiresAt())) {
            throw AppException.of(ErrorCode.EMAIL_VERIFY_TOKEN_INVALID);
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setEmailVerifyToken(null);
        user.setEmailVerifyTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified userId={}", user.getId());
    }

    @Override
    @Transactional
    public void resendVerifyEmail(AuthDto.ResendVerifyEmailRequest req) {
        UserEntity user = userRepository.findByEmailAndDeletedFalse(req.getEmail())
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));

        if (user.isEmailVerified()) {
            throw AppException.of(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        String newToken = UUID.randomUUID().toString();
        user.setEmailVerifyToken(newToken);
        user.setEmailVerifyTokenExpiresAt(Instant.now().plusSeconds(86400));
        userRepository.save(user);

        // TODO: publish event gửi mail
        log.info("Resend verify email userId={}", user.getId());
    }

    @Override
    @Transactional
    public void forgotPassword(AuthDto.ForgotPasswordRequest req) {
        // Không tiết lộ email có tồn tại hay không
        userRepository.findByEmailAndDeletedFalse(req.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiresAt(Instant.now().plusSeconds(3600)); // 1h
            userRepository.save(user);
            // TODO: publish event gửi mail
            log.info("Password reset requested userId={}", user.getUsername());
        });
    }

    @Override
    public void resetPassword(AuthDto.ResetPasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw AppException.of(ErrorCode.PASSWORD_MISMATCH);
        }

        UserEntity user = userRepository.findByPasswordResetTokenAndDeletedFalse(req.getToken())
                .orElseThrow(() -> AppException.of(ErrorCode.RESET_TOKEN_INVALID));

        if (user.getPasswordResetTokenExpiresAt() != null
                && Instant.now().isAfter(user.getPasswordResetTokenExpiresAt())) {
            throw AppException.of(ErrorCode.RESET_TOKEN_INVALID);
        }

        if (passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw AppException.of(ErrorCode.PASSWORD_SAME_AS_OLD);
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);

        // Thu hồi tất cả refresh token sau đổi mật khẩu
        tokenStore.revokeAllRefresh(user.getId());

        log.info("Password reset userId={}", user.getId());
    }

    @Override
    public void changePassword(AuthDto.ChangePasswordRequest req, UUID userId) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw AppException.of(ErrorCode.PASSWORD_MISMATCH);
        }

        UserEntity user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw AppException.of(ErrorCode.PASSWORD_INCORRECT);
        }

        if (passwordEncoder.matches(req.getNewPassword(), user.getPasswordHash())) {
            throw AppException.of(ErrorCode.PASSWORD_SAME_AS_OLD);
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        // Thu hồi tất cả sessions
        tokenStore.revokeAllRefresh(userId);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void handleFailedLogin(UserEntity user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= securityProp.getMaxFailedLoginAttempts()) {
            long lockSeconds = securityProp.getAccountLockDurationSeconds();
            user.setLockedUntil(Instant.now().plusSeconds(lockSeconds));
            log.warn("Account locked userId={} attempts={}", user.getId(), attempts);
        }

        userRepository.save(user);
    }

    private AuthDto.LoginResponse buildTokenResponse(UserEntity user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        tokenStore.saveRefresh(user.getId(), tokenProvider.extractJti(refreshToken));

        return AuthDto.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresAt(tokenProvider.extractExpiration(accessToken))
                .refreshTokenExpiresAt(tokenProvider.extractExpiration(refreshToken))
                .mfaRequired(false)
                .user(toUserInfo(user))
                .build();
    }

    public AuthDto.UserInfo toUserInfo(UserEntity user) {
        Set<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName).collect(Collectors.toSet());
        Set<String> perms = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(PermissionEntity::getName).collect(Collectors.toSet());

        return AuthDto.UserInfo.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .mfaEnabled(user.isMfaEnabled())
                .roles(roles)
                .permissions(perms)
                .build();
    }
}
