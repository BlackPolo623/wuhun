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
package org.l2jmobius.gameserver.network.clientpackets.skillenchantextract;

import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.skillenchantextract.ExExtractSkillEnchant;

/**
 * @author Liamxroy
 */
public class RequestExtractSkillEnchant extends ClientPacket
{
	private byte _result = 1; // Fail
	private int _skillId;
	private int _skillLevel;
	private int _skillSubLevel;
	private int _itemId;
	private long _LCoinFee = 308600L;
	private int _rewardId = 57;
	
	@Override
	protected void readImpl()
	{
		_skillId = readInt();
		_skillLevel = readInt();
		_skillSubLevel = readInt();
		_itemId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		switch (_skillSubLevel)
		{
			case 1001:
			{
				_LCoinFee = 35280;
				_rewardId = 100678; // (3 Stars) Guaranteed +1 Skill Enchantment Coupon
				break;
			}
			case 1002:
			{
				_LCoinFee = 97020;
				_rewardId = 100679; // (3 Stars) Guaranteed +2 Skill Enchantment Coupon
				break;
			}
			case 1003:
			{
				_LCoinFee = 308600;
				_rewardId = 100680; // (3 Stars) Guaranteed +3 Skill Enchantment Coupon
				break;
			}
		}
		
		long feeAdena = 3000000L;
		final Skill enchantedSkill = SkillData.getInstance().getSkill(_skillId, _skillLevel, _skillSubLevel);
		final Skill playerSkill = player.getKnownSkill(_skillId);
		final Skill normalSkill = SkillData.getInstance().getSkill(_skillId, _skillLevel, 0);
		final Item lCoin = player.getInventory().getItemByItemId(_itemId);
		final ItemTemplate reward = ItemData.getInstance().getTemplate(_rewardId);
		if (reward == null)
		{
			player.sendPacket(SystemMessageId.THE_ENCHANTMENT_CANNOT_BE_EXTRACTED);
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " trying get a reward from extract enchanted skill that does not exist.");
			return;
		}
		
		if (enchantedSkill == null)
		{
			player.sendPacket(SystemMessageId.THE_ENCHANTMENT_CANNOT_BE_EXTRACTED);
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " trying extract enchanted skill that does not exist. Skill:" + _skillId);
			return;
		}
		
		if (playerSkill == null)
		{
			player.sendPacket(SystemMessageId.THE_ENCHANTMENT_CANNOT_BE_EXTRACTED);
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " trying extract enchanted skill that does not have. Skill:" + _skillId);
			return;
		}
		
		if (playerSkill.getSubLevel() != _skillSubLevel)
		{
			player.sendPacket(SystemMessageId.THE_ENCHANTMENT_CANNOT_BE_EXTRACTED);
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " trying extract enchanted skill that is different that the one in the list. Skill:" + enchantedSkill.getId());
			return;
		}
		
		if (lCoin == null)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_L2_COINS);
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " trying extract enchanted skill without L-Coins (Client should have disabled the button).");
			return;
		}
		
		if (lCoin.getCount() < _LCoinFee)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_L2_COINS);
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " trying extract enchanted skill without the proper amount of L-Coins (Client should have disabled the button).");
			return;
		}
		
		if (player.getAdena() < feeAdena)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			return;
		}
		
		if (!player.getInventory().validateCapacity(1))
		{
			final SystemMessage sme = new SystemMessage(SystemMessageId.UNABLE_TO_EXTRACT_BECAUSE_INVENTORY_IS_FULL);
			player.sendPacket(sme);
			return;
		}
		
		if (player.isInCombat())
		{
			final SystemMessage sma = new SystemMessage(SystemMessageId.UNABLE_TO_EXTRACT_WHILE_IN_COMBAT_MODE);
			player.sendPacket(sma);
			return;
		}
		
		player.reduceAdena(ItemProcessType.FEE, feeAdena, null, true);
		if (player.destroyItem(ItemProcessType.FEE, lCoin, _LCoinFee, null, true))
		{
			player.removeSkill(enchantedSkill);
			player.addSkill(normalSkill, true);
			player.sendSkillList();
			player.updateShortcuts(_skillId, _skillLevel, 0);
			player.storeMe();
			
			player.addItem(ItemProcessType.REWARD, _rewardId, 1, 0, player, true);
			
			final SystemMessage smi = new SystemMessage(SystemMessageId.EXTRACTED_S1_S2_SUCCESSFULLY);
			smi.addItemName(reward.getId());
			smi.addString("x1");
			player.sendPacket(smi);
			
			_result = 0; // Success
			_skillSubLevel = playerSkill.getSubLevel();
		}
		
		player.sendPacket(new ExExtractSkillEnchant(_result, _skillId, _skillLevel, _skillSubLevel));
	}
}
