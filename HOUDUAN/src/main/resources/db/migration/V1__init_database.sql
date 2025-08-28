-- 创建数据库表结构
-- 视频解析微信小程序后端数据库初始化脚本

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `open_id` VARCHAR(64) NOT NULL COMMENT '微信openId',
    `union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信unionId',
    `nickname` VARCHAR(100) DEFAULT NULL COMMENT '用户昵称',
    `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `points` INT DEFAULT 0 COMMENT '用户积分',
    `sign_count` INT DEFAULT 0 COMMENT '签到次数',
    `continuous_sign_days` INT DEFAULT 0 COMMENT '连续签到天数',
    `last_sign_time` DATETIME DEFAULT NULL COMMENT '最后签到时间',
    `video_parse_count` INT DEFAULT 0 COMMENT '视频解析次数',
    `user_status` TINYINT DEFAULT 1 COMMENT '用户状态：1-正常，0-禁用',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_open_id` (`open_id`),
    KEY `idx_union_id` (`union_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_last_login_time` (`last_login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 视频解析记录表
CREATE TABLE IF NOT EXISTS `video_parse_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `open_id` VARCHAR(64) NOT NULL COMMENT '用户openId',
    `original_url` TEXT NOT NULL COMMENT '原始视频链接',
    `video_title` VARCHAR(500) DEFAULT NULL COMMENT '视频标题',
    `parsed_video_url` TEXT DEFAULT NULL COMMENT '解析后的视频链接',
    `cover_image_url` TEXT DEFAULT NULL COMMENT '封面图片链接',
    `image_atlas` TEXT DEFAULT NULL COMMENT '图集JSON数据',
    `platform` VARCHAR(50) DEFAULT NULL COMMENT '视频平台',
    `parse_status` TINYINT DEFAULT 0 COMMENT '解析状态：1-成功，0-解析中，-1-失败',
    `failure_reason` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `parse_duration` INT DEFAULT NULL COMMENT '解析耗时（毫秒）',
    `user_ip` VARCHAR(50) DEFAULT NULL COMMENT '用户IP',
    `user_agent` TEXT DEFAULT NULL COMMENT '用户代理',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_open_id` (`open_id`),
    KEY `idx_platform` (`platform`),
    KEY `idx_parse_status` (`parse_status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='视频解析记录表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value` TEXT DEFAULT NULL COMMENT '配置值',
    `config_desc` VARCHAR(500) DEFAULT NULL COMMENT '配置描述',
    `config_type` VARCHAR(50) DEFAULT 'system' COMMENT '配置类型',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用：1-启用，0-禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_type` (`config_type`),
    KEY `idx_is_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 插入默认配置数据
INSERT INTO `system_config` (`config_key`, `config_value`, `config_desc`, `config_type`, `is_enabled`) VALUES
('app_name', '短视频去水印', '应用名称', 'app', 1),
('app_version', '1.0.0', '应用版本', 'app', 1),
('app_description', '支持主流短视频平台的无水印解析', '应用描述', 'app', 1),
('daily_signin_points', '10', '每日签到积分奖励', 'business', 1),
('parsing_record_retention_minutes', '30', '解析记录保留时间（分钟）', 'business', 1),
('max_daily_parse_count', '50', '每日最大解析次数', 'business', 1),
('supported_platforms', '["抖音","快手","小红书","微视","火山","西瓜视频","皮皮虾","最右","微博"]', '支持的平台列表', 'business', 1),
('contact_info', '如有问题请联系客服', '联系信息', 'app', 1),
('privacy_policy_url', '', '隐私政策链接', 'app', 1),
('user_agreement_url', '', '用户协议链接', 'app', 1),
('topMiniImg', '/images/watermark_before.png', '顶部推荐小程序图片', 'app', 1),
('topMiniTitle', '推荐小程序', '顶部推荐小程序标题', 'app', 1),
('topMiniAppId', '', '顶部推荐小程序AppId', 'app', 1),
('topMiniPath', '', '顶部推荐小程序跳转路径', 'app', 1);

-- 创建索引优化查询性能
CREATE INDEX `idx_user_open_id_deleted` ON `user` (`open_id`, `is_deleted`);
CREATE INDEX `idx_video_record_open_id_time` ON `video_parse_record` (`open_id`, `create_time`, `is_deleted`);
CREATE INDEX `idx_video_record_status_time` ON `video_parse_record` (`parse_status`, `create_time`, `is_deleted`);
CREATE INDEX `idx_config_key_enabled` ON `system_config` (`config_key`, `is_enabled`, `is_deleted`);