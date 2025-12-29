/*
 Navicat Premium Data Transfer

 Source Server         : Lineage2
 Source Server Type    : MariaDB
 Source Server Version : 101001
 Source Host           : localhost:3306
 Source Schema         : l2jmobius_wuhun

 Target Server Type    : MariaDB
 Target Server Version : 101001
 File Encoding         : 65001

 Date: 26/11/2025 20:39:32
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for world_drop_settings
-- ----------------------------
DROP TABLE IF EXISTS `world_drop_settings`;
CREATE TABLE `world_drop_settings`  (
  `id` int(11) NOT NULL DEFAULT 1,
  `last_reset` bigint(20) NOT NULL COMMENT '上次重置時間',
  `next_reset` bigint(20) NOT NULL COMMENT '下次重置時間',
  `reset_days` int(11) NOT NULL DEFAULT 7 COMMENT '重置週期(天數)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of world_drop_settings
-- ----------------------------
INSERT INTO `world_drop_settings` VALUES (1, 1764160754000, 1764765554000, 7);

SET FOREIGN_KEY_CHECKS = 1;
