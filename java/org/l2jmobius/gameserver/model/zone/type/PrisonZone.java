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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.prison.PrisonManager;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * A simple no prison zone
 * @author Liamxroy
 */
public class PrisonZone extends ZoneType
{
	private static List<SkillHolder> effectsList = new ArrayList<>();
	private static Location entranceLoc;
	
	public PrisonZone(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		switch (name)
		{
			case "skillIdLvl":
			{
				effectsList = new ArrayList<>();
				if (!value.isEmpty())
				{
					for (String skill : value.split(";"))
					{
						effectsList.add(new SkillHolder(Integer.parseInt(skill.split(",")[0]), Integer.parseInt(skill.split(",")[1])));
					}
				}
				break;
			}
			case "defaultSpawnLoc":
			{
				entranceLoc = null;
				if (!value.isEmpty())
				{
					entranceLoc = new Location(Integer.parseInt(value.split(",")[0]), Integer.parseInt(value.split(",")[1]), Integer.parseInt(value.split(",")[2]));
				}
				break;
			}
			default:
			{
				super.setParameter(name, value);
			}
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.PRISON, true);
			creature.setInsideZone(ZoneId.NO_BOOKMARK, true);
			creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
			
			final Player player = creature.asPlayer();
			if (player.isPrisoner())
			{
				player.getPrisonerInfo().startSentenceTimer(player);
				applyEffects(player);
				
				Broadcast.toAllOnlinePlayersOnScreen(player.getName() + ", Underground Labyrinth is available.");
				Broadcast.toAllOnlinePlayers(new PlaySound("systemmsg_eu.17"));
			}
			else
			{
				player.teleToLocation(PrisonManager.getReleaseLoc(1));
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.PRISON, false);
			creature.setInsideZone(ZoneId.NO_BOOKMARK, false);
			creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
			
			final Player player = creature.asPlayer();
			if (player.isPrisoner())
			{
				player.getPrisonerInfo().stopSentenceTimer();
				if (player.isOnline())
				{
					if (entranceLoc != null)
					{
						player.teleToLocation(entranceLoc);
					}
					else
					{
						player.teleToLocation(-77371, -46372, -11499); // Underground Labyrinth
					}
				}
			}
			
			stopEffects(player);
		}
	}
	
	private void applyEffects(Player player)
	{
		if (!effectsList.isEmpty())
		{
			for (SkillHolder skillH : effectsList)
			{
				skillH.getSkill().activateSkill(player, player);
			}
		}
	}
	
	private void stopEffects(Player player)
	{
		if (!effectsList.isEmpty())
		{
			for (SkillHolder skillH : effectsList)
			{
				player.stopSkillEffects(null, skillH.getSkillId());
			}
		}
	}
}
