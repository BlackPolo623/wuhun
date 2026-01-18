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

 Date: 17/01/2026 05:11:02
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for scratch_cards
-- ----------------------------
DROP TABLE IF EXISTS `scratch_cards`;
CREATE TABLE `scratch_cards`  (
  `player_id` int(11) NOT NULL,
  `card_type` int(11) NOT NULL DEFAULT 1,
  `board_state` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `opened_positions` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `opened_count` int(11) NULL DEFAULT 0,
  `accumulated_count` int(11) NULL DEFAULT 0,
  `purchase_time` bigint(20) NOT NULL,
  PRIMARY KEY (`player_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of scratch_cards
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
