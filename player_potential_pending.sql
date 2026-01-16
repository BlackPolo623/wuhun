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

 Date: 09/01/2026 04:16:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for player_potential_pending
-- ----------------------------
DROP TABLE IF EXISTS `player_potential_pending`;
CREATE TABLE `player_potential_pending`  (
  `player_id` int(11) NOT NULL,
  `pending_type` tinyint(4) NOT NULL COMMENT '1=結合重骰, 2=自由重骰',
  `old_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '舊數據(JSON格式)',
  `new_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '新數據(JSON格式)',
  `created_time` bigint(20) NOT NULL,
  PRIMARY KEY (`player_id`) USING BTREE,
  INDEX `idx_type`(`pending_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of player_potential_pending
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
