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
package org.l2jmobius.gameserver.network.clientpackets.newhenna;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.HennaCombinationData;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.combination.CombinationItemType;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.henna.CombinationHenna;
import org.l2jmobius.gameserver.model.item.henna.CombinationHennaReward;
import org.l2jmobius.gameserver.model.item.henna.Henna;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.newhenna.NewHennaPotenCompose;

/**
 * @author Index, Serenitty
 */
public class RequestNewHennaCompose extends ClientPacket
{
	private int _slotOneIndex;
	private int _slotOneItemId;
	private int _slotTwoItemId;
	
	@Override
	protected void readImpl()
	{
		_slotOneIndex = readInt();
		_slotOneItemId = readInt();
		_slotTwoItemId = readInt();
	}

	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}

		final Inventory inventory = player.getInventory();
		if ((player.getHenna(_slotOneIndex) == null) //
				|| ((_slotOneItemId != -1) && (inventory.getItemByObjectId(_slotOneItemId) == null)) //
				|| ((_slotTwoItemId != -1) && (inventory.getItemByObjectId(_slotTwoItemId) == null)))
		{
			return;
		}

		final Henna henna = player.getHenna(_slotOneIndex);

		// 獲取實際使用的材料 ItemId
		int actualItemId = -1;
		if (_slotOneItemId != -1)
		{
			actualItemId = inventory.getItemByObjectId(_slotOneItemId).getId();
		}
		else if (_slotTwoItemId != -1)
		{
			actualItemId = inventory.getItemByObjectId(_slotTwoItemId).getId();
		}

		// 根據染料ID和材料ID查詢配置
		final CombinationHenna combinationHennas = HennaCombinationData.getInstance().getByHennaAndItem(henna.getDyeId(), actualItemId);
		if (combinationHennas == null)
		{
			player.sendPacket(new NewHennaPotenCompose(henna.getDyeId(), -1, false));
			return;
		}

		// 驗證材料ID是否匹配
		if (((_slotOneItemId != -1) && (combinationHennas.getItemOne() != inventory.getItemByObjectId(_slotOneItemId).getId())) //
				|| ((_slotTwoItemId != -1) && (combinationHennas.getItemTwo() != inventory.getItemByObjectId(_slotTwoItemId).getId())))
		{
			PacketLogger.info(getClass().getSimpleName() + ": " + player + " has modified client or combination data is outdated!" + System.lineSeparator() + "Henna DyeId: " + henna.getDyeId() + " ItemOne: " + combinationHennas.getItemOne() + " ItemTwo: " + combinationHennas.getItemTwo());
		}

		final long commission = combinationHennas.getCommission();
		if (commission > player.getAdena())
		{
			return;
		}

		final ItemHolder one = new ItemHolder(combinationHennas.getItemOne(), combinationHennas.getCountOne());
		final ItemHolder two = new ItemHolder(combinationHennas.getItemTwo(), combinationHennas.getCountTwo());
		if (((_slotOneItemId != -1) && ((inventory.getItemByItemId(one.getId()) == null) || (inventory.getItemByItemId(one.getId()).getCount() < one.getCount()))) //
				|| ((_slotTwoItemId != -1) && ((inventory.getItemByItemId(two.getId()) == null) || (inventory.getItemByItemId(two.getId()).getCount() < two.getCount()))))
		{
			player.sendPacket(new NewHennaPotenCompose(henna.getDyeId(), -1, false));
			return;
		}

		// 先銷毀物品
		if (((_slotOneItemId != -1) && (inventory.destroyItemByItemId(ItemProcessType.FEE, one.getId(), one.getCount(), player, null) == null)) //
				|| ((_slotTwoItemId != -1) && (inventory.destroyItemByItemId(ItemProcessType.FEE, two.getId(), two.getCount(), player, null) == null)) //
				|| (inventory.destroyItemByItemId(ItemProcessType.FEE, Inventory.ADENA_ID, commission, player, null) == null))
		{
			player.sendPacket(new NewHennaPotenCompose(henna.getDyeId(), -1, false));
			return;
		}

		// 判斷成功失敗並更新染料
		CombinationHennaReward reward;
		boolean success = false;
		if (Rnd.get(0, 100) <= combinationHennas.getChance())
		{
			success = true;
			reward = combinationHennas.getReward(CombinationItemType.ON_SUCCESS);
			player.removeHenna(_slotOneIndex, false);
			player.addHenna(_slotOneIndex, HennaData.getInstance().getHenna(reward.getHennaId()));
			if (reward.getId() > 0)
			{
				player.addItem(ItemProcessType.REWARD, reward.getId(), reward.getCount(), null, false);
			}
		}
		else
		{
			reward = combinationHennas.getReward(CombinationItemType.ON_FAILURE);
			if (henna.getDyeId() != reward.getHennaId())
			{
				player.removeHenna(_slotOneIndex, false);
				player.addHenna(_slotOneIndex, HennaData.getInstance().getHenna(reward.getHennaId()));
			}
			if (reward.getId() > 0)
			{
				player.addItem(ItemProcessType.REWARD, reward.getId(), reward.getCount(), null, false);
			}
		}

		// 物品銷毀和獎勵發放完成後，構建 InventoryUpdate
		final InventoryUpdate iu = new InventoryUpdate();
		if (_slotOneItemId != -1)
		{
			if (inventory.getItemByItemId(one.getId()) != null)
			{
				iu.addModifiedItem(inventory.getItemByItemId(one.getId()));
			}
		}
		if (_slotTwoItemId != -1)
		{
			if (inventory.getItemByItemId(two.getId()) != null)
			{
				iu.addModifiedItem(inventory.getItemByItemId(two.getId()));
			}
		}
		iu.addModifiedItem(inventory.getItemByItemId(Inventory.ADENA_ID));
		if (reward.getId() > 0)
		{
			iu.addModifiedItem(inventory.getItemByItemId(reward.getId()));
		}

		// 先發送物品更新
		player.sendPacket(iu);
		// 再發送結果封包
		player.sendPacket(new NewHennaPotenCompose(reward.getHennaId(), reward.getId() == 0 ? -1 : reward.getId(), success));
	}
}
