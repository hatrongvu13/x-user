package com.htv.xuser.model.repository;

import com.htv.xuser.model.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    // ── Tìm kèm roles + permissions (1 query, tránh N+1) ─────────────────────

    @Query("""
            SELECT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.email = :email
              AND u.deleted = false
            """)
    Optional<UserEntity> findByEmailWithRoles(@Param("email") String email);

    @Query("""
            SELECT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.id = :id
              AND u.deleted = false
            """)
    Optional<UserEntity> findByIdWithRoles(@Param("id") UUID id);

    // ── Tìm đơn giản ──────────────────────────────────────────────────────────

    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    Optional<UserEntity> findByEmailVerifyTokenAndDeletedFalse(String token);

    Optional<UserEntity> findByPasswordResetTokenAndDeletedFalse(String token);

    // ── Kiểm tra tồn tại ──────────────────────────────────────────────────────

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByUsernameAndDeletedFalse(String username);

    // ── Danh sách phân trang + filter ─────────────────────────────────────────

    @Query("""
            SELECT u FROM UserEntity u
            WHERE u.deleted = false
              AND (:status IS NULL OR u.status = :status)
              AND (:keyword IS NULL
                   OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.username)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<UserEntity> search(
            @Param("status") UserEntity.UserStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    // ── Cập nhật trực tiếp (không load entity) ───────────────────────────────

    @Modifying
    @Query("UPDATE UserEntity u SET u.failedLoginAttempts = 0, u.lockedUntil = NULL WHERE u.id = :id")
    void resetFailedAttempts(@Param("id") UUID id);
}
