package com.learningplatform.backend.shared.auth.repository;

import java.util.Optional;

import com.learningplatform.backend.shared.auth.model.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByCode(String code);
}
