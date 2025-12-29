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

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.AdminTeleportType;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author Mobius
 */
public class MoveToward extends ClientPacket
{
	private int _heading;
	
	@Override
	protected void readImpl()
	{
		_heading = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		if (!PlayerConfig.ENABLE_KEYBOARD_MOVEMENT)
		{
			return;
		}
		
		final Player player = getPlayer();
		if ((player == null) || player.isControlBlocked() || (player.getTeleMode() != AdminTeleportType.NORMAL))
		{
			return;
		}
		
		final double angle = LocationUtil.convertHeadingToDegree(_heading);
		final double radian = Math.toRadians(angle);
		final double course = Math.toRadians(180);
		final double frontDistance = player.getMoveSpeed();
		final int x1 = (int) (Math.cos(Math.PI + radian + course) * frontDistance);
		final int y1 = (int) (Math.sin(Math.PI + radian + course) * frontDistance);
		final int x = player.getX() + x1;
		final int y = player.getY() + y1;
		final Location destination = GeoEngine.getInstance().getValidLocation(player.getX(), player.getY(), player.getZ(), x, y, player.getZ(), player.getInstanceWorld());
		
		player.setCursorKeyMovement(true);
		player.setLastServerPosition(player.getX(), player.getY(), player.getZ());
		player.getAI().setIntention(Intention.MOVE_TO, destination);
		
		// Remove queued skill upon move request.
		if (player.getQueuedSkill() != null)
		{
			player.setQueuedSkill(null, null, false, false);
		}
		
		// Mobius: Check spawn protections.
		player.onActionRequest();
	}
}
