/* Backfill only missing statuses. Rows explicitly set to HIDDEN remain HIDDEN. */
USE [MentoraDB];
GO
SET XACT_ABORT ON;

INSERT dbo.classroom_node_status(classroom_id,node_id,status,updated_at)
SELECT c.id,n.id,N'VISIBLE',SYSDATETIME()
FROM dbo.classrooms c
INNER JOIN dbo.learning_nodes n ON n.learning_path_id=c.learning_path_id
WHERE NOT EXISTS(
    SELECT 1 FROM dbo.classroom_node_status s
    WHERE s.classroom_id=c.id AND s.node_id=n.id
);

PRINT CONCAT(N'>>> Missing classroom node statuses made VISIBLE: ',@@ROWCOUNT);
GO
