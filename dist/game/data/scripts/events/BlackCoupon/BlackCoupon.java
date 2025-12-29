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
package events.BlackCoupon;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.managers.events.BlackCouponManager;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.model.item.enums.BlackCouponRestoreCategory;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;

public class BlackCoupon extends LongTimeEvent implements IXmlReader
{
	private BlackCoupon()
	{
		final BlackCouponManager manager = BlackCouponManager.getInstance();
		manager.setEventStatus(isEventPeriod());
		if (isEventPeriod())
		{
			load();
		}
	}
	
	@Override
	public void load()
	{
		if (isEventPeriod())
		{
			parseDatapackFile("data/scripts/events/BlackCoupon/eventDetails.xml");
		}
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		final BlackCouponManager manager = BlackCouponManager.getInstance();
		final AtomicBoolean needToParse = new AtomicBoolean(true);
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "configurations", configNode ->
			{
				final StatSet configSet = new StatSet(parseAttributes(configNode));
				manager.setCouponItem(new ItemHolder(configSet.getInt("couponID", 98022), configSet.getLong("couponCount", 1)));
				manager.setMultisellId(configSet.getInt("multisellID", 3247899));
				final List<Long> range = new ArrayList<>();
				Arrays.stream(configSet.getString("showItemsInRange", "1651134344000;2524608000000").split(";")).forEach(v -> range.add(Long.parseLong(v)));
				manager.setDateRange(range.get(0), range.get(1));
				needToParse.set(configSet.getBoolean("parseExchangeItemsBelow", true));
			});
			manager.setRestoreIdFromXml(needToParse.get());
			if (needToParse.get())
			{
				final HashMap<Integer, HashMap<BlackCouponRestoreCategory, Integer>> xmlRestoreList = new HashMap<>(new HashMap<>());
				forEach(listNode, "exchange", exchangeNode ->
				{
					forEach(exchangeNode, "items", categoryNode ->
					{
						forEach(categoryNode, "item", itemNode ->
						{
							final StatSet itemSet = new StatSet(parseAttributes(itemNode));
							final int id1 = itemSet.getInt("breakID");
							final int id2 = itemSet.getInt("restoreID", id1);
							xmlRestoreList.put(id1, new HashMap<>(Map.of(new StatSet(parseAttributes(categoryNode)).getEnum("category", BlackCouponRestoreCategory.class), id2)));
						});
					});
				});
				manager.setXmlRestoreList(xmlRestoreList);
			}
		});
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
		if (player != null)
		{
			BlackCouponManager.getInstance().restorePlayerRecords(player.getObjectId());
		}
	}
	
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogout(OnPlayerLogout event)
	{
		if (!isEventPeriod())
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player != null)
		{
			BlackCouponManager.getInstance().removePlayerRecords(player.getObjectId());
		}
	}
	
	public static void main(String[] args)
	{
		new BlackCoupon();
	}
}
