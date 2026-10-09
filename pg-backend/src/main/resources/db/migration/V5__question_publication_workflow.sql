ALTER TABLE question
    ADD COLUMN difficulty VARCHAR(16) NOT NULL DEFAULT 'MEDIUM' COMMENT '难度：EASY/MEDIUM/HARD' AFTER title,
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT/PUBLISHED' AFTER difficulty,
    ADD INDEX idx_question_status_difficulty (status, difficulty);
