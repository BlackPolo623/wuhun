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
package org.l2jmobius.gameserver.network.clientpackets.characterstyle;

import org.l2jmobius.gameserver.data.enums.CharacterStyleCategoryType;
import org.l2jmobius.gameserver.data.xml.CharacterStylesData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoEquipSlot;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.characterstyle.ExCharacterStyleSelect;

/**
 * @author Brado
 */
public class ExRequestCharacterStyleSelect extends ClientPacket
{
	private int _styleType;
	private int _styleId;
	
	@Override
	protected void readImpl()
	{
		_styleType = readInt();
		_styleId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final CharacterStyleCategoryType category = CharacterStyleCategoryType.getByClientId(_styleType);
		if (_styleId != 0)
		{
			final ItemHolder applyCostHolder = CharacterStylesData.getInstance().getSwapCostItemByCategory(category);
			if (applyCostHolder != null)
			{
				if (player.getInventory().getInventoryItemCount(applyCostHolder.getId(), -1) < applyCostHolder.getCount())
				{
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
					return;
				}
				
				if (player.getInventory().destroyItemByItemId(ItemProcessType.DESTROY, applyCostHolder.getId(), applyCostHolder.getCount(), player, null) == null)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
					return;
				}
			}
		}
		
		player.setActiveCharacterStyle(category, _styleId);
		player.sendPacket(ExCharacterStyleSelect.STATIC_PACKET);

		// 根據不同造型類型發送對應的更新封包
		switch (category)
		{
			case APPEARANCE_WEAPON:
			{
				player.sendPacket(new ExUserInfoEquipSlot(player));
				player.broadcastUserInfo();
				break;
			}
			case APPEARANCE_ARMOR:
			{
				player.sendPacket(new ExUserInfoEquipSlot(player));
				player.broadcastUserInfo();
				break;
			}
			case KILL_EFFECT:
			case CHAT_BACKGROUND:
			{
				// 特效和背景不需要更新裝備外觀
				player.broadcastUserInfo();
				break;
			}
		}
	}
}
