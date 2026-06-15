IF OBJECT_ID('dbo.classroom_questions', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.classroom_questions (
        id              INT IDENTITY(1,1) PRIMARY KEY,
        classroom_id    INT NOT NULL,
        student_id      INT NOT NULL,
        content         NVARCHAR(1000) NOT NULL,
        status          NVARCHAR(20) NOT NULL CONSTRAINT DF_classroom_questions_status DEFAULT 'OPEN',
        created_at      DATETIME2 NOT NULL CONSTRAINT DF_classroom_questions_created_at DEFAULT SYSDATETIME(),
        answered_at     DATETIME2 NULL,
        answered_by     INT NULL,
        answer_content  NVARCHAR(2000) NULL,

        CONSTRAINT FK_classroom_questions_classroom
            FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT FK_classroom_questions_student
            FOREIGN KEY (student_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_classroom_questions_answered_by
            FOREIGN KEY (answered_by) REFERENCES dbo.users(id),
        CONSTRAINT CHK_classroom_questions_status
            CHECK (status IN ('OPEN', 'ANSWERED'))
    );

    CREATE INDEX IX_classroom_questions_classroom_created
        ON dbo.classroom_questions(classroom_id, created_at DESC);

    CREATE INDEX IX_classroom_questions_student
        ON dbo.classroom_questions(classroom_id, student_id, created_at DESC);
END

IF OBJECT_ID('dbo.classroom_question_replies', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.classroom_question_replies (
        id              INT IDENTITY(1,1) PRIMARY KEY,
        question_id     INT NOT NULL,
        responder_id    INT NOT NULL,
        content         NVARCHAR(2000) NOT NULL,
        official        BIT NOT NULL CONSTRAINT DF_classroom_question_replies_official DEFAULT 0,
        created_at      DATETIME2 NOT NULL CONSTRAINT DF_classroom_question_replies_created_at DEFAULT SYSDATETIME(),

        CONSTRAINT FK_classroom_question_replies_question
            FOREIGN KEY (question_id) REFERENCES dbo.classroom_questions(id),
        CONSTRAINT FK_classroom_question_replies_responder
            FOREIGN KEY (responder_id) REFERENCES dbo.users(id)
    );

    CREATE INDEX IX_classroom_question_replies_question_created
        ON dbo.classroom_question_replies(question_id, created_at ASC);
END

IF OBJECT_ID('dbo.classroom_question_replies', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.classroom_question_replies', 'official') IS NULL
BEGIN
    ALTER TABLE dbo.classroom_question_replies
        ADD official BIT NOT NULL CONSTRAINT DF_classroom_question_replies_official DEFAULT 0;
END

IF COL_LENGTH('dbo.classroom_questions', 'answer_content') IS NOT NULL
BEGIN
    EXEC('
        INSERT INTO dbo.classroom_question_replies (question_id, responder_id, content, official, created_at)
        SELECT q.id, q.answered_by, q.answer_content, 1, COALESCE(q.answered_at, q.created_at)
        FROM dbo.classroom_questions q
        WHERE q.answer_content IS NOT NULL
          AND q.answered_by IS NOT NULL
          AND NOT EXISTS (
              SELECT 1
              FROM dbo.classroom_question_replies r
              WHERE r.question_id = q.id
          );
    ');
END

EXEC('
    UPDATE r
    SET official = CASE
        WHEN roles.name = ''LECTURER'' THEN 1
        WHEN cm.role_in_class = ''TA'' THEN 1
        ELSE 0
    END
    FROM dbo.classroom_question_replies r
    JOIN dbo.classroom_questions q ON q.id = r.question_id
    JOIN dbo.users u ON u.id = r.responder_id
    JOIN dbo.roles roles ON roles.id = u.role_id
    LEFT JOIN dbo.classroom_members cm
        ON cm.classroom_id = q.classroom_id
       AND cm.user_id = r.responder_id
       AND cm.status = ''ACTIVE'';

    UPDATE q
    SET status = CASE
            WHEN EXISTS (
                SELECT 1
                FROM dbo.classroom_question_replies r
                WHERE r.question_id = q.id
                  AND r.official = 1
            ) THEN ''ANSWERED''
            ELSE ''OPEN''
        END,
        answered_by = official_reply.responder_id,
        answered_at = official_reply.created_at
    FROM dbo.classroom_questions q
    OUTER APPLY (
        SELECT TOP 1 r.responder_id, r.created_at
        FROM dbo.classroom_question_replies r
        WHERE r.question_id = q.id
          AND r.official = 1
        ORDER BY r.created_at DESC, r.id DESC
    ) official_reply;
');
