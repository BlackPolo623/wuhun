/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.EnchantStarHolder;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillEnchantData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillEnchantType;
import org.l2jmobius.gameserver.model.skill.holders.SkillEnchantHolder;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExEnchantSkillResult;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.newskillenchant.ExSkillEnchantInfo;

/**
 * @author Mobius
 */
public class RequestExEnchantSkill extends ClientPacket
{
	private static final Logger LOGGER_ENCHANT = Logger.getLogger("enchant.skills");
	
	private SkillEnchantType _type;
	private int _skillId;
	private int _skillLevel;
	private int _skillSubLevel;
	
	@Override
	protected void readImpl()
	{
		final int type = readInt();
		if ((type < 0) || (type >= SkillEnchantType.values().length))
		{
			PacketLogger.warning("Client send incorrect type " + type + " on packet: " + getClass().getSimpleName());
			return;
		}
		
		_type = SkillEnchantType.values()[type];
		_skillId = readInt();
		_skillLevel = readShort();
		_skillSubLevel = readShort();
	}
	
	@Override
	protected void runImpl()
	{
		if (!getClient().getFloodProtectors().canPerformPlayerAction())
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_skillId <= 0) || (_skillLevel <= 0) || (_skillSubLevel < 0))
		{
			PacketLogger.warning(player + " tried to exploit RequestExEnchantSkill!");
			return;
		}
		
		if (!player.isAllowedToEnchantSkills())
		{
			return;
		}
		
		if (player.isSellingBuffs())
		{
			return;
		}
		
		if (player.isInOlympiadMode())
		{
			return;
		}
		
		if (player.isInStoreMode())
		{
			return;
		}
		
		final int skillId = player.getReplacementSkill(_skillId);
		Skill skill = player.getKnownSkill(skillId);
		if (skill == null)
		{
			return;
		}
		
		if (!skill.isEnchantable())
		{
			return;
		}
		
		if (skill.getLevel() != _skillLevel)
		{
			return;
		}
		
		if (skill.getSubLevel() > 0)
		{
			if (_type == SkillEnchantType.CHANGE)
			{
				final int group1 = _skillSubLevel % 1000;
				final int group2 = skill.getSubLevel() % 1000;
				if (group1 != group2)
				{
					PacketLogger.warning(getClass().getSimpleName() + ": Client: " + getClient() + " send incorrect sub level group: " + group1 + " expected: " + group2 + " for skill " + _skillId);
					return;
				}
			}
			else if ((skill.getSubLevel() + 1) != _skillSubLevel)
			{
				PacketLogger.warning(getClass().getSimpleName() + ": Client: " + getClient() + " send incorrect sub level: " + _skillSubLevel + " expected: " + (skill.getSubLevel() + 1) + " for skill " + _skillId);
				return;
			}
		}
		
		final SkillEnchantHolder skillEnchantHolder = SkillEnchantData.getInstance().getSkillEnchant(skill.getId());
		if (skillEnchantHolder == null)
		{
			PacketLogger.warning(getClass().getSimpleName() + " request enchant skill does not have star lvl skillId-" + skill.getId());
			return;
		}
		
		final EnchantStarHolder starHolder = SkillEnchantData.getInstance().getEnchantStar(skillEnchantHolder.getStarLevel());
		if (starHolder == null)
		{
			PacketLogger.warning(getClass().getSimpleName() + " request enchant skill does not have star lvl-" + skill.getId());
			return;
		}
		
		if (player.getAdena() < 1000000)
		{
			return;
		}
		
		player.reduceAdena(ItemProcessType.FEE, 1000000, null, true);
		
		final int starLevel = starHolder.getLevel();
		if (Rnd.get(100) <= SkillEnchantData.getInstance().getChanceEnchantMap(skill))
		{
			final Skill enchantedSkill = SkillData.getInstance().getSkill(skillId, _skillLevel, _skillSubLevel);
			if (GeneralConfig.LOG_SKILL_ENCHANTS)
			{
				final StringBuilder sb = new StringBuilder();
				LOGGER_ENCHANT.info(sb.append("Success, Character:").append(player.getName()).append(" [").append(player.getObjectId()).append("] Account:").append(player.getAccountName()).append(" IP:").append(player.getIPAddress()).append(", +").append(enchantedSkill.getLevel()).append(" ").append(enchantedSkill.getSubLevel()).append(" - ").append(enchantedSkill.getName()).append(" (").append(enchantedSkill.getId()).append("), ").toString());
			}
			
			final long reuse = player.getSkillRemainingReuseTime(skill.getReuseHashCode());
			if (reuse > 0)
			{
				player.addTimeStamp(enchantedSkill, reuse);
			}
			
			player.addSkill(enchantedSkill, true);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.SKILL_ENCHANT_WAS_SUCCESSFUL_S1_HAS_BEEN_ENCHANTED);
			sm.addSkillName(skillId);
			player.sendPacket(sm);
			
			player.sendPacket(ExEnchantSkillResult.STATIC_PACKET_TRUE);
			player.setSkillEnchantExp(starLevel, 0);
			player.setSkillTryEnchant(starLevel);
			skill = player.getKnownSkill(skillId);
		}
		else
		{
			player.sendPacket(ExEnchantSkillResult.STATIC_PACKET_FALSE);
			final int curExp = player.getSkillEnchantExp(starHolder.getLevel());
			if (curExp > 900000)
			{
				player.setSkillEnchantExp(starLevel, 0);
			}
			else
			{
				player.setSkillEnchantExp(starLevel, Math.min(900001, 90000 * player.getSkillTryEnchant(starLevel)));
				player.increaseTrySkillEnchant(starLevel);
			}
		}
		
		player.sendPacket(new ExSkillEnchantInfo(skill, player));
		player.updateShortcuts(skill.getId(), skill.getLevel(), skill.getSubLevel());
		
		player.broadcastUserInfo();
		player.sendSkillList();
	}
}
