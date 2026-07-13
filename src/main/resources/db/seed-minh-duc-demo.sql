/* =====================================================================
   MENTORA - FULL DEMO DATA FOR PHAM MINH DUC

   Run order:
     1. schema.sql
     2. migration-node-levels.sql
     3. migration-node-levels-followup.sql
     4. migration-classroom-completed.sql
     5. migration-level-material-reviews.sql
     6. this file

   Demo accounts (all passwords are: password)
     Lecturer: demo.lecturer@mentora.local
     Student:  demo.student@mentora.local      (progress + attempt history)
     Student:  demo.newstudent@mentora.local   (clean starting state)

   The script is idempotent. Delete subject code MDC-DEMO if a full reseed
   is required. It intentionally does not delete any existing application data.
   ===================================================================== */

USE [MentoraDB];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID(N'dbo.node_levels', N'U') IS NULL
   OR OBJECT_ID(N'dbo.attempt_grants', N'U') IS NULL
   OR OBJECT_ID(N'dbo.student_level_reviews', N'U') IS NULL
BEGIN
    THROW 51000, N'Run the node-level migration scripts before this seed.', 1;
END;
GO

IF EXISTS (SELECT 1 FROM dbo.subjects WHERE code = N'MDC-DEMO')
BEGIN
    PRINT N'>>> MDC-DEMO already exists. Seed was not changed.';
    PRINT N'>>> To reseed, remove the MDC-DEMO data first, then run this file again.';
