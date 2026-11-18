package com.learningplatform.backend.module.overview.service;

import com.learningplatform.backend.module.core.model.LearningModule;
import com.learningplatform.backend.module.core.service.ModuleApiRequestValidator;
import com.learningplatform.backend.module.core.service.ModuleCoreService;
import com.learningplatform.backend.module.overview.dto.ModuleOverviewDto;
import com.learningplatform.backend.module.overview.repository.ModuleOverviewRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class ModuleOverviewService {

    private final ModuleCoreService modules;
    private final ModuleOverviewRepository overviews;

    public ModuleOverviewService(ModuleCoreService modules, ModuleOverviewRepository overviews) {
        this.modules = modules;
        this.overviews = overviews;
    }

    public ModuleOverviewDto getOverview(String moduleSlug, String languageCode) {
        ModuleApiRequestValidator.validateLanguageCode(languageCode);
        LearningModule module = modules.getVisibleModule(moduleSlug);
        var overview = overviews.findVisibleByModuleSlugAndLanguage(module.getSlug(), languageCode)
                .orElseThrow(ModuleOverviewService::notFound);
        return new ModuleOverviewDto(module.getSlug(), languageCode, overview.getContent());
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Module overview not found");
    }
}
