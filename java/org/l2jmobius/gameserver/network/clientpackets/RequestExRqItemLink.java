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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.network.serverpackets.ExRpItemLink;

/**
 * @author KenM
 */
public class RequestExRqItemLink extends ClientPacket
{
	private int _objectId;

	@Override
	protected void readImpl()
	{
		_objectId = readInt();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}

		System.out.println("[ItemLink] 收到請求 - 請求者: " + player.getName() + " | objectId: " + _objectId);

		// Try ground items first (registered in World)
		final WorldObject object = World.getInstance().findObject(_objectId);
		if ((object != null) && object.isItem())
		{
			final Item item = (Item) object;
			System.out.println("[ItemLink] 在World找到物品 - 名稱: " + item.getName() + " | isPublished: " + item.isPublished() + " | ownerId: " + item.getOwnerId());
			if (item.isPublished())
			{
				System.out.println("[ItemLink] 發送封包 (World物品)");
				player.sendPacket(new ExRpItemLink(item));
			}
			else
			{
				System.out.println("[ItemLink] 物品未published，不發送封包");
			}
			return;
		}

		System.out.println("[ItemLink] World找不到，搜尋玩家背包...");

		// Fallback: search inventory items (not registered in World)
		for (Player online : World.getInstance().getPlayers())
		{
			final Item item = online.getInventory().getItemByObjectId(_objectId);
			if (item != null)
			{
				System.out.println("[ItemLink] 在背包找到物品 - 擁有者: " + online.getName() + " | 名稱: " + item.getName() + " | isPublished: " + item.isPublished() + " | ownerId: " + item.getOwnerId());
				if (item.isPublished())
				{
					System.out.println("[ItemLink] 發送封包 (背包物品)");
					player.sendPacket(new ExRpItemLink(item));
				}
				else
				{
					System.out.println("[ItemLink] 物品未published，不發送封包");
				}
				return;
			}
		}

		System.out.println("[ItemLink] 找不到任何物品，objectId: " + _objectId);
	}
}
