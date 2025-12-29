DROP TABLE IF EXISTS `black_coupon`;
CREATE TABLE `black_coupon`  (
  `owner_id` int(11) NOT NULL,
  `item_id` int(11) NOT NULL,
  `enchant_level` smallint(3) NOT NULL,
  `add_time` bigint(20) NOT NULL,
  PRIMARY KEY (`owner_id`, `item_id`, `enchant_level`, `add_time`) USING BTREE
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
SET FOREIGN_KEY_CHECKS = 1;
