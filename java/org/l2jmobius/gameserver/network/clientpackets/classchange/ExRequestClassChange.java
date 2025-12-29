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
package org.l2jmobius.gameserver.network.clientpackets.classchange;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.model.ElementalSpirit;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExClassChangeSetAlarm;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ElementalSpiritInfo;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ExElementalSpiritAttackType;

/**
 * @author Mobius
 */
public class ExRequestClassChange extends ClientPacket
{
	private int _classId;
	
	@Override
	protected void readImpl()
	{
		_classId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Check if class id is valid.
		boolean canChange = false;
		for (PlayerClass cId : player.getPlayerClass().getNextClasses())
		{
			if (cId.getId() == _classId)
			{
				canChange = true;
				break;
			}
		}
		
		if (!canChange)
		{
			PacketLogger.warning(player + " tried to change class from " + player.getPlayerClass() + " to " + PlayerClass.getPlayerClass(_classId) + "!");
			return;
		}
		
		// Check for player proper class group and level.
		canChange = false;
		final int playerLevel = player.getLevel();
		if (player.isInCategory(CategoryType.FIRST_CLASS_GROUP) && (playerLevel >= 18))
		{
			canChange = CategoryData.getInstance().isInCategory(CategoryType.SECOND_CLASS_GROUP, _classId);
		}
		else if (player.isInCategory(CategoryType.SECOND_CLASS_GROUP) && (playerLevel >= 38))
		{
			canChange = CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, _classId);
		}
		else if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (playerLevel >= 76))
		{
			canChange = CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, _classId);
		}
		
		// Change class.
		if (canChange)
		{
			player.setPlayerClass(_classId);
			if (player.isSubClassActive())
			{
				player.getSubClasses().get(player.getClassIndex()).setPlayerClass(player.getActiveClass());
			}
			else
			{
				player.setBaseClass(player.getActiveClass());
			}
			
			// Class change rewards.
			if (!PlayerConfig.DISABLE_TUTORIAL)
			{
				switch (player.getPlayerClass())
				{
					case KNIGHT:
					case ELVEN_KNIGHT:
					case PALUS_KNIGHT:
					case DEATH_BLADE_HUMAN:
					case DEATH_BLADE_ELF:
					case DEATH_BLADE_DARK_ELF:
					case DIVINE_TEMPLAR_1:
					{
						player.addItem(ItemProcessType.REWARD, 93028, 1, player, true); // Aden Sword.
						player.addItem(ItemProcessType.REWARD, 93493, 1, player, true); // Moon Armor Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case WARRIOR:
					{
						player.addItem(ItemProcessType.REWARD, 93028, 1, player, true); // Aden Sword.
						player.addItem(ItemProcessType.REWARD, 93034, 1, player, true); // Aden Spear.
						player.addItem(ItemProcessType.REWARD, 93493, 1, player, true); // Moon Armor Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case ROGUE:
					case ELVEN_SCOUT:
					case ASSASSIN:
					{
						player.addItem(ItemProcessType.REWARD, 93029, 1, player, true); // Aden Dagger.
						player.addItem(ItemProcessType.REWARD, 93030, 1, player, true); // Aden Bow.
						player.addItem(ItemProcessType.REWARD, 1341, 2000, player, true); // Bone Arrow.
						player.addItem(ItemProcessType.REWARD, 93494, 1, player, true); // Moon Shell Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case WARG_1:
					{
						player.addItem(ItemProcessType.REWARD, 93035, 1, player, true); // Aden Fist.
						player.addItem(ItemProcessType.REWARD, 93494, 1, player, true); // Moon Shell Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case WIZARD:
					case CLERIC:
					case ELVEN_WIZARD:
					case ORACLE:
					case DARK_WIZARD:
					case SHILLIEN_ORACLE:
					case ORC_SHAMAN:
					case ELEMENT_WEAVER_1:
					{
						player.addItem(ItemProcessType.REWARD, 93033, 1, player, true); // Two-Handed Blunt Weapon of Aden.
						player.addItem(ItemProcessType.REWARD, 93495, 1, player, true); // Moon Cape Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case ORC_RAIDER:
					{
						player.addItem(ItemProcessType.REWARD, 93032, 1, player, true); // Two-handed Sword of Aden.
						player.addItem(ItemProcessType.REWARD, 93493, 1, player, true); // Moon Armor Set.
						player.addItem(ItemProcessType.REWARD, 93497, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case ORC_MONK:
					{
						player.addItem(ItemProcessType.REWARD, 93035, 1, player, true); // Aden Fist.
						player.addItem(ItemProcessType.REWARD, 93493, 1, player, true); // Moon Armor Set.
						player.addItem(ItemProcessType.REWARD, 93497, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case ARTISAN:
					case SCAVENGER:
					{
						player.addItem(ItemProcessType.REWARD, 93031, 1, player, true); // Aden Club.
						player.addItem(ItemProcessType.REWARD, 93034, 1, player, true); // Aden Spear.
						player.addItem(ItemProcessType.REWARD, 93493, 1, player, true); // Moon Armor Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case TROOPER:
					{
						player.addItem(ItemProcessType.REWARD, 93037, 1, player, true); // Aden Ancient Sword.
						player.addItem(ItemProcessType.REWARD, 93494, 1, player, true); // Moon Shell Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case WARDER:
					{
						player.addItem(ItemProcessType.REWARD, 93030, 1, player, true); // Aden Bow.
						player.addItem(ItemProcessType.REWARD, 1341, 2000, player, true); // Bone Arrow.
						player.addItem(ItemProcessType.REWARD, 93494, 1, player, true); // Moon Shell Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case SOUL_FINDER:
					{
						player.addItem(ItemProcessType.REWARD, 93036, 1, player, true); // Aden Rapier.
						player.addItem(ItemProcessType.REWARD, 93494, 1, player, true); // Moon Shell Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case SHARPSHOOTER:
					{
						player.addItem(ItemProcessType.REWARD, 94892, 1, player, true); // D-Grade Elemental Orb Sealed.
						player.addItem(ItemProcessType.REWARD, 94897, 1, player, true); // Aden Pistols
						player.addItem(ItemProcessType.REWARD, 93494, 1, player, true); // Moon Shell Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case RIDER:
					{
						player.addItem(ItemProcessType.REWARD, 93034, 1, player, true); // Aden Spear.
						player.addItem(ItemProcessType.REWARD, 93493, 1, player, true); // Moon Armor Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case ASSASSIN_MALE_1:
					case ASSASSIN_FEMALE_1:
					{
						player.addItem(ItemProcessType.REWARD, 94998, 1, player, true); // Maingauche.
						player.addItem(ItemProcessType.REWARD, 93494, 1, player, true); // Moon Shell Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case ASSASSIN_FEMALE_3:
					case ASSASSIN_MALE_3:
					{
						player.setAssassinationPoints(1);
						break;
					}
					case BLOOD_ROSE_1:
					{
						player.addItem(ItemProcessType.REWARD, 94998, 1, player, true); // Maingauche.
						player.addItem(ItemProcessType.REWARD, 93495, 1, player, true); // Moon Cape Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
					case HATAMOTO:
					{
						player.addItem(ItemProcessType.REWARD, 129, 1, player, true); // Sword of Revolution.
						player.addItem(ItemProcessType.REWARD, 93493, 1, player, true); // Moon Armor Set.
						player.addItem(ItemProcessType.REWARD, 93496, 1, player, true); // 1st Class Transfer Gift Box.
						break;
					}
				}
			}
			
			// Elemental Spirits.
			if (player.isInCategory(CategoryType.THIRD_CLASS_GROUP))
			{
				if (player.getSpirits() == null)
				{
					player.initElementalSpirits();
				}
				
				for (ElementalSpirit spirit : player.getSpirits())
				{
					if (spirit.getStage() == 0)
					{
						spirit.upgrade();
					}
				}
				
				final UserInfo userInfo = new UserInfo(player);
				userInfo.addComponentType(UserInfoType.ATT_SPIRITS);
				player.sendPacket(userInfo);
				player.sendPacket(new ElementalSpiritInfo(player, (byte) 0));
				player.sendPacket(new ExElementalSpiritAttackType(player));
			}
			
			if (PlayerConfig.AUTO_LEARN_SKILLS)
			{
				player.giveAvailableSkills(PlayerConfig.AUTO_LEARN_FS_SKILLS, true, PlayerConfig.AUTO_LEARN_SKILLS_WITHOUT_ITEMS);
			}
			
			player.store(false); // Save player cause if server crashes before this char is saved, he will lose class.
			player.broadcastUserInfo();
			player.sendSkillList();
			player.sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
			
			if (PlayerConfig.DISABLE_TUTORIAL && !player.isInCategory(CategoryType.FOURTH_CLASS_GROUP) //
				&& ((player.isInCategory(CategoryType.SECOND_CLASS_GROUP) && (playerLevel >= 38)) //
					|| (player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && (playerLevel >= 76))))
			{
				player.sendPacket(ExClassChangeSetAlarm.STATIC_PACKET);
			}
		}
	}
}
