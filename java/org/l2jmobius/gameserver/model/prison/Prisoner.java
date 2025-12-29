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
package org.l2jmobius.gameserver.model.prison;

import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PrisonConfig;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.model.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.serverpackets.prison.ExPrisonUserDonation;

/**
 * @author Liamxroy
 */
public class Prisoner
{
	private long _sentenceTime = 0L;
	private long _timeSpent = 0L;
	private ScheduledFuture<?> sentenceTask = null;
	private int _charId = 0;
	private ItemHolder _bailHolder = null;
	private ItemHolder _donationBailHolder = null;
	private int _currentBail = 0;
	private int _zoneId = 0;
	
	public Prisoner()
	{
	}
	
	public Prisoner(int charId, int zoneId, long sentenceTime)
	{
		_charId = charId;
		_zoneId = zoneId;
		_sentenceTime = sentenceTime;
		_timeSpent = 0L;
		loadBailHolder(zoneId);
		_currentBail = 0;
	}
	
	public Prisoner(int charId, int zoneId, long sentenceTime, long timeSpent, int bailAmount)
	{
		_charId = charId;
		_zoneId = zoneId;
		_sentenceTime = sentenceTime;
		_timeSpent = timeSpent;
		_currentBail = 0;
		loadBailHolder(zoneId);
	}
	
	public void startSentenceTimer(Player player)
	{
		sentenceTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			_timeSpent += 60000;
			if ((player != null) && player.isOnline())
			{
				World.getInstance().getPlayer(_charId).sendMessage("Time left in Underground Labyrinth: " + ((_sentenceTime - _timeSpent) / 60000) + " minutes.");
			}
			
			if (_timeSpent >= _sentenceTime)
			{
				if ((player != null) && player.isOnline())
				{
					World.getInstance().getPlayer(_charId).sendMessage("Your time in Underground Labyrinth is over, next time be careful!");
				}
				
				processFreedom(true);
				sentenceTask.cancel(false);
			}
		}, 60000, 60000);
	}
	
	public void stopSentenceTimer()
	{
		sentenceTask.cancel(true);
		sentenceTask = null;
	}
	
	public void endSentence()
	{
		if (sentenceTask != null)
		{
			sentenceTask.cancel(true);
		}
	}
	
	public long getSentenceTime()
	{
		return _sentenceTime;
	}
	
	public int getZoneId()
	{
		return _zoneId;
	}
	
	public long getTimeSpent()
	{
		return _timeSpent;
	}
	
	public long getTimeLeft()
	{
		return (_sentenceTime - _timeSpent) / 1000;
	}
	
	private void loadBailHolder(int id)
	{
		switch (id)
		{
			case 1:
			{
				setBailHolder(PrisonConfig.BAIL_ZONE_1);
				setDonationBailHolder(PrisonConfig.DONATION_BAIL_ZONE_1);
				break;
			}
			case 2:
			{
				setBailHolder(PrisonConfig.BAIL_ZONE_2);
				setDonationBailHolder(PrisonConfig.DONATION_BAIL_ZONE_2);
				break;
			}
			default:
			{
				setBailHolder(PrisonConfig.BAIL_ZONE_1);
				setDonationBailHolder(new ItemHolder(57, 2000000000));
				break;
			}
		}
	}
	
	public void requestFreedomByDonation(Player player)
	{
		if (!player.destroyItemByItemId(ItemProcessType.FEE, _donationBailHolder.getId(), _donationBailHolder.getCount(), player, true))
		{
			player.sendPacket(new ExPrisonUserDonation(false));
		}
		else
		{
			player.getPrisonerInfo().processFreedom(false);
			player.sendPacket(new ExPrisonUserDonation(true));
		}
	}
	
	public void processFreedom(boolean fromTimer)
	{
		Player player = World.getInstance().getPlayer(_charId);
		if (player != null)
		{
			_sentenceTime = 0L;
			_timeSpent = 0L;
			player.setReputation(PrisonManager.getRepPointsReceived(_zoneId));
			player.teleToLocation(PrisonManager.getReleaseLoc(_zoneId), 250);
			_zoneId = 0;
			setBailHolder(null);
			PrisonManager.PRISONERS.remove(_charId);
			
			if (!fromTimer && (sentenceTask != null))
			{
				sentenceTask.cancel(true);
				sentenceTask = null;
			}
		}
	}
	
	public int getCurrentBail()
	{
		return _currentBail;
	}
	
	public void setCurrentBail(int currentBail)
	{
		_currentBail = currentBail;
	}
	
	public void increaseCurrentBail(int amount)
	{
		_currentBail += amount;
		
		if ((_currentBail >= _bailHolder.getCount()) && (_timeSpent >= _sentenceTime))
		{
			processFreedom(false);
		}
	}
	
	public ItemHolder getBailHolder()
	{
		return _bailHolder;
	}
	
	public void setBailHolder(ItemHolder bailHolder)
	{
		_bailHolder = bailHolder;
	}
	
	public ItemHolder getDonationBailHolder()
	{
		return _donationBailHolder;
	}
	
	public void setDonationBailHolder(ItemHolder donationBailHolder)
	{
		_donationBailHolder = donationBailHolder;
	}
}
