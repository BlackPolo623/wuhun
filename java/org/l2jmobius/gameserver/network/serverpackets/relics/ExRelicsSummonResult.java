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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.holders.RelicCouponHolder;
import org.l2jmobius.gameserver.data.holders.RelicSummonCategoryHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * @author Brado
 */
public class ExRelicsSummonResult extends ServerPacket
{
	public static final byte RESULT_ERROR = 0;
	public static final byte RESULT_SUCCESS = 1;
	public static final byte RESULT_OVER_100_COUPON_ERROR = 2;
	
	private final List<Integer> _relics = new ArrayList<>();
	
	private final byte _result;
	private final RelicCouponHolder _relicCoupon;
	private final RelicSummonCategoryHolder _summonCategoryHolder;
	
	public ExRelicsSummonResult(RelicCouponHolder relicCoupon, List<Integer> relics)
	{
		_result = RESULT_SUCCESS;
		_relicCoupon = relicCoupon;
		_summonCategoryHolder = null;
		_relics.addAll(relics);
	}
	
	public ExRelicsSummonResult(RelicSummonCategoryHolder holder, List<Integer> relics)
	{
		_result = RESULT_SUCCESS;
		_relicCoupon = null;
		_summonCategoryHolder = holder;
		_relics.addAll(relics);
	}
	
	public ExRelicsSummonResult(byte result)
	{
		_result = RESULT_SUCCESS;
		_relicCoupon = null;
		_summonCategoryHolder = null;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		if (_relics.isEmpty())
		{
			return;
		}
		
		ServerPackets.EX_RELICS_SUMMON_RESULT.writeId(this, buffer);
		buffer.writeByte(_result);
		buffer.writeInt(_relicCoupon == null ? _summonCategoryHolder.getCategoryId() : _relicCoupon.getItemId());
		buffer.writeInt(_relics.size());
		for (int obtainedRelicId : _relics)
		{
			buffer.writeInt(obtainedRelicId);
		}
	}
}
