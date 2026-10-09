ALTER TABLE question
    MODIFY COLUMN content MEDIUMTEXT NULL COMMENT '题目内容',
    MODIFY COLUMN answer MEDIUMTEXT NULL COMMENT '题解',
    MODIFY COLUMN judgeCase MEDIUMTEXT NULL COMMENT '判题用例（JSON 数组）',
    ADD COLUMN source VARCHAR(128) NULL COMMENT '题目来源' AFTER answer,
    ADD COLUMN sourceUrl VARCHAR(1024) NULL COMMENT '原题链接' AFTER source,
    ADD COLUMN license VARCHAR(128) NULL COMMENT '内容许可证' AFTER sourceUrl,
    ADD COLUMN packageType VARCHAR(32) NULL COMMENT '导入包格式' AFTER license,
    ADD COLUMN referenceLanguage VARCHAR(32) NULL COMMENT '标准程序语言' AFTER packageType,
    ADD COLUMN referenceSolution MEDIUMTEXT NULL COMMENT '标准程序源码' AFTER referenceLanguage;

CREATE TABLE IF NOT EXISTS external_problem (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source VARCHAR(32) NOT NULL COMMENT '外部平台',
    externalId VARCHAR(64) NOT NULL COMMENT '平台题目标识',
    title VARCHAR(512) NOT NULL,
    url VARCHAR(1024) NOT NULL,
    difficulty VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    rating INT NULL,
    tags VARCHAR(2048) NULL COMMENT 'JSON 标签数组',
    solvedCount BIGINT NOT NULL DEFAULT 0,
    syncTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_external_source_id (source, externalId),
    KEY idx_external_recommend (isDelete, difficulty, rating, solvedCount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部练习题元数据';
