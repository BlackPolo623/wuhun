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
package org.l2jmobius.gameserver.network.clientpackets.commission;

import java.util.function.Predicate;

import org.l2jmobius.gameserver.managers.ItemCommissionManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.commission.CommissionItemType;
import org.l2jmobius.gameserver.model.commission.CommissionTreeType;
import org.l2jmobius.gameserver.model.item.ItemTemplate;
import org.l2jmobius.gameserver.model.item.type.CrystalType;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.commission.ExCloseCommission;

/**
 * @author NosBit, Mobius
 */
public class RequestCommissionList extends ClientPacket
{
	private int _treeViewDepth;
	private int _itemType;
	private int _type;
	private int _grade;
	private String _query;
	
	@Override
	protected void readImpl()
	{
		_treeViewDepth = readInt();
		_itemType = readInt();
		_type = readInt();
		_grade = readInt();
		_query = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!ItemCommissionManager.isPlayerAllowedToInteract(player))
		{
			player.sendPacket(ExCloseCommission.STATIC_PACKET);
			return;
		}
		
		Predicate<ItemTemplate> filter = _ -> true;
		switch (_treeViewDepth)
		{
			case 1:
			{
				final CommissionTreeType commissionTreeType = CommissionTreeType.findByClientId(_itemType);
				if (commissionTreeType != null)
				{
					filter = filter.and(i -> commissionTreeType.getCommissionItemTypes().contains(i.getCommissionItemType()));
				}
				break;
			}
			case 2:
			{
				final CommissionItemType commissionItemType = CommissionItemType.findByClientId(_itemType);
				if (commissionItemType != null)
				{
					filter = filter.and(i -> i.getCommissionItemType() == commissionItemType);
				}
				break;
			}
		}
		
		switch (_type)
		{
			case 0: // General
			{
				filter = filter.and(_ -> true); // TODO: condition
				break;
			}
			case 1: // Rare
			{
				filter = filter.and(_ -> true); // TODO: condition
				break;
			}
		}
		
		switch (_grade)
		{
			case 0:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.NONE);
				break;
			}
			case 1:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.D);
				break;
			}
			case 2:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.C);
				break;
			}
			case 3:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.B);
				break;
			}
			case 4:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.A);
				break;
			}
			case 5:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.S);
				break;
			}
			case 6:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.S80);
				break;
			}
			case 7:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.R);
				break;
			}
			case 8:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.R95);
				break;
			}
			case 9:
			{
				filter = filter.and(i -> i.getCrystalType() == CrystalType.R99);
				break;
			}
		}
		
		filter = filter.and(i -> _query.isEmpty() || i.getName().toLowerCase().contains(_query.toLowerCase()));
		ItemCommissionManager.getInstance().showAuctions(player, filter);
	}
}
