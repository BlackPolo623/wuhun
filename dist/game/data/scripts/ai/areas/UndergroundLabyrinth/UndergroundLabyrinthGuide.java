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
package ai.areas.UndergroundLabyrinth;

import org.l2jmobius.gameserver.config.PrisonConfig;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;

/**
 * @author Liamxroy
 */
public class UndergroundLabyrinthGuide extends Script
{
	// NPCs
	private static final int ROBIN = 34360;
	
	// Items
	private static final int MARKS_OF_REPENTANCE = 97783;
	private static final int LCOIN = 91663;
	
	// Locations
	private static final Location ADEN = new Location(147433, 26627, -2205);
	
	private UndergroundLabyrinthGuide()
	{
		addStartNpc(ROBIN);
		addFirstTalkId(ROBIN);
		addTalkId(ROBIN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "teleport_out":
			{
				if (!player.isPrisoner())
				{
					player.teleToLocation(ADEN);
				}
				else
				{
					player.sendMessage("你還是囚犯，無法傳送。");
				}
				break;
			}
			case "pay_marks":
			{
				if ((player.getInventory().getItemByItemId(MARKS_OF_REPENTANCE) == null) || !(player.getInventory().getItemByItemId(MARKS_OF_REPENTANCE).getCount() < PrisonConfig.MARK_RELEASE_AMOUNT))
				{
					player.sendMessage("你沒有足夠的懺悔之印。收集 100 個後再來。");
					break;
				}

				if (player.destroyItemByItemId(ItemProcessType.FEE, MARKS_OF_REPENTANCE, PrisonConfig.MARK_RELEASE_AMOUNT, player, true))
				{
					player.setReputation(-135000);
					player.getPrisonerInfo().processFreedom(false);
				}
				break;
			}
			case "pay_lcoin":
			{
				final long count = PrisonConfig.LCOIN_RELEASE_AMOUNT;
				if ((player.getInventory().getItemByItemId(LCOIN) == null) || (player.getInventory().getItemByItemId(LCOIN).getCount() < count))
				{
					player.sendMessage("你沒有足夠的 L-Coin 進行此交易。");
					break;
				}

				if (player.destroyItemByItemId(ItemProcessType.FEE, LCOIN, count, player, true))
				{
					player.getPrisonerInfo().processFreedom(false);
				}
				break;
			}
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new UndergroundLabyrinthGuide();
	}
}
