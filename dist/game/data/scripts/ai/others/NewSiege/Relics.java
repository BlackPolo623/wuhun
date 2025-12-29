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
package ai.others.NewSiege;

import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.zone.type.SiegeZone;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

public class Relics extends Script
{
	// NPCs
	private static final int RELIC_GIRAN = 36792;
	private static final int RELIC_GODDART = 36793;
	private static final int ARTIFACT_GIRAN = 35147;
	private static final int ARTIFACT_GODDART_1 = 35323;
	private static final int ARTIFACT_GODDART_2 = 35322;
	
	// Zone
	private static final SiegeZone GIRAN_SIEGE_ZONE = ZoneManager.getInstance().getZoneByName("giran_castle_battlefield_territory", SiegeZone.class);
	
	public Relics()
	{
		addKillId(RELIC_GIRAN, RELIC_GODDART);
		addSpawnId(ARTIFACT_GIRAN, ARTIFACT_GODDART_1, ARTIFACT_GODDART_2);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setDecayed(false);
		npc.disableCoreAI(true);
		npc.setImmobilized(true);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == RELIC_GIRAN)
		{
			GIRAN_SIEGE_ZONE.broadcastPacket(new ExShowScreenMessage(NpcStringId.GIRAN_HOLY_ARTIFACT_HAS_APPEARED, ExShowScreenMessage.TOP_CENTER, 10000, true));
			addSpawn(ARTIFACT_GIRAN, new Location(113926, 145088, -2760), false, 60 * 60 * 1000); // 1 hour
		}
		else
		{
			addSpawn(ARTIFACT_GODDART_1, new Location(145288, -45385, -1727), false, 60 * 60 * 1000); // 1 hour
			addSpawn(ARTIFACT_GODDART_2, new Location(149652, -45371, -1727), false, 60 * 60 * 1000); // 1 hour
			GIRAN_SIEGE_ZONE.broadcastPacket(new ExShowScreenMessage(NpcStringId.GIRAN_HOLY_ARTIFACT_HAS_APPEARED, ExShowScreenMessage.TOP_CENTER, 10000, true));
		}
	}
	
	public static void main(String[] args)
	{
		new Relics();
	}
}
