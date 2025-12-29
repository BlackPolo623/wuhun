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
package handlers.chathandlers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Universal (VIP) chat handler.
 * @author Griunvaldas
 */
public class ChatUniversal implements IChatHandler
{
	private static final Map<Integer, Instant> REUSE = new ConcurrentHashMap<>();
	
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.UNIVERSAL,
	};
	
	@Override
	public void onChat(ChatType type, Player activeChar, String target, String text, boolean shareLocation)
	{
		if (!activeChar.hasPremiumStatus())
		{
			activeChar.sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_L_COINS);
			return;
		}
		
		final Instant now = Instant.now();
		if (!REUSE.isEmpty())
		{
			REUSE.values().removeIf(now::isAfter);
		}
		
		if (GeneralConfig.JAIL_DISABLE_CHAT && activeChar.isJailed() && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
		}
		else if (shareLocation && (activeChar.getInventory().getInventoryItemCount(Inventory.LCOIN_ID, -1) < GeneralConfig.SHARING_LOCATION_COST))
		{
			activeChar.sendPacket(SystemMessageId.THERE_ARE_NOT_ENOUGH_L_COINS);
		}
		else if (shareLocation && ((activeChar.getMovieHolder() != null) || activeChar.isFishing() || activeChar.isInInstance() || activeChar.isOnEvent() || activeChar.isInOlympiadMode() || activeChar.inObserverMode() || activeChar.isInTraingCamp() || activeChar.isInTimedHuntingZone() || activeChar.isInsideZone(ZoneId.SIEGE)))
		{
			activeChar.sendPacket(SystemMessageId.LOCATION_CANNOT_BE_SHARED_SINCE_THE_CONDITIONS_ARE_NOT_MET);
		}
		else
		{
			// Verify if player is not spaming.
			if (GeneralConfig.WORLD_CHAT_INTERVAL.getSeconds() > 0)
			{
				final Instant instant = REUSE.getOrDefault(activeChar.getObjectId(), null);
				if ((instant != null) && instant.isAfter(now) && !activeChar.getAccessLevel().isGm())
				{
					final Duration timeDiff = Duration.between(now, instant);
					final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_S1_SEC_UNTIL_YOU_ARE_ABLE_TO_USE_WORLD_CHAT);
					msg.addInt((int) timeDiff.getSeconds());
					activeChar.sendPacket(msg);
					return;
				}
			}
			
			if (shareLocation)
			{
				activeChar.destroyItemByItemId(ItemProcessType.FEE, Inventory.LCOIN_ID, GeneralConfig.SHARING_LOCATION_COST, activeChar, true);
			}
			
			final CreatureSay cs = new CreatureSay(activeChar, type, activeChar.getName(), text, shareLocation);
			for (Player player : World.getInstance().getPlayers())
			{
				if (activeChar.isNotBlocked(player))
				{
					player.sendPacket(cs);
				}
			}
			
			if (GeneralConfig.WORLD_CHAT_INTERVAL.getSeconds() > 0)
			{
				REUSE.put(activeChar.getObjectId(), now.plus(GeneralConfig.WORLD_CHAT_INTERVAL));
			}
		}
	}
	
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
