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
package org.l2jmobius.gameserver.network.clientpackets.stats;

import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author Mobius
 */
public class ExSetStatusBonus extends ClientPacket
{
	private int _str;
	private int _dex;
	private int _con;
	private int _int;
	private int _wit;
	private int _men;
	
	@Override
	protected void readImpl()
	{
		readShort(); // unk
		readShort(); // totalBonus
		_str = readShort();
		_dex = readShort();
		_con = readShort();
		_int = readShort();
		_wit = readShort();
		_men = readShort();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if ((_str < 0) || (_dex < 0) || (_con < 0) || (_int < 0) || (_wit < 0) || (_men < 0))
		{
			return;
		}
		
		final int usedPoints = player.getVariables().getInt(PlayerVariables.STAT_POINTS, 0);
		final int effectBonus = (int) player.getStat().getValue(Stat.ELIXIR_USAGE_LIMIT, 0);
		final int elixirsAvailable = player.getVariables().getInt(PlayerVariables.ELIXIRS_AVAILABLE, 0) + effectBonus;
		final int currentPoints = _str + _dex + _con + _int + _wit + _men;
		final int possiblePoints = player.getLevel() < 76 ? 0 : ((player.getLevel() - 75) + elixirsAvailable) - usedPoints;
		if ((possiblePoints <= 0) || (currentPoints > possiblePoints))
		{
			return;
		}
		
		if (_str > 0)
		{
			player.getVariables().set(PlayerVariables.STAT_STR, player.getVariables().getInt(PlayerVariables.STAT_STR, 0) + _str);
		}
		
		if (_dex > 0)
		{
			player.getVariables().set(PlayerVariables.STAT_DEX, player.getVariables().getInt(PlayerVariables.STAT_DEX, 0) + _dex);
		}
		
		if (_con > 0)
		{
			player.getVariables().set(PlayerVariables.STAT_CON, player.getVariables().getInt(PlayerVariables.STAT_CON, 0) + _con);
		}
		
		if (_int > 0)
		{
			player.getVariables().set(PlayerVariables.STAT_INT, player.getVariables().getInt(PlayerVariables.STAT_INT, 0) + _int);
		}
		
		if (_wit > 0)
		{
			player.getVariables().set(PlayerVariables.STAT_WIT, player.getVariables().getInt(PlayerVariables.STAT_WIT, 0) + _wit);
		}
		
		if (_men > 0)
		{
			player.getVariables().set(PlayerVariables.STAT_MEN, player.getVariables().getInt(PlayerVariables.STAT_MEN, 0) + _men);
		}
		
		player.getStat().recalculateStats(true);
		
		// Calculate stat increase skills.
		player.calculateStatIncreaseSkills();
		player.updateUserInfo();
	}
}
