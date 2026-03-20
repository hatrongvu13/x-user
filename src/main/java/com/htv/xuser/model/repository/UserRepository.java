package com.htv.xuser.model.repository;

import com.htv.xuser.model.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserEntity> findByEmail(String email);
}
