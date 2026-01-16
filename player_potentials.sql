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

 Date: 09/01/2026 04:16:47
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for player_potentials
-- ----------------------------
DROP TABLE IF EXISTS `player_potentials`;
CREATE TABLE `player_potentials`  (
  `player_id` int(11) NOT NULL,
  `slot_index` tinyint(4) NOT NULL,
  `skill_id` int(11) NOT NULL,
  PRIMARY KEY (`player_id`, `slot_index`) USING BTREE,
  INDEX `idx_player`(`player_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of player_potentials
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
