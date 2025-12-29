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
package instances.GolbergRoom;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * @author RobikBobik, Mobius
 * @NOTE: Party instance retail like work.
 * @TODO: Golberg skills
 */
public class GolbergRoom extends InstanceScript
{
	// NPCs
	private static final int SORA = 34091;
	private static final int GOLBERG = 18359;
	private static final int GOLBERG_TREASURE_CHEST = 18357;
	
	// Items
	private static final int GOLBERG_KEY_ROOM = 91636;
	
	// Misc
	private static final int TEMPLATE_ID = 207;
	
	public GolbergRoom()
	{
		super(TEMPLATE_ID);
		addStartNpc(SORA);
		addKillId(GOLBERG, GOLBERG_TREASURE_CHEST);
		addInstanceLeaveId(TEMPLATE_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "ENTER":
			{
				final Party party = player.getParty();
				if (party == null)
				{
					return "no_party.htm";
				}
				
				if (!hasQuestItems(player, GOLBERG_KEY_ROOM))
				{
					return "no_item.htm";
				}
				
				takeItems(player, GOLBERG_KEY_ROOM, 1);
				enterInstance(player, npc, TEMPLATE_ID);
				final Instance world = player.getInstanceWorld();
				if (world != null)
				{
					for (Player member : party.getMembers())
					{
						if (member == player)
						{
							continue;
						}
						
						member.teleToLocation(player, 10, world);
					}
					
					startQuestTimer("GOLBERG_MOVE", 5000, world.getNpc(GOLBERG), player);
				}
				break;
			}
			case "GOLBERG_MOVE":
			{
				final Instance world = player.getInstanceWorld();
				if (world != null)
				{
					player.sendPacket(new ExShowScreenMessage("Rats have become kings while I've been dormant.", 5000));
					startQuestTimer("NEXT_TEXT", 7000, world.getNpc(GOLBERG), player);
				}
				
				npc.moveToLocation(11711, -86508, -10928, 0);
				break;
			}
			case "NEXT_TEXT":
			{
				final Instance world = player.getInstanceWorld();
				if (world != null)
				{
					player.sendPacket(new ExShowScreenMessage("Zaken or whatever is going wild all over the southern sea.", 5000));
					startQuestTimer("NEXT_TEXT_2", 7000, world.getNpc(GOLBERG), player);
				}
				break;
			}
			case "NEXT_TEXT_2":
			{
				final Instance world = player.getInstanceWorld();
				if (world != null)
				{
					player.sendPacket(new ExShowScreenMessage("Who dare enter my place? Zaken sent you?", 5000));
				}
				break;
			}
			case "SPAWN_TRESURE":
			{
				final Instance world = player.getInstanceWorld();
				if (world == null)
				{
					return null;
				}
				
				if (world.getParameters().getInt("treasureCounter", 0) == 0)
				{
					world.getParameters().set("treasureCounter", 0);
				}
				
				if (player.isGM())
				{
					if (world.getParameters().getInt("treasureCounter", 0) <= 27)
					{
						addSpawn(GOLBERG_TREASURE_CHEST, 11708 + getRandom(-1000, 1000), -86505 + getRandom(-1000, 1000), -10928, 0, true, -1, true, player.getInstanceId());
						startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
						world.getParameters().increaseInt("treasureCounter", 1);
					}
				}
				else if (player.getParty() != null)
				{
					switch (player.getParty().getMemberCount())
					{
						case 2:
						{
							if (world.getParameters().getInt("treasureCounter", 0) <= 1)
							{
								addSpawn(GOLBERG_TREASURE_CHEST, 11708 + getRandom(-1000, 1000), -86505 + getRandom(-1000, 1000), -10928, 0, true, -1, true, player.getInstanceId());
								startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
								world.getParameters().increaseInt("treasureCounter", 1);
							}
							break;
						}
						case 3:
						{
							if (world.getParameters().getInt("treasureCounter", 0) <= 2)
							{
								addSpawn(GOLBERG_TREASURE_CHEST, 11708 + getRandom(-1000, 1000), -86505 + getRandom(-1000, 1000), -10928, 0, true, -1, true, player.getInstanceId());
								startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
								world.getParameters().increaseInt("treasureCounter", 1);
							}
							break;
						}
						case 4:
						{
							if (world.getParameters().getInt("treasureCounter", 0) <= 4)
							{
								addSpawn(GOLBERG_TREASURE_CHEST, 11708 + getRandom(-1000, 1000), -86505 + getRandom(-1000, 1000), -10928, 0, true, -1, true, player.getInstanceId());
								startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
								world.getParameters().increaseInt("treasureCounter", 1);
							}
							break;
						}
						case 5:
						{
							if (world.getParameters().getInt("treasureCounter", 0) <= 7)
							{
								addSpawn(GOLBERG_TREASURE_CHEST, 11708 + getRandom(-1000, 1000), -86505 + getRandom(-1000, 1000), -10928, 0, true, -1, true, player.getInstanceId());
								startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
								world.getParameters().increaseInt("treasureCounter", 1);
							}
							break;
						}
						case 6:
						{
							if (world.getParameters().getInt("treasureCounter", 0) <= 10)
							{
								addSpawn(GOLBERG_TREASURE_CHEST, 11708 + getRandom(-1000, 1000), -86505 + getRandom(-1000, 1000), -10928, 0, true, -1, true, player.getInstanceId());
								startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
								world.getParameters().increaseInt("treasureCounter", 1);
							}
							break;
						}
						case 7:
						{
							if (world.getParameters().getInt("treasureCounter", 0) <= 13)
							{
								addSpawn(GOLBERG_TREASURE_CHEST, 11708 + getRandom(-1000, 1000), -86505 + getRandom(-1000, 1000), -10928, 0, true, -1, true, player.getInstanceId());
								startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
								world.getParameters().increaseInt("treasureCounter", 1);
							}
							break;
						}
						case 8:
						{
							if (world.getParameters().getInt("treasureCounter", 0) <= 16)
							{
								addSpawn(GOLBERG_TREASURE_CHEST, 11708 + getRandom(-1000, 1000), -86505 + getRandom(-1000, 1000), -10928, 0, true, -1, true, player.getInstanceId());
								startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
								world.getParameters().increaseInt("treasureCounter", 1);
							}
							break;
						}
						case 9:
						{
							if (world.getParameters().getInt("treasureCounter", 0) <= 27)
							{
								addSpawn(GOLBERG_TREASURE_CHEST, 11708 + getRandom(-1000, 1000), -86505 + getRandom(-1000, 1000), -10928, 0, true, -1, true, player.getInstanceId());
								startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
								world.getParameters().increaseInt("treasureCounter", 1);
							}
							break;
						}
					}
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		switch (npc.getId())
		{
			case GOLBERG:
			{
				startQuestTimer("SPAWN_TRESURE", 1000, npc, player);
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					world.finishInstance();
				}
				break;
			}
			case GOLBERG_TREASURE_CHEST:
			{
				break;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new GolbergRoom();
	}
}
