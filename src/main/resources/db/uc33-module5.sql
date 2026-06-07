-- Module 5 – UC33: classroom_members, classroom_node_status, node_progress
-- Chạy script này trên mentora_db2 trước khi dùng tính năng lộ trình sinh viên.

IF OBJECT_ID(N'dbo.classroom_members', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.classroom_members (
        id              INT IDENTITY(1,1) PRIMARY KEY,
        classroom_id    INT NOT NULL,
        user_id         INT NOT NULL,
        role_in_class   NVARCHAR(20) NOT NULL,
        status          NVARCHAR(20) NOT NULL,
        joined_at       DATETIME2 NULL,
        CONSTRAINT UQ_classroom_members UNIQUE (classroom_id, user_id),
        CONSTRAINT CHK_member_status CHECK (status IN ('PENDING', 'ACTIVE', 'BANNED')),
        CONSTRAINT FK_cm_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT FK_cm_user FOREIGN KEY (user_id) REFERENCES dbo.users(id)
    );
END;
GO

IF OBJECT_ID(N'dbo.classroom_members', N'U') IS NOT NULL
   AND EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE name = N'CHK_member_status'
          AND parent_object_id = OBJECT_ID(N'dbo.classroom_members')
   )
BEGIN
    ALTER TABLE dbo.classroom_members DROP CONSTRAINT CHK_member_status;
END;
GO

IF OBJECT_ID(N'dbo.classroom_members', N'U') IS NOT NULL
BEGIN
    ALTER TABLE dbo.classroom_members
    ADD CONSTRAINT CHK_member_status
    CHECK (status IN ('PENDING', 'ACTIVE', 'BANNED'));
END;
GO

IF OBJECT_ID(N'dbo.classroom_node_status', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.classroom_node_status (
        id           INT IDENTITY(1,1) PRIMARY KEY,
        classroom_id INT NOT NULL,
        node_id      INT NOT NULL,
        status       NVARCHAR(20) NOT NULL,
        updated_at   DATETIME2 NULL,
        CONSTRAINT UQ_classroom_node_status UNIQUE (classroom_id, node_id),
        CONSTRAINT FK_cns_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT FK_cns_node FOREIGN KEY (node_id) REFERENCES dbo.learning_nodes(id)
    );
END;
GO

IF OBJECT_ID(N'dbo.classroom_node_status', N'U') IS NOT NULL
   AND COL_LENGTH(N'dbo.classroom_node_status', N'node_id') IS NOT NULL
   AND NOT EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE name = N'UQ_classroom_node_status'
          AND parent_object_id = OBJECT_ID(N'dbo.classroom_node_status')
   )
BEGIN
    ALTER TABLE dbo.classroom_node_status
    ADD CONSTRAINT UQ_classroom_node_status UNIQUE (classroom_id, node_id);
END;
GO

IF OBJECT_ID(N'dbo.node_progress', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.node_progress (
        id               INT IDENTITY(1,1) PRIMARY KEY,
        classroom_id     INT NOT NULL,
        student_id       INT NOT NULL,
        learning_node_id INT NOT NULL,
        is_completed     BIT NOT NULL CONSTRAINT DF_node_progress_completed DEFAULT (0),
        completed_at     DATETIME2 NULL,
        CONSTRAINT UQ_node_progress UNIQUE (classroom_id, student_id, learning_node_id),
        CONSTRAINT FK_np_classroom FOREIGN KEY (classroom_id) REFERENCES dbo.classrooms(id),
        CONSTRAINT FK_np_student FOREIGN KEY (student_id) REFERENCES dbo.users(id),
        CONSTRAINT FK_np_node FOREIGN KEY (learning_node_id) REFERENCES dbo.learning_nodes(id)
    );
END;
GO
