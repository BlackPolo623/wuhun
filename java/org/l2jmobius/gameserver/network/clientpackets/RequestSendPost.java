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
package org.l2jmobius.gameserver.network.clientpackets;

import static org.l2jmobius.gameserver.model.itemcontainer.Inventory.ADENA_ID;
import static org.l2jmobius.gameserver.model.itemcontainer.Inventory.MAX_ADENA;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.CharInfoTable;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.model.AccessLevel;
import org.l2jmobius.gameserver.model.BlockList;
import org.l2jmobius.gameserver.model.Message;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.itemcontainer.Mail;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExNoticePostSent;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Migi, DS, Mobius
 */
public class RequestSendPost extends ClientPacket
{
	private static final int BATCH_LENGTH = 12; // length of the one item
	
	private static final int MAX_RECV_LENGTH = 16;
	private static final int MAX_SUBJ_LENGTH = 128;
	private static final int MAX_TEXT_LENGTH = 512;
	private static final int MAX_ATTACHMENTS = 8;
	private static final int INBOX_SIZE = 240;
	private static final int OUTBOX_SIZE = 240;
	
	private static final int MESSAGE_FEE = 100;
	private static final int MESSAGE_FEE_PER_SLOT = 1000; // 100 adena message fee + 1000 per each item slot
	
	private String _receiver;
	private boolean _isCod;
	private String _subject;
	private String _text;
	private AttachmentItem[] _items = null;
	private long _reqAdena;
	
	@Override
	protected void readImpl()
	{
		_receiver = readString();
		_isCod = readInt() != 0;
		_subject = readString();
		_text = readString();
		
		final int attachCount = readInt();
		if ((attachCount < 0) || (attachCount > PlayerConfig.MAX_ITEM_IN_PACKET) || (((attachCount * BATCH_LENGTH) + 8) != remaining()))
		{
			return;
		}
		
		if (attachCount > 0)
		{
			_items = new AttachmentItem[attachCount];
			for (int i = 0; i < attachCount; i++)
			{
				final int objectId = readInt();
				final long count = readLong();
				if ((objectId < 1) || (count < 1))
				{
					_items = null;
					return;
				}
				
				_items[i] = new AttachmentItem(objectId, count);
			}
		}
		
		_reqAdena = readLong();
		if (_reqAdena < 0)
		{
			_items = null;
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (!GeneralConfig.ALLOW_MAIL)
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!GeneralConfig.ALLOW_ATTACHMENTS)
		{
			_items = null;
			_isCod = false;
			_reqAdena = 0;
		}
		
		if (!player.getAccessLevel().allowTransaction())
		{
			player.sendMessage("Transactions are disabled for your Access Level.");
			return;
		}
		
		if (player.isInCombat() && (_items != null))
		{
			player.sendPacket(SystemMessageId.NOT_AVAILABLE_IN_COMBAT);
			return;
		}
		
		if (player.isDead() && (_items != null))
		{
			player.sendPacket(SystemMessageId.YOU_ARE_DEAD_AND_CANNOT_PERFORM_THIS_ACTION);
			return;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SEND_ANYTHING_WHILE_TRADING);
			return;
		}
		
