-- Some legacy databases were converted to the canonical userName column
-- manually and therefore missed the final column added by V4. Keep this
-- repair idempotent so it is safe both for those databases and fresh schemas.
SET @has_user_name_update_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'userNameUpdateTime'
);

SET @repair_user_name_update_time = IF(
    @has_user_name_update_time = 0,
    'ALTER TABLE `user` ADD COLUMN userNameUpdateTime DATETIME NULL COMMENT ''最近一次修改用户名的时间'' AFTER userProfile',
    'SELECT 1'
);

PREPARE repair_statement FROM @repair_user_name_update_time;
EXECUTE repair_statement;
DEALLOCATE PREPARE repair_statement;
