-- =======================================================
-- 寶玉系統資料庫表結構
-- =======================================================

-- 玩家寶玉數據表
CREATE TABLE IF NOT EXISTS `player_jewel_data` (
  `char_id` INT NOT NULL,
  `is_activated` TINYINT(1) NOT NULL DEFAULT 0,
  `current_value_stage` INT NOT NULL DEFAULT 0,
  `current_bonus_stage` INT NOT NULL DEFAULT 0,
  `stage1_value` BIGINT NOT NULL DEFAULT 0,
  `stage2_value` BIGINT NOT NULL DEFAULT 0,
  `stage3_value` BIGINT NOT NULL DEFAULT 0,
  `stage4_value` BIGINT NOT NULL DEFAULT 0,
  `stage5_value` BIGINT NOT NULL DEFAULT 0,
  `stage6_value` BIGINT NOT NULL DEFAULT 0,
  `stage7_value` BIGINT NOT NULL DEFAULT 0,
  `stage8_value` BIGINT NOT NULL DEFAULT 0,
  `stage9_value` BIGINT NOT NULL DEFAULT 0,
  `stage10_value` BIGINT NOT NULL DEFAULT 0,
  `stage11_value` BIGINT NOT NULL DEFAULT 0,
  `stage12_value` BIGINT NOT NULL DEFAULT 0,
  `stage13_value` BIGINT NOT NULL DEFAULT 0,
  `stage14_value` BIGINT NOT NULL DEFAULT 0,
  `stage15_value` BIGINT NOT NULL DEFAULT 0,
  `stage16_value` BIGINT NOT NULL DEFAULT 0,
  `stage17_value` BIGINT NOT NULL DEFAULT 0,
  `stage18_value` BIGINT NOT NULL DEFAULT 0,
  `stage19_value` BIGINT NOT NULL DEFAULT 0,
  `stage20_value` BIGINT NOT NULL DEFAULT 0,
  `last_update` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 索引優化
CREATE INDEX IF NOT EXISTS `idx_jewel_activated` ON `player_jewel_data` (`is_activated`);
