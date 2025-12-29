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
package instances.KelbimFortress;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.model.KeyValuePair;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.BuffInfo;
import org.l2jmobius.gameserver.model.skill.SkillCaster;

/**
 * @author Index
 */
public class KelbimFortressNpcAction extends Script
{
	private static final Map<Integer, KeyValuePair<String, String>> ON_ACTION_DIALOGUE = new HashMap<>();
	static
	{
		ON_ACTION_DIALOGUE.put(34377, new KeyValuePair<>("34377-no.htm", "34377.htm"));
		ON_ACTION_DIALOGUE.put(18828, new KeyValuePair<>("18828-no.htm", "18828.htm"));
		ON_ACTION_DIALOGUE.put(18829, new KeyValuePair<>("18829-no.htm", "18829.htm"));
	}
	
	private static final Map<Integer, KeyValuePair<String, String>> ON_KELBIM_STATUS = new HashMap<>();
	static
	{
		ON_KELBIM_STATUS.put(18828, new KeyValuePair<>("18828.htm", "18828-ex.htm"));
		ON_KELBIM_STATUS.put(18829, new KeyValuePair<>("18829.htm", "18829-ex.htm"));
	}
	
	private static final int OVERLOAD_SKILL_ID = 48690;
	private static final Map<Integer, Long> OVERLOAD_FEE_ON_TELEPORT = new HashMap<>(50);
	static
	{
		for (int index = 0; index <= 40; index++)
		{
			// First teleport is free.
			// Next will take fee in 2_500_000 adena up to 10_000_000.
			OVERLOAD_FEE_ON_TELEPORT.put(index, ((index == 0 ? 0 : 2_500_000) + (((10_000_000L - 2_500_000) / 50) * index)));
		}
	}
	
	private static final AtomicInteger DUMMY_ATOMIC_VALUE = new AtomicInteger(-1);
	
	private static final Map<String, Integer> AVAILABLE_OPTIONS_TO_TELEPORT = new HashMap<>(6);
	static
	{
		AVAILABLE_OPTIONS_TO_TELEPORT.put("TELEPORT_ME_TO 0", 0);
		AVAILABLE_OPTIONS_TO_TELEPORT.put("TELEPORT_ME_TO 1", 1);
		AVAILABLE_OPTIONS_TO_TELEPORT.put("TELEPORT_ME_TO 2", 2);
		AVAILABLE_OPTIONS_TO_TELEPORT.put("TELEPORT_ME_TO 3", 3);
		AVAILABLE_OPTIONS_TO_TELEPORT.put("TELEPORT_ME_TO 4", 4);
		AVAILABLE_OPTIONS_TO_TELEPORT.put("TELEPORT_ME_TO 5", 5);
	}
	
	private static final List<Entry<Long, Location>> TELEPORT_LOCATIONS_18828 = new ArrayList<>();
	static
	{
		TELEPORT_LOCATIONS_18828.add(new SimpleEntry<>(100000L, new Location(-44440, 45816, -1574)));
		TELEPORT_LOCATIONS_18828.add(new SimpleEntry<>(200000L, new Location(-52264, 47672, -1578)));
		TELEPORT_LOCATIONS_18828.add(new SimpleEntry<>(500000L, new Location(-55069, 52357, -2164)));
	}
	
	private static final Location[] TELEPORT_LOCATIONS_18829 =
	{
		new Location(-54707, 60038, -269),
		new Location(-55374, 59126, -269),
		new Location(-56814, 59018, -269),
	};
	
