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

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @author Serenitty
 */
public class RequestChangeNicknameEmote extends ClientPacket
{
	private static final int ESPECIAL_COLOR_TITLE_EMOTE = 95892;
	private static final int ESPECIAL_COLOR_TITLE_SEALED = 94764;
	private static final int ESPECIAL_STYLISH_COLOR_TITLE = 49662;
	private static final int[] COLORS =
	{
		0x9393FF, // Pink 1
		0x7C49FC, // Rose Pink 2
		0x97F8FC, // Yellow 3
		0xFA9AEE, // Lilac 4
		0xFF5D93, // Cobalt Violet 5
		0x00FCA0, // Mint Green 6
		0xA0A601, // Peacock Green 7
		0x7898AF, // Ochre 8
		0x486295, // Chocolate 9
		0x999999, // Silver 10 ** good here
		0xF3DC09, // SkyBlue 11
		0x05D3F6, // Gold 12
		0x3CB1F4, // Orange 13
		0xF383F3, // Pink 14
		0x0909F3, // Red 15
		0xF3DC09, // SkyBlue 16
		0x000000, // dummy
	};
	
	private int _colorNum;
	private int _itemId;
	private String _title;
	
	@Override
	protected void readImpl()
	{
		_itemId = readInt();
		_colorNum = readInt();
		_title = readSizedString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item item = player.getInventory().getItemByItemId(_itemId);
		if ((item == null) || (item.getEtcItem() == null) || (item.getEtcItem().getHandlerName() == null) || !item.getEtcItem().getHandlerName().equalsIgnoreCase("NicknameColor"))
		{
			return;
		}
		
		if ((_colorNum < 0) || (_colorNum >= COLORS.length))
		{
			return;
		}
		
		// FIXME: Title icon uses {}.
		// if (_title.contains("{"))
		// {
		// player.sendMessage("Cannot use this type of characters {}");
		// return;
		// }
		
		if (((_itemId == ESPECIAL_COLOR_TITLE_EMOTE) || (_itemId == ESPECIAL_COLOR_TITLE_SEALED) || (_itemId == ESPECIAL_STYLISH_COLOR_TITLE)) && player.destroyItem(ItemProcessType.NONE, item, 1, null, true))
		{
			player.setTitle(_title);
			player.getAppearance().setTitleColor(COLORS[_colorNum - 1]);
			player.broadcastUserInfo();
			player.sendPacket(SystemMessageId.YOUR_TITLE_HAS_BEEN_CHANGED);
			return;
		}
		
		if (player.destroyItem(ItemProcessType.NONE, item, 1, null, true))
		{
			int skyblue = _colorNum - 2;
			if ((skyblue > 11) && (player.getLevel() >= 90))
			{
				skyblue = 15;
			}
			
			player.setTitle(_title);
			player.sendPacket(SystemMessageId.YOUR_TITLE_HAS_BEEN_CHANGED);
			player.getAppearance().setTitleColor(COLORS[skyblue]);
			player.broadcastUserInfo();
		}
		
	}
	
}
