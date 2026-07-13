USE [MentoraDB];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

DECLARE @email NVARCHAR(255)=N'demo.freshstudent@mentora.local';
DECLARE @password NVARCHAR(255)=N'$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG'; -- password
DECLARE @studentRoleId INT=(SELECT id FROM dbo.roles WHERE name=N'STUDENT');
DECLARE @classroomId INT=(
    SELECT TOP 1 c.id
    FROM dbo.classrooms c
    INNER JOIN dbo.subjects s ON s.id=c.subject_id
    WHERE s.code=N'MDC-DEMO'
    ORDER BY c.id DESC
);

IF @studentRoleId IS NULL THROW 51001,N'Không tìm thấy role STUDENT.',1;
IF @classroomId IS NULL THROW 51002,N'Chưa có lớp MDC-DEMO. Hãy chạy seed-minh-duc-demo.sql trước.',1;

IF NOT EXISTS(SELECT 1 FROM dbo.users WHERE email=@email)
    INSERT dbo.users(full_name,email,password,role_id,status,email_verified)
    VALUES(N'Lê Chi - Sinh viên demo mới',@email,@password,@studentRoleId,N'ACTIVE',1);

DECLARE @studentId INT=(SELECT id FROM dbo.users WHERE email=@email);

IF NOT EXISTS(SELECT 1 FROM dbo.classroom_members WHERE classroom_id=@classroomId AND user_id=@studentId)
    INSERT dbo.classroom_members(classroom_id,user_id,role_in_class,status,joined_at)
    VALUES(@classroomId,@studentId,N'STUDENT',N'ACTIVE',SYSDATETIME());
ELSE
    UPDATE dbo.classroom_members SET status=N'ACTIVE',role_in_class=N'STUDENT'
    WHERE classroom_id=@classroomId AND user_id=@studentId;

/* Guarantee a completely fresh learning state if this script is rerun. */
DELETE r FROM dbo.student_level_reviews r WHERE r.student_id=@studentId AND r.classroom_id=@classroomId;
DELETE g FROM dbo.attempt_grants g WHERE g.student_id=@studentId AND g.classroom_id=@classroomId;
DELETE aq FROM dbo.attempt_questions aq
INNER JOIN dbo.node_level_attempts a ON a.id=aq.attempt_id
WHERE a.student_id=@studentId AND a.classroom_id=@classroomId;
DELETE a FROM dbo.node_level_attempts a WHERE a.student_id=@studentId AND a.classroom_id=@classroomId;
DELETE p FROM dbo.node_progress p WHERE p.student_id=@studentId AND p.classroom_id=@classroomId;
DELETE qr FROM dbo.classroom_question_replies qr
INNER JOIN dbo.classroom_questions q ON q.id=qr.question_id
WHERE q.student_id=@studentId AND q.classroom_id=@classroomId;
DELETE q FROM dbo.classroom_questions q WHERE q.student_id=@studentId AND q.classroom_id=@classroomId;

COMMIT TRANSACTION;
PRINT N'>>> Fresh demo student ready.';
PRINT N'>>> Email: demo.freshstudent@mentora.local';
PRINT N'>>> Password: password';
GO
