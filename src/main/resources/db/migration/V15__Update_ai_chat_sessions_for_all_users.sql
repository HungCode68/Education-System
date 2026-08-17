ALTER TABLE ai_chat_sessions ADD COLUMN user_id BIGINT;

UPDATE ai_chat_sessions acs
INNER JOIN students s ON acs.student_id = s.id
SET acs.user_id = s.user_id
WHERE acs.student_id IS NOT NULL;
