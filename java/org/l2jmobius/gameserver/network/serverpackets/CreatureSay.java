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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.WritableBuffer;
import org.l2jmobius.gameserver.data.enums.CharacterStyleCategoryType;
import org.l2jmobius.gameserver.managers.MentorManager;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.managers.SharedTeleportManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * @author Mobius
 */
public class CreatureSay extends ServerPacket
{
	private final Creature _sender;
	private final ChatType _chatType;
	private String _senderName = null;
	private String _text = null;
	private int _charId = 0;
	private int _messageId = -1;
	private int _mask;
	private List<String> _parameters;
	private boolean _shareLocation;
	
	/**
	 * @param sender
	 * @param receiver
	 * @param name
	 * @param chatType
	 * @param text
	 */
	public CreatureSay(Player sender, Player receiver, String name, ChatType chatType, String text)
	{
		this(sender, receiver, name, chatType, text, false);
	}
	
	/**
	 * @param sender
	 * @param receiver
	 * @param name
	 * @param chatType
	 * @param text
	 * @param shareLocation
	 */
	public CreatureSay(Player sender, Player receiver, String name, ChatType chatType, String text, boolean shareLocation)
	{
		_sender = sender;
		_senderName = name;
		_chatType = chatType;
		_text = text;
		_shareLocation = shareLocation;
		if (receiver != null)
		{
			if (receiver.getFriendList().contains(sender.getObjectId()))
			{
				_mask |= 0x01;
			}
			
			if ((receiver.getClanId() > 0) && (receiver.getClanId() == sender.getClanId()))
			{
				_mask |= 0x02;
			}
			
			if ((MentorManager.getInstance().getMentee(receiver.getObjectId(), sender.getObjectId()) != null) || (MentorManager.getInstance().getMentee(sender.getObjectId(), receiver.getObjectId()) != null))
			{
				_mask |= 0x04;
			}
			
			if ((receiver.getAllyId() > 0) && (receiver.getAllyId() == sender.getAllyId()))
			{
				_mask |= 0x08;
			}
		}
		
		// Does not shows level
		if (sender.isGM())
		{
			_mask |= 0x10;
		}
	}
	
	public CreatureSay(Creature sender, ChatType chatType, String senderName, String text)
	{
		this(sender, chatType, senderName, text, false);
	}
	
	public CreatureSay(Creature sender, ChatType chatType, String senderName, String text, boolean shareLocation)
	{
		_sender = sender;
		_chatType = chatType;
		_senderName = senderName;
		_text = text;
		_shareLocation = shareLocation;
	}
	
	public CreatureSay(Creature sender, ChatType chatType, NpcStringId npcStringId)
	{
		_sender = sender;
		_chatType = chatType;
		_messageId = npcStringId.getId();
		if (sender != null)
		{
			_senderName = sender.getName();
		}
	}
	
	public CreatureSay(ChatType chatType, int charId, SystemMessageId systemMessageId)
	{
		_sender = null;
		_chatType = chatType;
		_charId = charId;
		_messageId = systemMessageId.getId();
	}
	
	/**
	 * String parameter for argument S1,S2,.. in npcstring-e.dat
	 * @param text
	 */
	public void addStringParameter(String text)
	{
		if (_parameters == null)
		{
			_parameters = new ArrayList<>();
		}
		
		_parameters.add(text);
	}
	
	@Override
	public void writeImpl(GameClient client, WritableBuffer buffer)
	{
		ServerPackets.SAY2.writeId(this, buffer);
		buffer.writeInt(_sender == null ? 0 : _sender.getObjectId());
		buffer.writeInt(_chatType.getClientId());
		if (_senderName != null)
		{
			buffer.writeString(_senderName);
		}
		else
		{
			buffer.writeInt(_charId);
		}
		
		buffer.writeInt(_messageId); // High Five NPCString ID
		if (_text != null)
		{
			buffer.writeString(_text);
			if ((_sender != null) && (_sender.isPlayer() || _sender.isFakePlayer()) && (_chatType == ChatType.WHISPER))
			{
				buffer.writeByte(_mask);
				if ((_mask & 0x10) == 0)
				{
					buffer.writeByte(_sender.getLevel());
				}
			}
		}
		else if (_parameters != null)
		{
			for (String s : _parameters)
			{
				buffer.writeString(s);
			}
		}
		
		// Rank
		if ((_sender != null) && _sender.isPlayer())
		{
			final Clan clan = _sender.getClan();
			if ((clan != null) && ((_chatType == ChatType.CLAN) || (_chatType == ChatType.ALLIANCE)))
			{
				buffer.writeByte(0); // unknown clan byte
			}
			
			final Player player = _sender.asPlayer();
			final int rank = RankManager.getInstance().getPlayerGlobalRank(player);
			if ((rank == 0) || (rank > 100))
			{
				buffer.writeByte(0);
			}
			else if (rank <= 10)
			{
				buffer.writeByte(1);
			}
			else if (rank <= 50)
			{
				buffer.writeByte(2);
			}
			else if (rank <= 100)
			{
				buffer.writeByte(3);
			}
			
			if (clan != null)
			{
				buffer.writeByte(clan.getCastleId());
			}
			else
			{
				buffer.writeByte(0);
			}
			
			if (_shareLocation)
			{
				buffer.writeByte(1);
				buffer.writeShort(SharedTeleportManager.getInstance().nextId(_sender));
			}
			else
			{
				buffer.writeInt(0);
			}
			
			// Scholar hat in front of name.
			buffer.writeByte(player.isMentee());
			
			// Chat background.
			buffer.writeInt(player.getVariables().getInt(PlayerVariables.ACTIVE_CHARACTER_STYLE + CharacterStyleCategoryType.CHAT_BACKGROUND, 0));
		}
		else
		{
			buffer.writeByte(0);
		}
	}
	
	@Override
	public void runImpl(Player player)
	{
		if (player != null)
		{
			player.broadcastSnoop(_chatType, _senderName, _text, this);
		}
	}
}
