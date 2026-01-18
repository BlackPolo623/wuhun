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

 Date: 18/01/2026 13:04:35
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for player_base
-- ----------------------------
DROP TABLE IF EXISTS `player_base`;
CREATE TABLE `player_base`  (
  `player_id` int(11) NOT NULL,
  `player_name` varchar(35) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `instance_id` int(11) NOT NULL,
  `template_id` int(11) NOT NULL,
  `max_monster_count` int(11) NULL DEFAULT 50,
  `created_time` bigint(20) NOT NULL,
  `can_summon_boss` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`player_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of player_base
-- ----------------------------

-- ----------------------------
-- Table structure for player_base_monsters
-- ----------------------------
DROP TABLE IF EXISTS `player_base_monsters`;
CREATE TABLE `player_base_monsters`  (
  `base_owner_id` int(11) NOT NULL,
  `spawn_index` int(11) NOT NULL,
  `monster_id` int(11) NOT NULL,
  `monster_count` int(11) NOT NULL,
  PRIMARY KEY (`base_owner_id`, `spawn_index`) USING BTREE,
  CONSTRAINT `player_base_monsters_ibfk_1` FOREIGN KEY (`base_owner_id`) REFERENCES `player_base` (`player_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of player_base_monsters
-- ----------------------------

-- ----------------------------
-- Table structure for player_base_visitors
-- ----------------------------
DROP TABLE IF EXISTS `player_base_visitors`;
CREATE TABLE `player_base_visitors`  (
  `base_owner_id` int(11) NOT NULL,
  `visitor_id` int(11) NOT NULL,
  `visitor_name` varchar(35) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `added_time` bigint(20) NOT NULL,
  PRIMARY KEY (`base_owner_id`, `visitor_id`) USING BTREE,
  CONSTRAINT `player_base_visitors_ibfk_1` FOREIGN KEY (`base_owner_id`) REFERENCES `player_base` (`player_id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of player_base_visitors
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
