package com.learningplatform.backend.module.reference.service;

import java.net.URI;
import java.util.List;

import com.learningplatform.backend.module.core.model.LearningModule;
import com.learningplatform.backend.module.core.service.ModuleApiRequestValidator;
import com.learningplatform.backend.module.core.service.ModuleCoreService;
import com.learningplatform.backend.module.reference.dto.ModuleReferenceDto;
import com.learningplatform.backend.module.reference.dto.ReferenceItemDto;
import com.learningplatform.backend.module.reference.repository.ModuleReferenceLanguageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class ModuleReferenceService {

    private final ModuleCoreService modules;
    private final ModuleReferenceLanguageRepository translations;

    public ModuleReferenceService(
            ModuleCoreService modules,
            ModuleReferenceLanguageRepository translations) {
        this.modules = modules;
        this.translations = translations;
    }

    public ModuleReferenceDto getReferences(String moduleSlug, String languageCode) {
        ModuleApiRequestValidator.validateLanguageCode(languageCode);
        LearningModule module = modules.getVisibleModule(moduleSlug);
        List<ReferenceItemDto> references = translations
                .findVisibleByModuleAndLanguage(module.getId(), languageCode)
                .stream()
                .map(translation -> {
                    var reference = translation.getReference();
                    String url = reference.getUrl();
                    validateHttpsUrl(url);
                    return new ReferenceItemDto(
                            reference.getId(),
                            translation.getTitle(),
                            url,
                            translation.getDescription() == null ? "" : translation.getDescription(),
                            reference.getSortOrder());
                })
                .toList();

        if (references.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Module references not found");
        }
        return new ModuleReferenceDto(module.getSlug(), languageCode, references);
    }

    private static void validateHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null || uri.getHost().isBlank()
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Reference URL must be an absolute HTTPS URL");
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Invalid stored Module Reference URL");
        }
    }
}
