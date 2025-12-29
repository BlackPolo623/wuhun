DROP TABLE IF EXISTS `aden_laboratory`;
CREATE TABLE IF NOT EXISTS `aden_laboratory` (
  `charId` int(10) NOT NULL,
  `bossId` int(10) NOT NULL,
  `unlockedPage` int(10) NOT NULL,
  `openedCardsCount` int(10) NOT NULL,
  `specialDrawnOptions` text DEFAULT NULL,
  `specialConfirmedOptions` text DEFAULT NULL,
  `transcendLevel` int(10) NOT NULL,
  PRIMARY KEY (`charId`, `bossId`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;