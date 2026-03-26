package com.htv.xuser.model.repository;

import com.htv.xuser.model.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    @Query("""
            SELECT r FROM RoleEntity r
            LEFT JOIN FETCH r.permissions
            WHERE r.id = :id
              AND r.deleted = false
            """)
    Optional<RoleEntity> findByIdWithPermissions(@Param("id") UUID id);

    Optional<RoleEntity> findByNameAndDeletedFalse(String name);

    boolean existsByNameAndDeletedFalse(String name);

    List<RoleEntity> findAllByDeletedFalseOrderByName();
}
