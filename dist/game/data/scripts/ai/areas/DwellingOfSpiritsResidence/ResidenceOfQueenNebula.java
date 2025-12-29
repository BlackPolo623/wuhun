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
package ai.areas.DwellingOfSpiritsResidence;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.AbnormalVisualEffect;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author RobikBobik
 */
public class ResidenceOfQueenNebula extends Script
{
	// NPCs
	private static final int NEBULA = 29106;
	private static final int WATER_SLIME = 29111;
	
	// Skills
	private static final int AQUA_RAGE = 50036;
	private static final SkillHolder AQUA_RAGE_1 = new SkillHolder(AQUA_RAGE, 1);
	private static final SkillHolder AQUA_RAGE_2 = new SkillHolder(AQUA_RAGE, 2);
	private static final SkillHolder AQUA_RAGE_3 = new SkillHolder(AQUA_RAGE, 3);
	private static final SkillHolder AQUA_RAGE_4 = new SkillHolder(AQUA_RAGE, 4);
	private static final SkillHolder AQUA_RAGE_5 = new SkillHolder(AQUA_RAGE, 5);
	private static final SkillHolder AQUA_SUMMON = new SkillHolder(50037, 1);
	
	public ResidenceOfQueenNebula()
	{
		addKillId(NEBULA, WATER_SLIME);
		addAttackId(NEBULA);
		addSpawnId(NEBULA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "SPAWN_WATER_SLIME":
			{
				final Instance world = npc.getInstanceWorld();
				if (world != null)
				{
					final Player plr = world.getPlayers().stream().findAny().get();
					startQuestTimer("CAST_AQUA_RAGE", 60000 + getRandom(-15000, 15000), npc, plr);
					if (npc.getId() == NEBULA)
					{
						npc.doCast(AQUA_SUMMON.getSkill());
						for (int i = 0; i < getRandom(4, 6); i++)
						{
							addSpawn(npc, WATER_SLIME, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), true, -1, true, npc.getInstanceId());
							startQuestTimer("SPAWN_WATER_SLIME", 300000, npc, null);
						}
					}
				}
				break;
			}
			case "PLAYER_PARA":
			{
				if (player.getAffectedSkillLevel(AQUA_RAGE) == 5)
				{
					player.getEffectList().startAbnormalVisualEffect(AbnormalVisualEffect.FROZEN_PILLAR);
					player.setImmobilized(true);
					startQuestTimer("PLAYER_UNPARA", 5000, npc, player);
				}
				break;
			}
			case "PLAYER_UNPARA":
			{
				player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, AQUA_RAGE_5.getSkill());
				player.getEffectList().stopAbnormalVisualEffect(AbnormalVisualEffect.FROZEN_PILLAR);
				player.setImmobilized(false);
				break;
			}
			case "CAST_AQUA_RAGE":
			{
				startQuestTimer("CAST_AQUA_RAGE", 5000, npc, player);
				if ((player.isInsideRadius3D(npc, 1000)))
				{
					if (player.getAffectedSkillLevel(AQUA_RAGE) == 1)
					{
						if (SkillCaster.checkUseConditions(npc, AQUA_RAGE_2.getSkill()))
						{
							npc.doCast(AQUA_RAGE_2.getSkill());
						}
					}
					else if (player.getAffectedSkillLevel(AQUA_RAGE) == 2)
					{
						if (SkillCaster.checkUseConditions(npc, AQUA_RAGE_3.getSkill()))
						{
							npc.doCast(AQUA_RAGE_3.getSkill());
						}
					}
					else if (player.getAffectedSkillLevel(AQUA_RAGE) == 3)
					{
						if (SkillCaster.checkUseConditions(npc, AQUA_RAGE_4.getSkill()))
						{
							npc.doCast(AQUA_RAGE_4.getSkill());
						}
					}
					else if (player.getAffectedSkillLevel(AQUA_RAGE) == 4)
					{
						if (SkillCaster.checkUseConditions(npc, AQUA_RAGE_5.getSkill()))
						{
							npc.doCast(AQUA_RAGE_5.getSkill());
							startQuestTimer("PLAYER_PARA", 100, npc, player);
						}
					}
					else if (player.getAffectedSkillLevel(AQUA_RAGE) == 5)
					{
						npc.abortCast();
					}
					else if (SkillCaster.checkUseConditions(npc, AQUA_RAGE_1.getSkill()))
					{
						npc.doCast(AQUA_RAGE_1.getSkill());
					}
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("SPAWN_WATER_SLIME", 300000, npc, null);
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		switch (npc.getId())
		{
			case NEBULA:
			{
				cancelQuestTimer("CAST_AQUA_RAGE", npc, player);
				cancelQuestTimer("SPAWN_WATER_SLIME", npc, player);
				break;
			}
			case WATER_SLIME:
			{
				if (player.getAffectedSkillLevel(AQUA_RAGE) == 1)
				{
					if (getRandomBoolean())
					{
						player.stopSkillEffects(AQUA_RAGE_1.getSkill());
					}
				}
				else if (player.getAffectedSkillLevel(AQUA_RAGE) == 2)
				{
					if (getRandomBoolean())
					{
						player.stopSkillEffects(AQUA_RAGE_2.getSkill());
						final Skill skill = SkillData.getInstance().getSkill(AQUA_RAGE, 1);
						skill.applyEffects(player, player);
					}
				}
				else if (player.getAffectedSkillLevel(AQUA_RAGE) == 3)
				{
					if (getRandomBoolean())
					{
						player.stopSkillEffects(AQUA_RAGE_3.getSkill());
						final Skill skill = SkillData.getInstance().getSkill(AQUA_RAGE, 2);
						skill.applyEffects(player, player);
					}
				}
				else if (player.getAffectedSkillLevel(AQUA_RAGE) == 4)
				{
					if (getRandomBoolean())
					{
						player.stopSkillEffects(AQUA_RAGE_4.getSkill());
						final Skill skill = SkillData.getInstance().getSkill(AQUA_RAGE, 3);
						skill.applyEffects(player, player);
					}
				}
				break;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new ResidenceOfQueenNebula();
	}
}
