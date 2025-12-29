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
package ai.areas.FrostCastleZone;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.util.Broadcast;

/**
 * @author Serenitty
 */
public class FrostLordAvailability
{
	private static final Logger LOGGER = Logger.getLogger(FrostLordAvailability.class.getName());

	// 開放日：週二、三、四、五、六 晚上6點開放
	private static final int[] ACTIVATION_DAYS =
			{
					Calendar.TUESDAY,
					Calendar.WEDNESDAY,
					Calendar.THURSDAY,
					Calendar.FRIDAY,
					Calendar.SATURDAY
			};

	// 關閉日：週三、四、五、六、日 下午3點關閉（開放日的隔日）
	private static final int[] DEACTIVATION_DAYS =
			{
					Calendar.WEDNESDAY,
					Calendar.THURSDAY,
					Calendar.FRIDAY,
					Calendar.SATURDAY,
					Calendar.SUNDAY
			};

	private static final int[] ACTIVATION_TIME =
			{
					18,  // 晚上6點開放
					0
			};

	private static final int[] DEACTIVATION_TIME =
			{
					15,  // 下午3點關閉
					0
			};

	private static final long ONE_DAY_IN_MILLIS = TimeUnit.DAYS.toMillis(1);

	// 繁體中文公告訊息
	private static final String ACTIVATION_MESSAGE = "【冰凍君主城堡】副本已開放!勇士們,準備迎接挑戰吧!";
	private static final String DEACTIVATION_MESSAGE = "【冰凍君主城堡】副本已關閉,期待下次再會!";

	public FrostLordAvailability()
	{
		frostLordCastleActivation(ACTIVATION_DAYS, ACTIVATION_TIME);
		frostLordCastleDeactivation(DEACTIVATION_DAYS, DEACTIVATION_TIME);
	}

	private void frostLordCastleActivation(int[] daysOfWeek, int[] time)
	{
		for (int dayOfWeek : daysOfWeek)
		{
			final long initialDelay = getNextDelay(dayOfWeek, time[0], time[1]);
			final long period = ONE_DAY_IN_MILLIS * 7;
			ThreadPool.scheduleAtFixedRate(this::enableFrostLord, initialDelay, period);
		}
	}

	private void frostLordCastleDeactivation(int[] daysOfWeek, int[] time)
	{
		for (int dayOfWeek : daysOfWeek)
		{
			final long initialDelay = getNextDelay(dayOfWeek, time[0], time[1]);
			final long period = ONE_DAY_IN_MILLIS * 7;
			ThreadPool.scheduleAtFixedRate(this::disableFrostLord, initialDelay, period);
		}
	}

	private long getNextDelay(int dayOfWeek, int hour, int minute)
	{
		final Calendar now = Calendar.getInstance();
		final int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
		final int daysUntilNextActivation = ((dayOfWeek + 7) - currentDayOfWeek) % 7;

		final Calendar activationTime = Calendar.getInstance();
		activationTime.add(Calendar.DAY_OF_YEAR, daysUntilNextActivation);
		activationTime.set(Calendar.HOUR_OF_DAY, hour);
		activationTime.set(Calendar.MINUTE, minute);
		activationTime.set(Calendar.SECOND, 0);

		if (activationTime.getTimeInMillis() < now.getTimeInMillis())
		{
			activationTime.add(Calendar.DAY_OF_YEAR, 7);
		}

		return activationTime.getTimeInMillis() - now.getTimeInMillis();
	}

	private void enableFrostLord()
	{
		GlobalVariablesManager.getInstance().set("AvailableFrostLord", true);
		LOGGER.info("冰凍君主城堡已開放。");
		Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(ACTIVATION_MESSAGE, ExShowScreenMessage.TOP_CENTER, 10000));
	}

	private void disableFrostLord()
	{
		GlobalVariablesManager.getInstance().set("AvailableFrostLord", false);
		LOGGER.info("冰凍君主城堡已關閉。");
		Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(DEACTIVATION_MESSAGE, ExShowScreenMessage.TOP_CENTER, 10000));
	}

	public static void main(String[] args)
	{
		new FrostLordAvailability();
	}
}