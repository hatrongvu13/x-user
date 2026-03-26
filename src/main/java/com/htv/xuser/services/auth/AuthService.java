package com.htv.xuser.services.auth;

import com.htv.xuser.model.dto.AuthDto;

import java.util.UUID;

public interface AuthService {
    AuthDto.RegisterResponse register(AuthDto.RegisterRequest req);
    AuthDto.LoginResponse login(AuthDto.LoginRequest req);
    AuthDto.RefreshTokenResponse refreshToken(AuthDto.RefreshTokenRequest req);
    void logout(AuthDto.LogoutRequest req, String rawAccessToken);
    void verifyEmail(AuthDto.VerifyEmailRequest req);
    void resendVerifyEmail(AuthDto.ResendVerifyEmailRequest req);
    void forgotPassword(AuthDto.ForgotPasswordRequest req);
    void resetPassword(AuthDto.ResetPasswordRequest req);
    void changePassword(AuthDto.ChangePasswordRequest req, UUID userId);
}
