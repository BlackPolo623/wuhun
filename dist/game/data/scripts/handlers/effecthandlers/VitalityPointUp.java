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
package handlers.effecthandlers;

import java.util.LinkedList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.effects.AbstractEffect;
import org.l2jmobius.gameserver.model.effects.EffectType;
import org.l2jmobius.gameserver.model.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.model.item.instance.Item;
import org.l2jmobius.gameserver.model.skill.Skill;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;

/**
 * Vitality Point Up effect implementation.
 * @author Adry_85, Mobius
 */
public class VitalityPointUp extends AbstractEffect
{
	private final int _value;
	
	public VitalityPointUp(StatSet params)
	{
		_value = params.getInt("value", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.VITALITY_POINT_UP;
	}
	
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && effected.isPlayer();
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		player.updateVitalityPoints(_value, false, false);
		
		final UserInfo ui = new UserInfo(player);
		ui.addComponentType(UserInfoType.VITA_FAME);
		player.sendPacket(ui);
		
		// Send item list to update vitality items with red icons in inventory.
		ThreadPool.schedule(() ->
		{
			final List<Item> items = new LinkedList<>();
			ITEMS: for (Item i : player.getInventory().getItems())
			{
				if (i.getTemplate().hasSkills())
				{
					for (ItemSkillHolder s : i.getTemplate().getAllSkills())
					{
						if (s.getSkill().hasEffectType(EffectType.VITALITY_POINT_UP))
						{
							items.add(i);
							continue ITEMS;
						}
					}
				}
			}
			
			if (!items.isEmpty())
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItems(items);
				player.sendInventoryUpdate(iu);
			}
		}, 1000);
	}
}
