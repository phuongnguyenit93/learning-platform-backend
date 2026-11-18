CREATE TABLE LEARNING_PLATFORM.MODULE_REFERENCE_LANGUAGE (
                                                             REFERENCE_ID NUMBER(19) NOT NULL,

                                                             LANGUAGE_CODE VARCHAR2(10 CHAR) NOT NULL,

                                                             TITLE VARCHAR2(300 CHAR) NOT NULL,

                                                             DESCRIPTION VARCHAR2(1000 CHAR),

                                                             CONSTRAINT PK_MODULE_REFERENCE_LANGUAGE
                                                                 PRIMARY KEY (REFERENCE_ID, LANGUAGE_CODE),

                                                             CONSTRAINT FK_MREFL_REFERENCE
                                                                 FOREIGN KEY (REFERENCE_ID)
                                                                     REFERENCES LEARNING_PLATFORM.MODULE_REFERENCE(ID)
                                                                     ON DELETE CASCADE,

                                                             CONSTRAINT FK_MREFL_LANGUAGE
                                                                 FOREIGN KEY (LANGUAGE_CODE)
                                                                     REFERENCES LEARNING_PLATFORM.LANGUAGE(CODE)
);

CREATE INDEX LEARNING_PLATFORM.IX_MREFL_LANGUAGE
    ON LEARNING_PLATFORM.MODULE_REFERENCE_LANGUAGE (
                                                    LANGUAGE_CODE
        );
