-- 用户名允许每 30 天修改一次；题单采用主表 + 题目关联表。
ALTER TABLE `user`
    ADD COLUMN userNameUpdateTime DATETIME NULL COMMENT '最近一次修改用户名的时间' AFTER userProfile;

CREATE TABLE IF NOT EXISTS question_list (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '题单 ID',
    name VARCHAR(64) NOT NULL COMMENT '题单名称',
    userId BIGINT NOT NULL COMMENT '创建用户 ID',
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_question_list_user (userId, isDelete, updateTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户题单';

CREATE TABLE IF NOT EXISTS question_list_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联 ID',
    questionListId BIGINT NOT NULL COMMENT '题单 ID',
    questionId BIGINT NOT NULL COMMENT '题目 ID',
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_list_question (questionListId, questionId),
    KEY idx_question_list_item_question (questionId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题单题目';
