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
package org.l2jmobius.gameserver.network.clientpackets.enchant.challengepoint;

import org.l2jmobius.gameserver.data.xml.EnchantChallengePointData;
import org.l2jmobius.gameserver.data.xml.EnchantChallengePointData.EnchantChallengePointsItemInfo;
import org.l2jmobius.gameserver.data.xml.EnchantChallengePointData.EnchantChallengePointsOptionInfo;
import org.l2jmobius.gameserver.data.xml.EnchantItemData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.model.item.enchant.EnchantScroll;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.item.type.CrystalType;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.enchant.challengepoint.ExEnchantChallengePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.enchant.challengepoint.ExSetEnchantChallengePoint;
import org.l2jmobius.gameserver.network.serverpackets.enchant.single.ExChangedEnchantTargetItemProbList;
import org.l2jmobius.gameserver.network.serverpackets.enchant.single.ExChangedEnchantTargetItemProbList.EnchantProbInfo;

/**
 * @author Serenitty
 */
public class ExRequestSetEnchantChallengePoint extends ClientPacket
{
	private int _useType;
	private boolean _useTicket;
	
	@Override
	protected void readImpl()
	{
		_useType = readInt();
		_useTicket = readBoolean();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final EnchantItemRequest request = player.getRequest(EnchantItemRequest.class);
		if ((request == null) || request.isProcessing())
		{
			player.sendPacket(new ExSetEnchantChallengePoint(false));
			return;
		}
		
		final Item item = request.getEnchantingItem();
		if (item == null)
		{
			player.sendPacket(new ExSetEnchantChallengePoint(false));
			return;
		}
		
		final EnchantChallengePointsItemInfo info = EnchantChallengePointData.getInstance().getInfoByItemId(item.getId());
		if (info == null)
		{
			player.sendPacket(new ExSetEnchantChallengePoint(false));
			return;
		}
		
		final int groupId = info.groupId();
		if (_useTicket)
		{
			final int remainingRecharges = player.getChallengeInfo().getChallengePointsRecharges(groupId, _useType);
			if (remainingRecharges > 0)
			{
				player.getChallengeInfo().setChallengePointsPendingRecharge(groupId, _useType);
				player.sendPacket(new ExSetEnchantChallengePoint(true));
				player.sendPacket(new ExEnchantChallengePointInfo(player));
			}
			else
			{
				player.sendPacket(new ExSetEnchantChallengePoint(false));
				return;
			}
		}
		else
		{
			final int remainingRecharges = player.getChallengeInfo().getChallengePointsRecharges(groupId, _useType);
			if (remainingRecharges < EnchantChallengePointData.getInstance().getMaxTicketCharge())
			{
				int remainingPoints = player.getChallengeInfo().getChallengePoints().getOrDefault(groupId, 0);
				final int fee = EnchantChallengePointData.getInstance().getFeeForOptionIndex(_useType);
				if (remainingPoints >= fee)
				{
					remainingPoints -= fee;
					player.getChallengeInfo().getChallengePoints().put(groupId, remainingPoints);
					player.getChallengeInfo().addChallengePointsRecharge(groupId, _useType, 1);
					player.getChallengeInfo().setChallengePointsPendingRecharge(groupId, _useType);
					player.sendPacket(new ExSetEnchantChallengePoint(true));
					player.sendPacket(new ExEnchantChallengePointInfo(player));
				}
				else
				{
					player.sendPacket(new ExSetEnchantChallengePoint(false));
					return;
				}
			}
		}
		
		final EnchantScroll scrollTemplate = EnchantItemData.getInstance().getEnchantScroll(request.getEnchantingScroll());
		final double chance = scrollTemplate.getChance(player, item);
		
		double challengePointsChance = 0;
		final int pendingGroupId = player.getChallengeInfo().getChallengePointsPendingRecharge()[0];
		final int pendingOptionIndex = player.getChallengeInfo().getChallengePointsPendingRecharge()[1];
		if ((pendingGroupId == groupId) && ((pendingOptionIndex == EnchantChallengePointData.OPTION_PROB_INC1) || (pendingOptionIndex == EnchantChallengePointData.OPTION_PROB_INC2)))
		{
			final EnchantChallengePointsOptionInfo optionInfo = EnchantChallengePointData.getInstance().getOptionInfo(pendingGroupId, pendingOptionIndex);
			if ((optionInfo != null) && (item.getEnchantLevel() >= optionInfo.minEnchant()) && (item.getEnchantLevel() <= optionInfo.maxEnchant()))
			{
				challengePointsChance = optionInfo.chance();
			}
		}
		
		final int crystalLevel = item.getTemplate().getCrystalType().getLevel();
		final double enchantRateStat = (crystalLevel > CrystalType.NONE.getLevel()) && (crystalLevel < CrystalType.EVENT.getLevel()) ? player.getStat().getValue(Stat.ENCHANT_RATE) : 0;
		
		player.sendPacket(new ExChangedEnchantTargetItemProbList(new EnchantProbInfo(item.getObjectId(), (int) ((chance + challengePointsChance + enchantRateStat) * 100), (int) (chance * 100), (int) (challengePointsChance * 100), (int) (enchantRateStat * 100))));
	}
}
