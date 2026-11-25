-- =====================================================
-- 校园活动报名系统 (xiaoyuanhdbmxt) 数据库建表脚本
-- =====================================================

DROP DATABASE IF EXISTS xiaoyuanhdbmxt;
CREATE DATABASE xiaoyuanhdbmxt DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE xiaoyuanhdbmxt;

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------
-- 1. user 用户表
-- -----------------------------------------------------
CREATE TABLE `user` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt哈希,兼容存量明文)',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `role` VARCHAR(20) NOT NULL DEFAULT 'student' COMMENT '角色: admin=管理员, student=学生',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -----------------------------------------------------
-- 2. activity 活动表
-- -----------------------------------------------------
CREATE TABLE `activity` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(100) NOT NULL COMMENT '活动标题',
  `description` TEXT COMMENT '活动描述',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '活动地点',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `deadline` DATETIME DEFAULT NULL COMMENT '报名截止时间',
  `max_participants` INT DEFAULT 0 COMMENT '最大参与人数(0不限)',
  `current_participants` INT DEFAULT 0 COMMENT '当前已通过审核的参与人数',
  `cover_image` VARCHAR(255) DEFAULT NULL COMMENT '封面图片',
  `status` VARCHAR(20) DEFAULT 'published' COMMENT '状态: published=已发布, ongoing=进行中, ended=已结束, offline=已下线',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- -----------------------------------------------------
