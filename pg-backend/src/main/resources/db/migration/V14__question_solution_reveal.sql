CREATE TABLE IF NOT EXISTS question_solution_reveal (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '提前查看记录 ID',
    questionId BIGINT NOT NULL COMMENT '题目 ID',
    userId BIGINT NOT NULL COMMENT '用户 ID',
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_solution_reveal_question_user (questionId, userId),
    KEY idx_solution_reveal_user (userId, createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主动提前查看题解记录';
