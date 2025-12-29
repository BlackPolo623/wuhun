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
package org.l2jmobius.gameserver.network.clientpackets.captcha;

import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.actor.request.CaptchaRequest;
import org.l2jmobius.gameserver.model.captcha.Captcha;
import org.l2jmobius.gameserver.model.captcha.CaptchaGenerator;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.captcha.ReceiveBotCaptchaImage;
import org.l2jmobius.gameserver.network.serverpackets.captcha.ReceiveBotCaptchaResult;

public class RequestCaptchaAnswer extends ClientPacket
{
	private int _answer;
	
	@Override
	protected void readImpl()
	{
		readLong(); // captchaId not needed since we store the information on CaptchaRequest
		_answer = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		CaptchaRequest request = player.getRequest(CaptchaRequest.class);
		if (request != null)
		{
			if (_answer == request.getCaptcha().getCode())
			{
				player.sendPacket(ReceiveBotCaptchaResult.SUCCESS);
				player.sendPacket(SystemMessageId.IDENTIFICATION_COMPLETED_HAVE_A_GOOD_TIME_WITH_LINEAGE_II_THANK_YOU);
				request.cancelTimeout();
				player.removeRequest(CaptchaRequest.class);
			}
			else
			{
				onWrongCode(player, request);
			}
		}
		else
		{
			final Captcha captcha = CaptchaGenerator.getInstance().next();
			request = new CaptchaRequest(player, captcha);
			player.addRequest(request);
			player.sendPacket(new ReceiveBotCaptchaImage(captcha, request.getRemainingTime()));
		}
	}
	
	private void onWrongCode(Player player, CaptchaRequest request)
	{
		if (request.isLimitReached())
		{
			request.cancelTimeout();
			BotReportTable.getInstance().punishBotDueUnsolvedCaptcha(player);
		}
		else
		{
			final Captcha captcha = CaptchaGenerator.getInstance().next();
			request.newRequest(captcha);
			player.sendPacket(new ReceiveBotCaptchaImage(captcha, request.getRemainingTime()));
			final SystemMessage msg = new SystemMessage(SystemMessageId.WRONG_AUTHENTICATION_CODE_IF_YOU_ENTER_THE_WRONG_CODE_S1_TIME_S_THE_SYSTEM_WILL_QUALIFY_YOU_AS_A_PROHIBITED_SOFTWARE_USER_AND_CHARGE_A_PENALTY_ATTEMPTS_LEFT_S2);
			msg.addInt(request.maxAttemps());
			msg.addInt(request.remainingAttemps());
			player.sendPacket(msg);
		}
	}
}
