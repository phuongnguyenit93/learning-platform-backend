package com.learningplatform.backend.shared.language.repository;

import java.util.List;
import java.util.Optional;

import com.learningplatform.backend.shared.language.model.Language;
import org.springframework.data.repository.Repository;

/** Shared read-only language lookup. */
public interface LanguageRepository extends Repository<Language, String> {

    List<Language> findByActiveTrueOrderByCodeAsc();

    Optional<Language> findByCodeAndActiveTrue(String code);
}
