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
package ai.others;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.serverpackets.ranking.ExRankingBuffZoneNpcInfo;

/**
 * @author Mobius
 */
public class RankingSkillBonuses extends Script
{
	// Skills
	private static final Skill SERVER_LEVEL_RANKING_1ST_CLASS = SkillData.getInstance().getSkill(60003, 1);
	private static final Skill SERVER_LEVEL_RANKING_2ND_CLASS = SkillData.getInstance().getSkill(60004, 1);
	private static final Skill SERVER_LEVEL_RANKING_3RD_CLASS = SkillData.getInstance().getSkill(60005, 1);
	private static final Skill HUMAN_LEVEL_RANKING_1ST_CLASS = SkillData.getInstance().getSkill(60006, 1);
	private static final Skill ELF_LEVEL_RANKING_1ST_CLASS = SkillData.getInstance().getSkill(60007, 1);
	private static final Skill DARK_ELF_LEVEL_RANKING_1ST_CLASS = SkillData.getInstance().getSkill(60008, 1);
	private static final Skill ORC_LEVEL_RANKING_1ST_CLASS = SkillData.getInstance().getSkill(60009, 1);
	private static final Skill DWARF_LEVEL_RANKING_1ST_CLASS = SkillData.getInstance().getSkill(60010, 1);
	private static final Skill KAMAEL_LEVEL_RANKING_1ST_CLASS = SkillData.getInstance().getSkill(60011, 1);
	private static final Skill SYLPH_LEVEL_RANKING_1ST_CLASS = SkillData.getInstance().getSkill(46033, 1);
	private static final Skill HIGH_ELF_LEVEL_RANKING_1ST_CLASS = SkillData.getInstance().getSkill(46034, 1);
	private static final Skill SERVER_RANKING_BENEFIT_1 = SkillData.getInstance().getSkill(60012, 1);
	private static final Skill SERVER_RANKING_BENEFIT_2 = SkillData.getInstance().getSkill(60013, 1);
	private static final Skill SERVER_RANKING_BENEFIT_3 = SkillData.getInstance().getSkill(60014, 1);
	private static final Skill RACE_RANKING_BENEFIT = SkillData.getInstance().getSkill(60015, 1);
	private static final Skill HUMAN_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54204, 1);
	private static final Skill ELF_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54210, 1);
	private static final Skill DARK_ELF_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54211, 1);
	private static final Skill ORC_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54209, 1);
	private static final Skill DWARF_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54212, 1);
	private static final Skill KAMAEL_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54205, 1);
	private static final Skill DEATH_KNIGHT_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54208, 1);
	private static final Skill SYLPH_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54226, 1);
	private static final Skill VANGUARD_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54239, 1);
	private static final Skill ASSASSIN_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54242, 1);
	private static final Skill HIGH_ELF_MALE_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54254, 1);
	private static final Skill HIGH_ELF_FEMALE_LEVEL_TRANSFORM_CLASS = SkillData.getInstance().getSkill(54255, 1);
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Remove existing effects and skills.
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, SERVER_LEVEL_RANKING_1ST_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, SERVER_LEVEL_RANKING_2ND_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, SERVER_LEVEL_RANKING_3RD_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, HUMAN_LEVEL_RANKING_1ST_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, ELF_LEVEL_RANKING_1ST_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, DARK_ELF_LEVEL_RANKING_1ST_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, ORC_LEVEL_RANKING_1ST_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, DWARF_LEVEL_RANKING_1ST_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, KAMAEL_LEVEL_RANKING_1ST_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, HUMAN_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, ELF_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, DARK_ELF_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, ORC_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, DWARF_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, KAMAEL_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, DEATH_KNIGHT_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, SYLPH_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, VANGUARD_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, ASSASSIN_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, HIGH_ELF_MALE_LEVEL_TRANSFORM_CLASS);
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, HIGH_ELF_FEMALE_LEVEL_TRANSFORM_CLASS);
		player.removeSkill(SERVER_RANKING_BENEFIT_1);
		player.removeSkill(SERVER_RANKING_BENEFIT_2);
		player.removeSkill(SERVER_RANKING_BENEFIT_3);
		player.removeSkill(RACE_RANKING_BENEFIT);
		
		// Add global rank skills.
		int rank = RankManager.getInstance().getPlayerGlobalRank(player);
		if (rank > 0)
		{
			if (rank <= 1)
			{
				player.sendPacket(new ExRankingBuffZoneNpcInfo());
				SERVER_LEVEL_RANKING_1ST_CLASS.applyEffects(player, player);
				player.addSkill(SERVER_RANKING_BENEFIT_1, false);
				player.addSkill(SERVER_RANKING_BENEFIT_2, false);
				player.addSkill(SERVER_RANKING_BENEFIT_3, false);
			}
			else if (rank <= 30)
			{
				SERVER_LEVEL_RANKING_2ND_CLASS.applyEffects(player, player);
				player.addSkill(SERVER_RANKING_BENEFIT_1, false);
				player.addSkill(SERVER_RANKING_BENEFIT_2, false);
			}
			else if (rank <= 100)
			{
				SERVER_LEVEL_RANKING_3RD_CLASS.applyEffects(player, player);
				player.addSkill(SERVER_RANKING_BENEFIT_1, false);
			}
		}
		
		// Apply race rank effects.
		final int raceRank = RankManager.getInstance().getPlayerRaceRank(player);
		if (raceRank == 1)
		{
			switch (player.getRace())
			{
				case HUMAN:
				{
					HUMAN_LEVEL_RANKING_1ST_CLASS.applyEffects(player, player);
					break;
				}
				case ELF:
				{
					ELF_LEVEL_RANKING_1ST_CLASS.applyEffects(player, player);
					break;
				}
				case DARK_ELF:
				{
					DARK_ELF_LEVEL_RANKING_1ST_CLASS.applyEffects(player, player);
					break;
				}
				case ORC:
				{
					ORC_LEVEL_RANKING_1ST_CLASS.applyEffects(player, player);
					break;
				}
				case DWARF:
				{
					DWARF_LEVEL_RANKING_1ST_CLASS.applyEffects(player, player);
					break;
				}
				case KAMAEL:
				{
					KAMAEL_LEVEL_RANKING_1ST_CLASS.applyEffects(player, player);
					break;
				}
				case SYLPH:
				{
					SYLPH_LEVEL_RANKING_1ST_CLASS.applyEffects(player, player);
					break;
				}
				case HIGH_ELF:
				{
					HIGH_ELF_LEVEL_RANKING_1ST_CLASS.applyEffects(player, player);
					break;
				}
			}
			
			player.addSkill(RACE_RANKING_BENEFIT, false);
		}
		
		// Add race rank transform skills.
		final int raceTransform = RankManager.getInstance().getPlayerRaceRank(player);
		if ((raceTransform > 0) && (raceTransform <= 3))
		{
			switch (player.getRace())
			{
				case HUMAN:
				{
					if (player.isDeathKnight())
					{
						player.addSkill(DEATH_KNIGHT_LEVEL_TRANSFORM_CLASS, false);
					}
					else if (player.isAssassin())
					{
						player.addSkill(ASSASSIN_LEVEL_TRANSFORM_CLASS, false);
					}
					else
					{
						player.addSkill(HUMAN_LEVEL_TRANSFORM_CLASS, false);
					}
					break;
				}
				case ELF:
				{
					if (player.isDeathKnight())
					{
						player.addSkill(DEATH_KNIGHT_LEVEL_TRANSFORM_CLASS, false);
					}
					else
					{
						player.addSkill(ELF_LEVEL_TRANSFORM_CLASS, false);
					}
					break;
				}
				case DARK_ELF:
				{
					if (player.isDeathKnight())
					{
						player.addSkill(DEATH_KNIGHT_LEVEL_TRANSFORM_CLASS, false);
					}
					else if (player.isAssassin())
					{
						player.addSkill(ASSASSIN_LEVEL_TRANSFORM_CLASS, false);
					}
					else
					{
						player.addSkill(DARK_ELF_LEVEL_TRANSFORM_CLASS, false);
					}
					break;
				}
				case ORC:
				{
					if (player.isVanguard())
					{
						player.addSkill(VANGUARD_LEVEL_TRANSFORM_CLASS, false);
					}
					else
					{
						player.addSkill(ORC_LEVEL_TRANSFORM_CLASS, false);
					}
					break;
				}
				case DWARF:
				{
					player.addSkill(DWARF_LEVEL_TRANSFORM_CLASS, false);
					break;
				}
				case KAMAEL:
				{
					player.addSkill(KAMAEL_LEVEL_TRANSFORM_CLASS, false);
					break;
				}
				case SYLPH:
				{
					player.addSkill(SYLPH_LEVEL_TRANSFORM_CLASS, false);
					break;
				}
				case HIGH_ELF:
				{
					if (player.getAppearance().isFemale())
					{
						player.addSkill(HIGH_ELF_FEMALE_LEVEL_TRANSFORM_CLASS, false);
					}
					else
					{
						player.addSkill(HIGH_ELF_MALE_LEVEL_TRANSFORM_CLASS, false);
					}
					break;
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new RankingSkillBonuses();
	}
}
