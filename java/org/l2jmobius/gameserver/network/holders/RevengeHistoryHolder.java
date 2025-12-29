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
package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.gameserver.model.StatSet;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.network.enums.RevengeType;

/**
 * @author Mobius
 */
public class RevengeHistoryHolder
{
	private final String _killerName;
	private final String _killerClanName;
	private final int _killerLevel;
	private final int _killerRaceId;
	private final int _killerClassId;
	private final long _killTime;
	private final String _victimName;
	private final String _victimClanName;
	private final int _victimLevel;
	private final int _victimRaceId;
	private final int _victimClassId;
	private RevengeType _type;
	private boolean _wasShared;
	private long _shareTime;
	private int _showLocationRemaining;
	private int _teleportRemaining;
	private int _sharedTeleportRemaining;
	
	public RevengeHistoryHolder(Player killer, Player victim, RevengeType type)
	{
		_type = type;
		_wasShared = false;
		_killerName = killer.getName();
		_killerClanName = killer.getClan() == null ? "" : killer.getClan().getName();
		_killerLevel = killer.getLevel();
		_killerRaceId = killer.getRace().ordinal();
		_killerClassId = killer.getPlayerClass().getId();
		_killTime = System.currentTimeMillis();
		_shareTime = 0;
		_showLocationRemaining = 5;
		_teleportRemaining = 5;
		_sharedTeleportRemaining = 1;
		_victimName = victim.getName();
		_victimClanName = victim.getClan() == null ? "" : victim.getClan().getName();
		_victimLevel = victim.getLevel();
		_victimRaceId = victim.getRace().ordinal();
		_victimClassId = victim.getPlayerClass().getId();
	}
	
	public RevengeHistoryHolder(Player killer, Player victim, RevengeType type, int sharedTeleportRemaining, long killTime, long shareTime)
	{
		_type = type;
		_wasShared = true;
		_killerName = killer.getName();
		_killerClanName = killer.getClan() == null ? "" : killer.getClan().getName();
		_killerLevel = killer.getLevel();
		_killerRaceId = killer.getRace().ordinal();
		_killerClassId = killer.getPlayerClass().getId();
		_killTime = killTime;
		_shareTime = shareTime;
		_showLocationRemaining = 0;
		_teleportRemaining = 0;
		_sharedTeleportRemaining = sharedTeleportRemaining;
		_victimName = victim.getName();
		_victimClanName = victim.getClan() == null ? "" : victim.getClan().getName();
		_victimLevel = victim.getLevel();
		_victimRaceId = victim.getRace().ordinal();
		_victimClassId = victim.getPlayerClass().getId();
	}
	
	public RevengeHistoryHolder(StatSet killer, StatSet victim, RevengeType type, boolean wasShared, int showLocationRemaining, int teleportRemaining, int sharedTeleportRemaining, long killTime, long shareTime)
	{
		_type = type;
		_wasShared = wasShared;
		_killerName = killer.getString("name");
		_killerClanName = killer.getString("clan");
		_killerLevel = killer.getInt("level");
		_killerRaceId = killer.getInt("race");
		_killerClassId = killer.getInt("class");
		_killTime = killTime;
		_shareTime = shareTime;
		_showLocationRemaining = showLocationRemaining;
		_teleportRemaining = teleportRemaining;
		_sharedTeleportRemaining = sharedTeleportRemaining;
		_victimName = victim.getString("name");
		_victimClanName = victim.getString("clan");
		_victimLevel = victim.getInt("level");
		_victimRaceId = victim.getInt("race");
		_victimClassId = victim.getInt("class");
	}
	
	public RevengeType getType()
	{
		return _type;
	}
	
	public void setType(RevengeType type)
	{
		_type = type;
	}
	
	public boolean wasShared()
	{
		return _wasShared;
	}
	
	public void setShared(boolean wasShared)
	{
		_wasShared = wasShared;
	}
	
	public String getKillerName()
	{
		return _killerName;
	}
	
	public String getKillerClanName()
	{
		return _killerClanName;
	}
	
	public int getKillerLevel()
	{
		return _killerLevel;
	}
	
	public int getKillerRaceId()
	{
		return _killerRaceId;
	}
	
	public int getKillerClassId()
	{
		return _killerClassId;
	}
	
	public long getKillTime()
	{
		return _killTime;
	}
	
	public long getShareTime()
	{
		return _shareTime;
	}
	
	public void setShareTime(long shareTime)
	{
		_shareTime = shareTime;
	}
	
	public int getShowLocationRemaining()
	{
		return _showLocationRemaining;
	}
	
	public void setShowLocationRemaining(int count)
	{
		_showLocationRemaining = count;
	}
	
	public int getTeleportRemaining()
	{
		return _teleportRemaining;
	}
	
	public void setTeleportRemaining(int count)
	{
		_teleportRemaining = count;
	}
	
	public int getSharedTeleportRemaining()
	{
		return _sharedTeleportRemaining;
	}
	
	public void setSharedTeleportRemaining(int count)
	{
		_sharedTeleportRemaining = count;
	}
	
	public String getVictimName()
	{
		return _victimName;
	}
	
	public String getVictimClanName()
	{
		return _victimClanName;
	}
	
	public int getVictimLevel()
	{
		return _victimLevel;
	}
	
	public int getVictimRaceId()
	{
		return _victimRaceId;
	}
	
	public int getVictimClassId()
	{
		return _victimClassId;
	}
}
