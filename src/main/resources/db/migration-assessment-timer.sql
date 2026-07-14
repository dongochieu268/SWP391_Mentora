/* ---------------------------------------------------------------------
   Migration: add deadline_at to assessment_attempts
   Safe to run repeatedly against MentoraDB.
   --------------------------------------------------------------------- */
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'assessment_attempts' AND COLUMN_NAME = 'deadline_at'
)
BEGIN
    ALTER TABLE dbo.assessment_attempts
        ADD deadline_at DATETIME2 NULL;
END;
GO

/* ---------------------------------------------------------------------
   Migration: add deadline_at to node_level_attempts
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.node_level_attempts', N'deadline_at') IS NULL
BEGIN
    ALTER TABLE dbo.node_level_attempts
        ADD deadline_at DATETIME2 NULL;
END;
GO

/* ---------------------------------------------------------------------
   Migration: add timed_out to node_level_attempts when missing
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.node_level_attempts', N'timed_out') IS NULL
BEGIN
    ALTER TABLE dbo.node_level_attempts
        ADD timed_out BIT NOT NULL
            CONSTRAINT DF_nla_timed_out DEFAULT (0);
END;
GO

PRINT N'>>> Assessment timer columns are ready.';
GO
