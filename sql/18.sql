CREATE TABLE LEARNING_PLATFORM.MODULE_MENU_LANGUAGE (
                                                        MENU_ID NUMBER(19) NOT NULL,

                                                        LANGUAGE_CODE VARCHAR2(10 CHAR) NOT NULL,

                                                        TITLE VARCHAR2(300 CHAR) NOT NULL,

                                                        DESCRIPTION VARCHAR2(1000 CHAR),

                                                        CONSTRAINT PK_MODULE_MENU_LANGUAGE
                                                            PRIMARY KEY (MENU_ID, LANGUAGE_CODE),

                                                        CONSTRAINT FK_MML_MENU
                                                            FOREIGN KEY (MENU_ID)
                                                                REFERENCES LEARNING_PLATFORM.MODULE_MENU(ID)
                                                                ON DELETE CASCADE,

                                                        CONSTRAINT FK_MML_LANGUAGE
                                                            FOREIGN KEY (LANGUAGE_CODE)
                                                                REFERENCES LEARNING_PLATFORM.LANGUAGE(CODE)
);

CREATE INDEX LEARNING_PLATFORM.IX_MML_LANGUAGE
    ON LEARNING_PLATFORM.MODULE_MENU_LANGUAGE (
                                               LANGUAGE_CODE
        );