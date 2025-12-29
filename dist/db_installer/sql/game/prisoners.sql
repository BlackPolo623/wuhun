DROP TABLE IF EXISTS `prisoners`;
CREATE TABLE IF NOT EXISTS `prisoners` (
  `charId` INT UNSIGNED NOT NULL DEFAULT 0,
  `sentenceTime` bigint(13) unsigned NOT NULL DEFAULT '0',
  `timeSpent` bigint(13) unsigned NOT NULL DEFAULT '0',
  `zoneId` INT UNSIGNED NOT NULL DEFAULT 0,
  `bailAmount` INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;