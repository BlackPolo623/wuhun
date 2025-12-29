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
package instances.ValakasTemple.Teleporter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.groups.CommandChannel;
import org.l2jmobius.gameserver.model.script.InstanceScript;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import instances.ValakasTemple.ValakasTemple;

/**
 * @author Index
 */
public class ValakasTempleTeleport extends InstanceScript
{
	public static final int PARME_NPC_ID = 34258;
	private static final Map<Integer, Set<Player>> PLAYER_TO_LOGIN = new HashMap<>();
	
	private ValakasTempleTeleport()
	{
		super(ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID);
		addTalkId(PARME_NPC_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "VALAKAS_TEMPLE_ENTER_HTML":
			{
				return PARME_NPC_ID + "-01.htm";
			}
			case "VALAKAS_TEMPLE_SHOW_INFO":
			{
				return PARME_NPC_ID + "-02.htm";
			}
			case "VALAKAS_TEMPLE_SHOW_DEFAULT_PAGE":
			{
				return getHtm(player, "data/html/default/" + PARME_NPC_ID + ".htm");
			}
			case "VALAKAS_TEMPLE_ENTER_TO_INSTANCE":
			{
				if (checkRequirementsForEnter(player))
				{
					return PARME_NPC_ID + "-no_cc.htm";
				}
				
				PLAYER_TO_LOGIN.get(player.getObjectId()).forEach(p -> enterInstance(p, npc, ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID));
				PLAYER_TO_LOGIN.remove(player.getObjectId());
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	private static boolean checkRequirementsForEnter(Player requestor)
	{
		if (requestor.isGM())
		{
			PLAYER_TO_LOGIN.put(requestor.getObjectId(), new HashSet<>());
			PLAYER_TO_LOGIN.get(requestor.getObjectId()).addAll(requestor.isInParty() ? requestor.getParty().getMembers() : requestor.isInCommandChannel() ? requestor.getCommandChannel().getMembers() : Set.of(requestor));
		}
		else
		{
			if (!requestor.isInCommandChannel())
			{
				requestor.sendPacket(SystemMessageId.COMMAND_CHANNEL_INQUIRY);
				return true;
			}
			
			final CommandChannel currentChannel = requestor.getCommandChannel();
			if ((currentChannel.getMemberCount() < 15) || (currentChannel.getMemberCount() > 100))
			{
				currentChannel.getMembers().forEach(p -> p.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_DUE_TO_THE_PARTY_HAVING_EXCEEDED_THE_LIMIT));
				return true;
			}
			
			for (Player player : currentChannel.getMembers())
			{
				final SystemMessage sm = checkInstanceStatus(player);
				if (sm != null)
				{
					currentChannel.getMembers().forEach(p -> p.sendPacket(sm));
					return true;
				}
			}
			
			PLAYER_TO_LOGIN.put(requestor.getObjectId(), new HashSet<>());
			PLAYER_TO_LOGIN.get(requestor.getObjectId()).addAll(currentChannel.getMembers());
		}
		
		return false;
	}
	
	public static SystemMessage checkInstanceStatus(Player player)
	{
		if (player.getLevel() < 76)
		{
			return new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_LEVEL_REQUIREMENTS_AND_CANNOT_ENTER).addString(player.getName());
		}
		
		final long currentTime = System.currentTimeMillis();
		if (currentTime < InstanceManager.getInstance().getInstanceTime(player, ValakasTemple.VALAKAS_TEMPLE_INSTANCE_ID))
		{
			return new SystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET).addString(player.getName());
		}
		
		if (InstanceManager.getInstance().getPlayerInstance(player, true) != null)
		{
			return new SystemMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_C1_IS_IN_ANOTHER_INSTANCE_ZONE).addString(player.getName());
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new ValakasTempleTeleport();
	}
}
