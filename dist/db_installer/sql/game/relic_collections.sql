DROP TABLE IF EXISTS `relic_collections`;
CREATE TABLE `relic_collections` (
  `accountName` varchar(45) NOT NULL DEFAULT '',
  `relicId` int(11) unsigned NOT NULL DEFAULT 0,
  `relicLevel` int(11) NOT NULL,
  `relicCollectionId` int(3) unsigned NOT NULL DEFAULT 0,
  `index` tinyint(3) NOT NULL,
  PRIMARY KEY (`accountName`,`relicCollectionId`,`index`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;
