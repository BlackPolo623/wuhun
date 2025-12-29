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
package ai.bosses.SevenSignsRBs;

import java.util.List;

import org.l2jmobius.gameserver.managers.DatabaseSpawnManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.npc.RaidBossStatus;
import org.l2jmobius.gameserver.model.groups.Party;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.zone.type.NoRestartZone;

/**
 * @author RobikBobik
 * @NOTE: Retail like work
 */
public class SevenSignsRBs extends Script
{
	// NPCs
	private static final int ANAKIM_GATEKEEPER_SPIRIT = 31089;
	private static final int LILITH_GATEKEEPER_SPIRIT = 31087;
	private static final int GATEKEEPER_SPIRIT_OUT_TELEPORT = 31088;
	private static final int ANAKIM = 25286;
	private static final int LILITH = 25283;
	
	// Misc
	private static final int MAX_PLAYERS_IN_ZONE = 300;
	private static final NoRestartZone ANAKIM_ZONE = ZoneManager.getInstance().getZoneById(70052, NoRestartZone.class);
	private static final NoRestartZone LILITH_ZONE = ZoneManager.getInstance().getZoneById(70053, NoRestartZone.class);
	
	// TELEPORTS
	private static final Location TELEPORT_TO_LILITH = new Location(185657, -10112, -5496);
	private static final Location TELEPORT_TO_ANAKIM = new Location(-7283, 19086, -5496);
	
	private static final Location[] TELEPORT_TO_DARK_ELVEN =
	{
		new Location(12168, 17149, -4575),
		new Location(11688, 18219, -4585),
		new Location(10502, 17112, -4588),
		new Location(11169, 15922, -4585),
	};
	
	private static final Location[] TELEPORT_TO_ADEN =
	{
		new Location(148053, 26935, -2206),
		new Location(148053, 28017, -2269),
		new Location(146558, 28017, -2269),
		new Location(146558, 26935, -2206),
	};
	
