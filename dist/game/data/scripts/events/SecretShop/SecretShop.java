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
package events.SecretShop;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.managers.events.SecretShopEventManager;
import org.l2jmobius.gameserver.managers.events.SecretShopEventManager.SecretShopRewardHolder;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.model.script.LongTimeEvent;
import org.l2jmobius.gameserver.managers.events.SecretShopEventManager.TimePeriod;

/**
 * @author Serenitty
 * @URL https://l2central.info/essence/events_and_promos/2169.html
 */
public class SecretShop extends LongTimeEvent implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SecretShop.class.getName());
	
	private boolean _initialized = false;
	
	public SecretShop()
	{
	}
	
	@Override
	public void load()
	{
		parseDatapackFile("data/scripts/events/SecretShop/SecretShop.xml");
		_initialized = true;
	}

	@Override
	public void parseDocument(Document document, File file)
	{
		final Map<Integer, List<SecretShopRewardHolder>> rewardData = new HashMap<>();
		final AtomicInteger ticketId = new AtomicInteger(94871);

		// 改為使用 List 來儲存多個時間段
		final List<TimePeriod> timePeriods = new ArrayList<>();

		forEach(document, "list", listNode ->
		{
			// 解析 ticketId
			forEach(listNode, "ticketId", ticketIdNode ->
			{
				ticketId.set(Integer.parseInt(ticketIdNode.getTextContent()));
			});

			// 新的解析邏輯：處理多個時間段
			forEach(listNode, "timePeriods", timePeriodsNode ->
			{
				forEach(timePeriodsNode, "period", periodNode ->
				{
					// 從屬性中讀取開始和結束時間
					final NamedNodeMap periodAttrs = periodNode.getAttributes();
					final String startTime = periodAttrs.getNamedItem("startTime").getNodeValue();
					final String endTime = periodAttrs.getNamedItem("endTime").getNodeValue();

					// 分割時間字串 (格式: "HH:mm")
					final String[] startParts = startTime.split(":");
					final String[] endParts = endTime.split(":");

					final int startHour = Integer.parseInt(startParts[0]);
					final int startMinute = Integer.parseInt(startParts[1]);
					final int endHour = Integer.parseInt(endParts[0]);
					final int endMinute = Integer.parseInt(endParts[1]);

					// 創建 TimePeriod 物件並加入列表
					timePeriods.add(new TimePeriod(startHour, startMinute, endHour, endMinute));

					LOGGER.info("Added time period: " + startTime + " - " + endTime);
				});
			});

			// 保持原有的獎勵解析邏輯不變
			forEach(listNode, "rewards", rewardsNode ->
			{
				final int day = Integer.parseInt(rewardsNode.getAttributes().getNamedItem("day").getNodeValue());
				forEach(rewardsNode, "item", itemNode ->
				{
					final NamedNodeMap attrs = itemNode.getAttributes();
					final int itemId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					final int grade = Integer.parseInt(attrs.getNamedItem("grade").getNodeValue());
					final long itemCount = Long.parseLong(attrs.getNamedItem("count").getNodeValue());
					final long totalAmount = Long.parseLong(attrs.getNamedItem("totalAmount").getNodeValue());
					final double chance = Double.parseDouble(attrs.getNamedItem("chance").getNodeValue());

					List<SecretShopRewardHolder> rewardList = rewardData.get(day);
					if (rewardList == null)
					{
						rewardList = new ArrayList<>();
						rewardData.put(day, rewardList);
					}

					rewardList.add(new SecretShopRewardHolder(itemId, itemCount, grade, totalAmount, totalAmount, chance));
				});
			});
		});

		// 更新日誌訊息
		LOGGER.warning(getClass().getSimpleName() + ": Loaded " + rewardData.size() + " secret shop entries with " + timePeriods.size() + " time periods.");

		// 調用新的 init 方法，傳入時間段列表
		SecretShopEventManager.getInstance().init(rewardData, ticketId.get(), timePeriods);
	}
	
	@Override
	protected void startEvent()
	{
		super.startEvent();
		if (!_initialized)
		{
			load();
		}
		
		SecretShopEventManager.getInstance().startEvent();
	}
	
	@Override
	protected void stopEvent()
	{
		super.stopEvent();
		SecretShopEventManager.getInstance().stopEvent();
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
		
		SecretShopEventManager.getInstance().sendInfo(player);
	}
	
	public static void main(String[] args)
	{
		System.out.println("秘密商店活動已開啟！");
		new SecretShop();
	}
}
