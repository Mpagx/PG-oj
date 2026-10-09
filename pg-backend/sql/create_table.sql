# 数据库初始化
# @author <a href="https://github.com/liyupi">程序员鱼皮</a>
# @from <a href="https://yupi.icu">编程导航知识星球</a>

-- 创建库
create database if not exists my_db;

-- 切换库
use my_db;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userName     varchar(64)                            not null comment '唯一用户名，也是登录标识',
    userPassword varchar(512)                           not null comment '密码',
    unionId      varchar(256)                           null comment '微信开放平台id',
    mpOpenId     varchar(256)                           null comment '公众号openId',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userNameUpdateTime datetime                         null comment '最近一次修改用户名的时间',
    userEmail    varchar(254)                           null comment '已验证邮箱',
    emailVerifiedAt datetime                            null comment '邮箱验证时间',
    passwordChangedAt datetime                         null comment '密码最近修改时间',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    unique index uk_userName (userName),
    index idx_unionId (unionId),
    unique index uk_userEmail (userEmail)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 题目表
create table if not exists question
(
    id           bigint auto_increment comment 'id' primary key,
    title        varchar(512)                       null comment '标题',
    difficulty   varchar(16) default 'MEDIUM'       not null comment '难度：EASY/MEDIUM/HARD',
    status       varchar(16) default 'PUBLISHED'    not null comment '状态：DRAFT/PUBLISHED',
    content      mediumtext                         null comment '内容',
    tags         varchar(1024)                      null comment '标签列表（json 数组）',
    answer       mediumtext                         null comment '题目答案',
    source       varchar(128)                       null comment '题目来源',
    sourceUrl    varchar(1024)                      null comment '原题链接',
    license      varchar(128)                       null comment '内容许可证',
    packageType  varchar(32)                        null comment '导入包格式',
    referenceLanguage varchar(32)                   null comment '标准程序语言',
    referenceSolution mediumtext                    null comment '标准程序源码',
    submitNum    int      default 0                 not null comment '题目提交数',
    acceptedNum  int      default 0                 not null comment '题目通过数',
    judgeCase    mediumtext                          null comment '判题用例（json 数组）',
    judgeConfig  text                                null comment '判题配置（json 对象）',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_question_status_difficulty (status, difficulty)
) comment '题目' collate = utf8mb4_unicode_ci;

-- 题目提交表
create table if not exists question_submit
(
    id           bigint auto_increment comment 'id' primary key,
    language     varchar(128)                       not null comment '编程语言',
    code         text                               not null comment '用户代码',
    judgeInfo    text                               null comment '判题信息（json 对象）',
    status       int      default 0                 not null comment '判题状态（0 - 待判题、1 - 判题中、2 - 成功、3 - 失败）',
    judgeAttempt int      default 0                 not null comment '判题尝试次数',
    judgeToken   varchar(64)                        null comment '当前判题执行令牌',
    judgeDeadline datetime                           null comment '当前判题租约截止时间',
    nextRetryTime datetime                           null comment '下次允许重试时间',
    lastError    varchar(1024)                       null comment '最近一次系统错误',
    questionId   bigint                             not null comment '题目 id',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    index idx_questionId (questionId),
    index idx_userId (userId),
    index idx_judge_queue (status, nextRetryTime),
    index idx_judge_recovery (status, judgeDeadline)
) comment '题目提交';

-- 用户题单
create table if not exists question_list
(
    id         bigint auto_increment comment '题单 ID' primary key,
    name       varchar(64)                        not null comment '题单名称',
    userId     bigint                             not null comment '创建用户 ID',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_question_list_user (userId, isDelete, updateTime)
) comment '用户题单' collate = utf8mb4_unicode_ci;

-- 题单中的题目
create table if not exists question_list_item
(
    id             bigint auto_increment comment '关联 ID' primary key,
    questionListId bigint                             not null comment '题单 ID',
    questionId     bigint                             not null comment '题目 ID',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    unique index uk_question_list_question (questionListId, questionId),
    index idx_question_list_item_question (questionId)
) comment '题单题目' collate = utf8mb4_unicode_ci;

-- 用户通过题目后发布的题解，每人每题最多一篇
create table if not exists question_solution
(
    id         bigint auto_increment comment '题解 ID' primary key,
    questionId bigint                             not null comment '题目 ID',
    userId     bigint                             not null comment '作者 ID',
    title      varchar(100)                       not null comment '题解标题',
    content    mediumtext                         not null comment 'Markdown 题解正文',
    createTime datetime default CURRENT_TIMESTAMP not null,
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    isDelete   tinyint  default 0                 not null,
    unique index uk_solution_question_user (questionId, userId),
    index idx_solution_question_time (questionId, isDelete, updateTime),
    index idx_solution_user (userId, isDelete)
) comment '用户题解' collate = utf8mb4_unicode_ci;

-- 未通过题目时，用户主动选择提前查看题解的记录
create table if not exists question_solution_reveal
(
    id         bigint auto_increment comment '提前查看记录 ID' primary key,
    questionId bigint                             not null comment '题目 ID',
    userId     bigint                             not null comment '用户 ID',
    createTime datetime default CURRENT_TIMESTAMP not null,
    unique index uk_solution_reveal_question_user (questionId, userId),
    index idx_solution_reveal_user (userId, createTime)
) comment '用户主动提前查看题解记录' collate = utf8mb4_unicode_ci;
