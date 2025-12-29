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
package ai.areas.Hellbound.CitadelTeleport;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author MacuK
 */
public class CitadelTeleport extends Script
{
	// NPCs
	private static final int TELEPORT = 34244;
	private static final int CAPTURE_DEVICE = 34245;
	
	// Locations
	private static final Location[] LOCATIONS =
	{
		new Location(-10717, 282642, -13824),
		new Location(-12071, 272965, -9032),
		new Location(-12454, 284135, -15872),
		new Location(-12714, 272944, -12104),
		new Location(-12828, 284197, -9728),
		new Location(-14219, 282404, -11776),
		new Location(-21343, 282630, -15872),
		new Location(-21368, 282680, -9728),
		new Location(-21376, 282658, -11776),
		new Location(-21405, 282671, -13824),
		new Location(16679, 244326, 10496),
	};
	
	public CitadelTeleport()
	{
		addStartNpc(TELEPORT, CAPTURE_DEVICE);
		addTalkId(TELEPORT, CAPTURE_DEVICE);
		addFirstTalkId(TELEPORT, CAPTURE_DEVICE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("teleport"))
		{
			player.teleToLocation(getRandomEntry(LOCATIONS));
			return null;
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	public static void main(String[] args)
	{
		new CitadelTeleport();
	}
}
