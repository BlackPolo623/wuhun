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
package org.l2jmobius.gameserver.network.clientpackets.skillenchantguarantee;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.skillenchantguarantee.ExRequestSkillEnchantConfirm;

/**
 * @author Liamxroy
 */
public class RequestSkillEnchantConfirm extends ClientPacket
{
	private int _skillId;
	private int _itemId;
	private int _commisionId;
	
	private byte _result = 1; // Fail
	private int _echantSkillSubLevel = 0;
	private long _LCoinFee = 132300L;
	private long feeAdena = 3000000L;
	
	@Override
	protected void readImpl()
	{
		_skillId = readInt();
		_itemId = readInt();
		_commisionId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		switch (_itemId) // guarantee coupon
		{
			case 100678: // (3 Stars) Guaranteed +1 Skill Enchantment Coupon
			{
				_LCoinFee = 15120;
				_echantSkillSubLevel = 1001;
				feeAdena = 1000000L;
				break;
			}
			case 100679: // (3 Stars) Guaranteed +2 Skill Enchantment Coupon
			{
				_LCoinFee = 41580;
				_echantSkillSubLevel = 1002;
				feeAdena = 2000000L;
				break;
			}
			case 100680: // (3 Stars) Guaranteed +3 Skill Enchantment Coupon
			{
				_LCoinFee = 132300;
				_echantSkillSubLevel = 1003;
				feeAdena = 3000000L;
				break;
			}
		}
		
		final Skill playerSkill = player.getKnownSkill(_skillId);
		final Item lCoin = player.getInventory().getItemByItemId(_commisionId);
		final Item guaranteeEnchantCoupon = player.getInventory().getItemByItemId(_itemId);
		if (guaranteeEnchantCoupon == null)
		{
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " try guarantee enchanted skill without guarantee coupon.");
			return;
		}
		
		if (playerSkill == null)
		{
			player.sendPacket(SystemMessageId.THE_ENCHANTMENT_CANNOT_BE_EXTRACTED);
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " try guarantee enchant skill that does not have. Skill:" + _skillId);
			return;
		}
		
		if (lCoin == null)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_L2_COINS);
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " try guarantee enchanted skill without L-Coins.");
			return;
		}
		
		if (lCoin.getCount() < _LCoinFee)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_L2_COINS);
			PacketLogger.warning(getClass().getSimpleName() + ": " + player + " try guarantee enchanted skill without the proper amount of L-Coins.");
			return;
		}
		
		if (player.getAdena() < feeAdena)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			return;
		}
		
		final Skill enchantSkill = SkillData.getInstance().getSkill(_skillId, playerSkill.getLevel(), _echantSkillSubLevel);
		player.reduceAdena(ItemProcessType.FEE, feeAdena, null, true);
		if (player.destroyItem(ItemProcessType.FEE, lCoin, _LCoinFee, null, true) && player.destroyItem(ItemProcessType.FEE, guaranteeEnchantCoupon, 1, null, true))
		{
			player.removeSkill(playerSkill.getId());
			player.addSkill(enchantSkill, true);
			player.sendSkillList();
			player.updateShortcuts(_skillId, playerSkill.getLevel(), 0);
			player.storeMe();
			
			_result = 0; // Success
		}
		
		player.sendPacket(new ExRequestSkillEnchantConfirm(_result, _skillId, playerSkill.getLevel(), _echantSkillSubLevel));
	}
}
