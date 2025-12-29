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
package events.BalthusFestival;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.managers.events.BalthusEventManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.balthusevent.ExBalthusEvent;

/**
 * @author Index
 */
public class BalthusFestival extends LongTimeEvent implements IXmlReader
{
	// NPC
	private static final int SIBI = 34262;
	
	// Misc
	private static final SchedulingPattern CRON_PATTERN = new SchedulingPattern("30 6 * * *");
	
	public BalthusFestival()
	{
		addStartNpc(SIBI);
		addFirstTalkId(SIBI);
		addTalkId(SIBI);
		load();
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/scripts/events/BalthusFestival/rewards.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final BalthusEventManager manager = BalthusEventManager.getInstance();
		manager.setEventPeriod(getStartDate(), getEndDate());
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "parameters", parametersSet ->
			{
				final StatSet params = new StatSet();
				forEach(parametersSet, "param", paramNode ->
				{
					final StatSet paramSet = new StatSet(parseAttributes(paramNode));
					params.set(paramSet.getString("name"), paramSet.getString("value"));
				});
				manager.initParameters(params);
			});
			forEach(listNode, "reward", rewardNode ->
			{
				final StatSet periodSet = new StatSet(parseAttributes(rewardNode));
				final int from = periodSet.getInt("from", 0);
				final int to = periodSet.getInt("to", 0);
				final Entry<Integer, Integer> period = new SimpleEntry<>(from, to);
				forEach(rewardNode, "items", itemsNode ->
				{
					forEach(itemsNode, "item", itemNode ->
					{
						final StatSet itemSet = new StatSet(parseAttributes(itemNode));
						final int itemId = itemSet.getInt("id", 0);
						final long itemCount = itemSet.getLong("count", 1L);
						final double chanceToObtainByPlayer = itemSet.getDouble("chance", 0d);
						final int enchantLevel = itemSet.getInt("enchant", 0);
						final double chanceToNextGame = itemSet.getDouble("lotteryChance", 0d);
						final boolean redeemInAnyCase = itemSet.getBoolean("redeemInAnyCase", false);
						manager.addRewards(period, itemId, itemCount, chanceToObtainByPlayer, enchantLevel, chanceToNextGame, redeemInAnyCase);
					});
				});
			});
		});
		manager.init();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "34262-1.htm":
			{
				if (BalthusEventManager.getInstance().getDailySupplyItem() != null)
				{
					return "34262-1.htm";
				}
				
				return "34262-1b.htm";
			}
			case "34262-shop.htm":
			{
				return "34262-shop.htm";
			}
			case "giveSupplyBox":
			{
				final BalthusEventManager manager = BalthusEventManager.getInstance();
				if (manager.getDailySupplyItem() == null)
				{
					return "34262-1b.htm";
				}
				
				if (player.getLevel() < manager.getMinimumLevel())
				{
					return "34262-2.htm";
				}
				
				final long lastReceived = player.getVariables().getLong(PlayerVariables.BALTHUS_BAG, 0L);
				if (lastReceived > System.currentTimeMillis())
				{
					return "34262-3.htm";
				}
				
				if ((manager.getDailySupplyFeeItem() != null) && (player.getInventory().getInventoryItemCount(manager.getDailySupplyFeeItem().getId(), -1) < manager.getDailySupplyFeeItem().getCount()))
				{
					return "34262-2.htm";
				}
				
				if ((manager.getDailySupplyFeeItem() != null) && !player.destroyItem(ItemProcessType.FEE, player.getInventory().getItemByItemId(manager.getDailySupplyFeeItem().getId()).getObjectId(), manager.getDailySupplyFeeItem().getCount(), npc, true))
				{
					return "34262-2.htm";
				}
				
				player.addItem(ItemProcessType.REWARD, manager.getDailySupplyItem().getId(), manager.getDailySupplyItem().getCount(), npc, true);
				player.getVariables().set(PlayerVariables.BALTHUS_BAG, CRON_PATTERN.next(System.currentTimeMillis()));
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		player.sendPacket(new NpcHtmlMessage(npc.getObjectId(), 0, getHtm(player, "34262.htm"), 1));
		return null;
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		if (!isEventPeriod())
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		player.sendPacket(new ExBalthusEvent(player));
	}
	
	public static void main(String[] args)
	{
		new BalthusFestival();
	}
}