END
ELSE
BEGIN
    BEGIN TRY
        BEGIN TRANSACTION;

        /* ---------- Roles and predictable demo users ---------- */
        IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = N'LECTURER')
            INSERT INTO dbo.roles(name) VALUES (N'LECTURER');
        IF NOT EXISTS (SELECT 1 FROM dbo.roles WHERE name = N'STUDENT')
            INSERT INTO dbo.roles(name) VALUES (N'STUDENT');

        DECLARE @lecturerRole INT = (SELECT id FROM dbo.roles WHERE name = N'LECTURER');
        DECLARE @studentRole INT = (SELECT id FROM dbo.roles WHERE name = N'STUDENT');
        -- BCrypt for literal password: password
        DECLARE @password NVARCHAR(255) = N'$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

        IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = N'demo.lecturer@mentora.local')
            INSERT dbo.users(full_name,email,password,role_id,status,email_verified)
            VALUES(N'Nguyễn Minh - Giảng viên Demo',N'demo.lecturer@mentora.local',@password,@lecturerRole,N'ACTIVE',1);
        DECLARE @lecturerId INT = (SELECT id FROM dbo.users WHERE email=N'demo.lecturer@mentora.local');

        IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = N'demo.student@mentora.local')
            INSERT dbo.users(full_name,email,password,role_id,status,email_verified)
            VALUES(N'Phạm An - Sinh viên đang học',N'demo.student@mentora.local',@password,@studentRole,N'ACTIVE',1);
        DECLARE @studentId INT = (SELECT id FROM dbo.users WHERE email=N'demo.student@mentora.local');

        IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE email = N'demo.newstudent@mentora.local')
            INSERT dbo.users(full_name,email,password,role_id,status,email_verified)
            VALUES(N'Trần Bình - Sinh viên mới',N'demo.newstudent@mentora.local',@password,@studentRole,N'ACTIVE',1);
        DECLARE @newStudentId INT = (SELECT id FROM dbo.users WHERE email=N'demo.newstudent@mentora.local');

        /* ---------- Semester, subject and question bank ---------- */
        INSERT dbo.semesters(name,start_date,end_date,status)
        VALUES(N'Học kỳ Summer 2026 - Demo',DATEADD(DAY,-30,CAST(GETDATE() AS date)),DATEADD(DAY,90,CAST(GETDATE() AS date)),N'ACTIVE');
        DECLARE @semesterId INT = SCOPE_IDENTITY();

        INSERT dbo.subjects(code,name,description,status,created_at)
        VALUES(N'MDC-DEMO',N'Kỹ nghệ phần mềm',N'Dữ liệu trình diễn roadmap, level, tiến độ, điểm số và hỏi đáp lớp học.',N'ACTIVE',DATEADD(DAY,-60,SYSDATETIME()));
        DECLARE @subjectId INT = SCOPE_IDENTITY();

        -- A hidden subject makes the Subject Management screen less empty.
        IF NOT EXISTS (SELECT 1 FROM dbo.subjects WHERE code=N'MDC-OLD')
            INSERT dbo.subjects(code,name,description,status,created_at)
            VALUES(N'MDC-OLD',N'Nhập môn CNTT (đã ẩn)',N'Môn mẫu để demo trạng thái ẩn.',N'HIDDEN',DATEADD(DAY,-120,SYSDATETIME()));

        INSERT dbo.question_banks(subject_id,status,created_at)
        VALUES(@subjectId,N'ACTIVE',DATEADD(DAY,-45,SYSDATETIME()));
        DECLARE @bankId INT = SCOPE_IDENTITY();

        /* ---------- Materials and a healthy question pool ---------- */
        INSERT dbo.materials(subject_id,title,description,created_by,created_at)
        VALUES(@subjectId,N'Yêu cầu phần mềm',N'Functional/non-functional requirements và user story.',@lecturerId,DATEADD(DAY,-40,SYSDATETIME()));
        DECLARE @matReq INT = SCOPE_IDENTITY();
        INSERT dbo.materials(subject_id,title,description,created_by,created_at)
        VALUES(@subjectId,N'UML và thiết kế',N'Use case, class diagram và sequence diagram.',@lecturerId,DATEADD(DAY,-35,SYSDATETIME()));
        DECLARE @matUml INT = SCOPE_IDENTITY();
        INSERT dbo.materials(subject_id,title,description,created_by,created_at)
        VALUES(@subjectId,N'Kiểm thử phần mềm',N'Test case, boundary value và regression testing.',@lecturerId,DATEADD(DAY,-30,SYSDATETIME()));
        DECLARE @matTest INT = SCOPE_IDENTITY();

        -- Material-owned review content: unlocked for students after passing
        -- the level that references the material.
        INSERT dbo.node_contents(node_id,material_id,content_type,title,content_text,display_order,created_at)
        VALUES(NULL,@matReq,N'TEXT',N'Ôn tập: Phân loại yêu cầu',N'Functional requirement mô tả hệ thống làm gì. Non-functional requirement mô tả thuộc tính chất lượng và ràng buộc.',1,DATEADD(DAY,-29,SYSDATETIME())),
              (NULL,@matUml,N'TEXT',N'Ôn tập: Chọn biểu đồ UML',N'Use case mô tả tương tác; sequence mô tả thông điệp theo thời gian; class mô tả cấu trúc tĩnh.',1,DATEADD(DAY,-28,SYSDATETIME())),
              (NULL,@matTest,N'TEXT',N'Ôn tập: Boundary value',N'Với miền hợp lệ 0–10, tập giá trị biên điển hình gồm -1, 0, 1, 9, 10 và 11.',1,DATEADD(DAY,-27,SYSDATETIME()));

        DECLARE @m INT=1,@i INT,@materialId INT,@label NVARCHAR(100),@questionId INT;
        WHILE @m<=3
        BEGIN
            SET @materialId=CASE @m WHEN 1 THEN @matReq WHEN 2 THEN @matUml ELSE @matTest END;
            SET @label=CASE @m WHEN 1 THEN N'Yêu cầu phần mềm' WHEN 2 THEN N'UML và thiết kế' ELSE N'Kiểm thử phần mềm' END;
            SET @i=1;
            WHILE @i<=12
            BEGIN
                INSERT dbo.bank_questions(question_bank_id,created_by,content,difficulty,question_type,default_score,status,created_at,updated_at,material_id)
                VALUES(@bankId,@lecturerId,CONCAT(N'[',@label,N'] Câu ',@i,N': Chọn đáp án đúng nhất.'),
                    CASE WHEN @i<=4 THEN N'EASY' WHEN @i<=9 THEN N'MEDIUM' ELSE N'HARD' END,
                    N'MULTIPLE_CHOICE',1,N'ACTIVE',DATEADD(DAY,-20,SYSDATETIME()),DATEADD(DAY,-20,SYSDATETIME()),@materialId);
                SET @questionId=SCOPE_IDENTITY();
                INSERT dbo.bank_question_options(bank_question_id,content,is_correct,display_order)
                VALUES(@questionId,CONCAT(N'Đáp án đúng cho câu ',@i),1,0),
                      (@questionId,N'Phương án nhiễu B',0,1),(@questionId,N'Phương án nhiễu C',0,2),(@questionId,N'Phương án nhiễu D',0,3);
                SET @i+=1;
            END;
            SET @m+=1;
        END;

        /* ---------- Learning path: 3 sequential nodes, 2 levels each ---------- */
        INSERT dbo.learning_paths(subject_id,name,description,created_by,status,created_at)
        VALUES(@subjectId,N'Lộ trình Kỹ nghệ phần mềm thực chiến',N'Ba chặng học từ yêu cầu đến kiểm thử.',@lecturerId,N'ACTIVE',DATEADD(DAY,-40,SYSDATETIME()));
        DECLARE @pathId INT=SCOPE_IDENTITY();
        DECLARE @node1 INT,@node2 INT,@node3 INT,@level11 INT,@level12 INT,@level21 INT,@level22 INT,@level31 INT,@level32 INT;

        INSERT dbo.learning_nodes(learning_path_id,title,description,node_order,prerequisite_node_id,node_type,created_at)
        VALUES(@pathId,N'1. Phân tích yêu cầu',N'Xác định stakeholder, user story và acceptance criteria.',1,NULL,N'LESSON',DATEADD(DAY,-35,SYSDATETIME()));
        SET @node1=SCOPE_IDENTITY();
        INSERT dbo.learning_nodes(learning_path_id,title,description,node_order,prerequisite_node_id,node_type,created_at)
        VALUES(@pathId,N'2. Mô hình hóa hệ thống',N'Thiết kế hệ thống bằng các biểu đồ UML.',2,@node1,N'LESSON',DATEADD(DAY,-34,SYSDATETIME()));
        SET @node2=SCOPE_IDENTITY();
        INSERT dbo.learning_nodes(learning_path_id,title,description,node_order,prerequisite_node_id,node_type,created_at)
        VALUES(@pathId,N'3. Thiết kế kiểm thử',N'Xây dựng test case và chiến lược regression.',3,@node2,N'LESSON',DATEADD(DAY,-33,SYSDATETIME()));
        SET @node3=SCOPE_IDENTITY();

        -- A content item has exactly one owner: either a node or a material.
        -- These are node-owned lessons, so material_id must remain NULL.
        INSERT dbo.node_contents(node_id,material_id,content_type,title,content_text,display_order,created_at)
        VALUES(@node1,NULL,N'TEXT',N'Đọc: Cách viết user story',N'As a user, I want ... so that ...; kèm acceptance criteria rõ ràng.',1,DATEADD(DAY,-30,SYSDATETIME())),
              (@node1,NULL,N'LINK',N'Video phân tích yêu cầu',NULL,2,DATEADD(DAY,-30,SYSDATETIME())),
              (@node2,NULL,N'TEXT',N'Đọc: UML trong dự án',N'Chọn đúng loại biểu đồ cho từng mục tiêu giao tiếp.',1,DATEADD(DAY,-29,SYSDATETIME())),
              (@node3,NULL,N'TEXT',N'Đọc: Thiết kế test case',N'Mỗi test case có tiền điều kiện, bước chạy và kết quả mong đợi.',1,DATEADD(DAY,-28,SYSDATETIME()));

        INSERT dbo.node_levels(learning_node_id,level_number,title,max_score,passing_score,max_attempts,duration_minutes)
        VALUES(@node1,1,N'Cơ bản',10,5,NULL,NULL); SET @level11=SCOPE_IDENTITY();
        INSERT dbo.node_levels(learning_node_id,level_number,title,max_score,passing_score,max_attempts,duration_minutes)
        VALUES(@node1,2,N'Nâng cao',10,7,3,NULL); SET @level12=SCOPE_IDENTITY();
        INSERT dbo.node_levels(learning_node_id,level_number,title,max_score,passing_score,max_attempts,duration_minutes)
        VALUES(@node2,1,N'Cơ bản',10,5,NULL,NULL); SET @level21=SCOPE_IDENTITY();
        INSERT dbo.node_levels(learning_node_id,level_number,title,max_score,passing_score,max_attempts,duration_minutes)
        VALUES(@node2,2,N'Nâng cao',10,7,3,NULL); SET @level22=SCOPE_IDENTITY();
        INSERT dbo.node_levels(learning_node_id,level_number,title,max_score,passing_score,max_attempts,duration_minutes)
        VALUES(@node3,1,N'Cơ bản',10,5,NULL,NULL); SET @level31=SCOPE_IDENTITY();
        INSERT dbo.node_levels(learning_node_id,level_number,title,max_score,passing_score,max_attempts,duration_minutes)
        VALUES(@node3,2,N'Nâng cao',10,7,3,NULL); SET @level32=SCOPE_IDENTITY();

        INSERT dbo.level_materials(node_level_id,material_id,question_count)
        VALUES(@level11,@matReq,5),(@level12,@matReq,10),(@level21,@matUml,5),(@level22,@matUml,10),
              (@level31,@matTest,5),(@level32,@matTest,10);

        /* ---------- Classroom, members and visibility ---------- */
        INSERT dbo.classrooms(subject_id,learning_path_id,teacher_id,name,semester_id,status,invite_code,created_by,created_at)
        VALUES(@subjectId,@pathId,@lecturerId,N'SE1801 - Kỹ nghệ phần mềm',@semesterId,N'OPEN',N'MDC2026',@lecturerId,DATEADD(DAY,-25,SYSDATETIME()));
        DECLARE @classroomId INT=SCOPE_IDENTITY();
        INSERT dbo.classroom_members(classroom_id,user_id,role_in_class,status,joined_at)
        VALUES(@classroomId,@studentId,N'STUDENT',N'ACTIVE',DATEADD(DAY,-24,SYSDATETIME())),
              (@classroomId,@newStudentId,N'STUDENT',N'ACTIVE',DATEADD(DAY,-2,SYSDATETIME()));
        INSERT dbo.classroom_node_status(classroom_id,node_id,status,updated_at)
        VALUES(@classroomId,@node1,N'VISIBLE',DATEADD(DAY,-20,SYSDATETIME())),
              (@classroomId,@node2,N'VISIBLE',DATEADD(DAY,-15,SYSDATETIME())),
              (@classroomId,@node3,N'VISIBLE',DATEADD(DAY,-10,SYSDATETIME()));

        /* ---------- Progress + meaningful attempt history ---------- */
        INSERT dbo.node_progress(classroom_id,student_id,node_id,is_completed,completed_at,best_score,best_level_number)
        VALUES(@classroomId,@studentId,@node1,1,DATEADD(DAY,-7,SYSDATETIME()),8.50,2),
              (@classroomId,@studentId,@node2,0,NULL,6.00,1);

        INSERT dbo.node_level_attempts(node_level_id,student_id,classroom_id,attempt_number,score,status,started_at,submitted_at,passed,timed_out)
        VALUES(@level11,@studentId,@classroomId,1,4.00,N'SUBMITTED',DATEADD(DAY,-12,SYSDATETIME()),DATEADD(MINUTE,12,DATEADD(DAY,-12,SYSDATETIME())),0,0),
              (@level11,@studentId,@classroomId,2,7.50,N'SUBMITTED',DATEADD(DAY,-10,SYSDATETIME()),DATEADD(MINUTE,9,DATEADD(DAY,-10,SYSDATETIME())),1,0),
              (@level12,@studentId,@classroomId,1,8.50,N'SUBMITTED',DATEADD(DAY,-7,SYSDATETIME()),DATEADD(MINUTE,14,DATEADD(DAY,-7,SYSDATETIME())),1,0),
              (@level21,@studentId,@classroomId,1,6.00,N'SUBMITTED',DATEADD(DAY,-2,SYSDATETIME()),DATEADD(MINUTE,8,DATEADD(DAY,-2,SYSDATETIME())),1,0);
        -- Historical demo student already reviewed before taking Level 2.
        INSERT dbo.student_level_reviews(node_level_id,student_id,classroom_id,reviewed_at)
        VALUES(@level12,@studentId,@classroomId,DATEADD(DAY,-8,SYSDATETIME()));
        INSERT dbo.attempt_grants(node_level_id,student_id,classroom_id,extra_attempts,granted_by,granted_at)
        VALUES(@level12,@studentId,@classroomId,1,@lecturerId,DATEADD(DAY,-8,SYSDATETIME()));

        /* ---------- Q&A: open, answered, and a conversation ---------- */
        INSERT dbo.classroom_questions(classroom_id,student_id,content,status,created_at)
        VALUES(@classroomId,@studentId,N'Thầy cho em hỏi functional requirement khác non-functional requirement ở điểm nào ạ?',N'ANSWERED',DATEADD(DAY,-6,SYSDATETIME()));
        DECLARE @q1 INT=SCOPE_IDENTITY();
        INSERT dbo.classroom_question_replies(question_id,responder_id,content,official,created_at)
        VALUES(@q1,@lecturerId,N'Functional requirement mô tả hệ thống làm gì; non-functional requirement mô tả chất lượng và ràng buộc khi hệ thống thực hiện.',1,DATEADD(HOUR,2,DATEADD(DAY,-6,SYSDATETIME()))),
              (@q1,@studentId,N'Dạ em hiểu rồi, em sẽ bổ sung tiêu chí đo được cho yêu cầu hiệu năng.',0,DATEADD(HOUR,3,DATEADD(DAY,-6,SYSDATETIME())));
        UPDATE dbo.classroom_questions SET answered_by=@lecturerId,answered_at=DATEADD(HOUR,2,DATEADD(DAY,-6,SYSDATETIME())),
            answer_content=N'Functional requirement mô tả hệ thống làm gì; non-functional requirement mô tả chất lượng và ràng buộc khi hệ thống thực hiện.' WHERE id=@q1;

        INSERT dbo.classroom_questions(classroom_id,student_id,content,status,created_at)
        VALUES(@classroomId,@newStudentId,N'Khi nào nên dùng sequence diagram thay vì activity diagram ạ?',N'OPEN',DATEADD(HOUR,-5,SYSDATETIME())),
              (@classroomId,@studentId,N'Boundary value analysis áp dụng thế nào cho trường điểm từ 0 đến 10?',N'OPEN',DATEADD(HOUR,-2,SYSDATETIME()));

        COMMIT TRANSACTION;
        PRINT N'>>> Full MDC-DEMO data created successfully.';
        PRINT N'>>> Password for all demo accounts: password';
        PRINT N'>>> Lecturer: demo.lecturer@mentora.local';
        PRINT N'>>> Active student: demo.student@mentora.local';
        PRINT N'>>> New student: demo.newstudent@mentora.local';
        PRINT N'>>> Classroom invite code: MDC2026';
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
        THROW;
    END CATCH
END;
GO
