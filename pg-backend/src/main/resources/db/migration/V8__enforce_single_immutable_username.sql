-- Complete partially migrated local databases: copy the former login account
-- into the only username column before removing the obsolete column.
SET @has_user_account = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'userAccount'
);

SET @backfill_from_user_account = IF(
    @has_user_account > 0,
    'UPDATE `user` SET userName = userAccount',
    'SELECT 1'
);
PREPARE username_statement FROM @backfill_from_user_account;
EXECUTE username_statement;
DEALLOCATE PREPARE username_statement;

-- Fallbacks for the two known local rows in case userAccount was already
-- removed by a partial manual migration before its values were copied.
UPDATE `user`
SET userName = 'PGGG'
WHERE id = 2084469258210238466
  AND (userName IS NULL OR TRIM(userName) = '' OR userName = '彭戈');

UPDATE `user`
SET userName = 'AAAA'
WHERE id = 2085649027131916290
  AND (userName IS NULL OR TRIM(userName) = '');

-- Refuse future rows without the single canonical login/display/ranking name.
ALTER TABLE `user`
    MODIFY COLUMN userName VARCHAR(64) NOT NULL COMMENT '唯一用户名，也是登录、展示和排名标识';

SET @has_unique_user_name = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'userName'
      AND NON_UNIQUE = 0
);
SET @create_unique_user_name = IF(
    @has_unique_user_name = 0,
    'ALTER TABLE `user` ADD UNIQUE INDEX uk_userName (userName)',
    'SELECT 1'
);
PREPARE username_statement FROM @create_unique_user_name;
EXECUTE username_statement;
DEALLOCATE PREPARE username_statement;

SET @drop_user_account = IF(
    @has_user_account > 0,
    'ALTER TABLE `user` DROP COLUMN userAccount',
    'SELECT 1'
);
PREPARE username_statement FROM @drop_user_account;
EXECUTE username_statement;
DEALLOCATE PREPARE username_statement;
