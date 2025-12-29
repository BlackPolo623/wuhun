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
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.xml.TimedHuntingZoneData;
import org.l2jmobius.gameserver.managers.MapRegionManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * @author Mobius
 */
public class TimedHuntingZone extends ZoneType
{
	public TimedHuntingZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (!creature.isPlayer())
		{
			return;
		}
		
		final Player player = creature.asPlayer();
		if (player != null)
		{
			player.setInsideZone(ZoneId.TIMED_HUNTING, true);
			
			for (TimedHuntingZoneHolder holder : TimedHuntingZoneData.getInstance().getAllHuntingZones())
			{
				if (!player.isInTimedHuntingZone(holder.getZoneId()))
				{
					continue;
				}
				
				final int remainingTime = player.getTimedHuntingZoneRemainingTime(holder.getZoneId());
				if (remainingTime > 0)
				{
					player.startTimedHuntingZone(holder.getZoneId());
					if (holder.isPvpZone())
					{
						if (!player.isInsideZone(ZoneId.PVP))
						{
							player.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
						}
						
						player.setInsideZone(ZoneId.PVP, true);
						if (player.hasServitors())
						{
							player.getServitors().values().forEach(s -> s.setInsideZone(ZoneId.PVP, true));
						}
						
						if (player.hasPet())
						{
							player.getPet().setInsideZone(ZoneId.PVP, true);
						}
					}
					else if (holder.isNoPvpZone())
					{
						player.setInsideZone(ZoneId.NO_PVP, true);
						if (player.hasServitors())
						{
							player.getServitors().values().forEach(s -> s.setInsideZone(ZoneId.NO_PVP, true));
						}
						
						if (player.hasPet())
						{
							player.getPet().setInsideZone(ZoneId.NO_PVP, true);
						}
					}
					
					// Send player info to nearby players.
					if (!player.isTeleporting())
					{
						player.broadcastInfo();
					}
					return;
				}
				break;
			}
			
			if (!player.isGM())
			{
				player.teleToLocation(MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN));
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (!creature.isPlayer())
		{
			return;
		}
		
		final Player player = creature.asPlayer();
		if (player != null)
		{
			player.setInsideZone(ZoneId.TIMED_HUNTING, false);
			
			final TimedHuntingZoneHolder holder = player.getTimedHuntingZone();
			if (holder != null)
			{
				if (holder.isPvpZone())
				{
					player.setInsideZone(ZoneId.PVP, false);
					if (player.hasServitors())
					{
						player.getServitors().values().forEach(s -> s.setInsideZone(ZoneId.PVP, false));
					}
					
					if (player.hasPet())
					{
						player.getPet().setInsideZone(ZoneId.PVP, false);
					}
					
					if (!player.isInsideZone(ZoneId.PVP))
					{
						creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
					}
				}
				else if (holder.isNoPvpZone())
				{
					player.setInsideZone(ZoneId.NO_PVP, false);
					if (player.hasServitors())
					{
						player.getServitors().values().forEach(s -> s.setInsideZone(ZoneId.NO_PVP, false));
					}
					
					if (player.hasPet())
					{
						player.getPet().setInsideZone(ZoneId.NO_PVP, false);
					}
				}
				
				// Send player info to nearby players.
				if (!player.isTeleporting())
				{
					player.broadcastInfo();
				}
			}
		}
	}
}
