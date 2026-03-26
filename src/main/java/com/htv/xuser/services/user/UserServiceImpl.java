package com.htv.xuser.services.user;

import com.htv.xuser.exception.AppException;
import com.htv.xuser.exception.ErrorCode;
import com.htv.xuser.model.dto.UserDto;
import com.htv.xuser.model.entity.RoleEntity;
import com.htv.xuser.model.entity.UserEntity;
import com.htv.xuser.model.repository.RoleRepository;
import com.htv.xuser.model.repository.UserRepository;
import com.htv.xuser.model.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto.ProfileResponse getMyProfile(String email) {
        UserEntity user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserDto.ProfileResponse updateMyProfile(String email, UserDto.UpdateRequest req) {
        UserEntity user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));
        applyUpdate(user, req);
        return toProfileResponse(userRepository.save(user));
    }

    @Override
    public PageResponse<UserDto.SummaryResponse> search(UserDto.SearchRequest req) {
        var sort     = Sort.by(
                "desc".equalsIgnoreCase(req.getSortDir())
                        ? Sort.Direction.DESC : Sort.Direction.ASC,
                req.getSortBy());
        var pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        return PageResponse.of(
                userRepository.search(req.getStatus(), req.getKeyword(), pageable)
                        .map(this::toSummaryResponse));
    }

    @Override
    public UserDto.DetailResponse getById(UUID id) {
        UserEntity user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));
        return toDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDto.DetailResponse create(UserDto.AdminCreateRequest req) {
        if (userRepository.existsByEmailAndDeletedFalse(req.getEmail())) {
            throw AppException.of(ErrorCode.USER_EMAIL_EXISTS);
        }
        if (userRepository.existsByUsernameAndDeletedFalse(req.getUsername())) {
            throw AppException.of(ErrorCode.USER_USERNAME_EXISTS);
        }

        var user = UserEntity.builder()
                .email(req.getEmail())
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .status(UserEntity.UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        if (req.getRoleIds() != null) {
            req.getRoleIds().forEach(roleId ->
                    roleRepository.findByIdWithPermissions(roleId).ifPresent(user::addRole));
        }

        return toDetailResponse(userRepository.save(user));
    }

    @Override
    public UserDto.DetailResponse update(UUID id, UserDto.UpdateRequest req) {
        UserEntity user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));
        applyUpdate(user, req);
        return toDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto.DetailResponse updateStatus(UUID id, UserDto.UpdateStatusRequest req) {
        UserEntity user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));
        user.setStatus(req.getStatus());
        return toDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));
        user.setDeleted(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserDto.DetailResponse assignRole(UUID userId, UUID roleId) {
        UserEntity user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));
        RoleEntity role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> AppException.of(ErrorCode.ROLE_NOT_FOUND));

        boolean alreadyHas = user.getRoles().stream()
                .anyMatch(r -> r.getId().equals(roleId));
        if (alreadyHas) throw AppException.of(ErrorCode.ROLE_ALREADY_ASSIGNED);

        user.addRole(role);
        return toDetailResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto.DetailResponse removeRole(UUID userId, UUID roleId) {
        UserEntity user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> AppException.of(ErrorCode.USER_NOT_FOUND));
        RoleEntity role = roleRepository.findById(roleId)
                .orElseThrow(() -> AppException.of(ErrorCode.ROLE_NOT_FOUND));

        boolean has = user.getRoles().stream().anyMatch(r -> r.getId().equals(roleId));
        if (!has) throw AppException.of(ErrorCode.ROLE_NOT_ASSIGNED);

        user.removeRole(role);
        return toDetailResponse(userRepository.save(user));
    }

    // =========================================================================
    // MAPPERS
    // =========================================================================

    private void applyUpdate(UserEntity user, UserDto.UpdateRequest req) {
        if (req.getFirstName()   != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()    != null) user.setLastName(req.getLastName());
        if (req.getPhoneNumber() != null) user.setPhoneNumber(req.getPhoneNumber());
        if (req.getAvatarUrl()   != null) user.setAvatarUrl(req.getAvatarUrl());
    }

    private Set<String> roles(UserEntity u) {
        return u.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toSet());
    }

    private Set<String> perms(UserEntity u) {
        return u.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getName()).collect(Collectors.toSet());
    }

    private String fullName(UserEntity u) {
        if (u.getFirstName() == null && u.getLastName() == null) return null;
        return ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                + (u.getLastName()  != null ? u.getLastName()  : "")).strip();
    }

    public UserDto.ProfileResponse toProfileResponse(UserEntity u) {
        return UserDto.ProfileResponse.builder()
                .id(u.getId()).email(u.getEmail()).username(u.getUsername())
                .firstName(u.getFirstName()).lastName(u.getLastName())
                .fullName(fullName(u)).phoneNumber(u.getPhoneNumber())
                .avatarUrl(u.getAvatarUrl()).status(u.getStatus())
                .emailVerified(u.isEmailVerified()).mfaEnabled(u.isMfaEnabled())
                .mfaType(u.getMfaType() != null ? u.getMfaType().name() : null)
                .lastLoginAt(u.getLastLoginAt())
                .roles(roles(u)).permissions(perms(u))
                .build();
    }

    public UserDto.SummaryResponse toSummaryResponse(UserEntity u) {
        return UserDto.SummaryResponse.builder()
                .id(u.getId()).email(u.getEmail()).username(u.getUsername())
                .firstName(u.getFirstName()).lastName(u.getLastName())
                .fullName(fullName(u)).avatarUrl(u.getAvatarUrl())
                .status(u.getStatus()).emailVerified(u.isEmailVerified())
                .mfaEnabled(u.isMfaEnabled()).createdAt(u.getCreatedAt())
                .roles(roles(u))
                .build();
    }

    public UserDto.DetailResponse toDetailResponse(UserEntity u) {
        return UserDto.DetailResponse.builder()
                .id(u.getId()).email(u.getEmail()).username(u.getUsername())
                .firstName(u.getFirstName()).lastName(u.getLastName())
                .fullName(fullName(u)).phoneNumber(u.getPhoneNumber())
                .avatarUrl(u.getAvatarUrl()).status(u.getStatus())
                .emailVerified(u.isEmailVerified()).emailVerifiedAt(u.getEmailVerifiedAt())
                .mfaEnabled(u.isMfaEnabled())
                .mfaType(u.getMfaType() != null ? u.getMfaType().name() : null)
                .failedLoginAttempts(u.getFailedLoginAttempts())
                .lockedUntil(u.getLockedUntil()).lastLoginAt(u.getLastLoginAt())
                .lastLoginIp(u.getLastLoginIp()).passwordChangedAt(u.getPasswordChangedAt())
                .createdAt(u.getCreatedAt()).updatedAt(u.getUpdatedAt())
                .createdBy(u.getCreatedBy())
                .roles(roles(u)).permissions(perms(u))
                .build();
    }
}
