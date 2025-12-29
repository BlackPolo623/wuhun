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
package handlers.effecthandlers;

import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.data.xml.TimedHuntingZoneData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.huntingzones.TimeRestrictFieldUserAlarm;
import org.l2jmobius.gameserver.network.serverpackets.huntingzones.TimedHuntingZoneChargeResult;

/**
 * @author Mobius
 */
public class AddHuntingTime extends AbstractEffect
{
	private final int _zoneId;
	private final long _time;
	
	public AddHuntingTime(StatSet params)
	{
		_zoneId = params.getInt("zoneId", 0);
		_time = params.getLong("time", 3600000);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		final TimedHuntingZoneHolder holder = TimedHuntingZoneData.getInstance().getHuntingZone(_zoneId);
		if (holder == null)
		{
			return;
		}
		
		final long currentTime = System.currentTimeMillis();
		final long endTime = currentTime + player.getTimedHuntingZoneRemainingTime(_zoneId);
		if ((endTime > currentTime) && (((endTime - currentTime) + _time) > holder.getMaximumAddedTime()))
		{
			player.getInventory().addItem(ItemProcessType.REFUND, item.getId(), 1, player, player);
			player.sendMessage("You cannot exceed the time zone limit.");
			return;
		}
		
		final long remainRefill = player.getVariables().getInt(PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + _zoneId, holder.getRemainRefillTime());
		if ((_time < remainRefill) || (remainRefill == 0))
		{
			player.getInventory().addItem(ItemProcessType.REFUND, item.getId(), 1, player, player);
			player.sendMessage("You cannot exceed the time zone limit.");
			return;
		}
		
		final long remainTime = player.getVariables().getLong(PlayerVariables.HUNTING_ZONE_TIME + _zoneId, holder.getInitialTime());
		player.getVariables().set(PlayerVariables.HUNTING_ZONE_TIME + _zoneId, remainTime + _time);
		player.getVariables().set(PlayerVariables.HUNTING_ZONE_REMAIN_REFILL + _zoneId, remainRefill - (_time / 1000));
		player.sendPacket(new TimedHuntingZoneChargeResult(_zoneId, (int) ((remainTime + _time) / 1000), (int) (remainRefill - (_time / 1000)), (int) _time / 1000));
		
		if (player.isInTimedHuntingZone(_zoneId))
		{
			player.startTimedHuntingZone(_zoneId);
			player.sendPacket(new TimeRestrictFieldUserAlarm(player, _zoneId));
		}
	}
}
