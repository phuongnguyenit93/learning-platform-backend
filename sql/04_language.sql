-- Shared language lookup for all localized content (Overview, Knowledge, Quiz, etc.).
-- Run once as the Oracle schema owner after 01_learning_catalog.sql.
-- SQL scripts are intentionally NOT executed by the Spring Boot application.

CREATE TABLE LANGUAGE (
    CODE       VARCHAR2(10 CHAR) NOT NULL,
    NAME       VARCHAR2(100 CHAR) NOT NULL,
    IS_ACTIVE  NUMBER(1) DEFAULT 1 NOT NULL,

    CONSTRAINT PK_LANGUAGE PRIMARY KEY (CODE),
    CONSTRAINT CK_LANGUAGE_ACTIVE CHECK (IS_ACTIVE IN (0, 1))
);

INSERT INTO LANGUAGE (CODE, NAME, IS_ACTIVE) VALUES ('vi', 'Tiếng Việt', 1);
INSERT INTO LANGUAGE (CODE, NAME, IS_ACTIVE) VALUES ('en', 'English', 1);

COMMIT;
