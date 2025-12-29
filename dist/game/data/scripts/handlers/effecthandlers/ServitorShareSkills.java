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

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.serverpackets.pet.ExPetSkillList;

/**
 * @author Geremy
 */
public class ServitorShareSkills extends AbstractEffect
{
	// For Powerful servitor share (45054).
	private static final int[] SERVITOR_SHARE_PASSIVE_SKILLS =
	{
		50189,
		50468,
		50190,
		50353,
		50446,
		50444,
		50555,
		50445,
		50449,
		50448,
		50447,
		50450
	};
	
	public ServitorShareSkills(StatSet params)
	{
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		if ((player.getClient() == null) || (player.getClient().getConnectionState() != ConnectionState.IN_GAME))
		{
			return;
		}
		
		if (!effected.hasServitors())
		{
			return;
		}
		
		final Collection<Summon> summons = player.getServitors().values();
		for (int passiveSkillId : SERVITOR_SHARE_PASSIVE_SKILLS)
		{
			final BuffInfo passiveSkillEffect = player.getEffectList().getBuffInfoBySkillId(passiveSkillId);
			if (passiveSkillEffect != null)
			{
				for (Summon summon : summons)
				{
					summon.addSkill(passiveSkillEffect.getSkill());
					summon.broadcastInfo();
					if (summon.isPet())
					{
						player.sendPacket(new ExPetSkillList(true, summon.asPet()));
					}
				}
			}
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		if (!effected.hasServitors())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final Collection<Summon> summons = player.getServitors().values();
		for (int passiveSkillId : SERVITOR_SHARE_PASSIVE_SKILLS)
		{
			for (Summon summon : summons)
			{
				final BuffInfo passiveSkillEffect = summon.getEffectList().getBuffInfoBySkillId(passiveSkillId);
				if (passiveSkillEffect != null)
				{
					summon.removeSkill(passiveSkillEffect.getSkill(), true);
					summon.broadcastInfo();
					if (summon.isPet())
					{
						player.sendPacket(new ExPetSkillList(true, summon.asPet()));
					}
				}
			}
		}
	}
}