	public KelbimFortressNpcAction()
	{
		addFirstTalkId(ON_ACTION_DIALOGUE.keySet());
		addTalkId(ON_KELBIM_STATUS.keySet());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final Instance world = (player == null) || (npc == null) ? null : player.getInstanceWorld();
		if ((event == null) || (npc == null) || (world == null) || (world.getTemplateId() != KelbimFortressManager.INSTANCE_TEMPLATE_ID))
		{
			return super.onEvent(event, npc, player);
		}
		
		if (!KelbimFortressManager.isAvailableToEnter(player))
		{
			world.ejectPlayer(player);
			return ON_KELBIM_STATUS.get(npc.getId()).getKey();
		}
		
		final AtomicInteger id = AVAILABLE_OPTIONS_TO_TELEPORT.getOrDefault(event, -1) == -1 ? DUMMY_ATOMIC_VALUE : new AtomicInteger(AVAILABLE_OPTIONS_TO_TELEPORT.getOrDefault(event, -1));
		final AtomicBoolean isKelbimTeleport = new AtomicBoolean(false);
		if ((npc.getId() == 18828) && event.startsWith("TELEPORT_ME_TO"))
		{
			final String html = handle18828(world, event, npc, player, id, isKelbimTeleport);
			if (!isKelbimTeleport.get())
			{
				return html;
			}
		}
		
		if (isKelbimTeleport.get() || ((npc.getId() == 18829) && event.startsWith("TELEPORT_ME_TO")))
		{
			return handle18829(world, event, npc, player, id, isKelbimTeleport);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	private String handle18828(Instance world, String event, Npc npc, Player player, AtomicInteger id, AtomicBoolean isKelbimTeleport)
	{
		if ((id.get() == -1) || ((id.get() >= 3) && (world.getStatus() < KelbimFortressManager.KELBIM_SECOND)))
		{
			world.ejectPlayer(player);
			return ON_KELBIM_STATUS.get(npc.getId()).getKey();
		}
		
		if (id.get() >= 3)
		{
			id.set(id.get() - 3);
			isKelbimTeleport.set(true);
		}
		else if ((player.getAdena() > TELEPORT_LOCATIONS_18828.get(id.get()).getKey()) && player.reduceAdena(ItemProcessType.FEE, TELEPORT_LOCATIONS_18828.get(id.get()).getKey(), npc, true))
		{
			player.teleToLocation(TELEPORT_LOCATIONS_18828.get(id.get()).getValue(), false, world);
		}
		
		return null;
	}
	
	private String handle18829(Instance world, String event, Npc npc, Player player, AtomicInteger id, AtomicBoolean isKelbimTeleport)
	{
		if ((id.get() == -1) || (world.getStatus() < KelbimFortressManager.KELBIM_SECOND))
		{
			world.ejectPlayer(player);
			return ON_KELBIM_STATUS.get(npc.getId()).getKey();
		}
		
		final BuffInfo overloadBuffEffect = player.getEffectList().getBuffInfoBySkillId(OVERLOAD_SKILL_ID);
		int overloadSkillLevel = overloadBuffEffect == null ? 0 : overloadBuffEffect.getSkill().getLevel();
		if ((player.getAdena() > OVERLOAD_FEE_ON_TELEPORT.get(overloadSkillLevel)) && player.reduceAdena(ItemProcessType.FEE, OVERLOAD_FEE_ON_TELEPORT.get(overloadSkillLevel), npc, true))
		{
			SkillCaster.triggerCast(npc, player, SkillData.getInstance().getSkill(OVERLOAD_SKILL_ID, Math.min(overloadSkillLevel + 1, 50)), null, true);
			player.teleToLocation(TELEPORT_LOCATIONS_18829[id.get()], false, world);
		}
		
		return null;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = (player == null) || (npc == null) ? null : player.getInstanceWorld();
		if ((npc == null) || (world == null) || (world.getTemplateId() != KelbimFortressManager.INSTANCE_TEMPLATE_ID))
		{
			return super.onFirstTalk(npc, player);
		}
		
		return KelbimFortressManager.isAvailableToEnter(player) ? (world.getStatus() >= KelbimFortressManager.KELBIM_SECOND) && ON_KELBIM_STATUS.containsKey(npc.getId()) ? ON_KELBIM_STATUS.get(npc.getId()).getValue() : ON_ACTION_DIALOGUE.get(npc.getId()).getValue() : ON_ACTION_DIALOGUE.get(npc.getId()).getKey();
	}
	
	public static void main(String[] args)
	{
		new KelbimFortressNpcAction();
	}
}
