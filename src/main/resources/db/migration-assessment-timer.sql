/* ---------------------------------------------------------------------
   Migration: add deadline_at to assessment_attempts
   Run once against MentoraDB.
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
