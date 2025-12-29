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

import java.util.Collection;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Shadow;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * @author Liamxroy
 */
public class SummonShadow extends AbstractEffect
{
	private final int _despawnDelay;
	private final int _npcId;
	
	public SummonShadow(StatSet params)
	{
		_despawnDelay = params.getInt("despawnDelay", 10000);
		_npcId = params.getInt("npcId", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SUMMON_NPC;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isAlikeDead())
		{
			return;
		}
		
		if ((_npcId <= 0))
		{
			LOGGER.warning(SummonNpc.class.getSimpleName() + ": Invalid NPC ID or count skill ID: " + skill.getId());
			return;
		}
		
		final Player player = effector.asPlayer();
		if (player.isMounted())
		{
			return;
		}
		
		if (player.getSummonedNpcCount() >= 3)
		{
			return;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_npcId);
		if (npcTemplate == null)
		{
			LOGGER.warning(SummonNpc.class.getSimpleName() + ": Spawn of the nonexisting NPC ID: " + _npcId + ", skill ID:" + skill.getId());
			return;
		}
		
		// TODO: Probably move to shadows instead of summoned NPCs.
		final Collection<Npc> summonedShadows = player.getSummonedNpcs();
		if (!summonedShadows.isEmpty())
		{
			for (Npc npc : summonedShadows)
			{
				if (npc.getId() == _npcId)
				{
					npc.deleteMe();
					break;
				}
			}
		}
		
		final int x = player.getX();
		final int y = player.getY();
		final int z = player.getZ();
		final Shadow shadow = new Shadow(npcTemplate, player);
		shadow.fullRestore();
		shadow.setHeading(player.getHeading());
		shadow.spawnMe(x, y, z);
		shadow.setSummoner(player);
		shadow.setInstance(player.getInstanceWorld());
		shadow.scheduleDespawn(_despawnDelay);
		shadow.setShowSummonAnimation(true);
		player.addSummonedNpc(shadow);
	}
}
