CREATE TABLE IF NOT EXISTS `pet_unclaimed` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player_id` int(11) NOT NULL,
  `pet_item_id` int(11) NOT NULL,
  `tier` int(11) NOT NULL,
  `hatch_time` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `player_id` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
