/* =====================================================================
   Mentora LMS - System-test seed (ST-DEMO)

   Khac voi seed-node-levels-demo.sql (tai dung user co san, khong biet
   mat khau), file nay tao TAI KHOAN RIENG mat khau biet truoc de moi
   thanh vien / moi may chay test ra cung mot ket qua.

   Tao ra:
     - 5 tai khoan (mat khau deu la "password"):
         st.gv1@mentora.test   LECTURER  (chu du lieu chinh)
         st.gv2@mentora.test   LECTURER  (chu 1 material - dung tai hien bug #12)
         st.sv1@mentora.test   STUDENT   (ACTIVE trong lop)
         st.sv2@mentora.test   STUDENT   (ACTIVE trong lop)
         st.sv3@mentora.test   STUDENT   (KHONG thuoc lop nao - danh cho test join)
     - 1 mon ST-DEMO + question bank
     - 3 material:
         GV1: "ST Co ban" (6 cau), "ST Nang cao" (4 cau)
         GV2: "ST GV2" (3 cau) - tao SAU CUNG nen dung DAU danh sach
              (danh sach sap theo created_at DESC) => GV1 mo trang
              /lecturer/materials?subjectId=<ST-DEMO> se auto-select
              material cua GV2 -> tai hien bug 500 (business-process 9 #12)
     - 1 path (GV1) 2 node:
         Node 1: Level 1 (M1, 3 cau, khong gioi han luot)
                 Level 2 (M2, 3 cau, maxAttempts=2 - test het luot + grant L7)
         Node 2 (tien quyet Node 1): Level 1 (M1, 2 cau)
     - 1 lop OPEN cua GV1, ma moi ST2026OK, sv1+sv2 ACTIVE, moi node VISIBLE

   Khong tao attempt san - attempt sinh ra khi chay test.
   Admin dung tai khoan app tu tao: admin1@mentora.com / admin123.

   Idempotent: bo qua neu mon ST-DEMO da ton tai.
   Chay SAU schema.sql + migration-node-levels.sql (+ file follow-up).
   Cach chay dung (tranh hong tieng Viet): xem docs/README-systemtest.md
   ===================================================================== */

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

USE [MentoraDB];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

IF EXISTS (SELECT 1 FROM dbo.subjects WHERE code = N'ST-DEMO')
BEGIN
    PRINT N'>>> ST-DEMO already seeded - nothing to do.';
