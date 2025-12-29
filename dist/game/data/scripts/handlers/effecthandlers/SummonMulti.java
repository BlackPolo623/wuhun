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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.creature.Race;
import org.l2jmobius.gameserver.model.actor.instance.Servitor;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.Skill;

/**
 * SummonMulti effect implementation.
 * @author UnAfraid, Mobius
 */
public class SummonMulti extends AbstractEffect
{
	private final int _npcId;
	private final Map<Integer, Integer> _levelTemplates;
	private final float _expMultiplier;
	private final ItemHolder _consumeItem;
	private final int _lifeTime;
	private final int _consumeItemInterval;
	private final int _summonPoints;
	
	public SummonMulti(StatSet params)
	{
		_npcId = params.getInt("npcId", 0);
		if (_npcId > 0)
		{
			_levelTemplates = null;
		}
		else
		{
			final List<Integer> summonerLevels = params.getIntegerList("summonerLevels");
			final List<Integer> npcIds = params.getIntegerList("npcIds");
			_levelTemplates = new LinkedHashMap<>(npcIds.size());
			for (int i = 0; i < npcIds.size(); i++)
			{
				_levelTemplates.put(summonerLevels.get(i), npcIds.get(i));
			}
		}
		
		_expMultiplier = params.getFloat("expMultiplier", 1);
		_consumeItem = new ItemHolder(params.getInt("consumeItemId", 0), params.getInt("consumeItemCount", 1));
		_consumeItemInterval = params.getInt("consumeItemInterval", 0);
		_lifeTime = params.getInt("lifeTime", 3600) > 0 ? params.getInt("lifeTime", 3600) * 1000 : -1;
		_summonPoints = params.getInt("summonPoints", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SUMMON;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		if ((player.getSummonPoints() + _summonPoints) > player.getMaxSummonPoints())
		{
			return;
		}
		
		final NpcTemplate template;
		if (_npcId > 0)
		{
			template = NpcData.getInstance().getTemplate(_npcId);
		}
		else
		{
			Entry<Integer, Integer> levelTemplate = null;
			for (Entry<Integer, Integer> entry : _levelTemplates.entrySet())
			{
				if ((levelTemplate == null) || (player.getLevel() >= entry.getKey()))
				{
					levelTemplate = entry;
				}
			}
			
			if (levelTemplate != null)
			{
				template = NpcData.getInstance().getTemplate(levelTemplate.getValue());
			}
			else // Should never happen.
			{
				template = NpcData.getInstance().getTemplate(_levelTemplates.keySet().stream().findFirst().get());
			}
		}
		
		final Servitor summon = new Servitor(template, player);
		final int consumeItemInterval = (_consumeItemInterval > 0 ? _consumeItemInterval : (template.getRace() != Race.SIEGE_WEAPON ? 240 : 60)) * 1000;
		
		summon.setName(template.getName());
		summon.setTitle(effected.getName());
		summon.setReferenceSkill(skill.getId());
		summon.setExpMultiplier(_expMultiplier);
		summon.setLifeTime(_lifeTime);
		summon.setItemConsume(_consumeItem);
		summon.setItemConsumeInterval(consumeItemInterval);
		
		final int maxPetLevel = ExperienceData.getInstance().getMaxPetLevel();
		if (summon.getLevel() >= maxPetLevel)
		{
			summon.getStat().setExp(ExperienceData.getInstance().getExpForLevel(maxPetLevel - 1));
		}
		else
		{
			summon.getStat().setExp(ExperienceData.getInstance().getExpForLevel(summon.getLevel() % maxPetLevel));
		}
		
		// Summons must have their master buffs upon spawn.
		for (BuffInfo effect : player.getEffectList().getEffects())
		{
			final Skill sk = effect.getSkill();
			if (!sk.hasNegativeEffect() && !sk.isTransformation() && sk.isSharedWithSummon())
			{
				sk.applyEffects(player, summon, false, effect.getTime());
			}
		}
		
		summon.fullRestore();
		summon.setHeading(player.getHeading());
		summon.setSummonPoints(_summonPoints);
		player.addServitor(summon);
		summon.setShowSummonAnimation(true);
		summon.spawnMe();
		summon.setRunning();
	}
}