		if (player.isInventoryDisabled())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SEND_ANYTHING_WHILE_TRADING);
			return;
		}
		
		if (player.hasItemRequest())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SEND_ANYTHING_WHILE_ENCHANTING_IMBUING_WITH_ATTRIBUTES_OR_COMPOUNDING_WITH_JEWELS);
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SEND_ANYTHING_WITH_THE_PRIVATE_STORE_OR_WORKSHOP_OPEN);
			return;
		}
		
		if (_receiver.length() > MAX_RECV_LENGTH)
		{
			player.sendPacket(SystemMessageId.THE_ALLOWED_LENGTH_FOR_RECIPIENT_EXCEEDED);
			return;
		}
		
		if (_subject.length() > MAX_SUBJ_LENGTH)
		{
			player.sendPacket(SystemMessageId.THE_ALLOWED_LENGTH_FOR_A_TITLE_EXCEEDED);
			return;
		}
		
		if (_text.length() > MAX_TEXT_LENGTH)
		{
			// not found message for this
			player.sendPacket(SystemMessageId.THE_ALLOWED_LENGTH_FOR_A_TITLE_EXCEEDED);
			return;
		}
		
		if ((_items != null) && (_items.length > MAX_ATTACHMENTS))
		{
			player.sendPacket(SystemMessageId.ITEM_SELECTION_IS_POSSIBLE_UP_TO_8);
			return;
		}
		
		if ((_reqAdena < 0) || (_reqAdena > MAX_ADENA))
		{
			return;
		}
		
		if (_isCod)
		{
			if (_reqAdena == 0)
			{
				player.sendPacket(SystemMessageId.MIN_PAYABLE_AMOUNT_S1_ADENA);
				return;
			}
			
			if ((_items == null) || (_items.length == 0))
			{
				player.sendPacket(SystemMessageId.ATTACH_AN_ITEM_TO_SEND_IT_BY_PAID_MAIL);
				return;
			}
		}
		
		if (FakePlayerData.getInstance().isTalkable(_receiver))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BLOCKED_YOU_YOU_CANNOT_SEND_MAIL_TO_THIS_CHARACTER);
			sm.addString(FakePlayerData.getInstance().getProperName(_receiver));
			player.sendPacket(sm);
			return;
		}
		
		final int receiverId = CharInfoTable.getInstance().getIdByName(_receiver);
		if (receiverId <= 0)
		{
			player.sendPacket(SystemMessageId.A_LETTER_WILL_NOT_BE_DELIVERED_IF_THE_RECIPIENT_S_NAME_IS_NOT_FOUND_OR_THE_CHARACTER_HAS_BEEN_DELETED);
			return;
		}
		
		if (receiverId == player.getObjectId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SEND_A_MAIL_TO_YOURSELF);
			return;
		}
		
		final int level = CharInfoTable.getInstance().getAccessLevelById(receiverId);
		final AccessLevel accessLevel = AdminData.getInstance().getAccessLevel(level);
		if ((accessLevel != null) && accessLevel.isGm() && !player.getAccessLevel().isGm())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_MESSAGE_TO_C1_DID_NOT_REACH_ITS_RECIPIENT_YOU_CANNOT_SEND_MAIL_TO_THE_GM_STAFF);
			sm.addString(_receiver);
			player.sendPacket(sm);
			return;
		}
		
		if (player.isJailed() && ((GeneralConfig.JAIL_DISABLE_TRANSACTION && (_items != null)) || GeneralConfig.JAIL_DISABLE_CHAT))
		{
			player.sendPacket(SystemMessageId.YOU_CAN_ONLY_SEND_MAIL_WHEN_IN_A_PEACEFUL_ZONE);
			return;
		}
		
		if (BlockList.isInBlockList(receiverId, player.getObjectId()))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_BLOCKED_YOU_YOU_CANNOT_SEND_MAIL_TO_THIS_CHARACTER);
			sm.addString(_receiver);
			player.sendPacket(sm);
			return;
		}
		
		if (MailManager.getInstance().getOutboxSize(player.getObjectId()) >= OUTBOX_SIZE)
		{
			player.sendPacket(SystemMessageId.THE_MAIL_LIMIT_240_HAS_BEEN_EXCEEDED_AND_THIS_CANNOT_BE_FORWARDED);
			return;
		}
		
		if (MailManager.getInstance().getInboxSize(receiverId) >= INBOX_SIZE)
		{
			player.sendPacket(SystemMessageId.THE_MAIL_LIMIT_240_HAS_BEEN_EXCEEDED_AND_THIS_CANNOT_BE_FORWARDED);
			return;
		}
		
		if (!getClient().getFloodProtectors().canSendMail())
		{
			player.sendPacket(SystemMessageId.THE_PREVIOUS_MAIL_WAS_SENT_LESS_THAN_10_SEC_AGO_WAIT_A_BIT_AND_TRY_AGAIN);
			return;
		}
		
		final Message msg = new Message(player.getObjectId(), receiverId, _isCod, _subject, _text, _reqAdena);
		if (removeItems(player, msg))
		{
			player.setMultiSell(null); // Should not trade during mail.
			
			MailManager.getInstance().sendMessage(msg);
			player.sendPacket(ExNoticePostSent.valueOf(true));
			player.sendPacket(SystemMessageId.THE_MAIL_IS_SENT_2);
		}
	}
	
	private boolean removeItems(Player player, Message msg)
	{
		long currentAdena = player.getAdena();
		long fee = MESSAGE_FEE;
		if (_items != null)
		{
			for (AttachmentItem i : _items)
			{
				// Check validity of requested item
				final Item item = player.checkItemManipulation(i.getObjectId(), i.getCount(), "attach");
				if ((item == null) || !item.isTradeable() || item.isEquipped())
				{
					player.sendPacket(SystemMessageId.THE_ITEM_THAT_YOU_ARE_TRYING_TO_SEND_DOES_NOT_MEET_THE_REQUIREMENTS);
					return false;
				}
				
				fee += MESSAGE_FEE_PER_SLOT;
				if (item.getId() == ADENA_ID)
				{
					currentAdena -= i.getCount();
				}
			}
		}
		
		// Check if enough adena and charge the fee
		if ((currentAdena < fee) || !player.reduceAdena(ItemProcessType.FEE, fee, null, false))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SEND_ANYTHING_AS_YOU_DO_NOT_HAVE_ENOUGH_MONEY);
			return false;
		}
		
		if (_items == null)
		{
			return true;
		}
		
		final Mail attachments = msg.createAttachments();
		
		// message already has attachments ? oO
		if (attachments == null)
		{
			return false;
		}
		
		// Proceed to the transfer
		final InventoryUpdate playerIU = new InventoryUpdate();
		for (AttachmentItem i : _items)
		{
			// Check validity of requested item
			final Item oldItem = player.checkItemManipulation(i.getObjectId(), i.getCount(), "attach");
			if ((oldItem == null) || !oldItem.isTradeable() || oldItem.isEquipped())
			{
				PacketLogger.warning("Error adding attachment for char " + player.getName() + " (olditem == null)");
				return false;
			}
			
			final Item newItem = player.getInventory().transferItem(ItemProcessType.TRANSFER, i.getObjectId(), i.getCount(), attachments, player, msg.getReceiverName() + "[" + msg.getReceiverId() + "]");
			if (newItem == null)
			{
				PacketLogger.warning("Error adding attachment for char " + player.getName() + " (newitem == null)");
				continue;
			}
			
			newItem.setItemLocation(newItem.getItemLocation(), msg.getId());
			
			if ((oldItem.getCount() > 0) && (oldItem != newItem))
			{
				playerIU.addModifiedItem(oldItem);
			}
			else
			{
				playerIU.addRemovedItem(oldItem);
			}
		}
		
		// Send updated item list to the player
		player.sendInventoryUpdate(playerIU);
		
		return true;
	}
	
	private static class AttachmentItem
	{
		private final int _objectId;
		private final long _count;
		
		public AttachmentItem(int id, long num)
		{
			_objectId = id;
			_count = num;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public long getCount()
		{
			return _count;
		}
	}
}
