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
package ai.others.Deton;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * Deton teleport AI.
 * @author Tanatos
 */
public class Deton extends Script
{
	// NPC
	private static final int DETON = 34143;
	
	// Location
	private static final Location MASSACRE_SOUTH = new Location(179446, -7811, -3528);
	private static final Location MASSACRE_NORTH = new Location(188337, -25395, -1472);
	private static final Location MASSACRE_WEST = new Location(163430, -10822, -3520);
	private static final Location PLAINS_SOUTH = new Location(131435, 24184, -3728);
	private static final Location PLAINS_NORTH = new Location(139103, -575, -4240);
	private static final Location PLAINS_WEST = new Location(132390, 12644, -4040);
	private static final Location WARTORN_SOUTH = new Location(162516, 27401, -3712);
	private static final Location WARTORN_NORTH = new Location(164262, 1849, -3480);
	private static final Location WARTORN_WEST = new Location(153025, 10061, -3928);
	
	private Deton()
	{
		addStartNpc(DETON);
		addFirstTalkId(DETON);
		addTalkId(DETON);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "toMassacreSouth":
			{
				if (npc.getId() != DETON)
				{
					break;
				}
				
				player.teleToLocation(MASSACRE_SOUTH);
				break;
			}
			case "toMassacreNorth":
			{
				if (npc.getId() != DETON)
				{
					break;
				}
				
				player.teleToLocation(MASSACRE_NORTH);
				break;
			}
			case "toMassacreWest":
			{
				if (npc.getId() != DETON)
				{
					break;
				}
				
				player.teleToLocation(MASSACRE_WEST);
				break;
			}
			case "toPlainsSouth":
			{
				if (npc.getId() != DETON)
				{
					break;
				}
				
				player.teleToLocation(PLAINS_SOUTH);
				break;
			}
			case "toPlainsNorth":
			{
				if (npc.getId() != DETON)
				{
					break;
				}
				
				player.teleToLocation(PLAINS_NORTH);
				break;
			}
			case "toPlainsWest":
			{
				if (npc.getId() != DETON)
				{
					break;
				}
				
				player.teleToLocation(PLAINS_WEST);
				break;
			}
			case "toWartornSouth":
			{
				if (npc.getId() != DETON)
				{
					break;
				}
				
				player.teleToLocation(WARTORN_SOUTH);
				break;
			}
			case "toWartornNorth":
			{
				if (npc.getId() != DETON)
				{
					break;
				}
				
				player.teleToLocation(WARTORN_NORTH);
				break;
			}
			case "toWartornWest":
			{
				if (npc.getId() != DETON)
				{
					break;
				}
				
				player.teleToLocation(WARTORN_WEST);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.isInsideRadius2D(183568, -16014, 0, Npc.INTERACTION_DISTANCE)) // Field of Massacre
		{
			return "34143-01.htm";
		}
		
		if (npc.isInsideRadius2D(138736, 19622, 0, Npc.INTERACTION_DISTANCE)) // Plains of Glory
		{
			return "34143-02.htm";
		}
		
		if (npc.isInsideRadius2D(159860, 20769, 0, Npc.INTERACTION_DISTANCE)) // War-Torn Plains
		{
			return "34143-03.htm";
		}
		
		return super.onFirstTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new Deton();
	}
}
