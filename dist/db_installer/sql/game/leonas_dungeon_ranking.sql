DROP TABLE IF EXISTS `leonas_dungeon_ranking`;
CREATE TABLE IF NOT EXISTS `leonas_dungeon_ranking` (
  `charId` INT NOT NULL,
  `points` INT NOT NULL DEFAULT 0,
  `weekly_entries` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;
