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
package org.l2jmobius.gameserver.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.CaptchaConfig;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.instance.Monster;
import org.l2jmobius.gameserver.model.actor.request.CaptchaRequest;
import org.l2jmobius.gameserver.model.captcha.Captcha;
import org.l2jmobius.gameserver.model.captcha.CaptchaGenerator;
import org.l2jmobius.gameserver.model.events.Containers;
import org.l2jmobius.gameserver.model.events.EventType;
import org.l2jmobius.gameserver.model.events.holders.actor.creature.OnCreatureKilled;
import org.l2jmobius.gameserver.model.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.captcha.ReceiveBotCaptchaImage;

/**
 * @author Mobius
 */
public class CaptchaManager
{
	private static final Logger LOGGER = Logger.getLogger(CaptchaManager.class.getName());
	
	private static final Map<Integer, Integer> MONSTER_COUNTER = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_KILL_TIME = new ConcurrentHashMap<>();
	
	private final Consumer<OnCreatureKilled> _onCreatureKilled = event -> updateCounter(event.getAttacker(), event.getTarget());
	
	protected CaptchaManager()
	{
		if (CaptchaConfig.ENABLE_CAPTCHA)
		{
			Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_CREATURE_KILLED, _onCreatureKilled, this));
			LOGGER.info(getClass().getSimpleName() + ": Enabled.");
		}
	}
	
	public void updateCounter(Creature player, Creature monster)
	{
		if (!CaptchaConfig.ENABLE_CAPTCHA)
		{
			return;
		}
		
		if (!(player instanceof Player) || !(monster instanceof Monster))
		{
			return;
		}
		
		// Check if auto-play is enabled and player is auto-playing.
		final Player killer = player.asPlayer();
		if (GeneralConfig.ENABLE_AUTO_PLAY && killer.isAutoPlaying())
		{
			return; // Don't count kills when auto-play is enabled.
		}
		
		if (CaptchaConfig.KILL_COUNTER_RESET)
		{
			final long currentTime = System.currentTimeMillis();
			final long previousKillTime = LAST_KILL_TIME.getOrDefault(killer.getObjectId(), currentTime);
			if ((currentTime - previousKillTime) > CaptchaConfig.KILL_COUNTER_RESET_TIME)
			{
				MONSTER_COUNTER.put(killer.getObjectId(), 0);
			}
			
			LAST_KILL_TIME.put(killer.getObjectId(), currentTime);
		}
		
		int count = 1;
		if (MONSTER_COUNTER.get(killer.getObjectId()) != null)
		{
			count = MONSTER_COUNTER.get(killer.getObjectId()) + 1;
		}
		
		final int next = Rnd.get(CaptchaConfig.KILL_COUNTER_RANDOMIZATION);
		if ((CaptchaConfig.KILL_COUNTER + next) < count)
		{
			MONSTER_COUNTER.remove(killer.getObjectId());
			
			final Captcha captcha = CaptchaGenerator.getInstance().next();
			if (!killer.hasRequest(CaptchaRequest.class))
			{
				final CaptchaRequest request = new CaptchaRequest(killer, captcha);
				killer.addRequest(request);
				killer.sendPacket(new ReceiveBotCaptchaImage(captcha, request.getRemainingTime()));
				killer.sendPacket(SystemMessageId.PLEASE_ENTER_THE_AUTHENTICATION_CODE_IN_TIME_TO_CONTINUE_PLAYING);
			}
		}
		else
		{
			MONSTER_COUNTER.put(killer.getObjectId(), count);
		}
	}
	
	public static final CaptchaManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CaptchaManager INSTANCE = new CaptchaManager();
	}
}
