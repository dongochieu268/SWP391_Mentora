/* ============================================================
   Migration: trạng thái COMPLETED cho classrooms (L6 — Kết thúc lớp)

   CHK_classrooms_status hiện chỉ cho OPEN/CLOSE — nới thêm COMPLETED.
   Chạy sau migration-node-levels-followup.sql.
   ============================================================ */

IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CHK_classrooms_status')
BEGIN
    ALTER TABLE dbo.classrooms DROP CONSTRAINT CHK_classrooms_status;
END
GO

ALTER TABLE dbo.classrooms ADD CONSTRAINT CHK_classrooms_status
    CHECK (status IN (N'OPEN', N'CLOSE', N'COMPLETED'));
GO

PRINT N'>>> CHK_classrooms_status: OPEN / CLOSE / COMPLETED.';
