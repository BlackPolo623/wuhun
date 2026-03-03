-- ====================================
-- 寵物孵化系統數據表
-- ====================================

-- 玩家孵化數據表
CREATE TABLE IF NOT EXISTS `pet_hatching_data` (
  `player_id` INT NOT NULL,
  `slot_index` TINYINT NOT NULL,
  `egg_item_id` INT NOT NULL,
  `egg_tier` TINYINT NOT NULL COMMENT '0=一般,1=特殊,2=稀有,3=罕見,4=傳說',
  `start_time` BIGINT NOT NULL COMMENT '開始孵化時間戳(毫秒)',
  `hatch_duration` INT NOT NULL COMMENT '孵化所需時間(分鐘)',
  `feed_consumed` INT NOT NULL DEFAULT 0 COMMENT '已消耗飼料數量',
  `upgrade_chance` INT NOT NULL DEFAULT 10 COMMENT '當前進階機率(%)',
  PRIMARY KEY (`player_id`, `slot_index`),
  INDEX `idx_player` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寵物孵化數據';

-- 玩家飼料儲存表
CREATE TABLE IF NOT EXISTS `pet_feed_storage` (
  `player_id` INT NOT NULL PRIMARY KEY,
  `feed_count` INT NOT NULL DEFAULT 0 COMMENT '儲存的飼料數量',
  INDEX `idx_player` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家飼料儲存';

-- 玩家孵化統計表
CREATE TABLE IF NOT EXISTS `pet_hatching_stats` (
  `player_id` INT NOT NULL PRIMARY KEY,
  `total_hatches` INT NOT NULL DEFAULT 0 COMMENT '總孵化次數',
  `current_slots` TINYINT NOT NULL DEFAULT 1 COMMENT '當前孵化槽數量',
  INDEX `idx_player` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='玩家孵化統計';

-- 寵物收藏表
CREATE TABLE IF NOT EXISTS `pet_collection` (
  `player_id` INT NOT NULL,
  `pet_item_id` INT NOT NULL COMMENT '寵物道具ID(113001-113100)',
  `stored` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已儲存(0=未儲存,1=已儲存)',
  PRIMARY KEY (`player_id`, `pet_item_id`),
  INDEX `idx_player` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寵物收藏系統';
