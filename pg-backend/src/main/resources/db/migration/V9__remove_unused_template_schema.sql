-- Preserve rows for audit while hiding submissions that can no longer be
-- displayed because their user/question is gone or their source is empty.
UPDATE question_submit qs
LEFT JOIN question q ON q.id = qs.questionId
LEFT JOIN `user` u ON u.id = qs.userId
SET qs.isDelete = 1,
    qs.lastError = COALESCE(qs.lastError, '旧数据完整性清理：题目、用户或代码缺失')
WHERE qs.isDelete = 0
  AND (q.id IS NULL OR u.id IS NULL OR qs.code IS NULL OR TRIM(qs.code) = '');

-- Partially upgraded local databases missed the question workflow columns.
SET @has_difficulty = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question' AND COLUMN_NAME = 'difficulty'
);
SET @add_difficulty = IF(
    @has_difficulty = 0,
    'ALTER TABLE question ADD COLUMN difficulty VARCHAR(16) NOT NULL DEFAULT ''MEDIUM'' COMMENT ''难度：EASY/MEDIUM/HARD'' AFTER title',
    'SELECT 1'
);
PREPARE cleanup_statement FROM @add_difficulty;
EXECUTE cleanup_statement;
DEALLOCATE PREPARE cleanup_statement;

SET @has_publication_status = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question' AND COLUMN_NAME = 'status'
);
SET @add_publication_status = IF(
    @has_publication_status = 0,
    'ALTER TABLE question ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''PUBLISHED'' COMMENT ''状态：DRAFT/PUBLISHED'' AFTER difficulty',
    'SELECT 1'
);
PREPARE cleanup_statement FROM @add_publication_status;
EXECUTE cleanup_statement;
DEALLOCATE PREPARE cleanup_statement;

SET @has_question_workflow_index = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question'
      AND INDEX_NAME = 'idx_question_status_difficulty'
);
SET @add_question_workflow_index = IF(
    @has_question_workflow_index = 0,
    'ALTER TABLE question ADD INDEX idx_question_status_difficulty (status, difficulty)',
    'SELECT 1'
);
PREPARE cleanup_statement FROM @add_question_workflow_index;
EXECUTE cleanup_statement;
DEALLOCATE PREPARE cleanup_statement;

-- Make the counters agree with the remaining usable submission history.
UPDATE question q
LEFT JOIN (
    SELECT questionId,
           COUNT(*) AS submitCount,
           SUM(JSON_UNQUOTE(JSON_EXTRACT(judgeInfo, '$.message')) = 'Accepted') AS acceptedCount
    FROM question_submit
    WHERE isDelete = 0
    GROUP BY questionId
) totals ON totals.questionId = q.id
SET q.submitNum = COALESCE(totals.submitCount, 0),
    q.acceptedNum = COALESCE(totals.acceptedCount, 0);

-- The project no longer has a post, like or favourite feature and all three
-- tables were empty at audit time.
DROP TABLE IF EXISTS post_thumb;
DROP TABLE IF EXISTS post_favour;
DROP TABLE IF EXISTS post;

SET @has_question_thumb = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question' AND COLUMN_NAME = 'thumbNum'
);
SET @drop_question_thumb = IF(
    @has_question_thumb > 0,
    'ALTER TABLE question DROP COLUMN thumbNum',
    'SELECT 1'
);
PREPARE cleanup_statement FROM @drop_question_thumb;
EXECUTE cleanup_statement;
DEALLOCATE PREPARE cleanup_statement;

SET @has_question_favour = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'question' AND COLUMN_NAME = 'favourNum'
);
SET @drop_question_favour = IF(
    @has_question_favour > 0,
    'ALTER TABLE question DROP COLUMN favourNum',
    'SELECT 1'
);
PREPARE cleanup_statement FROM @drop_question_favour;
EXECUTE cleanup_statement;
DEALLOCATE PREPARE cleanup_statement;

SET @has_username_update_time = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'userNameUpdateTime'
);
SET @drop_username_update_time = IF(
    @has_username_update_time > 0,
    'ALTER TABLE `user` DROP COLUMN userNameUpdateTime',
    'SELECT 1'
);
PREPARE cleanup_statement FROM @drop_username_update_time;
EXECUTE cleanup_statement;
DEALLOCATE PREPARE cleanup_statement;

-- Repair comments that were created with the wrong client character set.
ALTER TABLE question_submit
    MODIFY COLUMN judgeAttempt INT NOT NULL DEFAULT 0 COMMENT '判题尝试次数',
    MODIFY COLUMN judgeToken VARCHAR(64) NULL COMMENT '当前判题执行令牌',
    MODIFY COLUMN judgeDeadline DATETIME NULL COMMENT '当前判题租约截止时间',
    MODIFY COLUMN nextRetryTime DATETIME NULL COMMENT '下次允许重试时间',
    MODIFY COLUMN lastError VARCHAR(1024) NULL COMMENT '最近一次系统错误';
