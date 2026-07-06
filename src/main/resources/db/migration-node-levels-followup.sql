/* =====================================================================
   Mentora LMS - Node Level Learning System - follow-up

   Additive-only follow-up to db/migration-node-levels.sql (already
   merged - that file is not edited).

   Idempotent: safe to run multiple times.
   Run AFTER db/migration-node-levels.sql.
   ===================================================================== */

USE [MentoraDB];
GO

SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH(N'dbo.attempt_question_options', N'selected') IS NULL
        ALTER TABLE dbo.attempt_question_options
            ADD selected BIT NOT NULL CONSTRAINT DF_aqo_selected DEFAULT (0);

    IF COL_LENGTH(N'dbo.node_level_attempts', N'passed') IS NULL
        ALTER TABLE dbo.node_level_attempts
            ADD passed BIT NOT NULL CONSTRAINT DF_nla_passed DEFAULT (0);

    IF COL_LENGTH(N'dbo.node_level_attempts', N'timed_out') IS NULL
        ALTER TABLE dbo.node_level_attempts
            ADD timed_out BIT NOT NULL CONSTRAINT DF_nla_timed_out DEFAULT (0);

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

/* Per-student extra attempts granted by the lecturer for one level in
   one classroom. Never deleted by the UI - audit trail via granted_by
   / granted_at. */
IF OBJECT_ID(N'dbo.attempt_grants', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.attempt_grants (
        id             INT IDENTITY(1,1) NOT NULL,
        node_level_id  INT NOT NULL,
        student_id     INT NOT NULL,
        classroom_id   INT NOT NULL,
        extra_attempts INT NOT NULL,
        granted_by     INT NOT NULL,
        granted_at     DATETIME2 NOT NULL CONSTRAINT DF_ag_granted_at DEFAULT (SYSDATETIME()),
        CONSTRAINT PK_attempt_grants PRIMARY KEY (id),
        CONSTRAINT FK_ag_level FOREIGN KEY (node_level_id) REFERENCES dbo.node_levels(id),
        CONSTRAINT FK_ag_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_ag_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT FK_ag_granted_by FOREIGN KEY (granted_by) REFERENCES dbo.users(id),
        CONSTRAINT CHK_ag_extra_attempts CHECK (extra_attempts >= 1)
    );

    CREATE INDEX IX_ag_level_student
        ON dbo.attempt_grants(node_level_id, student_id, classroom_id);
END;
GO

PRINT N'>>> Node Level follow-up (selected/passed/timed_out + attempt_grants) is ready.';
GO
