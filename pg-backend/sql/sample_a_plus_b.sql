-- Run explicitly after replacing @adminId with a real administrator ID.
-- Idempotent: does not overwrite an existing problem or its submissions.
SET @adminId = NULL;
INSERT INTO question (title, content, tags, answer, judgeCase, judgeConfig, userId)
SELECT 'A+B', '从标准输入读取两个整数 a、b，输出 a+b。输入示例：1 2；输出示例：3。|a|、|b| <= 1000000000。Java 使用 public class Main。', '["入门","数学"]', '使用 long 读取两个整数后相加。',
 '[{"input":"1 2","output":"3"},{"input":"0 0","output":"0"},{"input":"-1 2","output":"1"},{"input":"1000000000 1000000000","output":"2000000000"}]',
 '{"timeLimit":1000,"memoryLimit":262144,"stackLimit":65536}', @adminId
WHERE EXISTS (SELECT 1 FROM user WHERE id=@adminId AND userRole='admin' AND isDelete=0)
AND NOT EXISTS (SELECT 1 FROM question WHERE title='A+B' AND isDelete=0);
