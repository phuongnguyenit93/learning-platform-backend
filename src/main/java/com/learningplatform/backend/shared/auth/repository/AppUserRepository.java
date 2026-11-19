package com.learningplatform.backend.shared.auth.repository;

import java.util.Optional;

import com.learningplatform.backend.shared.auth.model.AppUser;
import com.learningplatform.backend.shared.auth.model.AppUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByGoogleSub(String googleSub);

    Optional<AppUser> findByGoogleSubAndStatus(String googleSub, AppUserStatus status);
}
