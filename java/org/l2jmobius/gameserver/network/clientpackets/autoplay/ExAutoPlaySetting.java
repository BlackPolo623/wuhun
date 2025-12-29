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
package org.l2jmobius.gameserver.network.clientpackets.autoplay;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.model.actor.Player;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.autoplay.ExAutoPlaySettingSend;
import org.l2jmobius.gameserver.taskmanagers.AutoPlayTaskManager;

/**
 * @author Mobius
 */
public class ExAutoPlaySetting extends ClientPacket
{
	private int _options;
	private boolean _active;
	private boolean _pickUp;
	private int _nextTargetMode;
	private boolean _shortRange;
	private int _potionPercent;
	private int _petPotionPercent;
	private boolean _respectfulHunting;
	private int _macroIndex;
	
	@Override
	protected void readImpl()
	{
		_options = readShort();
		_active = readByte() == 1;
		_pickUp = readByte() == 1;
		_nextTargetMode = readShort();
		_shortRange = readByte() == 1;
		_potionPercent = readInt();
		_petPotionPercent = readInt(); // 272
		_respectfulHunting = readByte() == 1;
		_macroIndex = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		// Skip first run. Fixes restored settings been overwritten.
		// Client sends a disabled ExAutoPlaySetting upon player login.
		if (player.hasResumedAutoPlay())
		{
			player.setResumedAutoPlay(false);
			return;
		}
		
		player.sendPacket(new ExAutoPlaySettingSend(_options, _active, _pickUp, _nextTargetMode, _shortRange, _potionPercent, _respectfulHunting, _petPotionPercent));
		player.getAutoPlaySettings().setAutoPotionPercent(_potionPercent);
		
		if (!GeneralConfig.ENABLE_AUTO_PLAY)
		{
			return;
		}
		
		final List<Integer> settings = new ArrayList<>(8);
		settings.add(0, _options);
		settings.add(1, _active ? 1 : 0);
		settings.add(2, _pickUp ? 1 : 0);
		settings.add(3, _nextTargetMode);
		settings.add(4, _shortRange ? 1 : 0);
		settings.add(5, _potionPercent);
		settings.add(6, _respectfulHunting ? 1 : 0);
		settings.add(7, _petPotionPercent);
		settings.add(8, _macroIndex); // Not used.
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_SETTINGS, settings);
		
		player.getAutoPlaySettings().setOptions(_options);
		player.getAutoPlaySettings().setPickup(_pickUp);
		player.getAutoPlaySettings().setNextTargetMode(_nextTargetMode);
		player.getAutoPlaySettings().setShortRange(_shortRange);
		player.getAutoPlaySettings().setRespectfulHunting(_respectfulHunting);
		player.getAutoPlaySettings().setAutoPetPotionPercent(_petPotionPercent);
		
		if (_active)
		{
			AutoPlayTaskManager.getInstance().startAutoPlay(player);
		}
		else
		{
			AutoPlayTaskManager.getInstance().stopAutoPlay(player);
		}
	}
}
