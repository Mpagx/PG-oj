ALTER TABLE user
    ADD COLUMN userEmail VARCHAR(254) NULL COMMENT '已验证邮箱' AFTER userProfile,
    ADD COLUMN emailVerifiedAt DATETIME NULL COMMENT '邮箱验证时间' AFTER userEmail,
    ADD COLUMN passwordChangedAt DATETIME NULL COMMENT '密码最近修改时间' AFTER emailVerifiedAt,
    ADD UNIQUE INDEX uk_userEmail (userEmail);
