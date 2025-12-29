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
package events.GoldenWheel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.managers.events.PaybackManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.events.holders.item.OnMultisellBuyItem;
import org.l2jmobius.gameserver.model.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.network.serverpackets.payback.PaybackUILauncher;

public class GoldenWheel extends LongTimeEvent implements IXmlReader
{
	// Item
	private static int GOLDEN_WHEEL_COIN = 94834;
	
	// Misc
	private static final List<Long> _multisells = new ArrayList<>();
	
	private GoldenWheel()
	{
		if (isEventPeriod())
		{
			load();
			PaybackManager.getInstance().init();
			PaybackManager.getInstance().addLocalString("ru", "minLevel", "Ваш уровень слишком низкий для участия в этом событии");
			PaybackManager.getInstance().addLocalString("ru", "maxLevel", "Ваш уровень слишком большой для участия в этом событии");
			PaybackManager.getInstance().addLocalString("en", "minLevel", "Your level so low for be participant in this event");
			PaybackManager.getInstance().addLocalString("en", "maxLevel", "Your level so high for be participant in this event");
		}
	}
	
	public void reloadRewards()
	{
		for (Player player : World.getInstance().getPlayers())
		{
			player.sendPacket(new PaybackUILauncher(false));
		}
		
		PaybackManager.getInstance().resetField();
		load();
		for (Player player : World.getInstance().getPlayers())
		{
			player.sendPacket(new PaybackUILauncher(true));
		}
	}
	
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/scripts/events/GoldenWheel/rewards.xml");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final PaybackManager manager = PaybackManager.getInstance();
		manager.setEndTime(getEndDate().getTime());
		forEach(document, "list", listNode ->
		{
			forEach(listNode, "params", paramNode ->
			{
				final StatSet paramSet = new StatSet(parseAttributes(paramNode));
				GOLDEN_WHEEL_COIN = paramSet.getInt("coinId", 94834);
				manager.setCoinID(GOLDEN_WHEEL_COIN);
				manager.setMinLevel(paramSet.getInt("minLevel"));
				manager.setMaxLevel(paramSet.getInt("maxLevel"));
			});
			forEach(listNode, "multisells", multisellNode ->
			{
				final String[] multisells = multisellNode.getTextContent().trim().split(";");
				for (String id : multisells)
				{
					_multisells.add(Long.parseLong(id));
				}
				
				manager.addToMultisells(_multisells);
			});
			forEach(listNode, "payback", paybackNode ->
			{
				final StatSet paramSet = new StatSet(parseAttributes(paybackNode));
				final int id = paramSet.getInt("id");
				final long count = paramSet.getLong("count");
				final AtomicReference<List<ItemChanceHolder>> rewards = new AtomicReference<>(new ArrayList<>());
				forEach(paybackNode, "item", itemNode ->
				{
					final StatSet itemSet = new StatSet(parseAttributes(itemNode));
					final double chance = itemSet.getDouble("chance", 100);
					rewards.get().add(new ItemChanceHolder(itemSet.getInt("id"), chance, itemSet.getLong("count"), (byte) itemSet.getInt("enchantLevel", 0)));
				});
				manager.addRewardsToHolder(id, count, rewards.get());
			});
		});
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (!isEventPeriod() || (player == null))
		{
			return;
		}
		
		PaybackManager.getInstance().addPlayerToList(player);
		player.sendPacket(new PaybackUILauncher(true));
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		if (!isEventPeriod() || (player == null))
		{
			return;
		}
		
		PaybackManager.getInstance().removePlayerFromList(player.getObjectId());
	}
	
	@RegisterEvent(EventType.ON_MULTISELL_BUY_ITEM)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onMultisellBuyItem(OnMultisellBuyItem event)
	{
		final Player player = event.getPlayer();
		if (!isEventPeriod() || (player == null))
		{
			return;
		}
		
		final PaybackManager manager = PaybackManager.getInstance();
		if (manager.getMultisells().contains(event.getMultisellId()))
		{
			for (ItemChanceHolder material : event.getResourceItems())
			{
				if (material.getId() == GOLDEN_WHEEL_COIN)
				{
					final long consumed = manager.getPlayerConsumedProgress(player.getObjectId());
					manager.changePlayerConsumedProgress(player.getObjectId(), consumed + (event.getAmount() * material.getCount()));
					manager.storePlayerProgress(player);
					break;
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		new GoldenWheel();
	}
}