	public SevenSignsRBs()
	{
		addStartNpc(ANAKIM_GATEKEEPER_SPIRIT, LILITH_GATEKEEPER_SPIRIT);
		addTalkId(ANAKIM_GATEKEEPER_SPIRIT, LILITH_GATEKEEPER_SPIRIT, GATEKEEPER_SPIRIT_OUT_TELEPORT);
		addKillId(ANAKIM, LILITH);
		addAttackId(ANAKIM, LILITH);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "ANAKIM_ENTER":
			{
				if (DatabaseSpawnManager.getInstance().getStatus(ANAKIM) != RaidBossStatus.ALIVE)
				{
					player.sendMessage("Anakim is not present at the moment");
					break;
				}
				
				if (player.isInParty())
				{
					final Party party = player.getParty();
					final boolean isInCC = party.isInCommandChannel();
					final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
					if (members.size() > (MAX_PLAYERS_IN_ZONE - ANAKIM_ZONE.getPlayersInside().size()))
					{
						player.sendMessage("Anakims Sanctum reached " + MAX_PLAYERS_IN_ZONE + " players. You cannot enter now.");
					}
					else
					{
						for (Player member : members)
						{
							if (!member.isInsideRadius3D(npc, 1000))
							{
								player.sendMessage("Player " + member.getName() + " must go closer to Gatekeeper Spirit.");
								break;
							}
							
							member.teleToLocation(TELEPORT_TO_ANAKIM);
						}
					}
				}
				else if (player.isGM())
				{
					player.teleToLocation(TELEPORT_TO_ANAKIM);
					player.sendMessage("SYS: You have entered as GM/Admin to Anakim Instance");
				}
				else
				{
					if (!player.isInsideRadius3D(npc, 1000))
					{
						player.sendMessage("You must go closer to Gatekeeper Spirit.");
						break;
					}
					
					player.teleToLocation(TELEPORT_TO_ANAKIM);
				}
				break;
			}
			case "LILITH_ENTER":
			{
				if (DatabaseSpawnManager.getInstance().getStatus(LILITH) != RaidBossStatus.ALIVE)
				{
					player.sendMessage("Lilith is not present at the moment");
					break;
				}
				
				if (player.isInParty())
				{
					final Party party = player.getParty();
					final boolean isInCC = party.isInCommandChannel();
					final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
					if (members.size() > (MAX_PLAYERS_IN_ZONE - LILITH_ZONE.getPlayersInside().size()))
					{
						player.sendMessage("Lilith Sanctum reached " + MAX_PLAYERS_IN_ZONE + " players. You cannot enter now.");
					}
					else
					{
						for (Player member : members)
						{
							if (!member.isInsideRadius3D(npc, 1000))
							{
								player.sendMessage("Player " + member.getName() + " must go closer to Gatekeeper Spirit.");
								break;
							}
							
							member.teleToLocation(TELEPORT_TO_LILITH);
						}
					}
				}
				else if (player.isGM())
				{
					player.teleToLocation(TELEPORT_TO_LILITH);
					player.sendMessage("SYS: You have entered as GM/Admin to Anakim Instance");
				}
				else
				{
					if (!player.isInsideRadius3D(npc, 1000))
					{
						player.sendMessage("You must go closer to Gatekeeper Spirit.");
						break;
					}
					
					player.teleToLocation(TELEPORT_TO_LILITH);
				}
				break;
			}
			case "REMOVE_PLAYERS_FROM_ZONE_ANAKIM":
			{
				for (Creature charInside : ANAKIM_ZONE.getCharactersInside())
				{
					if ((charInside != null) && charInside.isPlayer())
					{
						charInside.teleToLocation(-20185 + getRandom(50), 13476 + getRandom(50), -4901);
					}
				}
				break;
			}
			case "REMOVE_PLAYERS_FROM_ZONE_LILITH":
			{
				for (Creature charInside : LILITH_ZONE.getCharactersInside())
				{
					if ((charInside != null) && charInside.isPlayer())
					{
						charInside.teleToLocation(171346 + getRandom(50), -17599 + getRandom(50), -4901);
					}
				}
				break;
			}
			case "TELEPORT_OUT":
			{
				if (ANAKIM_ZONE.isInsideZone(player.getLocation()))
				{
					final Location destination = TELEPORT_TO_DARK_ELVEN[getRandom(TELEPORT_TO_DARK_ELVEN.length)];
					player.teleToLocation(destination.getX() + getRandom(100), destination.getY() + getRandom(100), destination.getZ());
					break;
				}
				
				if (LILITH_ZONE.isInsideZone(player.getLocation()))
				{
					final Location destination = TELEPORT_TO_ADEN[getRandom(TELEPORT_TO_ADEN.length)];
					player.teleToLocation(destination.getX() + getRandom(100), destination.getY() + getRandom(100), destination.getZ());
					break;
				}
				break;
			}
			case "ANAKIM_DEATH_CAST_LILITH_INVUL":
			{
				if (DatabaseSpawnManager.getInstance().getStatus(LILITH) == RaidBossStatus.ALIVE)
				{
					Npc LILITH_NPC = DatabaseSpawnManager.getInstance().getNpcs().get(LILITH);
					LILITH_NPC.setInvul(true);
					LILITH_NPC.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.INVINCIBILITY);
					startQuestTimer("LILITH_INVUL_END", 300000, null, player);
				}
				
				break;
			}
			case "LILITH_DEATH_CAST_ANAKIM_INVUL":
			{
				if (DatabaseSpawnManager.getInstance().getStatus(ANAKIM) == RaidBossStatus.ALIVE)
				{
					Npc ANAKIM_NPC = DatabaseSpawnManager.getInstance().getNpcs().get(ANAKIM);
					ANAKIM_NPC.setInvul(true);
					ANAKIM_NPC.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.INVINCIBILITY);
					startQuestTimer("ANAKIM_INVUL_END", 300000, null, player);
				}
				break;
			}
			case "LILITH_INVUL_END":
			{
				Npc LILITH_NPC = DatabaseSpawnManager.getInstance().getNpcs().get(LILITH);
				LILITH_NPC.setInvul(false);
				LILITH_NPC.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.INVINCIBILITY);
				break;
			}
			case "ANAKIM_INVUL_END":
			{
				Npc ANAKIM_NPC = DatabaseSpawnManager.getInstance().getNpcs().get(ANAKIM);
				ANAKIM_NPC.setInvul(false);
				ANAKIM_NPC.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.INVINCIBILITY);
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
			case ANAKIM:
			{
				startQuestTimer("ANAKIM_DEATH_CAST_LILITH_INVUL", 1000, null, null);
				addSpawn(GATEKEEPER_SPIRIT_OUT_TELEPORT, -6664, 18501, -5495, 0, false, 600000, false, npc.getInstanceId());
				break;
			}
			case LILITH:
			{
				startQuestTimer("LILITH_DEATH_CAST_ANAKIM_INVUL", 1000, null, null);
				addSpawn(GATEKEEPER_SPIRIT_OUT_TELEPORT, 185062, -9612, -5493, 0, false, 600000, false, npc.getInstanceId());
				break;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new SevenSignsRBs();
	}
}
