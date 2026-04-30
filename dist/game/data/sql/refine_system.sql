-- 精煉次數記錄表
CREATE TABLE IF NOT EXISTS `item_refine_charges` (
  `item_id`  INT(11) NOT NULL,
  `charges`  INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`item_id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

-- 精煉系統：新增 option4 欄位到 item_variations（若尚未存在）
ALTER TABLE `item_variations` ADD COLUMN IF NOT EXISTS `option4` INT(11) NOT NULL DEFAULT 0;
