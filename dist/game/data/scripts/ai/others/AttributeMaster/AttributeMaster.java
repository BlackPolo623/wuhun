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
package ai.others.AttributeMaster;

import java.util.Arrays;

import org.l2jmobius.gameserver.model.ElementalSpirit;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.events.EventDispatcher;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.player.OnElementalSpiritLearn;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ElementalSpiritInfo;
import org.l2jmobius.gameserver.network.serverpackets.elementalspirits.ExElementalSpiritAttackType;

/**
 * @author JoeAlisson
 */
public class AttributeMaster extends Script
{
	private static final int SVEIN = 34053;
	
	private AttributeMaster()
	{
		addStartNpc(SVEIN);
		addTalkId(SVEIN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("learn".equalsIgnoreCase(event))
		{
			if ((player.getLevel() < 76) || (player.getPlayerClass().level() < 3))
			{
				return "no-3rdClass.htm";
			}
			
			if (player.getSpirits() == null)
			{
				player.initElementalSpirits();
			}
			
			if (Arrays.stream(player.getSpirits()).allMatch(elementalSpirit -> elementalSpirit.getStage() > 0))
			{
				return "already.htm";
			}
			
			for (ElementalSpirit spirit : player.getSpirits())
			{
				if (spirit.getStage() == 0)
				{
					spirit.upgrade();
				}
			}
			
			final UserInfo userInfo = new UserInfo(player);
			userInfo.addComponentType(UserInfoType.ATT_SPIRITS);
			player.sendPacket(userInfo);
			player.sendPacket(new ElementalSpiritInfo(player, (byte) 0x01));
			player.sendPacket(new ExElementalSpiritAttackType(player));
			
			if (EventDispatcher.getInstance().hasListener(EventType.ON_ELEMENTAL_SPIRIT_LEARN, player))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnElementalSpiritLearn(player), player);
			}
			
			return "learn.htm";
		}
		
		return null;
	}
	
	public static void main(String[] args)
	{
		new AttributeMaster();
	}
}
