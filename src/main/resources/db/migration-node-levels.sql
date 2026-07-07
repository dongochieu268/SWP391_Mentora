/* =====================================================================
   Mentora LMS - Node Level Learning System (WS-A foundation)

   Additive migration only: creates the 6 new tables and alters
   node_progress / node_contents / bank_questions.

   Does NOT drop branching tables or columns — that happens in the
   final "remove branching" step after all workstreams are merged.

   Idempotent: safe to run multiple times.
   Run AFTER db/schema.sql on the MentoraDB database.
   ===================================================================== */

USE [MentoraDB];
GO

/* ---------------------------------------------------------------------
   1. materials
   Subject-scoped grouping layer over the subject's single QuestionBank.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.materials', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.materials (
        id          INT IDENTITY(1,1) NOT NULL,
        subject_id  INT NOT NULL,
        title       NVARCHAR(255) NOT NULL,
        description NVARCHAR(MAX) NULL,
        created_by  INT NOT NULL,
        created_at  DATETIME2 NOT NULL CONSTRAINT DF_materials_created_at DEFAULT (SYSDATETIME()),
        CONSTRAINT PK_materials PRIMARY KEY (id),
        CONSTRAINT FK_materials_subject FOREIGN KEY (subject_id) REFERENCES dbo.subjects(id),
        CONSTRAINT FK_materials_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id)
    );

    CREATE INDEX IX_materials_subject ON dbo.materials(subject_id);
END;
GO

/* ---------------------------------------------------------------------
   2. node_levels
   Ordered difficulty tiers within a LearningNode.
   Total question count of a level = SUM(level_materials.question_count)
   (deliberately not stored here — single source of truth).
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.node_levels', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.node_levels (
        id               INT IDENTITY(1,1) NOT NULL,
        learning_node_id INT NOT NULL,
        level_number     INT NOT NULL,
        title            NVARCHAR(255) NULL,
        max_score        DECIMAL(5,2) NOT NULL,
        passing_score    DECIMAL(5,2) NOT NULL,
        max_attempts     INT NULL,
        duration_minutes INT NULL,
        CONSTRAINT PK_node_levels PRIMARY KEY (id),
        CONSTRAINT uq_node_level UNIQUE (learning_node_id, level_number),
        CONSTRAINT FK_node_levels_node FOREIGN KEY (learning_node_id) REFERENCES dbo.learning_nodes(id),
        CONSTRAINT CHK_node_levels_level_number CHECK (level_number >= 1),
        CONSTRAINT CHK_node_levels_max_score CHECK (max_score > 0),
        CONSTRAINT CHK_node_levels_passing_score CHECK (passing_score > 0 AND passing_score <= max_score),
        CONSTRAINT CHK_node_levels_max_attempts CHECK (max_attempts IS NULL OR max_attempts >= 1),
        CONSTRAINT CHK_node_levels_duration CHECK (duration_minutes IS NULL OR duration_minutes >= 1)
    );
END;
GO

/* ---------------------------------------------------------------------
   3. level_materials
   Bridge NodeLevel -> Material with an exact question count
   (no percentages, no difficulty split — see spec OQ7).
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.level_materials', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.level_materials (
        id             INT IDENTITY(1,1) NOT NULL,
        node_level_id  INT NOT NULL,
        material_id    INT NOT NULL,
        question_count INT NOT NULL,
        CONSTRAINT PK_level_materials PRIMARY KEY (id),
        CONSTRAINT uq_level_material UNIQUE (node_level_id, material_id),
        CONSTRAINT FK_level_materials_level FOREIGN KEY (node_level_id) REFERENCES dbo.node_levels(id),
        CONSTRAINT FK_level_materials_material FOREIGN KEY (material_id) REFERENCES dbo.materials(id),
        CONSTRAINT CHK_level_materials_question_count CHECK (question_count >= 1)
    );
END;
GO

/* ---------------------------------------------------------------------
   4. node_level_attempts
   One student's single attempt at one level. Grading is immediate at
   submit — no separate GRADED status (matches assessment_attempts).
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.node_level_attempts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.node_level_attempts (
        id             INT IDENTITY(1,1) NOT NULL,
        node_level_id  INT NOT NULL,
        student_id     INT NOT NULL,
        classroom_id   INT NOT NULL,
        attempt_number INT NOT NULL,
        score          DECIMAL(5,2) NULL,
        status         NVARCHAR(20) NOT NULL CONSTRAINT DF_nla_status DEFAULT (N'IN_PROGRESS'),
        started_at     DATETIME2 NOT NULL,
        submitted_at   DATETIME2 NULL,
        CONSTRAINT PK_node_level_attempts PRIMARY KEY (id),
        CONSTRAINT uq_level_attempt UNIQUE (node_level_id, student_id, classroom_id, attempt_number),
        CONSTRAINT FK_nla_level FOREIGN KEY (node_level_id) REFERENCES dbo.node_levels(id),
        CONSTRAINT FK_nla_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_nla_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT CHK_nla_status CHECK (status IN (N'IN_PROGRESS', N'SUBMITTED')),
        CONSTRAINT CHK_nla_attempt_number CHECK (attempt_number >= 1)
    );

    CREATE INDEX IX_nla_student_level
        ON dbo.node_level_attempts(node_level_id, student_id, classroom_id, status);
END;
GO

/* ---------------------------------------------------------------------
   5. attempt_questions
   Per-attempt snapshot of a generated question. Content is copied from
   bank_questions at generation time so later bank edits never affect
   in-flight or historical attempts.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.attempt_questions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.attempt_questions (
        id                      INT IDENTITY(1,1) NOT NULL,
        attempt_id              INT NOT NULL,
        source_bank_question_id INT NULL,
        content                 NVARCHAR(2000) NOT NULL,
        difficulty              NVARCHAR(20) NOT NULL,
        display_order           INT NOT NULL,
        CONSTRAINT PK_attempt_questions PRIMARY KEY (id),
        CONSTRAINT FK_aq_attempt FOREIGN KEY (attempt_id) REFERENCES dbo.node_level_attempts(id),
        CONSTRAINT FK_aq_source_bank_question
            FOREIGN KEY (source_bank_question_id) REFERENCES dbo.bank_questions(id),
        CONSTRAINT CHK_aq_display_order CHECK (display_order >= 0)
    );

    CREATE INDEX IX_aq_attempt_order ON dbo.attempt_questions(attempt_id, display_order);
END;
GO

/* ---------------------------------------------------------------------
   6. attempt_question_options
   Snapshot of one answer option, shuffled per attempt.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.attempt_question_options', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.attempt_question_options (
        id                  INT IDENTITY(1,1) NOT NULL,
        attempt_question_id INT NOT NULL,
        content             NVARCHAR(1000) NOT NULL,
        is_correct          BIT NOT NULL,
        display_order       INT NOT NULL,
        CONSTRAINT PK_attempt_question_options PRIMARY KEY (id),
        CONSTRAINT FK_aqo_question
            FOREIGN KEY (attempt_question_id) REFERENCES dbo.attempt_questions(id) ON DELETE CASCADE,
        CONSTRAINT CHK_aqo_display_order CHECK (display_order >= 0)
    );

    CREATE INDEX IX_aqo_question_order
        ON dbo.attempt_question_options(attempt_question_id, display_order);
END;
GO

/* =====================================================================
   Alter existing tables
   ===================================================================== */
SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    /* node_progress: best score tracking across level attempts. */
    IF COL_LENGTH(N'dbo.node_progress', N'best_score') IS NULL
        ALTER TABLE dbo.node_progress ADD best_score DECIMAL(5,2) NULL;

    IF COL_LENGTH(N'dbo.node_progress', N'best_level_number') IS NULL
        ALTER TABLE dbo.node_progress ADD best_level_number INT NULL;

    /* bank_questions: optional material tag. NULL = invisible to level tests. */
    IF COL_LENGTH(N'dbo.bank_questions', N'material_id') IS NULL
        ALTER TABLE dbo.bank_questions ADD material_id INT NULL;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys
        WHERE name = N'FK_bank_questions_material'
          AND parent_object_id = OBJECT_ID(N'dbo.bank_questions')
    )
        EXEC(N'ALTER TABLE dbo.bank_questions
            ADD CONSTRAINT FK_bank_questions_material
            FOREIGN KEY (material_id) REFERENCES dbo.materials(id);');

    /* node_contents: optional material grouping. */
    IF COL_LENGTH(N'dbo.node_contents', N'material_id') IS NULL
        ALTER TABLE dbo.node_contents ADD material_id INT NULL;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys
        WHERE name = N'FK_nc_material'
          AND parent_object_id = OBJECT_ID(N'dbo.node_contents')
    )
        EXEC(N'ALTER TABLE dbo.node_contents
            ADD CONSTRAINT FK_nc_material
            FOREIGN KEY (material_id) REFERENCES dbo.materials(id);');

    /* node_contents.node_id becomes nullable so a content item can belong
       to a material without a node. SQL Server cannot ALTER a column that
       participates in an FK, so drop FK_nc_node, alter, re-add. */
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.node_contents')
          AND name = N'node_id'
          AND is_nullable = 0
    )
    BEGIN
        IF EXISTS (
            SELECT 1 FROM sys.foreign_keys
            WHERE name = N'FK_nc_node'
              AND parent_object_id = OBJECT_ID(N'dbo.node_contents')
        )
            ALTER TABLE dbo.node_contents DROP CONSTRAINT FK_nc_node;

        ALTER TABLE dbo.node_contents ALTER COLUMN node_id INT NULL;
    END;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys
        WHERE name = N'FK_nc_node'
          AND parent_object_id = OBJECT_ID(N'dbo.node_contents')
    )
        EXEC(N'ALTER TABLE dbo.node_contents
            ADD CONSTRAINT FK_nc_node
            FOREIGN KEY (node_id) REFERENCES dbo.learning_nodes(id);');

    /* Every content item must belong to a node or a material (or both). */
    IF NOT EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE name = N'ck_node_content_owner'
          AND parent_object_id = OBJECT_ID(N'dbo.node_contents')
    )
        EXEC(N'ALTER TABLE dbo.node_contents
            ADD CONSTRAINT ck_node_content_owner
            CHECK (node_id IS NOT NULL OR material_id IS NOT NULL);');

    /* Index for the level-test question pool lookup. */
    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = N'IX_bank_questions_material'
          AND object_id = OBJECT_ID(N'dbo.bank_questions')
    )
        EXEC(N'CREATE INDEX IX_bank_questions_material
            ON dbo.bank_questions(material_id) WHERE material_id IS NOT NULL;');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

PRINT N'>>> Node Level Learning System schema (WS-A) is ready.';
GO
