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
package org.l2jmobius.gameserver.network.serverpackets.commission;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.model.commission.CommissionItem;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.AbstractItemPacket;

/**
 * @author Mobius
 */
public class ExResponseCommissionList extends AbstractItemPacket
{
	public static final int MAX_CHUNK_SIZE = 120;
	
	private final CommissionListReplyType _replyType;
	private final List<CommissionItem> _items;
	private final int _chunkId;
	private final int _listIndexStart;
	
	public ExResponseCommissionList(CommissionListReplyType replyType)
	{
		this(replyType, Collections.emptyList(), 0);
	}
	
	public ExResponseCommissionList(CommissionListReplyType replyType, List<CommissionItem> items)
	{
		this(replyType, items, 0);
	}
	
	public ExResponseCommissionList(CommissionListReplyType replyType, List<CommissionItem> items, int chunkId)
	{
		this(replyType, items, chunkId, 0);
	}
	
	public ExResponseCommissionList(CommissionListReplyType replyType, List<CommissionItem> items, int chunkId, int listIndexStart)
	{
		_replyType = replyType;
		_items = items;
		_chunkId = chunkId;
		_listIndexStart = listIndexStart;
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.EX_RESPONSE_COMMISSION_LIST.writeId(this, buffer);
		buffer.writeInt(_replyType.getClientId());
		switch (_replyType)
		{
			case PLAYER_AUCTIONS:
			case AUCTIONS:
			{
				buffer.writeInt((int) Instant.now().getEpochSecond());
				buffer.writeInt(_chunkId);
				int chunkSize = _items.size() - _listIndexStart;
				if (chunkSize > MAX_CHUNK_SIZE)
				{
					chunkSize = MAX_CHUNK_SIZE;
				}
				
				buffer.writeInt(chunkSize);
				for (int i = _listIndexStart; i < (_listIndexStart + chunkSize); i++)
				{
					final CommissionItem commissionItem = _items.get(i);
					buffer.writeLong(commissionItem.getCommissionId());
					buffer.writeLong(commissionItem.getPricePerUnit());
					buffer.writeInt(0); // CommissionItemType seems client does not really need it.
					buffer.writeInt((commissionItem.getDurationInDays() - 1) / 2);
					buffer.writeInt((int) commissionItem.getEndTime().getEpochSecond());
					buffer.writeString(null); // Seller Name its not displayed somewhere so i am not sending it to decrease traffic.
					writeItem(commissionItem.getItemInfo(), buffer);
				}
				break;
			}
		}
	}
	
	public enum CommissionListReplyType
	{
		PLAYER_AUCTIONS_EMPTY(-2),
		ITEM_DOES_NOT_EXIST(-1),
		PLAYER_AUCTIONS(2),
		AUCTIONS(3);
		
		private final int _clientId;
		
		CommissionListReplyType(int clientId)
		{
			_clientId = clientId;
		}
		
		/**
		 * Gets the client id.
		 * @return the client id
		 */
		public int getClientId()
		{
			return _clientId;
		}
	}
}
