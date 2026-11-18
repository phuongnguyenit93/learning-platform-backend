package com.learningplatform.backend.module.menu.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.learningplatform.backend.module.core.model.LearningModule;
import com.learningplatform.backend.module.core.service.ModuleApiRequestValidator;
import com.learningplatform.backend.module.core.service.ModuleCoreService;
import com.learningplatform.backend.module.knowledge.model.Difficulty;
import com.learningplatform.backend.module.knowledge.repository.ModuleKnowledgeRepository;
import com.learningplatform.backend.module.menu.dto.ModuleMenuChapterDto;
import com.learningplatform.backend.module.menu.dto.ModuleMenuDto;
import com.learningplatform.backend.module.menu.dto.ModuleMenuSectionDto;
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

/**
 * Renders the visible, two-level localized menu without reading Markdown or
 * summary LOB columns. An untranslated node is not displayed in another locale.
 */
@Service
@Profile("oracle")
@Transactional(readOnly = true)
public class ModuleMenuService {

    /** Oracle supports no more than 1000 expressions in a single IN list. */
    private static final int IN_BATCH_SIZE = 900;

    private final ModuleCoreService modules;
    private final LanguageRepository languages;
    private final ModuleMenuRepository menus;
    private final ModuleMenuLanguageRepository menuLanguages;
    private final ModuleKnowledgeRepository knowledge;

    public ModuleMenuService(
            ModuleCoreService modules,
            LanguageRepository languages,
            ModuleMenuRepository menus,
            ModuleMenuLanguageRepository menuLanguages,
            ModuleKnowledgeRepository knowledge) {
        this.modules = modules;
        this.languages = languages;
        this.menus = menus;
        this.menuLanguages = menuLanguages;
        this.knowledge = knowledge;
    }

    public ModuleMenuDto getMenu(String moduleSlug, String languageCode) {
        ModuleApiRequestValidator.validateSlug(moduleSlug, 200);
        ModuleApiRequestValidator.validateLanguageCode(languageCode);
        LearningModule module = modules.getVisibleModule(moduleSlug);
        if (languages.findByCodeAndActiveTrue(languageCode).isEmpty()) {
            throw notFound();
        }

        List<ModuleMenu> chapters = menus.findActiveByModuleAndNodeType(
                module.getId(), MenuNodeType.CHAPTER);
        if (chapters.isEmpty()) {
            return new ModuleMenuDto(module.getSlug(), languageCode, List.of());
        }

        List<Long> chapterIds = chapters.stream().map(ModuleMenu::getId).toList();
        List<ModuleMenu> sections = new ArrayList<>();
        for (List<Long> batch : batches(chapterIds)) {
            sections.addAll(menus.findActiveChildrenForChapters(
                    batch, MenuNodeType.CHAPTER, MenuNodeType.SECTION));
        }

        List<Long> allMenuIds = new ArrayList<>(chapterIds);
        sections.forEach(section -> allMenuIds.add(section.getId()));
        Map<Long, ModuleMenuLanguage> translations = new HashMap<>();
        for (List<Long> batch : batches(allMenuIds)) {
            for (ModuleMenuLanguage translation : menuLanguages.findVisibleByMenuIdsAndLanguage(
                    batch, languageCode)) {
                translations.put(translation.getId().getMenuId(), translation);
            }
        }

        // A module with chapters but no titles in the requested locale has no
        // localized menu. Do not silently fall back to vi or another language.
        if (chapters.stream().noneMatch(chapter -> translations.containsKey(chapter.getId()))) {
            throw notFound();
        }

        List<Long> localizedSectionIds = sections.stream()
                .filter(section -> translations.containsKey(section.getId()))
                .map(ModuleMenu::getId).toList();
        Map<Long, ModuleKnowledgeRepository.KnowledgeAvailability> availability = new HashMap<>();
        for (List<Long> batch : batches(localizedSectionIds)) {
            for (var item : knowledge.findAvailabilityBySectionIdsAndLanguage(batch, languageCode)) {
                availability.put(item.getMenuId(), item);
            }
        }

        Map<Long, List<ModuleMenuSectionDto>> sectionsByChapter = new HashMap<>();
        for (ModuleMenu section : sections) {
            ModuleMenuLanguage title = translations.get(section.getId());
            if (title == null) {
                continue;
            }
            var published = availability.get(section.getId());
            boolean available = published != null && Integer.valueOf(1).equals(published.getAvailable());
            Difficulty difficulty = published == null || published.getDifficulty() == null
                    ? null : Difficulty.valueOf(published.getDifficulty());
            var result = new ModuleMenuSectionDto(
                    section.getId(), section.getSlug(), title.getTitle(),
                    title.getDescription(), section.getSortOrder(),
                    published == null ? null : published.getKnowledgeId(),
                    difficulty, available,
                    published != null && Integer.valueOf(1).equals(published.getHasSummary()));
            sectionsByChapter.computeIfAbsent(section.getMenuParentId(), ignored -> new ArrayList<>())
                    .add(result);
        }

        List<ModuleMenuChapterDto> result = new ArrayList<>();
        for (ModuleMenu chapter : chapters) {
            var title = translations.get(chapter.getId());
            if (title == null) {
                continue;
            }
            List<ModuleMenuSectionDto> chapterSections = sectionsByChapter.getOrDefault(
                    chapter.getId(), List.of());
            int lessonCount = (int) chapterSections.stream().filter(ModuleMenuSectionDto::available).count();
            result.add(new ModuleMenuChapterDto(
                    chapter.getId(), chapter.getSlug(), title.getTitle(),
                    title.getDescription(), chapter.getSortOrder(), lessonCount,
                    List.copyOf(chapterSections)));
        }
        return new ModuleMenuDto(module.getSlug(), languageCode, List.copyOf(result));
    }

    private static <T> List<List<T>> batches(List<T> items) {
        List<List<T>> result = new ArrayList<>();
        for (int index = 0; index < items.size(); index += IN_BATCH_SIZE) {
            result.add(items.subList(index, Math.min(index + IN_BATCH_SIZE, items.size())));
        }
        return result;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Localized module menu not found");
    }
}
