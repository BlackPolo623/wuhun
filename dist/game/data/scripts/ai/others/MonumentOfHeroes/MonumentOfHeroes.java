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
package ai.others.MonumentOfHeroes;

import java.util.List;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.olympiad.Hero;
import org.l2jmobius.gameserver.model.olympiad.Olympiad;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExHeroList;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * Monument of Heroes AI.
 * @author St3eT, Mobius
 */
public class MonumentOfHeroes extends Script
{
	// NPC
	private static final int MONUMENT = 31690;
	
	// Items
	private static final int HERO_CLOAK = 30372;
	// private static final int GLORIOUS_CLOAK = 30373;
	private static final int WINGS_OF_DESTINY_CIRCLET = 6842;
	private static final int[] WEAPONS =
	{
		6611, // Infinity Blade
		6612, // Infinity Cleaver
		6613, // Infinity Axe
		6614, // Infinity Rod
		6616, // Infinity Scepter
		6617, // Infinity Stinger
		6618, // Infinity Fang
		6619, // Infinity Bow
		6620, // Infinity Wing
		6621, // Infinity Spear
	};
	
	private MonumentOfHeroes()
	{
		if (OlympiadConfig.OLYMPIAD_ENABLED)
		{
			addStartNpc(MONUMENT);
			addFirstTalkId(MONUMENT);
			addTalkId(MONUMENT);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "MonumentOfHeroes-reward.html":
			{
				htmltext = event;
				break;
			}
			case "index":
			{
				htmltext = onFirstTalk(npc, player);
				break;
			}
			case "heroList":
			{
				player.sendPacket(new ExHeroList());
				break;
			}
			case "receiveCloak":
			{
				final int olympiadRank = getOlympiadRank(player);
				if (olympiadRank == 1)
				{
					if (!hasAtLeastOneQuestItem(player, HERO_CLOAK/* , GLORIOUS_CLOAK */))
					{
						if (player.isInventoryUnder80(false))
						{
							giveItems(player, HERO_CLOAK, 1);
						}
						else
						{
							player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
						}
					}
					else
					{
						htmltext = "MonumentOfHeroes-cloakHave.html";
					}
				}
				/*
				 * else if ((olympiadRank == 2) || (olympiadRank == 3)) { if (!hasAtLeastOneQuestItem(player, HERO_CLOAK, GLORIOUS_CLOAK)) { if (player.isInventoryUnder80(false)) { giveItems(player, GLORIOUS_CLOAK, 1); } else {
				 * player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY); } } else { htmltext = "MonumentOfHeroes-cloakHave.html"; } }
				 */
				else
				{
					htmltext = "MonumentOfHeroes-cloakNo.html";
				}
				break;
			}
			case "heroWeapon":
			{
				if (Hero.getInstance().isHero(player.getObjectId()))
				{
					if (player.isInventoryUnder80(false))
					{
						htmltext = hasAtLeastOneQuestItem(player, WEAPONS) ? "MonumentOfHeroes-weaponHave.html" : "MonumentOfHeroes-weaponList.html";
					}
					else
					{
						player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
					}
				}
				else
				{
					htmltext = "MonumentOfHeroes-weaponNo.html";
				}
				break;
			}
			case "heroCirclet":
			{
				if (Hero.getInstance().isHero(player.getObjectId()))
				{
					if (hasQuestItems(player, WINGS_OF_DESTINY_CIRCLET))
					{
						htmltext = "MonumentOfHeroes-circletHave.html";
					}
					else if (!player.isInventoryUnder80(false))
					{
						player.sendPacket(SystemMessageId.UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
					}
					else
					{
						giveItems(player, WINGS_OF_DESTINY_CIRCLET, 1);
					}
				}
				else
				{
					htmltext = "MonumentOfHeroes-circletNo.html";
				}
				break;
			}
			case "heroCertification":
			{
				if (Hero.getInstance().isUnclaimedHero(player.getObjectId()))
				{
					htmltext = "MonumentOfHeroes-heroCertification.html";
				}
				else if (Hero.getInstance().isHero(player.getObjectId()))
				{
					htmltext = "MonumentOfHeroes-heroCertificationAlready.html";
				}
				else
				{
					htmltext = "MonumentOfHeroes-heroCertificationNo.html";
				}
				break;
			}
			case "heroConfirm":
			{
				if (Hero.getInstance().isUnclaimedHero(player.getObjectId()))
				{
					if (!player.isSubClassActive())
					{
						if (player.getLevel() >= 55)
						{
							Hero.getInstance().claimHero(player);
							showOnScreenMsg(player, "Congratulations, " + player.getName() + "! You have become the Hero of " + ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName(), 10000);
							player.broadcastPacket(new PlaySound(1, "ns01_f", 0, 0, 0, 0, 0));
							htmltext = "MonumentOfHeroes-heroCertificationsDone.html";
						}
						else
						{
							htmltext = "MonumentOfHeroes-heroCertificationLevel.html";
						}
					}
					else
					{
						htmltext = "MonumentOfHeroes-heroCertificationSub.html";
					}
				}
				else
				{
					htmltext = "MonumentOfHeroes-heroCertificationNo.html";
				}
				break;
			}
			case "give_6611": // Infinity Blade
			case "give_6612": // Infinity Cleaver
			case "give_6613": // Infinity Axe
			case "give_6614": // Infinity Rod
			case "give_6616": // Infinity Scepter
			case "give_6617": // Infinity Stinger
			case "give_6618": // Infinity Fang
			case "give_6619": // Infinity Bow
			case "give_6620": // Infinity Wing
			case "give_6621": // Infinity Spear
			{
				final int weaponId = Integer.parseInt(event.replace("give_", ""));
				giveItems(player, weaponId, 1);
				break;
			}
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if ((!player.isInCategory(CategoryType.THIRD_CLASS_GROUP) && !player.isInCategory(CategoryType.FOURTH_CLASS_GROUP)) || (player.getLevel() < 55))
		{
			return "MonumentOfHeroes-noNoblesse.html";
		}
		
		return "MonumentOfHeroes-noblesse.html";
	}
	
	private int getOlympiadRank(Player player)
	{
		final List<String> names = Olympiad.getInstance().getClassLeaderBoard(player.getPlayerClass().getId());
		try
		{
			for (int i = 1; i <= 3; i++)
			{
				if (names.get(i - 1).equals(player.getName()))
				{
					return i;
				}
			}
		}
		catch (Exception e)
		{
			return -1;
		}
		
		return -1;
	}
	
	public static void main(String[] args)
	{
		new MonumentOfHeroes();
	}
}
