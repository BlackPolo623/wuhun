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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius
 */
public class ExStorageMaxCount extends ServerPacket
{
	private Player _player;
	private int _inventory;
	private int _warehouse;
	// private int _freight; // Removed with 152.
	private int _clan;
	private int _privateSell;
	private int _privateBuy;
	private int _receipeD;
	private int _recipe;
	private int _inventoryExtraSlots;
	private int _inventoryQuestItems;
	
	public ExStorageMaxCount(Player player)
	{
		if (!player.isSubclassLocked()) // Changing class.
		{
			_player = player;
			_inventory = player.getInventoryLimit();
			_warehouse = player.getWareHouseLimit();
			// _freight = Config.ALT_FREIGHT_SLOTS; // Removed with 152.
			_privateSell = player.getPrivateSellStoreLimit();
			_privateBuy = player.getPrivateBuyStoreLimit();
			_clan = PlayerConfig.WAREHOUSE_SLOTS_CLAN;
			_receipeD = player.getDwarfRecipeLimit();
			_recipe = player.getCommonRecipeLimit();
			_inventoryExtraSlots = (int) player.getStat().getValue(Stat.INVENTORY_NORMAL, 0);
			_inventoryQuestItems = PlayerConfig.INVENTORY_MAXIMUM_QUEST_ITEMS;
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (_player == null)
		{
			return;
		}
		
		ServerPackets.EX_STORAGE_MAX_COUNT.writeId(this, buffer);
		buffer.writeInt(_inventory);
		buffer.writeInt(_warehouse);
		// buffer.writeInt(_freight); // Removed with 152.
		buffer.writeInt(_clan);
		buffer.writeInt(_privateSell);
		buffer.writeInt(_privateBuy);
		buffer.writeInt(_receipeD);
		buffer.writeInt(_recipe);
		buffer.writeInt(_inventoryExtraSlots); // Belt inventory slots increase count
		buffer.writeInt(_inventoryQuestItems);
		buffer.writeInt(40); // TODO: Find me!
		buffer.writeInt(40); // TODO: Find me!
		buffer.writeInt(0x64); // Artifact slots (Fixed)
	}
}
