-- Localized Overview Markdown for MODULE_STRUCTURE nodes.
-- Prerequisites: 03_module_structure.sql and 04_language.sql.
-- One row per (module, language); missing translations have no row.
-- Run manually as the Oracle schema owner. No import/write runs in Spring Boot.

CREATE TABLE MODULE_OVERVIEW (
    ID_STRUCTURE     NUMBER(19) NOT NULL,
    LANGUAGE_CODE    VARCHAR2(10 CHAR) NOT NULL,
    MARKDOWN_CONTENT CLOB NOT NULL,

    CONSTRAINT PK_MODULE_OVERVIEW PRIMARY KEY (ID_STRUCTURE, LANGUAGE_CODE),
    CONSTRAINT FK_MO_STRUCTURE FOREIGN KEY (ID_STRUCTURE)
        REFERENCES MODULE_STRUCTURE (ID_STRUCTURE),
    CONSTRAINT FK_MO_LANGUAGE FOREIGN KEY (LANGUAGE_CODE)
        REFERENCES LANGUAGE (CODE)
);

-- Composite PK supports a single-module all-locales lookup.
-- This index supports queries for all modules in a given language.
CREATE INDEX IX_MO_LANGUAGE ON MODULE_OVERVIEW (LANGUAGE_CODE);

-- Note: the foreign key permits both GROUP and MODULE nodes; the importing
-- process must validate NODE_TYPE = 'MODULE' before writing an Overview.
