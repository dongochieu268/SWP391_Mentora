USE [MentoraDB];
GO

/*
   A node with level tests is complete only after the student passes its
   highest configured level. This repairs progress created by the previous
   behavior, which marked a node complete after any submitted level attempt.
   Idempotent: safe to run more than once.
*/
WITH final_levels AS (
    SELECT learning_node_id, MAX(level_number) AS final_level_number
    FROM dbo.node_levels
    GROUP BY learning_node_id
)
UPDATE progress
SET progress.is_completed = CASE WHEN final_pass.submitted_at IS NULL THEN 0 ELSE 1 END,
    progress.completed_at = final_pass.submitted_at
FROM dbo.node_progress progress
INNER JOIN final_levels final_level
    ON final_level.learning_node_id = progress.node_id
OUTER APPLY (
    SELECT MAX(attempt.submitted_at) AS submitted_at
    FROM dbo.node_level_attempts attempt
    INNER JOIN dbo.node_levels level_row ON level_row.id = attempt.node_level_id
    WHERE attempt.classroom_id = progress.classroom_id
      AND attempt.student_id = progress.student_id
      AND level_row.learning_node_id = progress.node_id
      AND level_row.level_number = final_level.final_level_number
      AND attempt.status = N'SUBMITTED'
      AND attempt.passed = 1
) final_pass;
GO