END
ELSE
BEGIN
    BEGIN TRANSACTION;

    /* -----------------------------------------------------------------
       Tai khoan test - luon tao moi theo email, khong tai dung user la.
       Hash BCrypt cua chuoi "password".
       ----------------------------------------------------------------- */
    DECLARE @lecturerRoleId INT = (SELECT id FROM dbo.roles WHERE name = N'LECTURER');
    DECLARE @studentRoleId  INT = (SELECT id FROM dbo.roles WHERE name = N'STUDENT');
    DECLARE @pw NVARCHAR(255) = N'$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

    DECLARE @gv1 INT = (SELECT id FROM dbo.users WHERE email = N'st.gv1@mentora.test');
    IF @gv1 IS NULL
    BEGIN
        INSERT INTO dbo.users (full_name, email, password, role_id, status, email_verified)
        VALUES (N'ST Giảng viên 1', N'st.gv1@mentora.test', @pw, @lecturerRoleId, N'ACTIVE', 1);
        SET @gv1 = SCOPE_IDENTITY();
    END;

    DECLARE @gv2 INT = (SELECT id FROM dbo.users WHERE email = N'st.gv2@mentora.test');
    IF @gv2 IS NULL
    BEGIN
        INSERT INTO dbo.users (full_name, email, password, role_id, status, email_verified)
        VALUES (N'ST Giảng viên 2', N'st.gv2@mentora.test', @pw, @lecturerRoleId, N'ACTIVE', 1);
        SET @gv2 = SCOPE_IDENTITY();
    END;

    DECLARE @sv1 INT = (SELECT id FROM dbo.users WHERE email = N'st.sv1@mentora.test');
    IF @sv1 IS NULL
    BEGIN
        INSERT INTO dbo.users (full_name, email, password, role_id, status, email_verified)
        VALUES (N'ST Sinh viên 1', N'st.sv1@mentora.test', @pw, @studentRoleId, N'ACTIVE', 1);
        SET @sv1 = SCOPE_IDENTITY();
    END;

    DECLARE @sv2 INT = (SELECT id FROM dbo.users WHERE email = N'st.sv2@mentora.test');
    IF @sv2 IS NULL
    BEGIN
        INSERT INTO dbo.users (full_name, email, password, role_id, status, email_verified)
        VALUES (N'ST Sinh viên 2', N'st.sv2@mentora.test', @pw, @studentRoleId, N'ACTIVE', 1);
        SET @sv2 = SCOPE_IDENTITY();
    END;

    DECLARE @sv3 INT = (SELECT id FROM dbo.users WHERE email = N'st.sv3@mentora.test');
    IF @sv3 IS NULL
    BEGIN
        INSERT INTO dbo.users (full_name, email, password, role_id, status, email_verified)
        VALUES (N'ST Sinh viên 3 (chưa vào lớp)', N'st.sv3@mentora.test', @pw, @studentRoleId, N'ACTIVE', 1);
        SET @sv3 = SCOPE_IDENTITY();
    END;

    /* -----------------------------------------------------------------
       Mon + bank.
       ----------------------------------------------------------------- */
    INSERT INTO dbo.subjects (code, name, description, status, created_at)
    VALUES (N'ST-DEMO', N'System Test Demo', N'Môn dành riêng cho system test — dữ liệu biết trước', N'ACTIVE', SYSDATETIME());
    DECLARE @subjectId INT = SCOPE_IDENTITY();

    INSERT INTO dbo.question_banks (subject_id, status, created_at)
    VALUES (@subjectId, N'ACTIVE', SYSDATETIME());
    DECLARE @bankId INT = SCOPE_IDENTITY();

    /* -----------------------------------------------------------------
       Material + cau hoi. Dap an dung LUON la "Phương án A".
       Thu tu tao quan trong: material cua GV2 tao SAU CUNG (moi nhat)
       de no dung dau danh sach -> tai hien bug #12 mot cach xac dinh.
       ----------------------------------------------------------------- */
    /* created_at gan tuong minh lech nhau tung giay: 3 INSERT trong cung
       transaction co the trung created_at den nano-giay -> ORDER BY
       created_at DESC hoa -> thu tu tuy y, mat tinh tat dinh cua repro
       bug #12 (da vap that 2026-07-13). Material GV2 phai MOI NHAT. */
    DECLARE @m1 INT, @m2 INT, @mGv2 INT, @now DATETIME2 = SYSDATETIME();

    INSERT INTO dbo.materials (subject_id, title, description, created_by, created_at)
    VALUES (@subjectId, N'ST Cơ bản', N'Material 1 của GV1 — 6 câu', @gv1, DATEADD(SECOND, -2, @now));
    SET @m1 = SCOPE_IDENTITY();

    INSERT INTO dbo.materials (subject_id, title, description, created_by, created_at)
    VALUES (@subjectId, N'ST Nâng cao', N'Material 2 của GV1 — 4 câu', @gv1, DATEADD(SECOND, -1, @now));
    SET @m2 = SCOPE_IDENTITY();

    INSERT INTO dbo.materials (subject_id, title, description, created_by, created_at)
    VALUES (@subjectId, N'ST GV2', N'Material của GV2 — đứng đầu danh sách, dùng tái hiện bug #12', @gv2, @now);
    SET @mGv2 = SCOPE_IDENTITY();

    DECLARE @i INT, @qId INT, @owner INT, @matId INT, @total INT, @label NVARCHAR(30), @m INT = 1;
    WHILE @m <= 3
    BEGIN
        SELECT @matId = CASE @m WHEN 1 THEN @m1 WHEN 2 THEN @m2 ELSE @mGv2 END,
               @total = CASE @m WHEN 1 THEN 6   WHEN 2 THEN 4   ELSE 3 END,
               @owner = CASE @m WHEN 1 THEN @gv1 WHEN 2 THEN @gv1 ELSE @gv2 END,
               @label = CASE @m WHEN 1 THEN N'ST Cơ bản' WHEN 2 THEN N'ST Nâng cao' ELSE N'ST GV2' END;

        SET @i = 1;
        WHILE @i <= @total
        BEGIN
            INSERT INTO dbo.bank_questions
                (question_bank_id, created_by, content, difficulty, question_type, default_score, status, material_id)
            VALUES
                (@bankId, @owner,
                 CONCAT(N'[', @label, N'] Câu ', @i, N': đáp án đúng là Phương án A.'),
                 CASE @i % 3 WHEN 1 THEN N'EASY' WHEN 2 THEN N'MEDIUM' ELSE N'HARD' END,
                 N'MULTIPLE_CHOICE', 1.00, N'ACTIVE', @matId);
            SET @qId = SCOPE_IDENTITY();

            INSERT INTO dbo.bank_question_options (bank_question_id, content, is_correct, display_order)
            VALUES
                (@qId, CONCAT(N'Phương án A (đúng) — câu ', @i), 1, 0),
                (@qId, CONCAT(N'Phương án B — câu ', @i), 0, 1),
                (@qId, CONCAT(N'Phương án C — câu ', @i), 0, 2),
                (@qId, CONCAT(N'Phương án D — câu ', @i), 0, 3);
            SET @i += 1;
        END;

        /* Moi material 1 noi dung TEXT (material-owned, node_id NULL). */
        INSERT INTO dbo.node_contents (node_id, material_id, content_type, title, content_text, display_order, created_at)
        VALUES (NULL, @matId, N'TEXT', CONCAT(N'Tổng quan ', @label), CONCAT(N'Nội dung ôn tập ', @label, N'.'), 1, SYSDATETIME());

        SET @m += 1;
    END;

    /* -----------------------------------------------------------------
       Path GV1: 2 node.
       ----------------------------------------------------------------- */
    INSERT INTO dbo.learning_paths (subject_id, name, description, created_by, status, created_at)
    VALUES (@subjectId, N'Lộ trình ST', N'Lộ trình system test — 2 node', @gv1, N'ACTIVE', SYSDATETIME());
    DECLARE @pathId INT = SCOPE_IDENTITY();

    /* Node 1 */
    INSERT INTO dbo.learning_nodes (learning_path_id, title, description, node_order, prerequisite_node_id, node_type, created_at)
    VALUES (@pathId, N'ST Node 1', N'Node đầu — 2 level', 1, NULL, N'LESSON', SYSDATETIME());
    DECLARE @node1 INT = SCOPE_IDENTITY();

    INSERT INTO dbo.node_contents (node_id, content_type, title, content_text, display_order, created_at)
    VALUES (@node1, N'TEXT', N'Bài đọc Node 1', N'Nội dung bài học node 1.', 1, SYSDATETIME());

    INSERT INTO dbo.node_levels (learning_node_id, level_number, title, max_score, passing_score, max_attempts, duration_minutes)
    VALUES (@node1, 1, N'Cơ bản', 10.00, 5.00, NULL, NULL);
    DECLARE @lvl INT = SCOPE_IDENTITY();
    INSERT INTO dbo.level_materials (node_level_id, material_id, question_count) VALUES (@lvl, @m1, 3);

    INSERT INTO dbo.node_levels (learning_node_id, level_number, title, max_score, passing_score, max_attempts, duration_minutes)
    VALUES (@node1, 2, N'Nâng cao', 10.00, 7.00, 2, NULL);
    SET @lvl = SCOPE_IDENTITY();
    INSERT INTO dbo.level_materials (node_level_id, material_id, question_count) VALUES (@lvl, @m2, 3);

    /* Node 2 - tien quyet Node 1 */
    INSERT INTO dbo.learning_nodes (learning_path_id, title, description, node_order, prerequisite_node_id, node_type, created_at)
    VALUES (@pathId, N'ST Node 2', N'Node sau — mở khi xong Node 1', 2, @node1, N'LESSON', SYSDATETIME());
    DECLARE @node2 INT = SCOPE_IDENTITY();

    INSERT INTO dbo.node_contents (node_id, content_type, title, content_text, display_order, created_at)
    VALUES (@node2, N'TEXT', N'Bài đọc Node 2', N'Nội dung bài học node 2.', 1, SYSDATETIME());

    INSERT INTO dbo.node_levels (learning_node_id, level_number, title, max_score, passing_score, max_attempts, duration_minutes)
    VALUES (@node2, 1, N'Cơ bản', 10.00, 5.00, NULL, NULL);
    SET @lvl = SCOPE_IDENTITY();
    INSERT INTO dbo.level_materials (node_level_id, material_id, question_count) VALUES (@lvl, @m1, 2);

    /* -----------------------------------------------------------------
       Hoc ky + lop + thanh vien + node visible.
       ----------------------------------------------------------------- */
    DECLARE @semesterId INT = (SELECT MIN(id) FROM dbo.semesters WHERE status = N'ACTIVE');
    IF @semesterId IS NULL
    BEGIN
        INSERT INTO dbo.semesters (name, start_date, end_date, status)
        VALUES (N'ST Semester', CAST(SYSDATETIME() AS DATE), DATEADD(MONTH, 4, CAST(SYSDATETIME() AS DATE)), N'ACTIVE');
        SET @semesterId = SCOPE_IDENTITY();
    END;

    INSERT INTO dbo.classrooms
        (subject_id, learning_path_id, teacher_id, name, semester_id, status, invite_code, created_by, created_at)
    VALUES
        (@subjectId, @pathId, @gv1, N'Lớp System Test', @semesterId, N'OPEN', N'ST2026OK', @gv1, SYSDATETIME());
    DECLARE @classroomId INT = SCOPE_IDENTITY();

    INSERT INTO dbo.classroom_members (classroom_id, user_id, role_in_class, status, joined_at)
    VALUES (@classroomId, @sv1, N'STUDENT', N'ACTIVE', SYSDATETIME()),
           (@classroomId, @sv2, N'STUDENT', N'ACTIVE', SYSDATETIME());

    INSERT INTO dbo.classroom_node_status (classroom_id, node_id, status, updated_at)
    SELECT @classroomId, ln.id, N'VISIBLE', SYSDATETIME()
    FROM dbo.learning_nodes ln WHERE ln.learning_path_id = @pathId;

    COMMIT TRANSACTION;

    PRINT N'>>> ST-DEMO seeded.';
    PRINT CONCAT(N'>>> subjectId=', @subjectId, N', classroomId=', @classroomId,
                 N', gv1=', @gv1, N', gv2=', @gv2, N', sv1=', @sv1, N', sv2=', @sv2, N', sv3=', @sv3);
    PRINT N'>>> Bug #12 repro: login st.gv1@mentora.test -> mo /lecturer/materials?subjectId= tren mon ST-DEMO (khong kem materialId).';
END;
GO
