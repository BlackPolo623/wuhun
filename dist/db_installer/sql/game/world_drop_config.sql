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

 Date: 26/11/2025 20:39:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for world_drop_config
-- ----------------------------
DROP TABLE IF EXISTS `world_drop_config`;
CREATE TABLE `world_drop_config`  (
  `item_id` int(11) NOT NULL,
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '道具名稱',
  `max_count` int(11) NOT NULL COMMENT '每週期最大掉落數量',
  `current_count` int(11) NOT NULL COMMENT '當前剩餘數量',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否啟用',
  PRIMARY KEY (`item_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of world_drop_config
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
