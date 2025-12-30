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
package ai.areas.Giran.EvasAvatar;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.skill.SkillCaster;
import org.l2jmobius.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Liamxroy
 */
public class EvasAvatar extends Script
{
	// NPC
	private static final int EVAS_AVATAR = 34455;
	
	// Skills
	private static final SkillHolder[] BLESSING_OF_WATER =
	{
		new SkillHolder(48934, 1),
		new SkillHolder(48934, 2),
		new SkillHolder(48934, 3),
		new SkillHolder(48934, 4),
		new SkillHolder(48934, 5),
		new SkillHolder(48934, 6),
	};
	
	// Items
	private static final int WONDROUS = 100884; // Wondrous Water of Life (Time-limited) Sealed
	private static final int TREE_LEAF = 100886; // Young Mother Tree's Leaf (Time-limited) Sealed
	
	// Misc
	private static final double TREE_LEAF_PROBABILITY = 5;
	
	private EvasAvatar()
	{
		addStartNpc(EVAS_AVATAR);
		addFirstTalkId(EVAS_AVATAR);
		addTalkId(EVAS_AVATAR);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("giveBuff"))
		{
			if (!hasQuestItems(player, WONDROUS))
			{
				return "34455-01.html";
			}
			
			takeItems(player, WONDROUS, 1);
			
			SkillCaster.triggerCast(npc, player, getBuffForDay().getSkill());
			
			// Buff players around
			World.getInstance().forEachVisibleObjectInRange(npc, Player.class, 50000, target ->
			{
				if ((target != player) && (target != null) && !target.isDead() && GeoEngine.getInstance().canSeeTarget(npc, target))
				{
					getBuffForDay().getSkill().applyEffects(npc, target);
				}
			});
			
			// Check probability for receiving Young Mother Tree's Leaf.
			if (getRandom(100) < TREE_LEAF_PROBABILITY)
			{
				giveItems(player, TREE_LEAF, 1);
			}
		}
		
		return null;
	}
	
	private SkillHolder getBuffForDay()
	{
		final GregorianCalendar date = new GregorianCalendar();
		final int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
		switch (dayOfWeek)
		{
			case Calendar.MONDAY:
			{
				return BLESSING_OF_WATER[0];
			}
			case Calendar.TUESDAY:
			{
				return BLESSING_OF_WATER[1];
			}
			case Calendar.WEDNESDAY:
			{
				return BLESSING_OF_WATER[2];
			}
			case Calendar.THURSDAY:
			{
				return BLESSING_OF_WATER[3];
			}
			case Calendar.FRIDAY:
			{
				return BLESSING_OF_WATER[4];
			}
			case Calendar.SATURDAY:
			case Calendar.SUNDAY:
			{
				return BLESSING_OF_WATER[5];
			}
			default:
			{
				return BLESSING_OF_WATER[0];
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	public static void main(String[] args)
	{
		new EvasAvatar();
	}
}
