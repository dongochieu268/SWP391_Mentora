/* =====================================================================
   Mentora LMS - Node Levels demo seed (WS-A)

   Creates a self-contained demo data set for manual testing of the
   Node Level Learning System:
     - 1 subject  (code NLVL-DEMO) + its question bank
     - 3 materials x 16 tagged questions (4 options each, 1 correct)
     - 1 learning path with 3 chained nodes x 2 levels
     - level_materials wiring (exact question_count per material)
     - 1 OPEN classroom with 1 ACTIVE student, all nodes VISIBLE

   Reuses the first existing LECTURER/STUDENT users when available;
   otherwise creates demo accounts whose password is "password"
   (well-known BCrypt hash), emails *@mentora.local.

   Idempotent: skips entirely if subject NLVL-DEMO already exists.
   Run AFTER db/schema.sql and db/migration-node-levels.sql.
   ===================================================================== */

USE [MentoraDB];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

IF EXISTS (SELECT 1 FROM dbo.subjects WHERE code = N'NLVL-DEMO')
BEGIN
    PRINT N'>>> NLVL-DEMO already seeded - nothing to do.';
END
ELSE
BEGIN
    BEGIN TRANSACTION;

    /* -----------------------------------------------------------------
       Users: reuse first lecturer/student, else create demo accounts.
       ----------------------------------------------------------------- */
    DECLARE @lecturerRoleId INT = (SELECT id FROM dbo.roles WHERE name = N'LECTURER');
    DECLARE @studentRoleId  INT = (SELECT id FROM dbo.roles WHERE name = N'STUDENT');
    DECLARE @bcryptPassword NVARCHAR(255) = N'$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG'; -- "password"

    DECLARE @lecturerId INT = (SELECT MIN(id) FROM dbo.users WHERE role_id = @lecturerRoleId);
    IF @lecturerId IS NULL
    BEGIN
        INSERT INTO dbo.users (full_name, email, password, role_id, status, email_verified)
        VALUES (N'GV Demo Node Levels', N'nlvl.lecturer@mentora.local', @bcryptPassword, @lecturerRoleId, N'ACTIVE', 1);
        SET @lecturerId = SCOPE_IDENTITY();
    END;

    DECLARE @studentId INT = (SELECT MIN(id) FROM dbo.users WHERE role_id = @studentRoleId);
    IF @studentId IS NULL
    BEGIN
        INSERT INTO dbo.users (full_name, email, password, role_id, status, email_verified)
        VALUES (N'SV Demo Node Levels', N'nlvl.student@mentora.local', @bcryptPassword, @studentRoleId, N'ACTIVE', 1);
        SET @studentId = SCOPE_IDENTITY();
    END;

    /* -----------------------------------------------------------------
       Subject + question bank (one bank per subject).
       ----------------------------------------------------------------- */
    INSERT INTO dbo.subjects (code, name, description, status, created_at)
    VALUES (N'NLVL-DEMO', N'Demo Node Levels', N'Môn học demo cho hệ thống level theo node', N'ACTIVE', SYSDATETIME());
    DECLARE @subjectId INT = SCOPE_IDENTITY();

    INSERT INTO dbo.question_banks (subject_id, status, created_at)
    VALUES (@subjectId, N'ACTIVE', SYSDATETIME());
    DECLARE @bankId INT = SCOPE_IDENTITY();

    /* -----------------------------------------------------------------
       3 materials x 16 tagged questions (difficulty rotates E/M/H).
       ----------------------------------------------------------------- */
    DECLARE @matBasic INT, @matMid INT, @matAdv INT;

    INSERT INTO dbo.materials (subject_id, title, description, created_by)
    VALUES (@subjectId, N'Tài liệu Cơ bản', N'Kiến thức nền tảng', @lecturerId);
    SET @matBasic = SCOPE_IDENTITY();

    INSERT INTO dbo.materials (subject_id, title, description, created_by)
    VALUES (@subjectId, N'Tài liệu Trung cấp', N'Kiến thức trung cấp', @lecturerId);
    SET @matMid = SCOPE_IDENTITY();

    INSERT INTO dbo.materials (subject_id, title, description, created_by)
    VALUES (@subjectId, N'Tài liệu Nâng cao', N'Kiến thức nâng cao', @lecturerId);
    SET @matAdv = SCOPE_IDENTITY();

    DECLARE @m INT = 1, @i INT, @matId INT, @matLabel NVARCHAR(50),
            @questionId INT, @difficulty NVARCHAR(20);
    WHILE @m <= 3
    BEGIN
        SELECT @matId    = CASE @m WHEN 1 THEN @matBasic WHEN 2 THEN @matMid ELSE @matAdv END,
               @matLabel = CASE @m WHEN 1 THEN N'Cơ bản' WHEN 2 THEN N'Trung cấp' ELSE N'Nâng cao' END;

        SET @i = 1;
        WHILE @i <= 16
        BEGIN
            SET @difficulty = CASE @i % 3 WHEN 1 THEN N'EASY' WHEN 2 THEN N'MEDIUM' ELSE N'HARD' END;

            INSERT INTO dbo.bank_questions
                (question_bank_id, created_by, content, difficulty, question_type, default_score, status, material_id)
            VALUES
                (@bankId, @lecturerId,
                 CONCAT(N'[', @matLabel, N'] Câu hỏi số ', @i, N': đáp án đúng là phương án A.'),
                 @difficulty, N'MULTIPLE_CHOICE', 1.00, N'ACTIVE', @matId);
            SET @questionId = SCOPE_IDENTITY();

            INSERT INTO dbo.bank_question_options (bank_question_id, content, is_correct, display_order)
            VALUES
                (@questionId, CONCAT(N'Phương án A (đúng) - câu ', @i), 1, 0),
                (@questionId, CONCAT(N'Phương án B - câu ', @i), 0, 1),
                (@questionId, CONCAT(N'Phương án C - câu ', @i), 0, 2),
                (@questionId, CONCAT(N'Phương án D - câu ', @i), 0, 3);

            SET @i += 1;
        END;

        /* One material-owned content item (node_id NULL - needs migration). */
        INSERT INTO dbo.node_contents (node_id, material_id, content_type, title, content_text, display_order, created_at)
        VALUES (NULL, @matId, N'TEXT', CONCAT(N'Tổng quan ', @matLabel),
                CONCAT(N'Nội dung ôn tập thuộc ', @matLabel, N'.'), 1, SYSDATETIME());

        SET @m += 1;
    END;

    /* -----------------------------------------------------------------
       Learning path with 3 chained nodes x 2 levels each.
       Level 1 "Cơ bản":   5 câu từ material Cơ bản,   max 10, pass 5.
       Level 2 "Nâng cao": 5 câu Trung cấp + 3 câu Nâng cao, max 10, pass 7,
                           giới hạn 3 lượt, 15 phút.
       ----------------------------------------------------------------- */
    INSERT INTO dbo.learning_paths (subject_id, name, description, created_by, status, created_at)
    VALUES (@subjectId, N'Lộ trình demo Node Levels', N'Lộ trình demo 3 node x 2 level', @lecturerId, N'ACTIVE', SYSDATETIME());
    DECLARE @pathId INT = SCOPE_IDENTITY();

    DECLARE @n INT = 1, @nodeId INT, @prevNodeId INT = NULL, @levelId INT;
    WHILE @n <= 3
    BEGIN
        INSERT INTO dbo.learning_nodes
            (learning_path_id, title, description, node_order, prerequisite_node_id, node_type, created_at)
        VALUES
            (@pathId, CONCAT(N'Node ', @n, N': Chủ đề ', @n),
             CONCAT(N'Nội dung chủ đề số ', @n), @n, @prevNodeId, N'LESSON', SYSDATETIME());
        SET @nodeId = SCOPE_IDENTITY();

        INSERT INTO dbo.node_contents (node_id, content_type, title, content_text, display_order, created_at)
        VALUES (@nodeId, N'TEXT', CONCAT(N'Bài đọc chủ đề ', @n),
                CONCAT(N'Nội dung bài học của node ', @n, N'.'), 1, SYSDATETIME());

        INSERT INTO dbo.node_levels (learning_node_id, level_number, title, max_score, passing_score, max_attempts, duration_minutes)
        VALUES (@nodeId, 1, N'Cơ bản', 10.00, 5.00, NULL, NULL);
        SET @levelId = SCOPE_IDENTITY();

        INSERT INTO dbo.level_materials (node_level_id, material_id, question_count)
        VALUES (@levelId, @matBasic, 5);

        INSERT INTO dbo.node_levels (learning_node_id, level_number, title, max_score, passing_score, max_attempts, duration_minutes)
        VALUES (@nodeId, 2, N'Nâng cao', 10.00, 7.00, 3, 15);
        SET @levelId = SCOPE_IDENTITY();

        INSERT INTO dbo.level_materials (node_level_id, material_id, question_count)
        VALUES (@levelId, @matMid, 5),
               (@levelId, @matAdv, 3);

        SET @prevNodeId = @nodeId;
        SET @n += 1;
    END;

    /* -----------------------------------------------------------------
       Semester + classroom + active student + visible nodes.
       ----------------------------------------------------------------- */
    DECLARE @semesterId INT = (SELECT MIN(id) FROM dbo.semesters WHERE status = N'ACTIVE');
    IF @semesterId IS NULL
    BEGIN
        INSERT INTO dbo.semesters (name, start_date, end_date, status)
        VALUES (N'Demo Semester', CAST(SYSDATETIME() AS DATE), DATEADD(MONTH, 4, CAST(SYSDATETIME() AS DATE)), N'ACTIVE');
        SET @semesterId = SCOPE_IDENTITY();
    END;

    INSERT INTO dbo.classrooms
        (subject_id, learning_path_id, teacher_id, name, semester_id, status, invite_code, created_by, created_at)
    VALUES
        (@subjectId, @pathId, @lecturerId, N'Lớp demo Node Levels', @semesterId, N'OPEN', N'NLVL2026', @lecturerId, SYSDATETIME());
    DECLARE @classroomId INT = SCOPE_IDENTITY();

    INSERT INTO dbo.classroom_members (classroom_id, user_id, role_in_class, status, joined_at)
    VALUES (@classroomId, @studentId, N'STUDENT', N'ACTIVE', SYSDATETIME());

    INSERT INTO dbo.classroom_node_status (classroom_id, node_id, status, updated_at)
    SELECT @classroomId, ln.id, N'VISIBLE', SYSDATETIME()
    FROM dbo.learning_nodes ln
    WHERE ln.learning_path_id = @pathId;

    COMMIT TRANSACTION;

    PRINT N'>>> NLVL-DEMO seeded: subject/bank, 3 materials x 16 questions,';
    PRINT N'>>> 1 path (3 nodes x 2 levels), 1 classroom with 1 active student.';
    PRINT CONCAT(N'>>> lecturerId=', @lecturerId, N', studentId=', @studentId,
                 N', subjectId=', @subjectId, N', classroomId=', @classroomId);
END;
GO
