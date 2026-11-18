CREATE TABLE LEARNING_PLATFORM.MODULE_ROADMAP_LANGUAGE (
                                                           ROADMAP_ID NUMBER(19) NOT NULL,

                                                           LANGUAGE_CODE VARCHAR2(10 CHAR) NOT NULL,

                                                           TITLE VARCHAR2(300 CHAR) NOT NULL,

                                                           PURPOSE VARCHAR2(1000 CHAR) NOT NULL,

                                                           OBJECTIVE VARCHAR2(2000 CHAR),

                                                           CONSTRAINT PK_MODULE_ROADMAP_LANGUAGE
                                                               PRIMARY KEY (ROADMAP_ID, LANGUAGE_CODE),

                                                           CONSTRAINT FK_MRL_ROADMAP
                                                               FOREIGN KEY (ROADMAP_ID)
                                                                   REFERENCES LEARNING_PLATFORM.MODULE_ROADMAP(ID)
                                                                   ON DELETE CASCADE,

                                                           CONSTRAINT FK_MRL_LANGUAGE
                                                               FOREIGN KEY (LANGUAGE_CODE)
                                                                   REFERENCES LEARNING_PLATFORM.LANGUAGE(CODE)
);

CREATE INDEX LEARNING_PLATFORM.IX_MRL_LANGUAGE
    ON LEARNING_PLATFORM.MODULE_ROADMAP_LANGUAGE (
                                                  LANGUAGE_CODE
        );
