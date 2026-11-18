CREATE TABLE LEARNING_PLATFORM.MODULE_KNOWLEDGE_LANGUAGE (
                                                             KNOWLEDGE_ID NUMBER(19) NOT NULL,

                                                             LANGUAGE_CODE VARCHAR2(10 CHAR) NOT NULL,

                                                             CONTENT CLOB,

                                                             STATUS VARCHAR2(30 CHAR),

                                                             FOCUS VARCHAR2(1000 CHAR),

                                                             PROBLEM_SOLVED VARCHAR2(1000 CHAR),

                                                             MECHANISM VARCHAR2(1000 CHAR),

                                                             APPLICATION_VALUE VARCHAR2(1000 CHAR),

                                                             SUMMARY CLOB,

                                                             CONSTRAINT PK_MODULE_KNOWLEDGE_LANGUAGE
                                                                 PRIMARY KEY (KNOWLEDGE_ID, LANGUAGE_CODE),

                                                             CONSTRAINT FK_MKL_KNOWLEDGE
                                                                 FOREIGN KEY (KNOWLEDGE_ID)
                                                                     REFERENCES LEARNING_PLATFORM.MODULE_KNOWLEDGE(ID)
                                                                     ON DELETE CASCADE,

                                                             CONSTRAINT FK_MKL_LANGUAGE
                                                                 FOREIGN KEY (LANGUAGE_CODE)
                                                                     REFERENCES LEARNING_PLATFORM.LANGUAGE(CODE)
);

CREATE INDEX LEARNING_PLATFORM.IX_MKL_LANGUAGE
    ON LEARNING_PLATFORM.MODULE_KNOWLEDGE_LANGUAGE (
                                                    LANGUAGE_CODE
        );
