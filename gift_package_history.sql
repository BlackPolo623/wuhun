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

 Date: 15/01/2026 02:54:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for gift_package_history
-- ----------------------------
DROP TABLE IF EXISTS `gift_package_history`;
CREATE TABLE `gift_package_history`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '記錄ID',
  `package_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '禮包ID',
  `package_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '禮包名稱',
  `account_name` varchar(45) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '帳號名稱',
  `char_name` varchar(35) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '角色名稱',
  `quantity` int(11) NOT NULL COMMENT '發送數量',
  `send_date` timestamp NULL DEFAULT current_timestamp() COMMENT '發送時間',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_account`(`account_name`) USING BTREE,
  INDEX `idx_package`(`package_id`) USING BTREE,
  INDEX `idx_send_date`(`send_date`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '禮包發送歷史記錄' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of gift_package_history
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
