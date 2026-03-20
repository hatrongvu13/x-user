package com.htv.xuser.services.security;

import com.htv.xuser.model.entity.UserEntity;
import com.htv.xuser.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * UserDetailsServiceImpl — cầu nối giữa Spring Security và UserEntity.
 *
 * Dùng bởi DaoAuthenticationProvider trong AuthenticationManager
 * khi xử lý POST /api/v1/auth/login.
 *
 * Username = email (không phải username field).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        user.getRoles().forEach(role -> {
            // Role — VD: ROLE_ADMIN
            authorities.add(new SimpleGrantedAuthority(role.getName()));
            // Permissions — VD: USER:READ
            role.getPermissions().forEach(perm ->
                    authorities.add(new SimpleGrantedAuthority(perm.getName()))
            );
        });

        return User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!user.isEmailVerified())         // chưa verify email = disabled
                .accountLocked(user.isLocked())
                .accountExpired(false)
                .credentialsExpired(false)
                .build();
    }
}
