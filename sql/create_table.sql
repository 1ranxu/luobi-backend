# 数据库初始化
-- 创建库
create database if not exists luobi;

-- 切换库
use luobi;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    gender       tinyint                                null comment '性别',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_userAccount (userAccount)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 图表信息表
create table if not exists chart
(
    id          bigint auto_increment comment 'id' primary key,
    goal        text                                   null comment '分析目标',
    rawData     text                                   null comment '原始数据',
    chartType   varchar(128)                           null comment '图表类型',
    chartName   varchar(128)                           null comment '图表名称',
    genChart    text                                   null comment 'AI生成的图表数据',
    genSummary  text                                   null comment 'AI生成的分析总结',
    userId      bigint                                 null comment '创建人id',
    status      varchar(128) default 'wait'            not null comment 'wait,succeeded,failed,running',
    execMessage text                                   null comment '执行信息',
    createTime  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint      default 0                 not null comment '是否删除'
) comment '图表信息' collate = utf8mb4_unicode_ci;


