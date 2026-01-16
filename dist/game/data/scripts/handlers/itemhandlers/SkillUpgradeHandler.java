/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.itemhandlers;

import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.model.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * 技能升級道具處理器 使用道具直接提升對應技能1級
 * @author 黑普羅
 */
public class SkillUpgradeHandler implements IItemHandler
{
	// ==================== 特殊道具配置 ====================
	private static final int SPECIAL_ITEM_MIN = 106100;
	private static final int SPECIAL_ITEM_MAX = 106106;

	// 額外獎勵機率 (1%)
	private static final double BONUS_CHANCE = 1.0;

	// 額外獎勵配置 [道具ID, 數量]
	// 你可以在這裡自由添加或修改獎勵
	private static final int[][] BONUS_REWARDS = {
			{106107, 1},
			{106108, 1},
			{106109, 1},
	};

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}

		final Player player = playable.asPlayer();
		final int itemId = item.getId();

		// 檢查是否為特殊道具
		final boolean isSpecialItem = (itemId >= SPECIAL_ITEM_MIN) && (itemId <= SPECIAL_ITEM_MAX);

		// 獲取道具的技能配置
		final List<ItemSkillHolder> skills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
		if (skills == null)
		{
			LOGGER.warning("道具 " + item.getId() + " 沒有配置技能!");
			return false;
		}

		// 如果不是特殊道具，檢查技能是否可以升級
		if (!isSpecialItem)
		{
			boolean canUpgrade = false;
			for (ItemSkillHolder skillInfo : skills)
			{
				if (skillInfo == null)
				{
					continue;
				}

				final Skill configSkill = skillInfo.getSkill();
				if (configSkill == null)
				{
					continue;
				}

				final int skillId = configSkill.getId();
				final Skill currentSkill = player.getKnownSkill(skillId);
				int targetLevel = (currentSkill == null) ? 1 : currentSkill.getLevel() + 1;
				final Skill targetSkill = SkillData.getInstance().getSkill(skillId, targetLevel);

				if (targetSkill != null)
				{
					canUpgrade = true;
					break;
				}
			}

			// 普通道具如果技能已滿級，不允許使用
			if (!canUpgrade)
			{
				player.sendPacket(new ExShowScreenMessage("技能已達最高等級!", 3000));
				return false;
			}
		}

		// 消耗道具
		if (!player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, player, false))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			return false;
		}

		// 處理技能升級
		boolean skillUpgraded = false;
		for (ItemSkillHolder skillInfo : skills)
		{
			if (skillInfo == null)
			{
				continue;
			}

			final Skill configSkill = skillInfo.getSkill();
			if (configSkill == null)
			{
				continue;
			}

			final int skillId = configSkill.getId();
			final Skill currentSkill = player.getKnownSkill(skillId);

			// 計算目標等級
			int targetLevel = (currentSkill == null) ? 1 : currentSkill.getLevel() + 1;

			// 檢查目標等級是否存在
			final Skill targetSkill = SkillData.getInstance().getSkill(skillId, targetLevel);
			if (targetSkill == null)
			{
				// 技能已滿級
				if (isSpecialItem)
				{
					// 特殊道具：提示技能已滿級但不影響使用
					player.sendPacket(new ExShowScreenMessage("技能已達最高等級", 2000));
				}
				else
				{
					// 普通道具：不應該走到這裡，因為前面已經檢查過
					player.sendPacket(new ExShowScreenMessage("技能已達最高等級!", 3000));
				}
				continue;
			}

			// 移除舊技能
			if (currentSkill != null)
			{
				player.removeSkill(currentSkill, true);
			}

			// 添加新技能
			player.addSkill(targetSkill, true);
			player.sendSkillList();

			// 發送成功訊息
			String message = (currentSkill == null) ?
					"獲得新技能: " + targetSkill.getName() + " Lv." + targetLevel :
					"技能升級: " + targetSkill.getName() + " Lv." + currentSkill.getLevel() + " → Lv." + targetLevel;

			player.sendPacket(new ExShowScreenMessage(message, 3000));

			LOGGER.info("玩家 " + player.getName() + " 使用技能書提升 " + targetSkill.getName() + " 至 Lv." + targetLevel);
			skillUpgraded = true;
		}

		// 如果是特殊道具，處理額外獎勵（無論技能是否升級）
		if (isSpecialItem)
		{
			processSpecialReward(player);
		}

		return true;
	}

	/**
	 * 處理特殊道具的額外獎勵
	 * @param player 玩家
	 */
	private void processSpecialReward(Player player)
	{
		// 檢查背包空間
		if (!player.getInventory().validateCapacity(1))
		{
			player.sendMessage("背包空間不足，無法獲得額外獎勵！");
			return;
		}

		// 1% 機率獲得獎勵
		if (Rnd.get(10000) < (BONUS_CHANCE * 100))
		{
			// 隨機選擇一個獎勵
			int[] reward = BONUS_REWARDS[Rnd.get(BONUS_REWARDS.length)];
			int itemId = reward[0];
			int count = reward[1];

			// 給予獎勵
			player.addItem(ItemProcessType.NONE, itemId, count, player, true);

			// 發送特殊訊息
			player.sendPacket(new ExShowScreenMessage("★★★ 恭喜！觸發額外獎勵！★★★", 5000));
			player.sendMessage("========================================");
			player.sendMessage("恭喜您觸發了1%的幸運獎勵！");
			player.sendMessage("========================================");

			LOGGER.info("玩家 " + player.getName() + " 使用特殊技能書觸發額外獎勵: 道具ID=" + itemId + " 數量=" + count);
		}
	}
}