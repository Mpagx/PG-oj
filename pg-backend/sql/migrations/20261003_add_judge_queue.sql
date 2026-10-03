-- 现有数据库只需执行一次；新建数据库请直接使用 ../create_table.sql。
alter table question_submit
    add column judgeAttempt int default 0 not null comment '判题尝试次数' after status,
    add column judgeToken varchar(64) null comment '当前判题执行令牌' after judgeAttempt,
    add column judgeDeadline datetime null comment '当前判题租约截止时间' after judgeToken,
    add column nextRetryTime datetime null comment '下次允许重试时间' after judgeDeadline,
    add column lastError varchar(1024) null comment '最近一次系统错误' after nextRetryTime,
    add index idx_judge_queue (status, nextRetryTime),
    add index idx_judge_recovery (status, judgeDeadline);
