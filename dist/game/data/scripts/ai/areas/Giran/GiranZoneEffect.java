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
package ai.areas.Giran;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.ListenerRegisterType;
import org.l2jmobius.gameserver.model.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.model.events.annotations.RegisterType;
import org.l2jmobius.gameserver.model.events.holders.OnDayNightChange;
import org.l2jmobius.gameserver.model.script.Script;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeClientEffectInfo;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * @author Mobius
 */
public class GiranZoneEffect extends Script
{
	private static final ZoneType GIRAN_ZONE = ZoneManager.getInstance().getZoneById(11020);
	private static final ZoneType GIRAN_SQUARE_WATER_ZONE = ZoneManager.getInstance().getZoneByName("Giran Square Water Zone");
	
	private static boolean _isNight = GameTimeTaskManager.getInstance().isNight();
	
	private GiranZoneEffect()
	{
		addEnterZoneId(GIRAN_ZONE.getId());
		changeWaterZoneStatus();
	}
	
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		sendEffect(creature);
	}
	
	@RegisterEvent(EventType.ON_DAY_NIGHT_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onDayNightChange(OnDayNightChange event)
	{
		_isNight = event.isNight();
		for (Creature creature : GIRAN_ZONE.getCharactersInside())
		{
			sendEffect(creature);
		}
		
		changeWaterZoneStatus();
	}
	
	private void sendEffect(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.sendPacket(_isNight ? FeatureConfig.GIRAN_NIGHT_EFFECT : FeatureConfig.GIRAN_DAY_EFFECT);
		}
	}
	
	private void changeWaterZoneStatus()
	{
		GIRAN_SQUARE_WATER_ZONE.setEnabled((_isNight ? FeatureConfig.GIRAN_NIGHT_EFFECT : FeatureConfig.GIRAN_DAY_EFFECT) == ExChangeClientEffectInfo.GIRAN_WATER);
	}
	
	public static void main(String[] args)
	{
		new GiranZoneEffect();
	}
}
