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
package org.l2jmobius.gameserver.model.actor.request;

import static java.lang.System.currentTimeMillis;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.l2jmobius.gameserver.config.custom.CaptchaConfig;
import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.captcha.Captcha;

public class CaptchaRequest extends AbstractRequest
{
	private static final byte MAX_ATTEMPTS = 3;
	
	private Captcha _captcha;
	private byte _count = 0;
	private final Instant _timeout;
	
	public CaptchaRequest(Player activeChar, Captcha captcha)
	{
		super(activeChar);
		_captcha = captcha;
		final long currentTime = currentTimeMillis();
		setTimestamp(currentTime);
		scheduleTimeout(Duration.ofMinutes(CaptchaConfig.VALIDATION_TIME).toMillis());
		_timeout = Instant.ofEpochMilli(currentTime).plus(CaptchaConfig.VALIDATION_TIME, ChronoUnit.MINUTES);
	}
	
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
	
	public int getRemainingTime()
	{
		return (int) (_timeout.minusMillis(currentTimeMillis()).getEpochSecond());
	}
	
	public void refresh(Captcha captcha)
	{
		_captcha = captcha;
	}
	
	public void newRequest(Captcha captcha)
	{
		_count++;
		_captcha = captcha;
	}
	
	public boolean isLimitReached()
	{
		return _count >= (MAX_ATTEMPTS - 1);
	}
	
	public Captcha getCaptcha()
	{
		return _captcha;
	}
	
	@Override
	public void onTimeout()
	{
		BotReportTable.getInstance().punishBotDueUnsolvedCaptcha(getPlayer());
	}
	
	public int maxAttemps()
	{
		return MAX_ATTEMPTS;
	}
	
	public int remainingAttemps()
	{
		return Math.max(MAX_ATTEMPTS - _count, 0);
	}
}
