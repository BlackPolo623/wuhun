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
package org.l2jmobius.gameserver.network.clientpackets.relics;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.data.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.xml.RelicCouponData;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.relics.RelicsProbList;

/**
 * @author Mobius, Brado
 */
public class RequestRelicsProbList extends ClientPacket
{
	private static final int TYPE_COUPON = 4;
	private static final int TYPE_SUMMON = 0;
	private static final int TYPE_COMBINE = 1;
	
	private int _type;
	private int _value;
	
	@Override
	protected void readImpl()
	{
		_type = readInt();
		_value = readInt(); // RelicGrade or ItemId
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || ((_type != 4) && (_value >= 7)))
		{
			return;
		}
		
		final Map<Integer, Long> relics = new LinkedHashMap<>();
		switch (_type)
		{
			case TYPE_COUPON:
			{
				final RelicCouponHolder coupon = RelicCouponData.getInstance().getCouponFromCouponItemId(_value);
				if (coupon == null)
				{
					return;
				}
				
				final Map<Integer, Long> possibleEntries = RelicCouponData.getInstance().getCachedChances(coupon.getItemId());
				for (Entry<Integer, Long> entry : possibleEntries.entrySet())
				{
					relics.put(entry.getKey(), entry.getValue());
				}
				break;
			}
			case TYPE_SUMMON:
			{
				for (RelicDataHolder relicHolder : RelicData.getInstance().getRelics().stream().filter(relic -> relic.getSummonChance() > 0).sorted(Comparator.comparingInt(RelicDataHolder::getGradeOrdinal).reversed()).collect(Collectors.toList()))
				{
					relics.put(relicHolder.getRelicId(), relicHolder.getSummonChance());
				}
				break;
			}
			case TYPE_COMBINE:
			{
				for (RelicDataHolder relicHolder : RelicData.getInstance().getRelicsByGrade(RelicGrade.values()[_value]))
				{
					relics.put(relicHolder.getRelicId(), RelicData.getInstance().calculateCompoundChance(relicHolder.getRelicId(), RelicGrade.values()[_value]));
				}
				
				if ((_value + 1) != RelicGrade.values().length)
				{
					for (RelicDataHolder relicHolder : RelicData.getInstance().getRelicsByGrade(RelicGrade.values()[_value + 1]))
					{
						relics.put(relicHolder.getRelicId(), RelicData.getInstance().calculateCompoundChance(relicHolder.getRelicId(), RelicGrade.values()[_value]));
					}
				}
				break;
			}
		}
		
		if (!relics.isEmpty())
		{
			player.sendPacket(new RelicsProbList(_type, _value, relics));
		}
	}
}
