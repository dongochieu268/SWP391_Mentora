/* =====================================================================
   Mentora LMS - Xoa sach du lieu ST-DEMO (di kem seed-systemtest.sql)

   Xoa moi thu thuoc mon ST-DEMO ke ca du lieu test sinh ra khi chay
   (attempt, grant, QnA, node_progress, material tao them trong mon...).
   GIU LAI 5 tai khoan st.* (vo hai, mat khau biet truoc, dung lai duoc).

   Thu tu xoa la FK-safe - da do bang tay 2026-07-13 (classroom_node_status
   va node_contents tung lam vap khi don theo thu tu ngay tho).

   Quy trinh reset chuan: chay file nay -> chay lai seed-systemtest.sql.
   ===================================================================== */

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO

USE [MentoraDB];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @subjectId INT = (SELECT id FROM dbo.subjects WHERE code = N'ST-DEMO');
IF @subjectId IS NULL
BEGIN
    PRINT N'>>> ST-DEMO not found - nothing to reset.';
END
ELSE
BEGIN
    BEGIN TRANSACTION;

    DECLARE @classrooms TABLE (id INT);
    INSERT INTO @classrooms SELECT id FROM dbo.classrooms WHERE subject_id = @subjectId;

    /* 1. Du lieu attempt (snapshot truoc, attempt sau) */
    DELETE aqo FROM dbo.attempt_question_options aqo
      JOIN dbo.attempt_questions aq ON aqo.attempt_question_id = aq.id
      JOIN dbo.node_level_attempts nla ON aq.attempt_id = nla.id
      WHERE nla.classroom_id IN (SELECT id FROM @classrooms);
    DELETE aq FROM dbo.attempt_questions aq
      JOIN dbo.node_level_attempts nla ON aq.attempt_id = nla.id
      WHERE nla.classroom_id IN (SELECT id FROM @classrooms);
    DELETE FROM dbo.node_level_attempts WHERE classroom_id IN (SELECT id FROM @classrooms);
    DELETE FROM dbo.attempt_grants      WHERE classroom_id IN (SELECT id FROM @classrooms);
    IF OBJECT_ID(N'dbo.student_level_reviews', N'U') IS NOT NULL
        DELETE FROM dbo.student_level_reviews WHERE classroom_id IN (SELECT id FROM @classrooms);

    /* 2. Tien do + QnA + legacy attempt cua lop */
    DELETE FROM dbo.node_progress WHERE classroom_id IN (SELECT id FROM @classrooms);
    DELETE r FROM dbo.classroom_question_replies r
      JOIN dbo.classroom_questions q ON r.question_id = q.id
      WHERE q.classroom_id IN (SELECT id FROM @classrooms);
    DELETE FROM dbo.classroom_questions WHERE classroom_id IN (SELECT id FROM @classrooms);
    DELETE FROM dbo.assessment_attempts WHERE classroom_id IN (SELECT id FROM @classrooms);

    /* 3. Lop */
    DELETE FROM dbo.classroom_node_status WHERE classroom_id IN (SELECT id FROM @classrooms);
    DELETE FROM dbo.classroom_members     WHERE classroom_id IN (SELECT id FROM @classrooms);
    DELETE FROM dbo.classrooms            WHERE id IN (SELECT id FROM @classrooms);

    /* 4. Legacy branching + assessment cua mon (neu test sinh ra) */
    DELETE FROM dbo.student_branch_assignments
      WHERE classroom_id IN (SELECT id FROM @classrooms);
    DELETE br FROM dbo.branch_rules br
      JOIN dbo.learning_nodes ln ON br.node_id = ln.id
      JOIN dbo.learning_paths lp ON ln.learning_path_id = lp.id
      WHERE lp.subject_id = @subjectId;
    DELETE qo FROM dbo.question_options qo
      JOIN dbo.questions q ON qo.question_id = q.id
      JOIN dbo.assessments a ON q.assessment_id = a.id
      WHERE a.subject_id = @subjectId;
    DELETE q FROM dbo.questions q
      JOIN dbo.assessments a ON q.assessment_id = a.id
      WHERE a.subject_id = @subjectId;
    DELETE FROM dbo.assessments WHERE subject_id = @subjectId;

    /* 5. Cau truc path (level_materials -> node_levels -> node_contents -> nodes -> path) */
    DELETE lm FROM dbo.level_materials lm
      JOIN dbo.node_levels nl ON lm.node_level_id = nl.id
      JOIN dbo.learning_nodes ln ON nl.learning_node_id = ln.id
      JOIN dbo.learning_paths lp ON ln.learning_path_id = lp.id
      WHERE lp.subject_id = @subjectId;
    DELETE nl FROM dbo.node_levels nl
      JOIN dbo.learning_nodes ln ON nl.learning_node_id = ln.id
      JOIN dbo.learning_paths lp ON ln.learning_path_id = lp.id
      WHERE lp.subject_id = @subjectId;
    DELETE nc FROM dbo.node_contents nc
      JOIN dbo.learning_nodes ln ON nc.node_id = ln.id
      JOIN dbo.learning_paths lp ON ln.learning_path_id = lp.id
      WHERE lp.subject_id = @subjectId;
    DELETE ln FROM dbo.learning_nodes ln
      JOIN dbo.learning_paths lp ON ln.learning_path_id = lp.id
      WHERE lp.subject_id = @subjectId;
    DELETE FROM dbo.learning_paths WHERE subject_id = @subjectId;

    /* 6. Material + bank (noi dung material truoc, cau hoi sau) */
    DELETE FROM dbo.node_contents
      WHERE material_id IN (SELECT id FROM dbo.materials WHERE subject_id = @subjectId);
    DELETE bqo FROM dbo.bank_question_options bqo
      JOIN dbo.bank_questions bq ON bqo.bank_question_id = bq.id
      JOIN dbo.question_banks qb ON bq.question_bank_id = qb.id
      WHERE qb.subject_id = @subjectId;
    DELETE bq FROM dbo.bank_questions bq
      JOIN dbo.question_banks qb ON bq.question_bank_id = qb.id
      WHERE qb.subject_id = @subjectId;
    DELETE FROM dbo.materials WHERE subject_id = @subjectId;
    DELETE FROM dbo.question_banks WHERE subject_id = @subjectId;

    /* 7. Mon (tien quyet neu co) */
    DELETE FROM dbo.subject_prerequisites
      WHERE subject_id = @subjectId OR prerequisite_subject_id = @subjectId;
    DELETE FROM dbo.subjects WHERE id = @subjectId;

    COMMIT TRANSACTION;
    PRINT N'>>> ST-DEMO reset done (tai khoan st.* giu nguyen).';
END;
GO
