-- userAccount was the real unique login identifier. Keep its data, remove the
-- duplicate display-name column, and expose one canonical username everywhere.
ALTER TABLE user DROP COLUMN userName;
ALTER TABLE user
    CHANGE COLUMN userAccount userName VARCHAR(64) NOT NULL COMMENT '唯一用户名，也是登录标识';
ALTER TABLE user RENAME INDEX uk_userAccount TO uk_userName;
ALTER TABLE user
    ADD COLUMN userNameUpdateTime DATETIME NULL COMMENT '最近一次修改用户名的时间' AFTER userProfile;
