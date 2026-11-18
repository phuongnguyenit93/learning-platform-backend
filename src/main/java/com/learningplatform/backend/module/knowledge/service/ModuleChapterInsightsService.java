package com.learningplatform.backend.module.knowledge.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.learningplatform.backend.module.core.model.LearningModule;
import com.learningplatform.backend.module.core.service.ModuleApiRequestValidator;
import com.learningplatform.backend.module.core.service.ModuleCoreService;
import com.learningplatform.backend.module.knowledge.dto.KnowledgeInsightLessonDto;
import com.learningplatform.backend.module.knowledge.dto.KnowledgeInsightMetadataDto;
import com.learningplatform.backend.module.knowledge.dto.KnowledgeMenuNodeDto;
import com.learningplatform.backend.module.knowledge.dto.ModuleChapterInsightsDto;
import com.learningplatform.backend.module.knowledge.model.Difficulty;
import com.learningplatform.backend.module.knowledge.repository.ModuleKnowledgeLanguageRepository;
import com.learningplatform.backend.module.knowledge.repository.ModuleKnowledgeRepository;
import com.learningplatform.backend.module.menu.model.MenuNodeType;
import com.learningplatform.backend.module.menu.model.ModuleMenu;
import com.learningplatform.backend.module.menu.model.ModuleMenuLanguage;
import com.learningplatform.backend.module.menu.repository.ModuleMenuLanguageRepository;
import com.learningplatform.backend.module.menu.repository.ModuleMenuRepository;
import com.learningplatform.backend.shared.language.repository.LanguageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Chapter-scoped comparison of published lesson metadata without loading CLOBs. */
@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class ModuleChapterInsightsService {

    private static final int IN_BATCH_SIZE = 900;

    private final ModuleCoreService modules;
    private final LanguageRepository languages;
    private final ModuleMenuRepository menus;
    private final ModuleMenuLanguageRepository menuLanguages;
    private final ModuleKnowledgeRepository knowledge;
    private final ModuleKnowledgeLanguageRepository knowledgeLanguages;

    public ModuleChapterInsightsService(
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

    public ModuleChapterInsightsDto getInsights(
            String moduleSlug, String chapterSlug, String languageCode) {
        ModuleApiRequestValidator.validateSlug(moduleSlug, 200);
        ModuleApiRequestValidator.validateSlug(chapterSlug, 150);
        ModuleApiRequestValidator.validateLanguageCode(languageCode);
        LearningModule module = modules.getVisibleModule(moduleSlug);
        if (languages.findByCodeAndActiveTrue(languageCode).isEmpty()) {
            throw notFound();
        }
        var chapter = menus.findActiveChapterByModuleAndSlug(
                module.getId(), MenuNodeType.CHAPTER, chapterSlug)
                .orElseThrow(ModuleChapterInsightsService::notFound);
        var chapterTitle = menuLanguages.findVisibleByMenuAndLanguage(
                chapter.getId(), languageCode).orElseThrow(ModuleChapterInsightsService::notFound);

        List<ModuleMenu> sections = menus.findActiveChildren(
                chapter.getId(), MenuNodeType.CHAPTER, MenuNodeType.SECTION);
        if (sections.isEmpty()) {
            return response(module, languageCode, chapter, chapterTitle, List.of());
        }
        Map<Long, ModuleMenuLanguage> titles = new HashMap<>();
        for (int start = 0; start < sections.size(); start += IN_BATCH_SIZE) {
            var ids = sections.subList(start, Math.min(start + IN_BATCH_SIZE, sections.size()))
                    .stream().map(ModuleMenu::getId).toList();
            for (var title : menuLanguages.findVisibleByMenuIdsAndLanguage(ids, languageCode)) {
                titles.put(title.getId().getMenuId(), title);
            }
        }

        List<Long> localizedIds = sections.stream()
                .filter(section -> titles.containsKey(section.getId()))
                .map(ModuleMenu::getId).toList();
        Map<Long, ModuleKnowledgeRepository.KnowledgeAvailability> published = new HashMap<>();
        for (int start = 0; start < localizedIds.size(); start += IN_BATCH_SIZE) {
            var ids = localizedIds.subList(start, Math.min(start + IN_BATCH_SIZE, localizedIds.size()));
            for (var available : knowledge.findAvailabilityBySectionIdsAndLanguage(ids, languageCode)) {
                if (Integer.valueOf(1).equals(available.getAvailable())) {
                    published.put(available.getMenuId(), available);
                }
            }
        }

        List<Long> knowledgeIds = published.values().stream()
                .map(ModuleKnowledgeRepository.KnowledgeAvailability::getKnowledgeId).toList();
        Map<Long, KnowledgeInsightMetadataDto> metadata = new HashMap<>();
        for (int start = 0; start < knowledgeIds.size(); start += IN_BATCH_SIZE) {
            var ids = knowledgeIds.subList(start, Math.min(start + IN_BATCH_SIZE, knowledgeIds.size()));
            for (var item : knowledgeLanguages.findInsightMetadataByKnowledgeIdsAndLanguage(ids, languageCode)) {
                metadata.put(item.knowledgeId(), item);
            }
        }

        List<KnowledgeInsightLessonDto> lessons = new ArrayList<>();
        for (ModuleMenu section : sections) {
            var available = published.get(section.getId());
            if (available == null) {
                continue;
            }
            var title = titles.get(section.getId());
            var item = metadata.get(available.getKnowledgeId());
            if (title == null || item == null) {
                continue;
            }
            lessons.add(new KnowledgeInsightLessonDto(
                    section.getId(), section.getSlug(), title.getTitle(), section.getSortOrder(),
                    available.getKnowledgeId(), Difficulty.valueOf(available.getDifficulty()),
                    KnowledgeMetadataValue.optional(item.focus()),
                    KnowledgeMetadataValue.optional(item.problemSolved()),
                    KnowledgeMetadataValue.optional(item.mechanism()),
                    KnowledgeMetadataValue.optional(item.applicationValue())));
        }
        return response(module, languageCode, chapter, chapterTitle, List.copyOf(lessons));
    }

    private static ModuleChapterInsightsDto response(
            LearningModule module, String languageCode, ModuleMenu chapter,
            ModuleMenuLanguage chapterTitle, List<KnowledgeInsightLessonDto> lessons) {
        return new ModuleChapterInsightsDto(
                module.getSlug(), languageCode,
                new KnowledgeMenuNodeDto(
                        chapter.getId(), chapter.getSlug(), chapterTitle.getTitle(), chapterTitle.getDescription()),
                lessons);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Localized chapter insights not found");
    }
}
