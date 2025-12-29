/*
MySQL Data Transfer
Source Host: localhost
Source Database: l2jmobiusessence
Target Host: localhost
Target Database: l2jmobiusessence
Date: 2025/11/15 15:13:23
*/

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for runmerchant_multipliers
-- ----------------------------
CREATE TABLE `runmerchant_multipliers` (
  `city_id` int(11) NOT NULL,
  `multiplier` double NOT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Records 
-- ----------------------------
INSERT INTO `runmerchant_multipliers` VALUES ('0', '0.16', '2025-11-15 13:58:50');
INSERT INTO `runmerchant_multipliers` VALUES ('1', '1.71', '2025-11-15 13:58:50');
INSERT INTO `runmerchant_multipliers` VALUES ('2', '3.15', '2025-11-15 13:58:50');
INSERT INTO `runmerchant_multipliers` VALUES ('3', '1', '2025-11-15 13:58:50');
INSERT INTO `runmerchant_multipliers` VALUES ('4', '1.18', '2025-11-15 13:58:50');
INSERT INTO `runmerchant_multipliers` VALUES ('5', '2.34', '2025-11-15 13:58:50');
