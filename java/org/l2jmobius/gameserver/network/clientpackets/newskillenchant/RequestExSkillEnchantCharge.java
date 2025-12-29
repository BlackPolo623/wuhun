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
package org.l2jmobius.gameserver.network.clientpackets.newskillenchant;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.data.holders.EnchantItemExpHolder;
import org.l2jmobius.gameserver.data.holders.EnchantStarHolder;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillEnchantData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillEnchantHolder;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.newskillenchant.ExSkillEnchantCharge;
import org.l2jmobius.gameserver.network.serverpackets.newskillenchant.ExSkillEnchantInfo;

/**
 * @author Serenitty
 */
public class RequestExSkillEnchantCharge extends ClientPacket
{
	private int _skillId;
	private int _skillLevel;
	private int _skillSubLevel;
	private final List<ItemHolder> _itemList = new ArrayList<>();
	
	@Override
	protected void readImpl()
	{
		_skillId = readInt();
		_skillLevel = readInt();
		_skillSubLevel = readInt();
		int size = readInt();
		for (int i = 0; i < size; i++)
		{
			final int objectId = readInt();
			final long count = readLong();
			if (count > 0)
			{
				_itemList.add(new ItemHolder(objectId, count));
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player != null)
		{
			final Skill skill = SkillData.getInstance().getSkill(_skillId, _skillLevel, _skillSubLevel);
			if (skill == null)
			{
				return;
			}
			
			final SkillEnchantHolder skillEnchantHolder = SkillEnchantData.getInstance().getSkillEnchant(skill.getId());
			if (skillEnchantHolder == null)
			{
				return;
			}
			
			final EnchantStarHolder starHolder = SkillEnchantData.getInstance().getEnchantStar(skillEnchantHolder.getStarLevel());
			if (starHolder == null)
			{
				return;
			}
			
			int curExp = player.getSkillEnchantExp(starHolder.getLevel());
			long feeAdena = 0;
			for (ItemHolder itemCharge : _itemList)
			{
				final Item item = player.getInventory().getItemByObjectId(itemCharge.getId());
				if (item == null)
				{
					PacketLogger.warning(getClass().getSimpleName() + " Player" + player.getName() + " trying charge skill exp enchant with not exist item by objectId - " + itemCharge.getId());
					continue;
				}
				
				final EnchantItemExpHolder itemExpHolder = SkillEnchantData.getInstance().getEnchantItem(starHolder.getLevel(), item.getId());
				if (itemExpHolder != null)
				{
					feeAdena = itemCharge.getCount() * starHolder.getFeeAdena();
					if (player.getAdena() < feeAdena)
					{
						player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
						player.sendPacket(new ExSkillEnchantCharge(skill.getId(), 0));
						return;
					}
					else if (itemExpHolder.getStarLevel() <= starHolder.getLevel())
					{
						curExp += itemExpHolder.getExp() * itemCharge.getCount();
						player.destroyItem(ItemProcessType.FEE, item, itemCharge.getCount(), null, true);
					}
					else
					{
						PacketLogger.warning(getClass().getSimpleName() + " Player" + player.getName() + " trying charge item with not support star level skillstarLevel-" + starHolder.getLevel() + " itemStarLevel-" + itemExpHolder.getStarLevel() + " itemId-" + itemExpHolder.getId());
					}
				}
				else
				{
					PacketLogger.warning(getClass().getSimpleName() + " Player" + player.getName() + " trying charge skill with missed item on XML  itemId-" + item.getId());
				}
			}
			
			player.setSkillEnchantExp(starHolder.getLevel(), Math.min(starHolder.getExpMax(), curExp));
			player.reduceAdena(ItemProcessType.FEE, feeAdena, null, true);
			player.sendPacket(new ExSkillEnchantCharge(skill.getId(), 0));
			player.sendPacket(new ExSkillEnchantInfo(skill, player));
		}
	}
}