-- 3. registration 报名表
-- -----------------------------------------------------
CREATE TABLE `registration` (
  `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` INT NOT NULL COMMENT '用户ID',
  `activity_id` INT NOT NULL COMMENT '活动ID',
  `register_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `status` VARCHAR(20) DEFAULT 'registered' COMMENT '状态: registered=待审核, approved=已通过, rejected=已拒绝, cancelled=已取消',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注/拒绝原因',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_activity_id` (`activity_id`),
  CONSTRAINT `fk_registration_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_registration_activity` FOREIGN KEY (`activity_id`) REFERENCES `activity` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名表';


-- =====================================================
-- 初始化数据
-- =====================================================

-- 用户数据 (密码统一: 123456)
INSERT INTO `user` (`username`, `password`, `real_name`, `role`, `phone`) VALUES
('admin',     '123456', '系统管理员', 'admin',   '13800000000'),
('admin02',   '123456', '李老师',     'admin',   '13900000001'),
('student01', '123456', '张三',       'student', '13800000001'),
('student02', '123456', '李四',       'student', '13800000002'),
('student03', '123456', '王五',       'student', '13800000003'),
('student04', '123456', '赵六',       'student', '13800000004'),
('student05', '123456', '钱七',       'student', '13800000005'),
('student06', '123456', '孙八',       'student', '13800000006'),
('student07', '123456', '周九',       'student', '13800000007'),
('student08', '123456', '吴十',       'student', '13800000008');

-- 活动数据 (当前日期基准: 2026-10-08)
INSERT INTO `activity` (`title`, `description`, `location`, `start_time`, `end_time`, `deadline`, `max_participants`, `current_participants`, `status`) VALUES
-- === 已发布 (published) - 可报名 ===
('2026秋季运动会', '一年一度的全校秋季田径运动会，项目包括100米、200米、400米、800米、1500米、3000米、跳高、跳远、铅球、标枪、4x100米接力等，请同学们积极报名参加！', '学校体育场', '2026-11-15 08:00:00', '2026-11-17 17:00:00', '2026-11-10 18:00:00', 500, 35, 'published'),
('校园编程大赛', '用代码改变世界！本次编程大赛分为算法组和应用组，支持个人报名或组队参赛（3-4人），决赛将进行现场答辩和代码展示。', '计算机学院实验楼B201', '2026-12-05 09:00:00', '2026-12-07 18:00:00', '2026-12-01 20:00:00', 120, 48, 'published'),
('迎新晚会', '2026级新生欢迎晚会！精彩节目轮番上演，还有互动游戏和抽奖环节。欢迎新同学和全体师生到场观看。', '学校大礼堂', '2026-10-25 19:00:00', '2026-10-25 22:00:00', '2026-10-22 12:00:00', 800, 200, 'published'),
('户外登山活动', '征服西山，挑战自我！本次登山活动路线约15公里，海拔上升800米，适合有一定运动基础的同学参加。请自备登山装备和补给。', '西山大门口集合', '2026-11-08 07:00:00', '2026-11-08 18:00:00', '2026-11-06 18:00:00', 60, 28, 'published'),
('学术讲座：AI前沿技术', '特邀清华大学张明教授带来《大语言模型与人工智能的未来》主题讲座，探讨AI技术发展趋势及应用场景。', '图书馆三楼报告厅', '2026-10-20 14:00:00', '2026-10-20 16:30:00', '2026-10-19 12:00:00', 200, 0, 'published'),

-- === 进行中 (ongoing) ===
('读书分享周', '为期一周的校园读书节，每日一场主题分享会。今日分享主题：《活着》——余华的生命哲学。', '图书馆二楼大厅', '2026-10-01 09:00:00', '2026-10-15 18:00:00', NULL, 0, 0, 'ongoing'),
('创新创业训练营', '为期两个月的大学生创新创业训练营，邀请成功创业者分享经验，提供项目孵化指导。每周六上课。', '创客中心A301', '2026-09-15 09:00:00', '2026-11-15 17:00:00', NULL, 50, 50, 'ongoing'),

-- === 已结束 (ended) ===
('2026春季校篮球赛', '全校男子篮球联赛，经过一个月的激烈角逐，最终计算机学院代表队夺冠！', '体育馆', '2026-03-01 14:00:00', '2026-04-05 17:00:00', '2026-02-25 18:00:00', 120, 120, 'ended'),
('校园歌手大赛', '校园十大歌手巅峰对决！本届冠军：计算机学院王小明。', '学校大礼堂', '2026-06-08 18:30:00', '2026-06-08 21:30:00', '2026-06-01 18:00:00', 300, 256, 'ended'),
('编程马拉松48小时', '48小时极限编程挑战赛！本届共有32支队伍参赛，"熬夜肝代码"团队获得冠军。', '计算机学院机房', '2026-07-15 09:00:00', '2026-07-17 09:00:00', '2026-07-10 18:00:00', 100, 96, 'ended');

-- 报名数据 (覆盖全部4种状态)
INSERT INTO `registration` (`user_id`, `activity_id`, `status`, `remark`) VALUES
-- 秋季运动会 (活动1)
(3, 1,  'approved',  '报100米跑'),
(4, 1,  'approved',  '报200米跑和4x100接力'),
(5, 1,  'approved',  '报跳远'),
(6, 1,  'approved',  '报铅球'),
(7, 1,  'registered','想报800米'),
(8, 1,  'registered','第一次参加运动会，有点紧张'),
(9, 1,  'rejected',  '超龄，已毕业'),

-- 校园编程大赛 (活动2)
(3, 2,  'approved',  '组队参加应用组'),
(6, 2,  'approved',  '算法组，独立参赛'),
(4, 2,  'registered','正在找队友'),
(7, 2,  'cancelled', '临时有事无法参加'),

-- 迎新晚会 (活动3)
(5, 3,  'approved',  '想看迎新晚会'),
(8, 3,  'approved',  '作为新生代表出席'),
(9, 3,  'registered','带朋友一起去'),

-- 户外登山 (活动4)
(4, 4,  'approved',  '登山爱好者'),
(7, 4,  'registered','锻炼身体'),
(6, 4,  'rejected',  '体检不合格，无法参加高强度运动'),

-- 读书分享周 (活动6, ongoing)
(3, 6,  'approved',  '每天都来读书分享会'),

-- 创新创业训练营 (活动7, ongoing)
(5, 7,  'approved',  '报名训练营创业方向'),
(8, 7,  'approved',  '希望学到创业知识'),

-- 春季篮球赛 (活动8, ended)
(4, 8,  'approved',  '我们班有啦啦队到场加油'),
(6, 8,  'approved',  '篮球爱好者'),

-- 校园歌手大赛 (活动9, ended)
(3, 9,  'approved',  '报了歌手大赛，进入决赛了'),
(9, 9,  'cancelled', '紧张到不敢上台了...'),

-- 编程马拉松 (活动10, ended)
(5, 10, 'approved',  '我们队拿了三等奖！'),
(7, 10, 'registered','下次一定能做得更好');

SET FOREIGN_KEY_CHECKS = 1;