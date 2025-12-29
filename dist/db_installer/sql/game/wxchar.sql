/*
MySQL Data Transfer
Source Host: localhost
Source Database: l2jmobiusessence
Target Host: localhost
Target Database: l2jmobiusessence
Date: 2025/11/15 15:13:12
*/

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for wxchar
-- ----------------------------
CREATE TABLE `wxchar` (
  `id` int(11) NOT NULL,
  `shuzhi` int(11) NOT NULL DEFAULT '0',
  `stat` varchar(40) COLLATE latin1_general_ci NOT NULL DEFAULT '0',
  `level` int(11) NOT NULL DEFAULT '0',
  KEY `idx_itemId` (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

-- ----------------------------
-- Records 
-- ----------------------------
INSERT INTO `wxchar` VALUES ('268500706', '52', 'patk', '1');
INSERT INTO `wxchar` VALUES ('268500968', '8', 'patk', '1');
INSERT INTO `wxchar` VALUES ('268500968', '38', 'matk', '1');
INSERT INTO `wxchar` VALUES ('268500968', '26', 'maxhp', '1');
