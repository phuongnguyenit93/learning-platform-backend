package com.learningplatform.backend.module_overview.repository;

import java.util.Optional;

import com.learningplatform.backend.module_overview.model.ModuleOverview;
import com.learningplatform.backend.module_overview.model.ModuleOverviewId;
import org.springframework.data.repository.Repository;

/** No save/delete surface: content is loaded into Oracle outside this backend. */
public interface ModuleOverviewRepository extends Repository<ModuleOverview, ModuleOverviewId> {

    Optional<ModuleOverview> findById(ModuleOverviewId id);
}
