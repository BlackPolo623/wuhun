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
package ai.others.ClanStrongholdDevice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeNpcState;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * @author Index
 */
public class ClanStrongholdDevice extends Script
{
	// NPCs
	private static final int CLAN_STRONGHOLD_DEVICE = 34156;
	private static final int[] NEARBY_MONSTER_IDS =
	{
		22200, // Porta
		22201, // Excuro
		22202, // Mordeo
		22203, // Ricenseo
		22204, // Krator
		22205, // Catherok
		22206, // Premo
		22207, // Validus
		22208, // Dicor
		22209, // Perum
		22210, // Torfe
		22211, // Death Lord
	};
	
	// Skill
	private static final SkillHolder CLAN_STRONGHOLD_EFFECT = new SkillHolder(48078, 1);
	
	// Misc
	private static final Map<Integer, Integer> CURRENT_CLAN_ID = new ConcurrentHashMap<>(); // Clan id key - NPC object id value (can be taken from npc.getScriptValue)
	private static final Map<Integer, Long> LAST_ATTACK = new ConcurrentHashMap<>(); // NPC object id key - Time value
	private static final Map<Integer, Location> DEVICE_LOCATION = new ConcurrentHashMap<>();
	
	private ClanStrongholdDevice()
	{
		addCreatureSeeId(CLAN_STRONGHOLD_DEVICE);
		addFirstTalkId(CLAN_STRONGHOLD_DEVICE);
		addAttackId(CLAN_STRONGHOLD_DEVICE);
		addSpawnId(CLAN_STRONGHOLD_DEVICE);
		addTalkId(CLAN_STRONGHOLD_DEVICE);
		addKillId(CLAN_STRONGHOLD_DEVICE);
		addKillId(NEARBY_MONSTER_IDS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc.getTemplate().getId() != CLAN_STRONGHOLD_DEVICE) || (player == null) || (event == null))
		{
			return super.onEvent(event, npc, player);
		}
		
		if (event.equals("capture"))
		{
			if (npc.isAlikeDead())
			{
				return super.onEvent(event, npc, player);
			}
			
			if (CURRENT_CLAN_ID.containsKey(npc.getScriptValue()))
			{
				return "34156-02.htm";
			}
			
			final Clan clan = player.getClan();
			if (clan == null)
			{
				return "34156-03.htm";
			}
			
			CURRENT_CLAN_ID.put(player.getClanId(), npc.getObjectId());
			npc.setScriptValue(player.getClanId());
			npc.setTitle(clan.getName());
			npc.setClanId(player.getClanId());
			npc.setDisplayEffect(2);
			npc.setInvul(false);
			npc.broadcastInfo();
			return "34156-01.htm";
		}
		else if (event.equals("back"))
		{
			if (npc.isAlikeDead())
			{
				return super.onEvent(event, npc, player);
			}
			
			return npc.getId() + (CURRENT_CLAN_ID.containsKey(npc.getScriptValue()) ? "-02" : "") + ".htm";
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.isAlikeDead())
		{
			return super.onFirstTalk(npc, player);
		}
		
		return npc.getId() + (CURRENT_CLAN_ID.containsKey(npc.getScriptValue()) ? "-01" : "") + ".htm";
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (npc.getTemplate().getId() == CLAN_STRONGHOLD_DEVICE)
		{
			creature.sendPacket(new ExChangeNpcState(npc.getObjectId(), CURRENT_CLAN_ID.containsKey(npc.getScriptValue()) ? 1 : 2));
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getTemplate().getId() != CLAN_STRONGHOLD_DEVICE)
		{
			return;
		}
		
		npc.disableCoreAI(true);
		npc.setAutoAttackable(false);
		npc.setImmobilized(true);
		npc.setDisplayEffect(1);
		npc.setUndying(false);
		npc.setScriptValue(0);
		npc.setInvul(true);
		npc.setClanId(0);
		npc.setTitle("");
		npc.broadcastInfo();
		npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.THE_CLAN_STRONGHOLD_DEVICE_CAN_BE_CAPTURED, 2, 5000, true));
		DEVICE_LOCATION.put(npc.getObjectId(), npc.getLocation());
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (CURRENT_CLAN_ID.containsKey(npc.getScriptValue()) && (LAST_ATTACK.getOrDefault(npc.getObjectId(), 0L) < (System.currentTimeMillis() - 5000)))
		{
			npc.broadcastPacket(new NpcSay(npc, ChatType.NPC_GENERAL, NpcStringId.AT_TACK_SIG_NAL_DE_TEC_TED_S1).addStringParameter(attacker.getName()));
			LAST_ATTACK.put(npc.getObjectId(), System.currentTimeMillis());
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getTemplate().getId() == CLAN_STRONGHOLD_DEVICE)
		{
			npc.setClanId(0);
			CURRENT_CLAN_ID.remove(npc.getScriptValue());
			LAST_ATTACK.remove(npc.getObjectId());
			DEVICE_LOCATION.remove(npc.getObjectId());
			return;
		}
		
		if (!CURRENT_CLAN_ID.containsKey(killer.getClanId()))
		{
			return;
		}
		
		CLAN_STRONGHOLD_EFFECT.getSkill().activateSkill(npc, killer);
		for (Player clanMate : World.getInstance().getVisibleObjects(killer, Player.class))
		{
			if (clanMate.getClanId() != killer.getClanId())
			{
				continue;
			}
			
			final Location deviceLocation = DEVICE_LOCATION.get(CURRENT_CLAN_ID.get(killer.getClanId()));
			if ((clanMate.calculateDistance2D(deviceLocation) < 900) && (Math.abs(clanMate.getZ() - deviceLocation.getZ()) < 200))
			{
				clanMate.doCast(CLAN_STRONGHOLD_EFFECT.getSkill());
			}
		}
	}
	
	public static void main(String[] args)
	{
		new ClanStrongholdDevice();
	}
}
