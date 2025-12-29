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
package ai.areas.LairOfAntharas.Tarr;

import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.zone.ZoneId;

/**
 * @author Mobius
 */
public class Tarr extends Script
{
	// NPC
	private static final int TARR = 34161;
	
	private Tarr()
	{
		addStartNpc(TARR);
		addTalkId(TARR);
		addFirstTalkId(TARR);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return getHtm(player, "34161.htm").replace("%bridge%", npc.isInsideZone(ZoneId.PEACE) ? "<Button ALIGN=LEFT ICON=\"TELEPORT\" action=\"bypass -h npc_%objectId%_teleport OTHER 2\">Bridge - 50,000 adena</Button>" : "");
	}
	
	public static void main(String[] args)
	{
		new Tarr();
	}
}
