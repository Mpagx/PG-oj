CREATE TABLE IF NOT EXISTS question_solution (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '题解 ID',
    questionId BIGINT NOT NULL COMMENT '题目 ID',
    userId BIGINT NOT NULL COMMENT '作者 ID',
    title VARCHAR(100) NOT NULL COMMENT '题解标题',
    content MEDIUMTEXT NOT NULL COMMENT 'Markdown 题解正文',
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solution_question_user (questionId, userId),
    KEY idx_solution_question_time (questionId, isDelete, updateTime),
    KEY idx_solution_user (userId, isDelete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户题解';
