USE [MentoraDB];
GO
IF OBJECT_ID(N'dbo.student_level_reviews',N'U') IS NULL
BEGIN
 CREATE TABLE dbo.student_level_reviews(
  id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_student_level_reviews PRIMARY KEY,
  node_level_id INT NOT NULL, student_id INT NOT NULL, classroom_id INT NOT NULL,
  reviewed_at DATETIME2 NOT NULL CONSTRAINT DF_student_level_reviews_at DEFAULT(SYSDATETIME()),
  CONSTRAINT uq_student_level_review UNIQUE(node_level_id,student_id,classroom_id),
  CONSTRAINT FK_slr_level FOREIGN KEY(node_level_id) REFERENCES dbo.node_levels(id),
  CONSTRAINT FK_slr_student FOREIGN KEY(student_id) REFERENCES dbo.users(id),
  CONSTRAINT FK_slr_classroom FOREIGN KEY(classroom_id) REFERENCES dbo.classrooms(id));
END;
GO

/* Preserve access for historical students who had already taken a higher
   level before the review confirmation step was introduced. */
INSERT dbo.student_level_reviews(node_level_id,student_id,classroom_id,reviewed_at)
SELECT a.node_level_id,a.student_id,a.classroom_id,COALESCE(MIN(a.started_at),SYSDATETIME())
FROM dbo.node_level_attempts a
INNER JOIN dbo.node_levels nl ON nl.id=a.node_level_id AND nl.level_number>1
WHERE NOT EXISTS(SELECT 1 FROM dbo.student_level_reviews r
 WHERE r.node_level_id=a.node_level_id AND r.student_id=a.student_id AND r.classroom_id=a.classroom_id)
GROUP BY a.node_level_id,a.student_id,a.classroom_id;
GO
