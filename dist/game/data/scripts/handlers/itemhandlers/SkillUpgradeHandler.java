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
 * 技能升級道具處理器
 * 使用道具直接提升對應技能1級
 * @author 黑普羅
 */
public class SkillUpgradeHandler implements IItemHandler
{
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}

		final Player player = playable.asPlayer();

		// 獲取道具的技能配置
		final List<ItemSkillHolder> skills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
		if (skills == null)
		{
			LOGGER.warning("道具 " + item.getId() + " 沒有配置技能!");
			return false;
		}

		boolean successfulUse = false;

		// 處理每個技能
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
				player.sendPacket(new ExShowScreenMessage("技能已達最高等級!", 3000));
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
			String message = (currentSkill == null)
					? "獲得新技能: " + targetSkill.getName() + " Lv." + targetLevel
					: "技能升級: " + targetSkill.getName() + " Lv." + currentSkill.getLevel() + " → Lv." + targetLevel;

			player.sendPacket(new ExShowScreenMessage(message, 3000));

			successfulUse = true;

			LOGGER.info("玩家 " + player.getName() + " 使用技能書提升 " + targetSkill.getName() + " 至 Lv." + targetLevel);
		}

		// 消耗道具
		if (successfulUse && !player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, player, false))
		{
			player.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			return false;
		}

		return successfulUse;
	}
}