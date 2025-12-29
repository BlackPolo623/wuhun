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
package events.LeonasDungeonEvent;

import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.managers.events.LeonasDungeonManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @URL https://l2wiki.com/essence/events_and_promos/2170.html
 * @author Serenitty, Mobius
 */
public class LeonasDungeonEvent extends LongTimeEvent
{
	// NPC
	private static final int LEONA = 15985;
	
	private LeonasDungeonEvent()
	{
		addFirstTalkId(LEONA);
		addTalkId(LEONA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("showRanking".equals(event))
		{
			final Set<Entry<Integer, Integer>> topPlayers = LeonasDungeonManager.getInstance().getTopPlayers(20).entrySet();
			final StringBuilder html = new StringBuilder();
			html.append("<html><title>Leona's Dungeon Ranking</title><body><table border=0 cellpadding=0 cellspacing=0 width=290 height=355 background=\"L2UI_CT1.Windows_DF_TooltipBG\"><tr><td align=center><br><center><table border=0 cellpadding=0 cellspacing=0 width=270><tr><td><button value=\"Rank\" width=40 height=27 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\"/></td><td><button value=\"Player Name\" width=100 height=27 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\"/></td><td><button value=\"Level\" width=40 height=27 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\"/></td><td><button value=\"Score\" width=100 height=27 back=\"L2UI_CT1.ListCTRL_DF_Title_Down\" fore=\"L2UI_CT1.ListCTRL_DF_Title\"/></td></tr></table><table border=0 cellspacing=0 cellpadding=0 height=24 width=270>");
			
			int rank = 0;
			for (Entry<Integer, Integer> entry : topPlayers)
			{
				rank++;
				final int playerObjectId = entry.getKey();
				final int score = entry.getValue();
				final String playerName = CharInfoTable.getInstance().getNameById(playerObjectId);
				final int level = CharInfoTable.getInstance().getLevelById(playerObjectId);
				html.append("<tr><td FIXWIDTH=40 align=center>");
				html.append(rank);
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(playerName);
				html.append("</td><td FIXWIDTH=40 align=center>");
				html.append(level);
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(score);
				html.append("</td></tr>");
			}
			html.append("</table></center></td></tr></table></center></body></html>");
			
			player.sendPacket(new NpcHtmlMessage(html.toString()));
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "15985.htm";
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInventory inventory = player.getInventory();
		if (inventory.getItemByItemId(97508) != null)
		{
			player.destroyItemByItemId(ItemProcessType.DESTROY, 97508, inventory.getItemByItemId(97508).getCount(), player, false);
		}
		
		if (inventory.getItemByItemId(97509) != null)
		{
			player.destroyItemByItemId(ItemProcessType.DESTROY, 97509, inventory.getItemByItemId(97509).getCount(), player, false);
		}
	}
	
	public static void main(String[] args)
	{
		new LeonasDungeonEvent();
	}
}
