/* =====================================================================
   Mentora LMS - Full SQL Server schema with Question Bank

   This script supports both:
   - Creating a new MentoraDB database from scratch.
   - Upgrading the previous Mentora schema if tables already exist.

   The database name matches src/main/resources/application.properties.
   ===================================================================== */

IF DB_ID(N'MentoraDB') IS NULL
BEGIN
    CREATE DATABASE [MentoraDB];
END;
GO

USE [MentoraDB];
GO

/* ---------------------------------------------------------------------
   1. roles
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.roles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.roles (
        id   INT IDENTITY(1,1) NOT NULL,
        name NVARCHAR(50) NOT NULL,
        CONSTRAINT PK_roles PRIMARY KEY (id),
        CONSTRAINT UQ_roles_name UNIQUE (name)
    );
END;
GO

/* ---------------------------------------------------------------------
   2. users
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.users', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.users (
        id             INT IDENTITY(1,1) NOT NULL,
        full_name      NVARCHAR(255) NULL,
        email          NVARCHAR(255) NULL,
        password       NVARCHAR(255) NULL,
        role_id        INT NOT NULL,
        status         NVARCHAR(20) NULL CONSTRAINT DF_users_status DEFAULT (N'ACTIVE'),
        email_verified BIT NULL,
        CONSTRAINT PK_users PRIMARY KEY (id),
        CONSTRAINT UQ_users_email UNIQUE (email),
        CONSTRAINT FK_users_role FOREIGN KEY (role_id) REFERENCES dbo.roles(id)
    );
END;
GO

/* ---------------------------------------------------------------------
   3. semesters
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.semesters', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.semesters (
        id         INT IDENTITY(1,1) NOT NULL,
        name       NVARCHAR(100) NOT NULL,
        start_date DATE NOT NULL,
        end_date   DATE NOT NULL,
        status     NVARCHAR(20) NOT NULL CONSTRAINT DF_semesters_status DEFAULT (N'ACTIVE'),
        CONSTRAINT PK_semesters PRIMARY KEY (id)
    );
END;
GO

/* ---------------------------------------------------------------------
   4. subjects
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.subjects', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.subjects (
        id          INT IDENTITY(1,1) NOT NULL,
        code        NVARCHAR(20) NOT NULL,
        name        NVARCHAR(200) NOT NULL,
        description NVARCHAR(2000) NULL,
        status      NVARCHAR(20) NOT NULL CONSTRAINT DF_subjects_status DEFAULT (N'ACTIVE'),
        created_at  DATETIME2 NULL,
        CONSTRAINT PK_subjects PRIMARY KEY (id),
        CONSTRAINT UQ_subjects_code UNIQUE (code),
        CONSTRAINT CHK_subjects_status CHECK (status IN (N'ACTIVE', N'HIDDEN'))
    );
END;
GO

/* ---------------------------------------------------------------------
   5. subject_prerequisites
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.subject_prerequisites', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.subject_prerequisites (
        subject_id              INT NOT NULL,
        prerequisite_subject_id INT NOT NULL,
        CONSTRAINT PK_subject_prerequisites PRIMARY KEY (subject_id, prerequisite_subject_id),
        CONSTRAINT FK_sp_subject FOREIGN KEY (subject_id) REFERENCES dbo.subjects(id),
        CONSTRAINT FK_sp_prerequisite FOREIGN KEY (prerequisite_subject_id) REFERENCES dbo.subjects(id)
    );
END;
GO

/* ---------------------------------------------------------------------
   6. question_banks
   One shared Question Bank for each subject.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.question_banks', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.question_banks (
        id         INT IDENTITY(1,1) NOT NULL,
        subject_id INT NOT NULL,
        status     NVARCHAR(20) NOT NULL CONSTRAINT DF_question_banks_status DEFAULT (N'ACTIVE'),
        created_at DATETIME2 NOT NULL CONSTRAINT DF_question_banks_created_at DEFAULT (SYSDATETIME()),
        CONSTRAINT PK_question_banks PRIMARY KEY (id),
        CONSTRAINT UQ_question_banks_subject UNIQUE (subject_id),
        CONSTRAINT FK_question_banks_subject FOREIGN KEY (subject_id) REFERENCES dbo.subjects(id),
        CONSTRAINT CHK_question_banks_status CHECK (status IN (N'ACTIVE'))
    );
END;
GO

/* ---------------------------------------------------------------------
   7. bank_questions
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.bank_questions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.bank_questions (
        id               INT IDENTITY(1,1) NOT NULL,
        question_bank_id INT NOT NULL,
        created_by       INT NOT NULL,
        content          NVARCHAR(2000) NOT NULL,
        difficulty       NVARCHAR(20) NOT NULL,
        question_type    NVARCHAR(30) NOT NULL,
        default_score    DECIMAL(10,2) NOT NULL,
        status           NVARCHAR(20) NOT NULL CONSTRAINT DF_bank_questions_status DEFAULT (N'ACTIVE'),
        created_at       DATETIME2 NOT NULL CONSTRAINT DF_bank_questions_created_at DEFAULT (SYSDATETIME()),
        updated_at       DATETIME2 NOT NULL CONSTRAINT DF_bank_questions_updated_at DEFAULT (SYSDATETIME()),
        CONSTRAINT PK_bank_questions PRIMARY KEY (id),
        CONSTRAINT FK_bank_questions_bank FOREIGN KEY (question_bank_id) REFERENCES dbo.question_banks(id),
        CONSTRAINT FK_bank_questions_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id),
        CONSTRAINT CHK_bank_questions_difficulty CHECK (difficulty IN (N'EASY', N'MEDIUM', N'HARD')),
        CONSTRAINT CHK_bank_questions_type CHECK (question_type IN (N'MULTIPLE_CHOICE', N'TRUE_FALSE')),
        CONSTRAINT CHK_bank_questions_status CHECK (status IN (N'ACTIVE', N'ARCHIVED')),
        CONSTRAINT CHK_bank_questions_default_score CHECK (default_score > 0)
    );
END;
GO

/* ---------------------------------------------------------------------
   8. bank_question_options
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.bank_question_options', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.bank_question_options (
        id               INT IDENTITY(1,1) NOT NULL,
        bank_question_id INT NOT NULL,
        content          NVARCHAR(1000) NOT NULL,
        is_correct       BIT NOT NULL,
        display_order    INT NOT NULL,
        CONSTRAINT PK_bank_question_options PRIMARY KEY (id),
        CONSTRAINT FK_bank_question_options_question
            FOREIGN KEY (bank_question_id) REFERENCES dbo.bank_questions(id) ON DELETE CASCADE,
        CONSTRAINT CHK_bank_question_options_display_order CHECK (display_order >= 0)
    );
END;
GO

/* ---------------------------------------------------------------------
   9. learning_paths
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.learning_paths', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.learning_paths (
        id          INT IDENTITY(1,1) NOT NULL,
        subject_id  INT NOT NULL,
        name        NVARCHAR(150) NOT NULL,
        description NVARCHAR(1000) NULL,
        created_by  INT NOT NULL,
        status      NVARCHAR(20) NOT NULL CONSTRAINT DF_learning_paths_status DEFAULT (N'ACTIVE'),
        created_at  DATETIME2 NULL,
        CONSTRAINT PK_learning_paths PRIMARY KEY (id),
        CONSTRAINT FK_lp_subject FOREIGN KEY (subject_id) REFERENCES dbo.subjects(id),
        CONSTRAINT FK_lp_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id),
        CONSTRAINT CHK_learning_paths_status CHECK (status IN (N'ACTIVE'))
    );
END;
GO

/* ---------------------------------------------------------------------
   10. learning_nodes
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.learning_nodes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.learning_nodes (
        id                   INT IDENTITY(1,1) NOT NULL,
        learning_path_id     INT NOT NULL,
        title                NVARCHAR(200) NOT NULL,
        description          NVARCHAR(2000) NULL,
        node_order           DECIMAL(18,9) NOT NULL,
        prerequisite_node_id INT NULL,
        node_type            NVARCHAR(20) NOT NULL CONSTRAINT DF_learning_nodes_type DEFAULT (N'LESSON'),
        branch_tag           NVARCHAR(10) NULL,
        branch_owner_node_id INT NULL,
        created_at           DATETIME2 NULL,
        CONSTRAINT PK_learning_nodes PRIMARY KEY (id),
        CONSTRAINT FK_ln_path FOREIGN KEY (learning_path_id) REFERENCES dbo.learning_paths(id),
        CONSTRAINT FK_ln_prerequisite FOREIGN KEY (prerequisite_node_id) REFERENCES dbo.learning_nodes(id),
        CONSTRAINT FK_ln_branch_owner FOREIGN KEY (branch_owner_node_id) REFERENCES dbo.learning_nodes(id),
        CONSTRAINT CHK_learning_nodes_type CHECK (node_type IN (N'LESSON', N'BRANCH_TEST')),
        CONSTRAINT CHK_learning_nodes_branch_tag CHECK (
            branch_tag IS NULL OR branch_tag IN (N'MAIN', N'PASS', N'FAIL')
        )
    );
END;
GO

/* ---------------------------------------------------------------------
   11. node_contents
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.node_contents', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.node_contents (
        id            INT IDENTITY(1,1) NOT NULL,
        node_id       INT NOT NULL,
        content_type  NVARCHAR(20) NOT NULL,
        title         NVARCHAR(200) NULL,
        content_url   NVARCHAR(1000) NULL,
        content_text  NVARCHAR(MAX) NULL,
        display_order INT NULL CONSTRAINT DF_node_contents_order DEFAULT (1),
        created_at    DATETIME2 NULL,
        CONSTRAINT PK_node_contents PRIMARY KEY (id),
        CONSTRAINT FK_nc_node FOREIGN KEY (node_id) REFERENCES dbo.learning_nodes(id),
        CONSTRAINT CHK_node_contents_type CHECK (content_type IN (N'TEXT', N'VIDEO', N'FILE', N'LINK'))
    );
END;
GO

/* ---------------------------------------------------------------------
   12. classrooms
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.classrooms', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.classrooms (
        id               INT IDENTITY(1,1) NOT NULL,
        subject_id       INT NOT NULL,
        learning_path_id INT NOT NULL,
        teacher_id       INT NOT NULL,
        name             NVARCHAR(200) NOT NULL,
        semester_id      INT NOT NULL,
        status           NVARCHAR(20) NOT NULL CONSTRAINT DF_classrooms_status DEFAULT (N'OPEN'),
        invite_code      NVARCHAR(100) NULL,
        created_by       INT NOT NULL,
        created_at       DATETIME2 NULL,
        CONSTRAINT PK_classrooms PRIMARY KEY (id),
        CONSTRAINT FK_cr_subject FOREIGN KEY (subject_id) REFERENCES dbo.subjects(id),
        CONSTRAINT FK_cr_path FOREIGN KEY (learning_path_id) REFERENCES dbo.learning_paths(id),
        CONSTRAINT FK_cr_teacher FOREIGN KEY (teacher_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_cr_semester FOREIGN KEY (semester_id) REFERENCES dbo.semesters(id),
        CONSTRAINT FK_cr_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id),
        CONSTRAINT CHK_classrooms_status CHECK (status IN (N'OPEN', N'CLOSE'))
    );
END;
GO

/* ---------------------------------------------------------------------
   13. classroom_members
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.classroom_members', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.classroom_members (
        id            INT IDENTITY(1,1) NOT NULL,
        classroom_id  INT NOT NULL,
        user_id       INT NOT NULL,
        role_in_class NVARCHAR(20) NOT NULL CONSTRAINT DF_cm_role DEFAULT (N'STUDENT'),
        status        NVARCHAR(20) NOT NULL CONSTRAINT DF_cm_status DEFAULT (N'PENDING'),
        joined_at     DATETIME2 NULL,
        expires_at    DATETIME2 NULL,
        CONSTRAINT PK_classroom_members PRIMARY KEY (id),
        CONSTRAINT UQ_classroom_members UNIQUE (classroom_id, user_id),
        CONSTRAINT FK_cm_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT FK_cm_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT CHK_cm_role CHECK (role_in_class IN (N'STUDENT', N'LECTURER', N'TA')),
        CONSTRAINT CHK_cm_status CHECK (status IN (N'PENDING', N'ACTIVE', N'BANNED'))
    );
END;
GO

/* ---------------------------------------------------------------------
   14. classroom_node_status
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.classroom_node_status', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.classroom_node_status (
        id           INT IDENTITY(1,1) NOT NULL,
        classroom_id INT NOT NULL,
        node_id      INT NOT NULL,
        status       NVARCHAR(20) NOT NULL CONSTRAINT DF_cns_status DEFAULT (N'HIDDEN'),
        updated_at   DATETIME2 NULL,
        CONSTRAINT PK_classroom_node_status PRIMARY KEY (id),
        CONSTRAINT UQ_classroom_node_status UNIQUE (classroom_id, node_id),
        CONSTRAINT FK_cns_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT FK_cns_node FOREIGN KEY (node_id) REFERENCES dbo.learning_nodes(id),
        CONSTRAINT CHK_cns_status CHECK (status IN (N'HIDDEN', N'VISIBLE'))
    );
END;
GO

/* ---------------------------------------------------------------------
   15. node_progress
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.node_progress', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.node_progress (
        id           INT IDENTITY(1,1) NOT NULL,
        classroom_id INT NOT NULL,
        student_id   INT NOT NULL,
        node_id      INT NOT NULL,
        is_completed BIT NOT NULL CONSTRAINT DF_node_progress_completed DEFAULT (0),
        completed_at DATETIME2 NULL,
        CONSTRAINT PK_node_progress PRIMARY KEY (id),
        CONSTRAINT UQ_node_progress UNIQUE (classroom_id, student_id, node_id),
        CONSTRAINT FK_np_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT FK_np_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_np_node FOREIGN KEY (node_id) REFERENCES dbo.learning_nodes(id)
    );
END;
GO

/* ---------------------------------------------------------------------
   16. assessments
   Each new assessment belongs to one subject.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.assessments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.assessments (
        id               INT IDENTITY(1,1) NOT NULL,
        subject_id       INT NOT NULL,
        title            NVARCHAR(200) NOT NULL,
        description      NVARCHAR(2000) NULL,
        type             NVARCHAR(30) NOT NULL CONSTRAINT DF_assessments_type DEFAULT (N'QUIZ'),
        delivery_mode    NVARCHAR(30) NOT NULL CONSTRAINT DF_assessments_delivery DEFAULT (N'SELF_PACED'),
        duration_minutes INT NOT NULL,
        total_score      DECIMAL(10,2) NOT NULL,
        status           NVARCHAR(20) NOT NULL CONSTRAINT DF_assessments_status DEFAULT (N'DRAFT'),
        created_by       INT NOT NULL,
        created_at       DATETIME2 NULL,
        CONSTRAINT PK_assessments PRIMARY KEY (id),
        CONSTRAINT FK_assessments_subject FOREIGN KEY (subject_id) REFERENCES dbo.subjects(id),
        CONSTRAINT FK_as_created_by FOREIGN KEY (created_by) REFERENCES dbo.users(id),
        CONSTRAINT CHK_assessments_type CHECK (type IN (N'QUIZ', N'BRANCHING_TEST', N'EXAM')),
        CONSTRAINT CHK_assessments_delivery CHECK (delivery_mode IN (N'SELF_PACED', N'SCHEDULED', N'IN_CLASS')),
        CONSTRAINT CHK_assessments_status CHECK (status IN (N'DRAFT', N'PUBLISHED', N'ARCHIVED'))
    );
END;
GO

/* ---------------------------------------------------------------------
   17. questions
   source_bank_question_id links the copied test question to its source.
   selection_method: CUSTOM, MANUAL_IMPORT or RANDOM_IMPORT.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.questions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.questions (
        id                      INT IDENTITY(1,1) NOT NULL,
        assessment_id           INT NOT NULL,
        source_bank_question_id INT NULL,
        content                 NVARCHAR(2000) NOT NULL,
        difficulty              NVARCHAR(20) NOT NULL,
        question_type           NVARCHAR(30) NOT NULL CONSTRAINT DF_questions_type DEFAULT (N'MULTIPLE_CHOICE'),
        score                   DECIMAL(10,2) NOT NULL,
        selection_method        NVARCHAR(30) NOT NULL CONSTRAINT DF_questions_selection_method DEFAULT (N'CUSTOM'),
        CONSTRAINT PK_questions PRIMARY KEY (id),
        CONSTRAINT FK_q_assessment FOREIGN KEY (assessment_id) REFERENCES dbo.assessments(id),
        CONSTRAINT FK_questions_source_bank_question
            FOREIGN KEY (source_bank_question_id) REFERENCES dbo.bank_questions(id),
        CONSTRAINT CHK_questions_difficulty CHECK (difficulty IN (N'EASY', N'MEDIUM', N'HARD')),
        CONSTRAINT CHK_questions_type CHECK (question_type IN (N'MULTIPLE_CHOICE', N'TRUE_FALSE')),
        CONSTRAINT CHK_questions_selection_method CHECK (
            selection_method IN (N'CUSTOM', N'MANUAL_IMPORT', N'RANDOM_IMPORT')
        )
    );
END;
GO

/* ---------------------------------------------------------------------
   18. question_options
   display_order stores the answer order after randomization.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.question_options', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.question_options (
        id            INT IDENTITY(1,1) NOT NULL,
        question_id   INT NOT NULL,
        content       NVARCHAR(1000) NOT NULL,
        is_correct    BIT NOT NULL,
        display_order INT NOT NULL CONSTRAINT DF_question_options_display_order DEFAULT (0),
        CONSTRAINT PK_question_options PRIMARY KEY (id),
        CONSTRAINT FK_qo_question FOREIGN KEY (question_id) REFERENCES dbo.questions(id),
        CONSTRAINT CHK_question_options_display_order CHECK (display_order >= 0)
    );
END;
GO

/* ---------------------------------------------------------------------
   19. assessment_attempts
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.assessment_attempts', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.assessment_attempts (
        id            INT IDENTITY(1,1) NOT NULL,
        assessment_id INT NOT NULL,
        classroom_id  INT NOT NULL,
        student_id    INT NOT NULL,
        score         DECIMAL(10,2) NULL,
        status        NVARCHAR(20) NOT NULL CONSTRAINT DF_aa_status DEFAULT (N'IN_PROGRESS'),
        started_at    DATETIME2 NULL,
        submitted_at  DATETIME2 NULL,
        CONSTRAINT PK_assessment_attempts PRIMARY KEY (id),
        CONSTRAINT FK_aa_assessment FOREIGN KEY (assessment_id) REFERENCES dbo.assessments(id),
        CONSTRAINT FK_aa_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT FK_aa_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
        CONSTRAINT CHK_aa_status CHECK (status IN (N'IN_PROGRESS', N'SUBMITTED'))
    );
END;
GO

/* ---------------------------------------------------------------------
   20. branch_rules
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.branch_rules', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.branch_rules (
        id            INT IDENTITY(1,1) NOT NULL,
        node_id       INT NOT NULL,
        assessment_id INT NOT NULL,
        min_score     INT NOT NULL,
        created_at    DATETIME2 NULL,
        CONSTRAINT PK_branch_rules PRIMARY KEY (id),
        CONSTRAINT UQ_branch_rules_node UNIQUE (node_id),
        CONSTRAINT FK_br_node FOREIGN KEY (node_id) REFERENCES dbo.learning_nodes(id),
        CONSTRAINT FK_br_assessment FOREIGN KEY (assessment_id) REFERENCES dbo.assessments(id)
    );
END;
GO

/* ---------------------------------------------------------------------
   21. student_branch_assignments
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.student_branch_assignments', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.student_branch_assignments (
        id              INT IDENTITY(1,1) NOT NULL,
        student_id      INT NOT NULL,
        branch_node_id  INT NOT NULL,
        classroom_id    INT NOT NULL,
        assigned_branch NVARCHAR(10) NOT NULL,
        attempt_score   INT NULL,
        assigned_at     DATETIME2 NULL,
        CONSTRAINT PK_student_branch_assignments PRIMARY KEY (id),
        CONSTRAINT UQ_sba UNIQUE (student_id, branch_node_id, classroom_id),
        CONSTRAINT FK_sba_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_sba_node FOREIGN KEY (branch_node_id) REFERENCES dbo.learning_nodes(id),
        CONSTRAINT FK_sba_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT CHK_sba_branch CHECK (assigned_branch IN (N'MAIN', N'PASS', N'FAIL'))
    );
END;
GO

/* =====================================================================
   Upgrade existing previous schema
   Dynamic SQL avoids SQL Server compile errors for newly added columns.
   ===================================================================== */
SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    /* Upgrade assessments created by the previous schema. */
    IF COL_LENGTH(N'dbo.assessments', N'subject_id') IS NULL
        ALTER TABLE dbo.assessments ADD subject_id INT NULL;

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys
        WHERE name = N'FK_assessments_subject'
          AND parent_object_id = OBJECT_ID(N'dbo.assessments')
    )
        EXEC(N'ALTER TABLE dbo.assessments
            ADD CONSTRAINT FK_assessments_subject
            FOREIGN KEY (subject_id) REFERENCES dbo.subjects(id);');

    /* Backfill assessment subject from the attached learning path. */
    EXEC(N'
        ;WITH AssessmentSubjects AS (
            SELECT br.assessment_id, MIN(lp.subject_id) AS subject_id
            FROM dbo.branch_rules br
            INNER JOIN dbo.learning_nodes ln ON ln.id = br.node_id
            INNER JOIN dbo.learning_paths lp ON lp.id = ln.learning_path_id
            GROUP BY br.assessment_id
            HAVING MIN(lp.subject_id) = MAX(lp.subject_id)
        )
        UPDATE a
        SET a.subject_id = x.subject_id
        FROM dbo.assessments a
        INNER JOIN AssessmentSubjects x ON x.assessment_id = a.id
        WHERE a.subject_id IS NULL;
    ');

    /* Upgrade questions created by the previous schema. */
    IF COL_LENGTH(N'dbo.questions', N'source_bank_question_id') IS NULL
        ALTER TABLE dbo.questions ADD source_bank_question_id INT NULL;

    IF COL_LENGTH(N'dbo.questions', N'selection_method') IS NULL
        ALTER TABLE dbo.questions
        ADD selection_method NVARCHAR(30) NOT NULL
            CONSTRAINT DF_questions_selection_method DEFAULT (N'CUSTOM');

    IF NOT EXISTS (
        SELECT 1 FROM sys.foreign_keys
        WHERE name = N'FK_questions_source_bank_question'
          AND parent_object_id = OBJECT_ID(N'dbo.questions')
    )
        EXEC(N'ALTER TABLE dbo.questions
            ADD CONSTRAINT FK_questions_source_bank_question
            FOREIGN KEY (source_bank_question_id) REFERENCES dbo.bank_questions(id);');

    IF NOT EXISTS (
        SELECT 1 FROM sys.check_constraints
        WHERE name = N'CHK_questions_selection_method'
          AND parent_object_id = OBJECT_ID(N'dbo.questions')
    )
        EXEC(N'ALTER TABLE dbo.questions
            ADD CONSTRAINT CHK_questions_selection_method
            CHECK (selection_method IN (N''CUSTOM'', N''MANUAL_IMPORT'', N''RANDOM_IMPORT''));');

    /* Upgrade options created by the previous schema. */
    IF COL_LENGTH(N'dbo.question_options', N'display_order') IS NULL
    BEGIN
        ALTER TABLE dbo.question_options ADD display_order INT NULL;
    END;

    IF EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.question_options')
          AND name = N'display_order'
          AND is_nullable = 1
    )
    BEGIN
        EXEC(N'
            ;WITH OrderedOptions AS (
                SELECT
                    id,
                    ROW_NUMBER() OVER (PARTITION BY question_id ORDER BY id) - 1 AS new_order
                FROM dbo.question_options
            )
            UPDATE qo
            SET qo.display_order = x.new_order
            FROM dbo.question_options qo
            INNER JOIN OrderedOptions x ON x.id = qo.id;

            ALTER TABLE dbo.question_options
            ALTER COLUMN display_order INT NOT NULL;
        ');
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.default_constraints dc
        INNER JOIN sys.columns c
            ON c.object_id = dc.parent_object_id
           AND c.column_id = dc.parent_column_id
        WHERE dc.parent_object_id = OBJECT_ID(N'dbo.question_options')
          AND c.name = N'display_order'
    )
        EXEC(N'ALTER TABLE dbo.question_options
            ADD CONSTRAINT DF_question_options_display_order DEFAULT (0) FOR display_order;');

    /* Create indexes needed by Question Bank queries. */
    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = N'IX_bank_questions_bank_status'
          AND object_id = OBJECT_ID(N'dbo.bank_questions')
    )
        CREATE INDEX IX_bank_questions_bank_status
            ON dbo.bank_questions(question_bank_id, status);

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = N'IX_bank_question_options_question_order'
          AND object_id = OBJECT_ID(N'dbo.bank_question_options')
    )
        CREATE INDEX IX_bank_question_options_question_order
            ON dbo.bank_question_options(bank_question_id, display_order);

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = N'IX_assessments_subject'
          AND object_id = OBJECT_ID(N'dbo.assessments')
    )
        EXEC(N'CREATE INDEX IX_assessments_subject ON dbo.assessments(subject_id);');

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = N'IX_questions_source_bank_question'
          AND object_id = OBJECT_ID(N'dbo.questions')
    )
        EXEC(N'CREATE INDEX IX_questions_source_bank_question
            ON dbo.questions(source_bank_question_id);');

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = N'IX_question_options_question_order'
          AND object_id = OBJECT_ID(N'dbo.question_options')
    )
        EXEC(N'CREATE INDEX IX_question_options_question_order
            ON dbo.question_options(question_id, display_order);');

    /* Enforce NOT NULL if every legacy assessment has a subject. */
    EXEC(N'
        IF NOT EXISTS (
            SELECT 1
            FROM dbo.assessments
            WHERE subject_id IS NULL
        )
            ALTER TABLE dbo.assessments ALTER COLUMN subject_id INT NOT NULL;
    ');

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* ---------------------------------------------------------------------
   Seed required roles
   --------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = N'ADMIN')
    INSERT INTO dbo.roles (name) VALUES (N'ADMIN');
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = N'LECTURER')
    INSERT INTO dbo.roles (name) VALUES (N'LECTURER');
IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = N'STUDENT')
    INSERT INTO dbo.roles (name) VALUES (N'STUDENT');
GO

/* ---------------------------------------------------------------------
   Show legacy assessments that still need a subject.
   Assign subject_id manually if this query returns rows.
   --------------------------------------------------------------------- */
EXEC(N'
    SELECT id, title, created_by
    FROM dbo.assessments
    WHERE subject_id IS NULL;
');
GO

PRINT N'>>> MentoraDB schema with Question Bank is ready.';
GO
