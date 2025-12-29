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
package org.l2jmobius.gameserver.model;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;

/**
 * @author Index
 */
public class ClientSettings
{
	private final Player _player;
	private boolean _announceDisabled;
	private boolean _partyRequestRestrictedFromOthers;
	private boolean _partyRequestRestrictedFromClan;
	private boolean _partyRequestRestrictedFromFriends;
	private boolean _friendRequestRestrictedFromOthers;
	private boolean _friendRequestRestrictedFromClan;
	private int _partyContributionType;
	
	public ClientSettings(Player player)
	{
		_player = player;
		
		final String variable = _player.getVariables().getString(PlayerVariables.CLIENT_SETTINGS, "");
		final StatSet settings = variable.isEmpty() ? new StatSet() : new StatSet(Arrays.stream(variable.split(",")).map(entry -> entry.split("=")).collect(Collectors.toMap(entry -> entry[0].replace("{", "").replace(" ", ""), entry -> entry[1].replace("}", "").replace(" ", ""))));
		_announceDisabled = settings.getBoolean("ANNOUNCE_DISABLED", false);
		_partyRequestRestrictedFromOthers = settings.getBoolean("PARTY_REQUEST_RESTRICTED_FROM_OTHERS", false);
		_partyRequestRestrictedFromClan = settings.getBoolean("PARTY_REQUEST_RESTRICTED_FROM_CLAN", false);
		_partyRequestRestrictedFromFriends = settings.getBoolean("PARTY_REQUEST_RESTRICTED_FROM_FRIENDS", false);
		_friendRequestRestrictedFromOthers = settings.getBoolean("FRIENDS_REQUEST_RESTRICTED_FROM_OTHERS", false);
		_friendRequestRestrictedFromClan = settings.getBoolean("FRIENDS_REQUEST_RESTRICTED_FROM_CLAN", false);
		_partyContributionType = settings.getInt("PARTY_CONTRIBUTION_TYPE", 0);
	}
	
	public void storeSettings()
	{
		final StatSet settings = new StatSet();
		settings.set("ANNOUNCE_DISABLED", _announceDisabled);
		settings.set("PARTY_REQUEST_RESTRICTED_FROM_OTHERS", _partyRequestRestrictedFromOthers);
		settings.set("PARTY_REQUEST_RESTRICTED_FROM_CLAN", _partyRequestRestrictedFromClan);
		settings.set("PARTY_REQUEST_RESTRICTED_FROM_FRIENDS", _partyRequestRestrictedFromFriends);
		settings.set("FRIENDS_REQUEST_RESTRICTED_FROM_OTHERS", _friendRequestRestrictedFromOthers);
		settings.set("FRIENDS_REQUEST_RESTRICTED_FROM_CLAN", _friendRequestRestrictedFromClan);
		settings.set("PARTY_CONTRIBUTION_TYPE", _partyContributionType);
		_player.getVariables().set(PlayerVariables.CLIENT_SETTINGS, settings.getSet());
	}
	
	public boolean isAnnounceDisabled()
	{
		return _announceDisabled;
	}
	
	public void setAnnounceEnabled(boolean enabled)
	{
		_announceDisabled = enabled;
		storeSettings();
	}
	
	public boolean isPartyRequestRestrictedFromOthers()
	{
		return _partyRequestRestrictedFromOthers;
	}
	
	public void setPartyRequestRestrictedFromOthers(boolean partyRequestRestrictedFromOthers)
	{
		_partyRequestRestrictedFromOthers = partyRequestRestrictedFromOthers;
	}
	
	public boolean isPartyRequestRestrictedFromClan()
	{
		return _partyRequestRestrictedFromClan;
	}
	
	public void setPartyRequestRestrictedFromClan(boolean partyRequestRestrictedFromClan)
	{
		_partyRequestRestrictedFromClan = partyRequestRestrictedFromClan;
	}
	
	public boolean isPartyRequestRestrictedFromFriends()
	{
		return _partyRequestRestrictedFromFriends;
	}
	
	public void setPartyRequestRestrictedFromFriends(boolean partyRequestRestrictedFromFriends)
	{
		_partyRequestRestrictedFromFriends = partyRequestRestrictedFromFriends;
	}
	
	public boolean isFriendRequestRestrictedFromOthers()
	{
		return _friendRequestRestrictedFromOthers;
	}
	
	public void setFriendRequestRestrictedFromOthers(boolean friendRequestRestrictedFromOthers)
	{
		_friendRequestRestrictedFromOthers = friendRequestRestrictedFromOthers;
	}
	
	public boolean isFriendRequestRestrictedFromClan()
	{
		return _friendRequestRestrictedFromClan;
	}
	
	public void setFriendRequestRestrictionFromClan(boolean friendRequestRestrictedFromClan)
	{
		_friendRequestRestrictedFromClan = friendRequestRestrictedFromClan;
	}
	
	public int getPartyContributionType()
	{
		return _partyContributionType;
	}
	
	public void setPartyContributionType(int partyContributionType)
	{
		_partyContributionType = partyContributionType;
		storeSettings();
	}
}
