package com.learningplatform.backend.module.knowledge.service;

import com.learningplatform.backend.module.core.model.LearningModule;
import com.learningplatform.backend.module.core.service.ModuleApiRequestValidator;
import com.learningplatform.backend.module.core.service.ModuleCoreService;
import com.learningplatform.backend.module.knowledge.dto.KnowledgeMenuNodeDto;
import com.learningplatform.backend.module.knowledge.dto.ModuleKnowledgeDto;
import com.learningplatform.backend.module.knowledge.model.ModuleKnowledgeLanguage;
import com.learningplatform.backend.module.knowledge.repository.ModuleKnowledgeLanguageRepository;
import com.learningplatform.backend.module.knowledge.repository.ModuleKnowledgeRepository;
import com.learningplatform.backend.module.menu.model.MenuNodeType;
import com.learningplatform.backend.module.menu.model.ModuleMenuLanguage;
import com.learningplatform.backend.module.menu.repository.ModuleMenuLanguageRepository;
import com.learningplatform.backend.module.menu.repository.ModuleMenuRepository;
import com.learningplatform.backend.shared.language.repository.LanguageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Returns WRITTEN Markdown lessons even when optional QA metadata is missing. */
@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class ModuleKnowledgeService {

    private final ModuleCoreService modules;
    private final LanguageRepository languages;
    private final ModuleMenuRepository menus;
    private final ModuleMenuLanguageRepository menuLanguages;
    private final ModuleKnowledgeRepository knowledge;
    private final ModuleKnowledgeLanguageRepository knowledgeLanguages;

    public ModuleKnowledgeService(
            ModuleCoreService modules,
            LanguageRepository languages,
            ModuleMenuRepository menus,
            ModuleMenuLanguageRepository menuLanguages,
            ModuleKnowledgeRepository knowledge,
            ModuleKnowledgeLanguageRepository knowledgeLanguages) {
        this.modules = modules;
        this.languages = languages;
        this.menus = menus;
        this.menuLanguages = menuLanguages;
        this.knowledge = knowledge;
        this.knowledgeLanguages = knowledgeLanguages;
    }

    public ModuleKnowledgeDto getKnowledge(
            String moduleSlug, String chapterSlug, String sectionSlug, String languageCode) {
        ModuleApiRequestValidator.validateSlug(moduleSlug, 200);
        ModuleApiRequestValidator.validateSlug(chapterSlug, 150);
        ModuleApiRequestValidator.validateSlug(sectionSlug, 150);
        ModuleApiRequestValidator.validateLanguageCode(languageCode);
        LearningModule module = modules.getVisibleModule(moduleSlug);
        if (languages.findByCodeAndActiveTrue(languageCode).isEmpty()) {
            throw notFound();
        }

        // All joins are checked at every level; a Section slug or Knowledge ID
        // on its own is insufficient to authorize the requested Module.
        var chapter = menus.findActiveChapterByModuleAndSlug(
                module.getId(), MenuNodeType.CHAPTER, chapterSlug).orElseThrow(ModuleKnowledgeService::notFound);
        var section = menus.findActiveSectionByChapterAndSlug(
                chapter.getId(), MenuNodeType.CHAPTER, MenuNodeType.SECTION, sectionSlug)
                .orElseThrow(ModuleKnowledgeService::notFound);
        ModuleMenuLanguage chapterTitle = menuLanguages.findVisibleByMenuAndLanguage(
                chapter.getId(), languageCode).orElseThrow(ModuleKnowledgeService::notFound);
        ModuleMenuLanguage sectionTitle = menuLanguages.findVisibleByMenuAndLanguage(
                section.getId(), languageCode).orElseThrow(ModuleKnowledgeService::notFound);
        var lesson = knowledge.findByMenuIdAndActiveTrue(section.getId())
                .orElseThrow(ModuleKnowledgeService::notFound);
        var translation = knowledgeLanguages.findVisibleByKnowledgeAndLanguage(
                lesson.getId(), languageCode).orElseThrow(ModuleKnowledgeService::notFound);
        if (lesson.getDifficult() == null || !readyToRead(translation)) {
            throw notFound();
        }
        return new ModuleKnowledgeDto(
                module.getSlug(), languageCode,
                new KnowledgeMenuNodeDto(
                        chapter.getId(), chapter.getSlug(), chapterTitle.getTitle(), chapterTitle.getDescription()),
                new KnowledgeMenuNodeDto(
                        section.getId(), section.getSlug(), sectionTitle.getTitle(), sectionTitle.getDescription()),
                lesson.getId(), lesson.getDifficult(),
                translation.getContent(), KnowledgeMetadataValue.optional(translation.getFocus()),
                KnowledgeMetadataValue.optional(translation.getProblemSolved()),
                KnowledgeMetadataValue.optional(translation.getMechanism()),
                KnowledgeMetadataValue.optional(translation.getApplicationValue()),
                KnowledgeMetadataValue.optional(translation.getSummary()));
    }

    /** Must match the CLOB-aware REGEXP_LIKE availability projection in ModuleKnowledgeRepository. */
    private static boolean readyToRead(ModuleKnowledgeLanguage translation) {
        return "WRITTEN".equals(translation.getStatus())
                && translation.getContent() != null
                && !translation.getContent().isBlank();
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Published lesson not found");
    }
}
