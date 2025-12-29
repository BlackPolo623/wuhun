DROP TABLE IF EXISTS `leonas_dungeon_ranking`;
CREATE TABLE IF NOT EXISTS `leonas_dungeon_ranking` (
  `charId` INT NOT NULL,
  `points` INT NOT NULL,
  PRIMARY KEY (`charId`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;
