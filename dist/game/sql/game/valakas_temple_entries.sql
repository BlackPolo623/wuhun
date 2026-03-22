CREATE TABLE IF NOT EXISTS `valakas_temple_entries` (
  `charId` INT UNSIGNED NOT NULL,
  `weekly_entries` INT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
