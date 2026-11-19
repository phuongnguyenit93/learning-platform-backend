package com.learningplatform.backend.shared.auth.repository;

import java.util.Collection;
import java.util.List;

import com.learningplatform.backend.shared.auth.model.AppUserRole;
import com.learningplatform.backend.shared.auth.model.AppUserRoleId;
import com.learningplatform.backend.shared.auth.model.AppUserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRoleRepository extends JpaRepository<AppUserRole, AppUserRoleId> {

    @EntityGraph(attributePaths = "role")
    List<AppUserRole> findByUser_IdIn(Collection<Long> userIds);

    boolean existsByUser_GoogleSubAndUser_StatusAndRole_Code(
            String googleSub, AppUserStatus status, String roleCode);
}
