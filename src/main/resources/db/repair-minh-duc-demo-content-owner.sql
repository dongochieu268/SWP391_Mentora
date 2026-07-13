/* Repair only the MDC-DEMO rows created by the older demo seed.
   Node-owned content must not also have material_id. */
USE [MentoraDB];
GO

SET XACT_ABORT ON;
BEGIN TRANSACTION;

UPDATE nc
SET nc.material_id = NULL
FROM dbo.node_contents nc
INNER JOIN dbo.learning_nodes ln ON ln.id = nc.node_id
INNER JOIN dbo.learning_paths lp ON lp.id = ln.learning_path_id
INNER JOIN dbo.subjects s ON s.id = lp.subject_id
WHERE s.code = N'MDC-DEMO'
  AND nc.node_id IS NOT NULL
  AND nc.material_id IS NOT NULL;

DECLARE @fixedRows INT = @@ROWCOUNT;
COMMIT TRANSACTION;

PRINT CONCAT(N'>>> Repaired MDC-DEMO node contents: ', @fixedRows, N' row(s).');
GO

/* Add review content for databases seeded before material unlock existed. */
INSERT dbo.node_contents(node_id,material_id,content_type,title,content_text,display_order,created_at)
SELECT NULL,m.id,N'TEXT',x.content_title,x.content_text,1,SYSDATETIME()
FROM dbo.materials m
INNER JOIN dbo.subjects s ON s.id=m.subject_id AND s.code=N'MDC-DEMO'
INNER JOIN (VALUES
    (N'Yêu cầu phần mềm',N'Ôn tập: Phân loại yêu cầu',N'Functional requirement mô tả hệ thống làm gì. Non-functional requirement mô tả thuộc tính chất lượng và ràng buộc.'),
    (N'UML và thiết kế',N'Ôn tập: Chọn biểu đồ UML',N'Use case mô tả tương tác; sequence mô tả thông điệp theo thời gian; class mô tả cấu trúc tĩnh.'),
    (N'Kiểm thử phần mềm',N'Ôn tập: Boundary value',N'Với miền hợp lệ 0–10, tập giá trị biên điển hình gồm -1, 0, 1, 9, 10 và 11.')
) x(material_title,content_title,content_text) ON x.material_title=m.title
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.node_contents nc
    WHERE nc.material_id=m.id AND nc.title=x.content_title
);
GO
