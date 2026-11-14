-- ============================================================
-- 校园二手商品交易平台  数据库初始化脚本
-- 数据库名：2024011302（同组同学学号，改成你自己的学号）
-- 环境：MySQL 8.0
-- ============================================================
DROP DATABASE IF EXISTS `2024011302`;
CREATE DATABASE `2024011302` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `2024011302`;

-- ------------------------------------------------------------
-- 1. 用户表（普通用户 / 商家 / 管理员 三合一）
-- ------------------------------------------------------------
CREATE TABLE `users` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`      VARCHAR(50)  NOT NULL COMMENT '登录账号',
  `password`      VARCHAR(100) NOT NULL COMMENT '密码（SHA-256加盐）',
  `name`          VARCHAR(50)  NOT NULL COMMENT '姓名',
  `phone`         VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
  `email`         VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
  `city`          VARCHAR(50)           DEFAULT NULL COMMENT '城市',
  `gender`        TINYINT               DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `bank_account`  VARCHAR(32)           DEFAULT NULL COMMENT '银行账号（16位）',
  `role`          VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER普通用户 MERCHANT商家 ADMIN管理员',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审核 ACTIVE正常 REJECTED驳回 BANNED封禁',
  `shop_name`     VARCHAR(100)          DEFAULT NULL COMMENT '店铺名称（商家）',
  `merchant_level` INT                  DEFAULT 1 COMMENT '商家等级 1-5',
  `license_img`   VARCHAR(255)          DEFAULT NULL COMMENT '营业执照图片',
  `id_card_img`   VARCHAR(255)          DEFAULT NULL COMMENT '身份证图片',
  `balance`       DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '钱包余额',
  `points`        INT          NOT NULL DEFAULT 0 COMMENT '积分',
  `credit_score`  DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT '买家好评率',
  `banned_until`  DATETIME              DEFAULT NULL COMMENT '封禁到期时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role_status` (`role`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 商品分类
-- ------------------------------------------------------------
CREATE TABLE `categories` (
  `id`    INT NOT NULL AUTO_INCREMENT,
  `name`  VARCHAR(50) NOT NULL,
  `sort`  INT DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ------------------------------------------------------------
-- 3. 商品表
-- ------------------------------------------------------------
CREATE TABLE `products` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT,
  `merchant_id`     BIGINT       NOT NULL COMMENT '商家ID',
  `category_id`     INT          NOT NULL COMMENT '分类ID',
  `name`            VARCHAR(100) NOT NULL COMMENT '商品名称',
  `price`           DECIMAL(10,2) NOT NULL COMMENT '售价',
  `original_price`  DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
  `size`            VARCHAR(50)  DEFAULT NULL COMMENT '尺寸规格',
  `description`     TEXT COMMENT '使用说明',
  `images`          VARCHAR(1000) DEFAULT NULL COMMENT '图片，多张用逗号分隔',
  `negotiable`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否允许议价',
  `stock`           INT          NOT NULL DEFAULT 1 COMMENT '库存',
  `condition_level` VARCHAR(20)  DEFAULT '九成新' COMMENT '新旧程度',
  `status`          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审核 ON_SALE在售 OFF_SHELF已下架 SOLD已售出 LOCKED锁定',
  `sales`           INT          NOT NULL DEFAULT 0 COMMENT '历史销量',
  `rating_avg`      DECIMAL(3,2) NOT NULL DEFAULT 5.00 COMMENT '商品平均评分',
  `rating_count`    INT          NOT NULL DEFAULT 0 COMMENT '评价数量',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant` (`merchant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ------------------------------------------------------------
-- 4. 购物车
-- ------------------------------------------------------------
CREATE TABLE `cart_items` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `quantity`   INT    NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ------------------------------------------------------------
-- 5. 订单表
-- ------------------------------------------------------------
CREATE TABLE `orders` (
  `id`               BIGINT NOT NULL AUTO_INCREMENT,
  `order_no`         VARCHAR(32) NOT NULL COMMENT '订单号',
  `buyer_id`         BIGINT NOT NULL,
  `merchant_id`      BIGINT NOT NULL,
  `product_id`       BIGINT NOT NULL,
  `product_name`     VARCHAR(100) NOT NULL,
  `product_image`    VARCHAR(255) DEFAULT NULL,
  `quantity`         INT NOT NULL DEFAULT 1,
  `amount`           DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `origin_amount`    DECIMAL(10,2) NOT NULL COMMENT '优惠前金额',
  `fee`              DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '平台手续费',
  `points_used`      INT NOT NULL DEFAULT 0 COMMENT '使用积分',
  `points_deduct`    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '积分抵扣金额',
  `status`           VARCHAR(20) NOT NULL COMMENT 'UNPAID待付款 PAID待收货 COMPLETED已完成 RETURN_REQUESTED退货申请中 RETURNED已退货 CANCELLED已取消',
  `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `pay_time`         DATETIME DEFAULT NULL,
  `confirm_deadline` DATETIME DEFAULT NULL COMMENT '自动确认收货截止时间（支付后7天）',
  `receive_time`     DATETIME DEFAULT NULL COMMENT '买家确认收货时间',
  `return_deadline`  DATETIME DEFAULT NULL COMMENT '退货截止时间（收货后24小时）',
  `return_reason`    VARCHAR(255) DEFAULT NULL,
  `settle_time`      DATETIME DEFAULT NULL COMMENT '货款结算时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_buyer` (`buyer_id`),
  KEY `idx_merchant` (`merchant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ------------------------------------------------------------
-- 6. 评价表
-- ------------------------------------------------------------
CREATE TABLE `reviews` (
  `id`          BIGINT NOT NULL AUTO_INCREMENT,
  `order_id`    BIGINT NOT NULL,
  `product_id`  BIGINT NOT NULL,
  `merchant_id` BIGINT NOT NULL,
  `buyer_id`    BIGINT NOT NULL,
  `type`        VARCHAR(30) NOT NULL COMMENT 'PRODUCT商品评价 MERCHANT服务态度评价 BUYER商家评价买家',
  `rating`      INT NOT NULL COMMENT '1-5星',
  `content`     VARCHAR(500) DEFAULT NULL,
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_buyer` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ------------------------------------------------------------
-- 7. 钱包流水
-- ------------------------------------------------------------
CREATE TABLE `wallet_txns` (
  `id`            BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`       BIGINT NOT NULL,
  `type`          VARCHAR(20) NOT NULL COMMENT 'RECHARGE充值 PAY支付 INCOME货款收入 FEE手续费 REFUND退款 POINTS_DEDUCT积分抵扣',
  `amount`        DECIMAL(10,2) NOT NULL COMMENT '变动金额，正数入账负数出账',
  `balance_after` DECIMAL(12,2) NOT NULL COMMENT '变动后余额',
  `remark`        VARCHAR(200) DEFAULT NULL,
  `order_id`      BIGINT DEFAULT NULL,
  `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水表';

-- ------------------------------------------------------------
-- 8. 黑名单（商家拉黑恶意买家）
-- ------------------------------------------------------------
CREATE TABLE `blacklist` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` BIGINT NOT NULL,
  `buyer_id`   BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_m_b` (`merchant_id`,`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单表';

-- ============================================================
-- 演示数据
-- 所有账号密码均为 123456
-- ============================================================
SET @pwd = 'b4f0245b247614122cfde773a6c828f02ca887802b37c8a6c132c39a7ecc6c2e';

-- 管理员
INSERT INTO `users` (`id`,`username`,`password`,`name`,`phone`,`email`,`city`,`gender`,`role`,`status`,`balance`) VALUES
(1,'admin',@pwd,'平台管理员','13800000000','admin@campus.com','北京',1,'ADMIN','ACTIVE',0);

-- 商家
INSERT INTO `users` (`id`,`username`,`password`,`name`,`phone`,`email`,`city`,`gender`,`bank_account`,`role`,`status`,`shop_name`,`merchant_level`,`license_img`,`id_card_img`,`balance`,`credit_score`) VALUES
(2,'wangji',@pwd,'王建国','13911112222','wangji@campus.com','北京',1,'6222021234567890','MERCHANT','ACTIVE','王记数码',3,'/uploads/license.jpg','/uploads/idcard.jpg',3260.50,98.60),
(3,'jiushu',@pwd,'陈书文','13922223333','jiushu@campus.com','北京',2,'6222022234567891','MERCHANT','ACTIVE','旧书阁',2,'/uploads/license.jpg','/uploads/idcard.jpg',742.20,99.20),
(4,'zasuo',@pwd,'刘洋','13933334444','zasuo@campus.com','北京',1,'6222023234567892','MERCHANT','ACTIVE','宿舍杂物铺',1,'/uploads/license.jpg','/uploads/idcard.jpg',186.40,96.50);

-- 普通用户
INSERT INTO `users` (`id`,`username`,`password`,`name`,`phone`,`email`,`city`,`gender`,`bank_account`,`role`,`status`,`balance`,`points`,`credit_score`) VALUES
(5,'liming',@pwd,'李明','13712345678','liming@campus.com','北京',1,'6222024234567893','USER','ACTIVE',1206.00,860,100.00),
(6,'zhangyu',@pwd,'张宇','13723456789','zhangyu@campus.com','北京',2,'6222025234567894','USER','ACTIVE',318.50,120,95.00),
(7,'wangxiaoyu',@pwd,'王小雨','13734567890','wxy@campus.com','北京',2,'6222026234567895','USER','PENDING',0,0,100.00),
(8,'suhui',@pwd,'苏慧','13745678901','suhui@campus.com','北京',2,'6222027234567896','MERCHANT','PENDING',0,0,100.00);

-- 待审核商家的店铺信息补充
UPDATE `users` SET `shop_name`='数码回收站', `license_img`='/uploads/license.jpg', `id_card_img`='/uploads/idcard.jpg', `merchant_level`=1 WHERE `id`=8;

INSERT INTO `categories` (`id`,`name`,`sort`) VALUES
(1,'数码电子',1),(2,'图书教材',2),(3,'生活用品',3),(4,'运动户外',4),(5,'服饰鞋包',5),(6,'其他',6);

INSERT INTO `products` (`id`,`merchant_id`,`category_id`,`name`,`price`,`original_price`,`size`,`description`,`images`,`negotiable`,`stock`,`condition_level`,`status`,`sales`,`rating_avg`,`rating_count`,`created_at`) VALUES
(1,2,1,'索尼 WH-1000XM4 头戴式降噪耳机',899.00,1999.00,'黑色 标准版','去年双十一买的，降噪很顶，图书馆戴着几乎听不见空调声。耳罩无磨损，配件盒、充电线、转接头都在，因为换了苹果的AirPods Max所以出掉。','/uploads/p1.jpg',1,1,'九成新','ON_SALE',12,4.80,9,'2026-10-08 10:12:00'),
(2,2,1,'AKKO 87键 机械键盘 茶轴',129.00,399.00,'87键 茶轴 白光','宿舍用了一年，键帽有轻微油光，轴体全部正常，没有连击。带原装拔键器和数据线，支持热插拔。','/uploads/p2.jpg',1,2,'九成新','ON_SALE',25,4.60,18,'2026-10-11 15:30:00'),
(3,2,1,'AOC 27英寸 2K 显示器',520.00,1099.00,'27寸 2K 75Hz','毕业学长留下的，屏幕无坏点无划痕，HDMI和DP线各一根，支架可升降。宿舍桌子放得下，自提优先。','/uploads/p3.jpg',1,1,'八成新','ON_SALE',8,4.70,6,'2026-10-15 09:05:00'),
(4,2,1,'罗技 M720 无线鼠标',89.00,129.00,'黑色','全新未拆封，买多了闲置。支持三设备切换，办公神器。','/uploads/p4.jpg',0,5,'全新','ON_SALE',31,4.90,22,'2026-10-20 11:45:00'),
(5,3,2,'数据结构与算法分析（C语言版）',18.00,45.00,'第二版','课本，重点章节有铅笔笔记和便签，不影响阅读。考试周救过我，希望它继续救下一个人。','/uploads/p5.jpg',1,3,'八成新','ON_SALE',46,4.90,35,'2026-09-28 14:20:00'),
(6,3,2,'考研数学复习全书 基础篇',35.00,79.00,'2026版','只做了前三章，后面全新。笔记是活页的，可以拆下来。','/uploads/p6.jpg',1,2,'九成新','ON_SALE',19,4.50,12,'2026-10-02 16:10:00'),
(7,3,2,'计算机网络：自顶向下方法',22.00,89.00,'第七版','书角有点卷，内页干净，配套的习题答案打印版一起送。','/uploads/p7.jpg',1,1,'七成新','ON_SALE',14,4.40,8,'2026-10-06 08:40:00'),
(8,4,4,'捷安特 ATX 山地自行车',260.00,899.00,'26寸 21速','骑了两年，链条上个月刚换，刹车皮也是新的。车锁一起给，钥匙两把。毕业带不走，只出校内。','/uploads/p8.jpg',1,1,'八成新','ON_SALE',6,4.70,4,'2026-10-09 17:25:00'),
(9,4,3,'小米 LED 护眼台灯',25.00,69.00,'白色','三档调光，无频闪，宿舍书桌用正好。灯管无发黄。','/uploads/p9.jpg',0,4,'九成新','ON_SALE',38,4.80,27,'2026-10-13 19:00:00'),
(10,4,4,'斯伯丁 7号篮球',45.00,128.00,'7号 室内外通用','打了半学期，皮面有磨损但不影响手感，气足。','/uploads/p10.jpg',1,1,'八成新','ON_SALE',11,4.30,7,'2026-10-17 12:15:00'),
(11,4,3,'24寸 万向轮行李箱',99.00,299.00,'24寸 深灰','就用过一次，飞机托运过一次，箱体有一道浅划痕。密码锁正常。','/uploads/p11.jpg',1,1,'九成新','ON_SALE',9,4.60,5,'2026-10-19 10:30:00'),
(12,4,3,'美的 1.5L 电热水壶',39.00,79.00,'1.5L 白色','全新未拆，宿舍限电不让用大功率，买回来一直放着。','/uploads/p12.jpg',0,3,'全新','PENDING',0,5.00,0,'2026-11-02 09:00:00');

-- 购物车
INSERT INTO `cart_items` (`user_id`,`product_id`,`quantity`) VALUES
(5,9,1),(5,5,2),(6,4,1);

-- 订单
INSERT INTO `orders` (`id`,`order_no`,`buyer_id`,`merchant_id`,`product_id`,`product_name`,`product_image`,`quantity`,`amount`,`origin_amount`,`fee`,`points_used`,`points_deduct`,`status`,`created_at`,`pay_time`,`confirm_deadline`,`receive_time`,`return_deadline`,`settle_time`) VALUES
(1,'20261015100001',5,3,5,'数据结构与算法分析（C语言版）','/uploads/p5.jpg',1,18.00,18.00,0.09,0,0.00,'COMPLETED','2026-10-15 10:00:12','2026-10-15 10:01:30','2026-10-22 10:01:30','2026-10-16 09:20:00','2026-10-17 09:20:00','2026-10-16 09:20:00'),
(2,'20261020093002',5,2,2,'AKKO 87键 机械键盘 茶轴','/uploads/p2.jpg',1,129.00,129.00,0.65,0,0.00,'COMPLETED','2026-10-20 09:30:22','2026-10-20 09:31:05','2026-10-27 09:31:05','2026-10-22 18:40:00','2026-10-23 18:40:00','2026-10-22 18:40:00'),
(3,'20261112143003',6,4,9,'小米 LED 护眼台灯','/uploads/p9.jpg',1,25.00,25.00,0.03,0,0.00,'PAID','2026-11-12 14:30:10','2026-11-12 14:30:45','2026-11-19 14:30:45',NULL,NULL,NULL),
(4,'20261113091504',5,2,4,'罗技 M720 无线鼠标','/uploads/p4.jpg',1,89.00,89.00,0.67,0,0.00,'RETURN_REQUESTED','2026-11-13 09:15:00','2026-11-13 09:15:40','2026-11-20 09:15:40','2026-11-13 20:10:00','2026-11-14 20:10:00',NULL);

-- 评价
INSERT INTO `reviews` (`order_id`,`product_id`,`merchant_id`,`buyer_id`,`type`,`rating`,`content`,`created_at`) VALUES
(1,5,3,5,'PRODUCT',5,'书比描述还新，笔记做得很整齐，反而帮我复习了。老板还多送了一支笔。','2026-10-16 21:10:00'),
(1,5,3,5,'MERCHANT',5,'发货快，约在图书馆门口，人很准时。','2026-10-16 21:11:00'),
(1,5,3,5,'BUYER',5,'买家很爽快，当天就确认收货了。','2026-10-16 22:00:00'),
(2,2,2,5,'PRODUCT',4,'键盘手感不错，就是键帽油光比图片上明显一点，其他都好。','2026-10-23 10:05:00'),
(2,2,2,5,'MERCHANT',5,'老板提前把轴体擦了一遍，还教我怎么调灯效。','2026-10-23 10:06:00');

-- 钱包流水
INSERT INTO `wallet_txns` (`user_id`,`type`,`amount`,`balance_after`,`remark`,`order_id`,`created_at`) VALUES
(5,'RECHARGE',1500.00,1500.00,'管理员充值',NULL,'2026-10-14 09:00:00'),
(5,'PAY',-18.00,1482.00,'购买 数据结构与算法分析（C语言版）',1,'2026-10-15 10:01:30'),
(5,'PAY',-129.00,1353.00,'购买 AKKO 87键 机械键盘 茶轴',2,'2026-10-20 09:31:05'),
(5,'PAY',-89.00,1264.00,'购买 罗技 M720 无线鼠标',4,'2026-11-13 09:15:40'),
(5,'RECHARGE',200.00,1464.00,'管理员充值',NULL,'2026-11-13 10:00:00'),
(3,'INCOME',17.91,724.29,'订单 20261015100001 货款入账（扣手续费0.09）',1,'2026-10-16 09:20:00'),
(2,'INCOME',128.35,3128.35,'订单 20261020093002 货款入账（扣手续费0.65）',2,'2026-10-22 18:40:00');
