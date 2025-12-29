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
package handlers.itemhandlers;


import org.l2jmobius.gameserver.config.VipSystemConfig;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.vip.VipManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.vip.ReceiveVipInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * VIP點數道具處理器
 * @author 黑普羅
 */
public class VipPoints implements IItemHandler
{
	private static final int[][] VIP_POINTS_CONFIG =
			{
					// { 道具ID, 對應的VIP點數 }
					{ 90001, 1000 },
					{ 90002, 5000 },
					{ 90003, 10000 },
					{ 90004, 20000 },
					{ 90005, 50000 },
			};

	private static final Map<Integer, Long> VIP_POINTS_MAP = new HashMap<>();

	static
	{
		for (int[] config : VIP_POINTS_CONFIG)
		{
			VIP_POINTS_MAP.put(config[0], (long) config[1]);
		}
	}

	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_GUARDIAN);
			return false;
		}

		if (!VipSystemConfig.VIP_SYSTEM_ENABLED)
		{
			return false;
		}

		final Player player = playable.asPlayer();
		final int itemId = item.getId();

		Long vipPointsToAdd = VIP_POINTS_MAP.get(itemId);

		if (vipPointsToAdd == null || vipPointsToAdd <= 0)
		{
			player.sendMessage("此道具無法使用。");
			return false;
		}

		byte oldTier = player.getVipTier();
		long oldPoints = player.getVipPoints();

		player.updateVipPoints(vipPointsToAdd);

		VipManager.getInstance().manageTier(player);

		player.sendPacket(new ReceiveVipInfo(player));

		byte newTier = player.getVipTier();
		if (newTier > oldTier)
		{
			player.sendMessage("════════════════════════════════");
			player.sendMessage("恭喜！你的VIP等級已提升至 " + newTier + " 級！");
			player.sendMessage("獲得 " + vipPointsToAdd + " VIP點數");
			player.sendMessage("目前總點數: " + player.getVipPoints());
			player.sendMessage("════════════════════════════════");
		}
		else
		{
			player.sendMessage("成功增加 " + vipPointsToAdd + " VIP點數！");
			player.sendMessage("目前總點數: " + player.getVipPoints());
		}

		player.destroyItem(ItemProcessType.NONE, item, 1, player, true);

		return true;
	}
}