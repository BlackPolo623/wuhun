CREATE TABLE IF NOT EXISTS `item_restore` (
  `ownerId` INT(11) NOT NULL,
  `objectId` INT(11) NOT NULL,
  `dateLost` datetime(0) NOT NULL,
  `killerObj` INT(11) NOT NULL
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;
